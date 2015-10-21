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

import org.apache.lucene.util.PriorityQueue;

import java.io.IOException;

/** Represents hits returned by {@link
 * IndexSearcher#search(Query,Filter,int)} and {@link
 * IndexSearcher#search(Query,int)}. */
public class TopDocs {
	
	/** The total number of hits for the query. */
	  public int totalHits;

	  /** The top hits for the query. */
	  public ScoreDoc[] scoreDocs;

	  /** Stores the maximum score value encountered, needed for normalizing. */
	  private float maxScore;
	  
	  /**
	   * Returns the maximum score value encountered. Note that in case
	   * scores are not tracked, this returns {@link Float#NaN}.
	   */
	  public float getMaxScore() {
	    return maxScore;
	  }
	  
	  /** Sets the maximum score value encountered. */
	  public void setMaxScore(float maxScore) {
	    this.maxScore = maxScore;
	  }

	  /** Constructs a TopDocs with a default maxScore=Float.NaN. */
	  TopDocs(int totalHits, ScoreDoc[] scoreDocs) {
	    this(totalHits, scoreDocs, Float.NaN);
	  }
	  
	  public TopDocs(int totalHits, ScoreDoc[] scoreDocs, float maxScore) {
		    this.totalHits = totalHits;
		    this.scoreDocs = scoreDocs;
		    this.maxScore = maxScore;
		  }
	  
	// Refers to one hit:
	  private static class ShardRef {
	    // Which shard (index into shardHits[]):
	    final int shardIndex;

	    // Which hit within the shard:
	    int hitIndex;

	    public ShardRef(int shardIndex) {
	      this.shardIndex = shardIndex;
	    }

	    @Override
	    public String toString() {
	      return "ShardRef(shardIndex=" + shardIndex + " hitIndex=" + hitIndex + ")";
	    }
	  };
	  
	// Specialized MergeSortQueue that just merges by
	  // relevance score, descending:
	  private static class ScoreMergeSortQueue extends PriorityQueue<ShardRef> {
		  final ScoreDoc[][] shardHits;
		  
		  public ScoreMergeSortQueue(TopDocs[] shardHits) {
			  super(shardHits.length);
			  this.shardHits = new ScoreDoc[shardHits.length][];
		      for(int shardIDX=0;shardIDX<shardHits.length;shardIDX++) {
		        this.shardHits[shardIDX] = shardHits[shardIDX].scoreDocs;
		      }
		  }
		  
		// Returns true if first is < second
		    @Override
		    public boolean lessThan(ShardRef first, ShardRef second) {
		      assert first != second;
		      final float firstScore = shardHits[first.shardIndex][first.hitIndex].score;
		      final float secondScore = shardHits[second.shardIndex][second.hitIndex].score;

		      if (firstScore < secondScore) {
		        return false;
		      } else if (firstScore > secondScore) {
		        return true;
		      } else {
		        // Tie break: earlier shard wins
		        if (first.shardIndex < second.shardIndex) {
		          return true;
		        } else if (first.shardIndex > second.shardIndex) {
		          return false;
		        } else {
		          // Tie break in same shard: resolve however the
		          // shard had resolved it:
		          assert first.hitIndex != second.hitIndex;
		          return first.hitIndex < second.hitIndex;
		        }
		      }
		    }
	  }
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  

}
