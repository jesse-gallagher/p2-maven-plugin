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

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import org.apache.maven.plugin.logging.Log;
import org.openntf.maven.p2.utils.P2DomUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.commons.util.StringUtil;

/**
 * Generates an old-style "site.xml" file for the provided p2 repository directory.
 * 
 * @author Jesse Gallagher
 * @since 2.1.0
 */
public class GenerateSiteXmlTask implements Runnable {
	private static final Pattern FEATURE_FILENAME_PATTERN = Pattern.compile("^([^_]+)_(.*)\\.jar$"); //$NON-NLS-1$
	
	private final Path p2Directory;
	private final String category;
	private final Log log;

	/**
	 * 
	 * @param p2Directory the directory containing the "features" and "plugins" directories.
	 * @param category an optional category to use, overriding any set in category.xml
	 */
	public GenerateSiteXmlTask(Path p2Directory, String category, Log log) {
		this.p2Directory = Objects.requireNonNull(p2Directory, "p2Directory cannot be null");
		this.category = category;
		this.log = log;
	}

	@Override
	public void run() {
		if (!Files.isDirectory(p2Directory)) {
			throw new IllegalStateException("Repository directory does not exist: " + p2Directory);
		} else {
			Path features = p2Directory.resolve("features"); //$NON-NLS-1$
			if(!Files.isDirectory(features)) {
				throw new IllegalStateException("Unable to find features directory: " + features);
			}
			
			try {
				Document doc = P2DomUtil.createDocument();
				Element root = P2DomUtil.createElement(doc, "site"); //$NON-NLS-1$
				
				// Create the category entry if applicable
				String category = this.category;
				Set<String> createdCategories = new HashSet<>();
				if(StringUtil.isNotEmpty(category)) {
					Element categoryDef = P2DomUtil.createElement(root, "category-def"); //$NON-NLS-1$
					categoryDef.setAttribute("name", category); //$NON-NLS-1$
					categoryDef.setAttribute("label", category); //$NON-NLS-1$
				}

				Document content = null;
				Path contentFile = p2Directory.resolve("content.xml"); //$NON-NLS-1$
				if(Files.isRegularFile(contentFile)) {
					try(InputStream is = Files.newInputStream(contentFile)) {
						content = P2DomUtil.createDocument(is);
					}
				} else {
					// Check for content.jar
					contentFile = p2Directory.resolve("content.jar"); //$NON-NLS-1$
					if(Files.isRegularFile(contentFile)) {
						try(InputStream is = Files.newInputStream(contentFile)) {
							try(ZipInputStream zis = new ZipInputStream(is)) {
								zis.getNextEntry();
								content = P2DomUtil.createDocument(zis);
							}
						}
					}
				}
				if(content == null) {
					content = P2DomUtil.createDocument();
				}

				List<Path> featureFiles = Files.list(features)
					.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jar")) //$NON-NLS-1$
					.collect(Collectors.toList());
				
				for(Path feature : featureFiles) {
					Matcher matcher = FEATURE_FILENAME_PATTERN.matcher(feature.getFileName().toString());
					if(!matcher.matches()) {
						throw new IllegalStateException("Could not match filename pattern to " + feature.getFileName());
					}
					if(log.isDebugEnabled()) {
						log.debug("Filename matcher groups: " + matcher.groupCount());
					}
					String featureName = matcher.group(1);
					String version = matcher.group(2);
					
					Element featureElement = P2DomUtil.createElement(root, "feature"); //$NON-NLS-1$
					String url = "features/" + feature.getFileName(); //$NON-NLS-1$
					featureElement.setAttribute("url", url); //$NON-NLS-1$
					featureElement.setAttribute("id", featureName); //$NON-NLS-1$
					featureElement.setAttribute("version", version); //$NON-NLS-1$
					
					if(StringUtil.isNotEmpty(category)) {
						Element categoryElement = P2DomUtil.createElement(featureElement, "category"); //$NON-NLS-1$
						categoryElement.setAttribute("name", category); //$NON-NLS-1$
					} else {
						// See if it's referenced in any content.xml features
						List<Node> matches = P2DomUtil.nodes(content, StringUtil.format("/repository/units/unit/requires/required[@name='{0}']", featureName + ".feature.group")); //$NON-NLS-1$
						if(!matches.isEmpty()) {
							for(Node match : matches) {
								if(match instanceof Element) {
									Element matchEl = (Element)match;
									Node unit = matchEl.getParentNode().getParentNode();
									// Make sure the parent is a category
									boolean isCategory = P2DomUtil.node(unit, "properties/property[@name='org.eclipse.equinox.p2.type.category']") != null; //$NON-NLS-1$
									if(isCategory) {
										String categoryName = P2DomUtil.node(unit, "properties/property[@name='org.eclipse.equinox.p2.name']/@value").get().getNodeValue(); //$NON-NLS-1$
										Element categoryElement = P2DomUtil.createElement(featureElement, "category"); //$NON-NLS-1$
										categoryElement.setAttribute("name", categoryName); //$NON-NLS-1$

										if(!createdCategories.contains(categoryName)) {
											Element categoryDef = P2DomUtil.createElement(root, "category-def"); //$NON-NLS-1$
											categoryDef.setAttribute("name", categoryName); //$NON-NLS-1$
											categoryDef.setAttribute("label", categoryName); //$NON-NLS-1$
											createdCategories.add(categoryName);
										}
										break;
									}
								}
							}
						}
					}
				}
				
				String xml = P2DomUtil.getXmlString(doc, null);
				Path output = p2Directory.resolve("site.xml"); //$NON-NLS-1$
				try(Writer w = Files.newBufferedWriter(output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
					w.write(xml);
				} catch (IOException e) {
					throw new RuntimeException("Error writing site.xml file", e);
				}
				
				log.info(StringUtil.format("Wrote site.xml contents to {0}", output));
			} catch(IOException e) {
				throw new RuntimeException("Exception while building site.xml document", e);
			}
		}
	}

}
