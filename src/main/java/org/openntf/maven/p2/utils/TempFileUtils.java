/**
 * Copyright (c) 2012-2022 Reficio (TM), Jesse Gallagher All Rights Reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openntf.maven.p2.utils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import aQute.lib.io.IO;

/**
 * Utility class to house methods useful for generating and managing temp files.
 * 
 * @author Jesse Gallagher
 * @since 2.3.0
 */
public enum TempFileUtils {
	;
	
	private static final Collection<File> FILES = new HashSet<>();
	
	/**
	 * Creates and tracks a temporary file in the default location.
	 * 
	 * @param prefix the prefix for the file name
	 * @param suffix the suffix for the file name
	 * @return the created temporary file
	 * @throws IOException if there is a lower-level exception creating the file
	 * @see File#createTempFile(String, String)
	 */
	public static File createTempFile(String prefix, String suffix) throws IOException {
		File file = File.createTempFile(prefix, suffix);
		FILES.add(file);
		return file;
	}
	
	/**
	 * Cleans any temporary files created via {@link #createTempFile(String, String)}.
	 * 
	 * <p>This method will ignore any exceptions occurred while deleting files.</p>
	 */
	public static void cleanTempFiles() {
		FILES.forEach(IO::delete);
		FILES.clear();
	}
}
