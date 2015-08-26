package org.apache.lucene.store;

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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Closeable;
import java.nio.file.NoSuchFileException;
import java.util.Collection; // for javadocs

import org.apache.lucene.util.IOUtils;

/** A Directory is a flat list of files.  Files may be written once, when they
 * are created.  Once a file is created it may only be opened for read, or
 * deleted.  Random access is permitted both when reading and writing.
 *
 * <p> Java's i/o APIs not used directly, but rather all i/o is
 * through this API.  This permits things such as: <ul>
 * <li> implementation of RAM-based indices;
 * <li> implementation indices stored in a database, via JDBC;
 * <li> implementation of an index as a single file;
 * </ul>
 *
 * Directory locking is implemented by an instance of {@link
 * LockFactory}, and can be changed for each Directory
 * instance using {@link #setLockFactory}.
 *
 */
public abstract class Directory implements Closeable {
	
	/**
	   * Returns an array of strings, one for each file in the directory.
	   * 
	   * @throws NoSuchDirectoryException if the directory is not prepared for any
	   *         write operations (such as {@link #createOutput(String, IOContext)}).
	   * @throws IOException in case of other IO errors
	   */
	  public abstract String[] listAll() throws IOException;

	  /** Returns true iff a file with the given name exists.
	   *
	   *  @deprecated This method will be removed in 5.0 */
	  @Deprecated
	  public abstract boolean fileExists(String name)
	       throws IOException;

	  /** Removes an existing file in the directory. */
	  public abstract void deleteFile(String name)
	       throws IOException;

	  /**
	   * Returns the length of a file in the directory. This method follows the
	   * following contract:
	   * <ul>
	   * <li>Throws {@link FileNotFoundException} or {@link NoSuchFileException}
	   * if the file does not exist.
	   * <li>Returns a value &ge;0 if the file exists, which specifies its length.
	   * </ul>
	   * 
	   * @param name the name of the file for which to return the length.
	   * @throws IOException if there was an IO error while retrieving the file's
	   *         length.
	   */
	  public abstract long fileLength(String name) throws IOException;
	  
	  /** Creates a new, empty file in the directory with the given name.
      Returns a stream writing this file. */
	  public abstract Indexou

}
