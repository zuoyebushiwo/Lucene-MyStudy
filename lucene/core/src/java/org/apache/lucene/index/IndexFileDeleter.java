package org.apache.lucene.index;

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

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NoSuchDirectoryException;
import org.apache.lucene.util.CollectionUtil;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.InfoStream;

/*
 * This class keeps track of each SegmentInfos instance that
 * is still "live", either because it corresponds to a
 * segments_N file in the Directory (a "commit", i.e. a
 * committed SegmentInfos) or because it's an in-memory
 * SegmentInfos that a writer is actively updating but has
 * not yet committed.  This class uses simple reference
 * counting to map the live SegmentInfos instances to
 * individual files in the Directory.
 *
 * The same directory file may be referenced by more than
 * one IndexCommit, i.e. more than one SegmentInfos.
 * Therefore we count how many commits reference each file.
 * When all the commits referencing a certain file have been
 * deleted, the refcount for that file becomes zero, and the
 * file is deleted.
 *
 * A separate deletion policy interface
 * (IndexDeletionPolicy) is consulted on creation (onInit)
 * and once per commit (onCommit), to decide when a commit
 * should be removed.
 *
 * It is the business of the IndexDeletionPolicy to choose
 * when to delete commit points.  The actual mechanics of
 * file deletion, retrying, etc, derived from the deletion
 * of commit points is the business of the IndexFileDeleter.
 *
 * The current default deletion policy is {@link
 * KeepOnlyLastCommitDeletionPolicy}, which removes all
 * prior commits when a new commit has completed.  This
 * matches the behavior before 2.2.
 *
 * Note that you must hold the write.lock before
 * instantiating this class.  It opens segments_N file(s)
 * directly with no retry logic.
 */

final class IndexFileDeleter implements Closeable {
	
	/* Files that we tried to delete but failed (likely
	   * because they are open and we are running on Windows),
	   * so we will retry them again later: */
	private Set<String> deleTable;
	
	/* Reference count for all files in the index.
	   * Counts how many existing commits reference a file.
	   **/
	private Map<String, RefCount> refCounts = new HashMap<String, IndexFileDeleter.RefCount>();
	
	/* Holds all commits (segments_N) currently in the index.
	   * This will have just 1 commit if you are using the
	   * default delete policy (KeepOnlyLastCommitDeletionPolicy).
	   * Other policies may leave commit points live for longer
	   * in which case this list would be longer than 1: */
	private List<CommitPoint> commits = new ArrayList<>();
	
	/* Holds files we had incref'd from the previous
	   * non-commit checkpoint: */
	  private final List<String> lastFiles = new ArrayList<>();

	  /* Commits that the IndexDeletionPolicy have decided to delete: */
	  private List<CommitPoint> commitsToDelete = new ArrayList<>();
	  
	  private final InfoStream infoStream;
	  private Directory directory;
	  private IndexDeletionPolicy policy;

	  final boolean startingCommitDeleted;
	  private SegmentInfos lastSegmentInfos;
	  
	  /** Change to true to see details of reference counts when
	   *  infoStream is enabled */
	  public static boolean VERBOSE_REF_COUNTS = false;
	  
	  private final IndexWriter writer;
	
	/**
	   * Tracks the reference count for a single index file:
	   */
	final private static class RefCount {
		
		// fileName used only for better assert error messages
		final String fileName;
		boolean initDone;
		public RefCount(String fileName) {
			this.fileName = fileName;
		}
		
		int count;
		
		public int IncRef() {
		      if (!initDone) {
		        initDone = true;
		      } else {
		        assert count > 0: Thread.currentThread().getName() + ": RefCount is 0 pre-increment for file \"" + fileName + "\"";
		      }
		      return ++count;
		    }

		    public int DecRef() {
		      assert count > 0: Thread.currentThread().getName() + ": RefCount is 0 pre-decrement for file \"" + fileName + "\"";
		      return --count;
		    }
		
	}
	
	/**
	   * Holds details for each commit point.  This class is
	   * also passed to the deletion policy.  Note: this class
	   * has a natural ordering that is inconsistent with
	   * equals.
	   */
	final private static class CommitPoint extends IndexCommit {
		
		Collection<String> files;
		String segmentsFileName;
		boolean deleted;
		Directory directory;
		Collection<CommitPoint> commitsToDelete;
		long generation;
		final Map<String, String> userData;
		private final int segmentCount;
		
		public CommitPoint(Collection<CommitPoint> commitsToDelete, Directory directory, SegmentInfos segmentInfos) throws IOException {
			this.directory = directory;
		      this.commitsToDelete = commitsToDelete;
		      userData = segmentInfos.getUserData();
		      segmentsFileName = segmentInfos.getSegmentsFileName();
		      generation = segmentInfos.getGeneration();
		      files = Collections.unmodifiableCollection(segmentInfos.files(directory, true));
		      segmentCount = segmentInfos.size();
		}
		

	    @Override
	    public String toString() {
	      return "IndexFileDeleter.CommitPoint(" + segmentsFileName + ")";
	    }

	    @Override
	    public int getSegmentCount() {
	      return segmentCount;
	    }

	    @Override
	    public String getSegmentsFileName() {
	      return segmentsFileName;
	    }

	    @Override
	    public Collection<String> getFileNames() {
	      return files;
	    }

	    @Override
	    public Directory getDirectory() {
	      return directory;
	    }

	    @Override
	    public long getGeneration() {
	      return generation;
	    }

	    @Override
	    public Map<String,String> getUserData() {
	      return userData;
	    }

	    /**
	     * Called only be the deletion policy, to remove this
	     * commit point from the index.
	     */
	    @Override
	    public void delete() {
	      if (!deleted) {
	        deleted = true;
	        commitsToDelete.add(this);
	      }
	    }

	    @Override
	    public boolean isDeleted() {
	      return deleted;
	    }
	  }
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
