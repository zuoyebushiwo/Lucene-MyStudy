package org.apache.lucene.analysis;

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

import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.util.CloseableThreadLocal;
import org.apache.lucene.util.Version;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * An Analyzer builds TokenStreams, which analyze text.  It thus represents a
 * policy for extracting index terms from text.
 * <p>
 * In order to define what analysis is done, subclasses must define their
 * {@link TokenStreamComponents TokenStreamComponents} in {@link #createComponents(String, Reader)}.
 * The components are then reused in each call to {@link #tokenStream(String, Reader)}.
 * <p>
 * Simple example:
 * <pre class="prettyprint">
 * Analyzer analyzer = new Analyzer() {
 *  {@literal @Override}
 *   protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
 *     Tokenizer source = new FooTokenizer(reader);
 *     TokenStream filter = new FooFilter(source);
 *     filter = new BarFilter(filter);
 *     return new TokenStreamComponents(source, filter);
 *   }
 * };
 * </pre>
 * For more examples, see the {@link org.apache.lucene.analysis Analysis package documentation}.
 * <p>
 * For some concrete implementations bundled with Lucene, look in the analysis modules:
 * <ul>
 *   <li><a href="{@docRoot}/../analyzers-common/overview-summary.html">Common</a>:
 *       Analyzers for indexing content in different languages and domains.
 *   <li><a href="{@docRoot}/../analyzers-icu/overview-summary.html">ICU</a>:
 *       Exposes functionality from ICU to Apache Lucene. 
 *   <li><a href="{@docRoot}/../analyzers-kuromoji/overview-summary.html">Kuromoji</a>:
 *       Morphological analyzer for Japanese text.
 *   <li><a href="{@docRoot}/../analyzers-morfologik/overview-summary.html">Morfologik</a>:
 *       Dictionary-driven lemmatization for the Polish language.
 *   <li><a href="{@docRoot}/../analyzers-phonetic/overview-summary.html">Phonetic</a>:
 *       Analysis for indexing phonetic signatures (for sounds-alike search).
 *   <li><a href="{@docRoot}/../analyzers-smartcn/overview-summary.html">Smart Chinese</a>:
 *       Analyzer for Simplified Chinese, which indexes words.
 *   <li><a href="{@docRoot}/../analyzers-stempel/overview-summary.html">Stempel</a>:
 *       Algorithmic Stemmer for the Polish Language.
 *   <li><a href="{@docRoot}/../analyzers-uima/overview-summary.html">UIMA</a>: 
 *       Analysis integration with Apache UIMA. 
 * </ul>
 */
public abstract class Analyzer implements Closeable {
	
	/**
	   * This class encapsulates the outer components of a token stream. It provides
	   * access to the source ({@link Tokenizer}) and the outer end (sink), an
	   * instance of {@link TokenFilter} which also serves as the
	   * {@link TokenStream} returned by
	   * {@link Analyzer#tokenStream(String, Reader)}.
	   */
	  public static class TokenStreamComponents {
	    /**
	     * Original source of the tokens.
	     */
	    protected final Tokenizer source;
	    /**
	     * Sink tokenstream, such as the outer tokenfilter decorating
	     * the chain. This can be the source if there are no filters.
	     */
	    protected final TokenStream sink;
	    
	    /** Internal cache only used by {@link Analyzer#tokenStream(String, String)}. */
	    transient ReusableStringReader reusableStringReader;

	    /**
	     * Creates a new {@link TokenStreamComponents} instance.
	     * 
	     * @param source
	     *          the analyzer's tokenizer
	     * @param result
	     *          the analyzer's resulting token stream
	     */
	    public TokenStreamComponents(final Tokenizer source,
	        final TokenStream result) {
	      this.source = source;
	      this.sink = result;
	    }
	    
	    /**
	     * Creates a new {@link TokenStreamComponents} instance.
	     * 
	     * @param source
	     *          the analyzer's tokenizer
	     */
	    public TokenStreamComponents(final Tokenizer source) {
	      this.source = source;
	      this.sink = source;
	    }

	    /**
	     * Resets the encapsulated components with the given reader. If the components
	     * cannot be reset, an Exception should be thrown.
	     * 
	     * @param reader
	     *          a reader to reset the source component
	     * @throws IOException
	     *           if the component's reset method throws an {@link IOException}
	     */
	    protected void setReader(final Reader reader) throws IOException {
	      source.setReader(reader);
	    }

	    /**
	     * Returns the sink {@link TokenStream}
	     * 
	     * @return the sink {@link TokenStream}
	     */
	    public TokenStream getTokenStream() {
	      return sink;
	    }

	    /**
	     * Returns the component's {@link Tokenizer}
	     *
	     * @return Component's {@link Tokenizer}
	     */
	    public Tokenizer getTokenizer() {
	      return source;
	    }
	  }

	/**
	   * Strategy defining how TokenStreamComponents are reused per call to
	   * {@link Analyzer#tokenStream(String, java.io.Reader)}.
	   */
	public static abstract class ReuseStrategy {
		
		/** Sole constructor. (For invocation by subclass constructors, typically implicit.) */
		public ReuseStrategy() {}
		
	}
	
}
