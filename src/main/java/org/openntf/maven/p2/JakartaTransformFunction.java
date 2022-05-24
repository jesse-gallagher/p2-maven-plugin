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
package org.openntf.maven.p2;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.transformer.AppOption;
import org.eclipse.transformer.TransformOptions;
import org.eclipse.transformer.Transformer;
import org.eclipse.transformer.Transformer.ResultCode;
import org.eclipse.transformer.jakarta.JakartaTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies Jakarta EE transformation to the provided file.
 * 
 * @author Jesse Gallagher
 * @since 2.3.0
 */
public class JakartaTransformFunction implements Function<File, File> {
	private Logger logger;
	
	public JakartaTransformFunction() {
		logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public File apply(File t) {
		try {
			String inputFileName = t.getAbsolutePath();
			
			File dest = File.createTempFile(t.getName(), ".jar"); //$NON-NLS-1$
			String outputFileName = dest.getAbsolutePath();

			Map<String, String> optionDefaults = JakartaTransform.getOptionDefaults();
			Function<String, URL> ruleLoader = JakartaTransform.getRuleLoader();
			TransformOptions options = new TransformOptions() {
				
				@Override
				public boolean hasOption(AppOption option) {
					switch(option) {
					case OVERWRITE:
						return true;
					default:
						return TransformOptions.super.hasOption(option);
					}
				}

				@Override
				public String getDefaultValue(AppOption option) {
					return optionDefaults.get(option.getLongTag());
				}
				
				@Override
				public Function<String, URL> getRuleLoader() {
					return ruleLoader;
				}
				
				@Override
				public String getInputFileName() {
					return inputFileName;
				}
				
				@Override
				public String getOutputFileName() {
					return outputFileName;
				}
			};
			
			Transformer transformer = new Transformer(logger, options);
			ResultCode result = transformer.run();
			switch(result) {
			case ARGS_ERROR_RC:
			case FILE_TYPE_ERROR_RC:
			case RULES_ERROR_RC:
			case TRANSFORM_ERROR_RC:
				throw new IllegalStateException("Received unexpected result from transformer: " + result);
			case SUCCESS_RC:
			default:
				return dest;
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Encountered exception converting file " + t, e);
		}
	}
}
