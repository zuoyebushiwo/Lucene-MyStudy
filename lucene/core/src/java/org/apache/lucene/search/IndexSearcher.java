package org.apache.lucene.search;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader; // javadocs
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.NIOFSDirectory;    // javadoc
import org.apache.lucene.util.ThreadInterruptedException;
import org.apache.lucene.index.IndexWriter; // javadocs

/** Implements search over a single IndexReader.
 *
 * <p>Applications usually need only call the inherited
 * {@link #search(Query,int)}
 * or {@link #search(Query,Filter,int)} methods. For
 * performance reasons, if your index is unchanging, you
 * should share a single IndexSearcher instance across
 * multiple searches instead of creating a new one
 * per-search.  If your index has changed and you wish to
 * see the changes reflected in searching, you should
 * use {@link DirectoryReader#openIfChanged(DirectoryReader)}
 * to obtain a new reader and
 * then create a new IndexSearcher from that.  Also, for
 * low-latency turnaround it's best to use a near-real-time
 * reader ({@link DirectoryReader#open(IndexWriter,boolean)}).
 * Once you have a new {@link IndexReader}, it's relatively
 * cheap to create a new IndexSearcher from it.
 * 
 * <a name="thread-safety"></a><p><b>NOTE</b>: <code>{@link
 * IndexSearcher}</code> instances are completely
 * thread safe, meaning multiple threads can call any of its
 * methods, concurrently.  If your application requires
 * external synchronization, you should <b>not</b>
 * synchronize on the <code>IndexSearcher</code> instance;
 * use your own (non-Lucene) objects instead.</p>
 */
public class IndexSearcher {
	final IndexReader reader;
	
	// NOTE: these members might change in incompatible ways
	// in the next release
	protected final IndexReaderContext readerContext;
	  protected final List<AtomicReaderContext> leafContexts;
	  /** used with executor - each slice holds a set of leafs executed within one thread */
	  protected final LeafSlice[] leafSlices;
	  
	// These are only used for multi-threaded search
	  private final ExecutorService executor;
	  
	// the default Similarity
	  private static final Similarity defaultSimilarity = new DefaultSimilarity();
	  
	  /**
	   * Expert: returns a default Similarity instance.
	   * In general, this method is only called to initialize searchers and writers.
	   * User code and query implementations should respect
	   * {@link IndexSearcher#getSimilarity()}.
	   * @lucene.internal
	   */
	  public static Similarity getDefaultSimilarity() {
	    return defaultSimilarity;
	  }
	  
	  /** The Similarity implementation used by this searcher. */
	  private Similarity similarity = defaultSimilarity;

	  
	  /** Creates a searcher searching the provided index. */
	  public IndexSearcher(IndexReader r) {
		  this(r, null);
	  }
	
	  /** Runs searches for each segment separately, using the
	   *  provided ExecutorService.  IndexSearcher will not
	   *  shutdown/awaitTermination this ExecutorService on
	   *  close; you must do so, eventually, on your own.  NOTE:
	   *  if you are using {@link NIOFSDirectory}, do not use
	   *  the shutdownNow method of ExecutorService as this uses
	   *  Thread.interrupt under-the-hood which can silently
	   *  close file descriptors (see <a
	   *  href="https://issues.apache.org/jira/browse/LUCENE-2239">LUCENE-2239</a>).
	   * 
	   * @lucene.experimental */
	public IndexSearcher(IndexReader r, ExecutorService executor) {
		this(r.getContext(), executor);
	}
	
