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

import java.io.IOException;

import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.RollingBuffer;
import org.apache.lucene.util.automaton.Automaton;

// TODO: maybe also toFST?  then we can translate atts into FST outputs/weights

/** Consumes a TokenStream and creates an {@link Automaton}
 *  where the transition labels are UTF8 bytes (or Unicode 
 *  code points if unicodeArcs is true) from the {@link
 *  TermToBytesRefAttribute}.  Between tokens we insert
 *  POS_SEP and for holes we insert HOLE.
 *
 * @lucene.experimental */
public class TokenStreamToAutomaton {
	
	private boolean preservePositionIncrements;
	  private boolean unicodeArcs;

	  /** Sole constructor. */
	  public TokenStreamToAutomaton() {
	    this.preservePositionIncrements = true;
	  }

	  /** Whether to generate holes in the automaton for missing positions, <code>true</code> by default. */
	  public void setPreservePositionIncrements(boolean enablePositionIncrements) {
	    this.preservePositionIncrements = enablePositionIncrements;
	  }

	  /** Whether to make transition labels Unicode code points instead of UTF8 bytes, 
	   *  <code>false</code> by default */
	  public void setUnicodeArcs(boolean unicodeArcs) {
	    this.unicodeArcs = unicodeArcs;
	  }
	  
	  private static class Position implements RollingBuffer.Resettable {
		    // Any tokens that ended at our position arrive to this state:
		    int arriving = -1;

		    // Any tokens that start at our position leave from this state:
		    int leaving = -1;

		    @Override
		    public void reset() {
		      arriving = -1;
		      leaving = -1;
		    }
		  }

		  private static class Positions extends RollingBuffer<Position> {
		    @Override
		    protected Position newInstance() {
		      return new Position();
		    }
		  }

}