	/**
	   * Creates a searcher searching the provided top-level {@link IndexReaderContext}.
	   * <p>
	   * Given a non-<code>null</code> {@link ExecutorService} this method runs
	   * searches for each segment separately, using the provided ExecutorService.
	   * IndexSearcher will not shutdown/awaitTermination this ExecutorService on
	   * close; you must do so, eventually, on your own. NOTE: if you are using
	   * {@link NIOFSDirectory}, do not use the shutdownNow method of
	   * ExecutorService as this uses Thread.interrupt under-the-hood which can
	   * silently close file descriptors (see <a
	   * href="https://issues.apache.org/jira/browse/LUCENE-2239">LUCENE-2239</a>).
	   * 
	   * @see IndexReaderContext
	   * @see IndexReader#getContext()
	   * @lucene.experimental
	   */
	public IndexSearcher(IndexReaderContext context, ExecutorService executor) {
	    assert context.isTopLevel: "IndexSearcher's ReaderContext must be topLevel for reader" + context.reader();
	    reader = context.reader();
	    this.executor = executor;
	    this.readerContext = context;
	    leafContexts = context.leaves();
	    this.leafSlices = executor == null ? null : slices(leafContexts);
	  }

	  /**
	   * Creates a searcher searching the provided top-level {@link IndexReaderContext}.
	   *
	   * @see IndexReaderContext
	   * @see IndexReader#getContext()
	   * @lucene.experimental
	   */
	  public IndexSearcher(IndexReaderContext context) {
	    this(context, null);
	  }
	  
	  /**
	   * Expert: Creates an array of leaf slices each holding a subset of the given leaves.
	   * Each {@link LeafSlice} is executed in a single thread. By default there
	   * will be one {@link LeafSlice} per leaf ({@link AtomicReaderContext}).
	   */
	  /**
	   * Expert: Creates an array of leaf slices each holding a subset of the given leaves.
	   * Each {@link LeafSlice} is executed in a single thread. By default there
	   * will be one {@link LeafSlice} per leaf ({@link AtomicReaderContext}).
	   */
	  protected LeafSlice[] slices(List<AtomicReaderContext> leaves) {
	    LeafSlice[] slices = new LeafSlice[leaves.size()];
	    for (int i = 0; i < slices.length; i++) {
	      slices[i] = new LeafSlice(leaves.get(i));
	    }
	    return slices;
	  }

	  
	  /** Return the {@link IndexReader} this searches. */
	  public IndexReader getIndexReader() {
	    return reader;
	  }

	  /** 
	   * Sugar for <code>.getIndexReader().document(docID)</code> 
	   * @see IndexReader#document(int) 
	   */
	  public Document doc(int docID) throws IOException {
		  return reader.document(docID);
	  }
	  
	  /** 
	   * Sugar for <code>.getIndexReader().document(docID, fieldVisitor)</code>
	   * @see IndexReader#document(int, StoredFieldVisitor) 
	   */
	  public void doc(int docID, StoredFieldVisitor fieldVisitor) throws IOException {
	    reader.document(docID, fieldVisitor);
	  }

	  /** 
	   * Sugar for <code>.getIndexReader().document(docID, fieldsToLoad)</code>
	   * @see IndexReader#document(int, Set) 
	   */
	  public Document doc(int docID, Set<String> fieldsToLoad) throws IOException {
	    return reader.document(docID, fieldsToLoad);
	  }
	  
	  /**
	   * @deprecated Use {@link #doc(int, Set)} instead.
	   */
	  @Deprecated
	  public final Document document(int docID, Set<String> fieldsToLoad) throws IOException {
	    return doc(docID, fieldsToLoad);
	  }

	  /** Expert: Set the Similarity implementation used by this IndexSearcher.
	   *
	   */
	  public void setSimilarity(Similarity similarity) {
	    this.similarity = similarity;
	  }

	  public Similarity getSimilarity() {
	    return similarity;
	  }
	  
	  /** @lucene.internal */
	  protected Query wrapFilter(Query query, Filter filter) {
	    return (filter == null) ? query : new FilteredQuery(query, filter);
	  }
	  




	/**
	   * A class holding a subset of the {@link IndexSearcher}s leaf contexts to be
	   * executed within a single thread.
	   * 
	   * @lucene.experimental
	   */
	public static class LeafSlice {
		final AtomicReaderContext[] leaves;

		public LeafSlice(AtomicReaderContext... leaves) {
			this.leaves = leaves;
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
