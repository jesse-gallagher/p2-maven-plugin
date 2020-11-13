/**
 * Copyright (c) 2012 Reficio (TM) - Reestablish your software! All Rights Reserved.
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
package org.reficio.p2.utils;


import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.io.Files;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @author Tom Bujok (tom.bujok@gmail.com)<br>
 *         Reficio (TM) - Reestablish your software!<br>
 *         http://www.reficio.org
 * @since 1.0.0
 */
public class JarUtils {

    public static void adjustSnapshotOutputVersion(File inputFile, File outputFile, String version) {
        Jar jar = null;
        try {
            jar = new Jar(inputFile);
            Manifest manifest = jar.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            attributes.putValue(Analyzer.BUNDLE_VERSION, version);
            jar.write(outputFile);
        } catch (Exception e) {
            throw new RuntimeException("Cannot open jar " + outputFile, e);
        } finally {
            if (jar != null) {
                jar.close();
            }
        }
    }
    
    /**
     * Opens the feature.xml in the given jar file and adjusts all version numbers/timestamps
     *
     * @param inputFile - inputFile
     * @param outputFile - outputFile
     * @param log - log
     * @param pluginDir - pluginDir
     * @param timestamp - timestamp
     *
     */
    public static void adjustFeatureXml(File inputFile, File outputFile, File pluginDir, Log log, String timestamp, MavenProject mavenProject) {
        Jar jar = null;
        File newXml = null;
        try {
        	jar = new Jar(inputFile);
	        Resource res = jar.getResource("feature.xml");
	        Document featureSpec = XmlUtils.parseXml(res.openInputStream());
	        
	        adjustFeatureQualifierVersionWithTimestamp(featureSpec, timestamp);
	        adjustFeaturePluginData(featureSpec, pluginDir, log, mavenProject);
            
	        File temp = new File(outputFile.getParentFile(),"temp");
	        temp.mkdir();
	        newXml = new File(temp,"feature.xml");
	        XmlUtils.writeXml(featureSpec, newXml);

            FileResource newRes = new FileResource(newXml);
            jar.putResource("feature.xml", newRes, true);
            jar.write(outputFile);
        } catch (Exception e) {
            throw new RuntimeException("Cannot open jar " + outputFile, e);
        } finally {
            if (jar != null) {
                jar.close();
            }
            if (newXml != null) {
            	newXml.delete();
            }
        }
    }

    public static void adjustFeatureQualifierVersionWithTimestamp(Document featureSpec, String timestamp) {
	        String version = featureSpec.getDocumentElement().getAttributeNode("version").getValue();
	        String newVersion = Utils.eclipseQualifierToTimeStamp(version, timestamp); 
	        featureSpec.getDocumentElement().getAttributeNode("version").setValue(newVersion);
    }

    static Comparator<File> fileComparator = new Comparator<File>() {
		@Override
		public int compare(File arg0, File arg1) {
			return arg0.getName().compareTo(arg1.getName());
		}
	};
    
	/**
	 * Adjust the pluginId TODO - this may be wrong if singleton is used
     *
     * @param pluginDir - pluginDir
     * @param featureSpec - featureSpec
     * @param log  - log
     *
     * @throws IOException - an exception
	 */
    public static void adjustFeaturePluginData(Document featureSpec, File pluginDir, Log log, MavenProject mavenProject) throws IOException {
	        //get list of all plugins
    	
	        NodeList plugins = featureSpec.getElementsByTagName("plugin");
	        for(int i=0; i<plugins.getLength(); ++i) {
	        	Node n = plugins.item(i);
	        	if (n instanceof Element) {
		        	Element el = (Element)n;
		        	String pluginId = el.getAttribute("id");
		        	File pluginFile = findPlugin(pluginDir, mavenProject, pluginId, el.getAttribute("version")); //$NON-NLS-1$
		        	
		        	if (pluginFile == null) {
		        		log.error("Cannot find plugin "+pluginId);
		        	} else {
		        		//String firstVersion = BundleUtils.INSTANCE.getBundleVersion(new Jar(firstFile));
		        		String lastVersion = BundleUtils.INSTANCE.getBundleVersion(new Jar(pluginFile)); //may throw IOException
		        		log.info("Adjusting version for plugin "+pluginId+" to "+lastVersion);
		        		el.setAttribute("version", lastVersion);
		        	}
	        	}
	        }
    }
    
    /**
     * @since 1.4.1
     */
    public static File findPlugin(File projectPluginDir, MavenProject mavenProject, String pluginId, String pluginVersion) throws IOException {
    	List<File> files = new ArrayList<>();
    	files.addAll(Arrays.asList(findFiles(projectPluginDir, pluginId)));
		// Look through the project's local p2 repositories
		for(Repository repo : mavenProject.getRepositories()) {
			// TODO support remote repos
			if("p2".equals(repo.getLayout()) && repo.getUrl().startsWith("file:/")) { //$NON-NLS-1$ //$NON-NLS-2$
				try {
					File repoPluginDir = new File(new File(new URI(repo.getUrl())), "plugins"); //$NON-NLS-1$
					files.addAll(Arrays.asList(findFiles(repoPluginDir, pluginId)));
				} catch (URISyntaxException e) {
					throw new IOException(e);
				}
			}
		}
    	
    	if (files.isEmpty()) {
    		return null;
    	} else {
    		File pluginFile;
    		if(StringUtils.isEmpty(pluginVersion) || "0.0.0".equals(pluginVersion)) { //$NON-NLS-1$
        		//in case more than one plugin with same id
        		Collections.sort(files,fileComparator);
        		pluginFile = files.get(files.size()-1);	
    		} else {
    			// Find an exact match
    			pluginFile = files.stream()
    				.filter(f -> f.getName().equals(pluginId + '_' + pluginVersion + ".jar")) //$NON-NLS-1$
    				.findFirst()
    				.orElseThrow(() -> new IllegalStateException("Unable to find plugin " + pluginId + ":" + pluginVersion));
    		}
    		if(!pluginFile.getParentFile().equals(projectPluginDir)) {
    			File destPlugin = new File(projectPluginDir, pluginFile.getName());
    			Files.copy(pluginFile, destPlugin);
    			return destPlugin;
    		} else {
    			return pluginFile;
    		}
    	}
    }
    /**
     * Locates the named feature in the file:/ p2 repositories known to this project
     * 
     * @since 1.4.1
     */
    public static File findFeature(MavenProject mavenProject, String featureId, String featureVersion) throws IOException {
    	List<File> files = new ArrayList<>();
    	// Look through the project's local p2 repositories
		for(Repository repo : mavenProject.getRepositories()) {
			// TODO support remote repos
			if("p2".equals(repo.getLayout()) && repo.getUrl().startsWith("file:/")) { //$NON-NLS-1$ //$NON-NLS-2$
				try {
					File repoFeatureDir = new File(new File(new URI(repo.getUrl())), "features"); //$NON-NLS-1$
					if(repoFeatureDir.exists()) {
						files.addAll(Arrays.asList(findFiles(repoFeatureDir, featureId)));
					}
				} catch (URISyntaxException e) {
					throw new IOException(e);
				}
			}
		}
    	
    	if (files.isEmpty()) {
    		throw new IllegalStateException("Unable to find feature " + featureId + ":" + featureVersion);
    	} else {
    		File pluginFile;
    		if(StringUtils.isEmpty(featureVersion) || "0.0.0".equals(featureVersion)) {
        		//in case more than one plugin with same id
        		Collections.sort(files,fileComparator);
        		pluginFile = files.get(files.size()-1);	
    		} else {
    			// Find an exact match
    			pluginFile = files.stream()
    				.filter(f -> f.getName().equals(featureId + '_' + featureVersion + ".jar")) //$NON-NLS-1$
    				.findFirst()
    				.orElseThrow(() -> new IllegalStateException("Unable to find feature " + featureId + ":" + featureVersion));
    		}
			return pluginFile;
    	}
    }
    
    static File[] findFiles(File pluginDir, final String pluginId) {
    	 return pluginDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith(pluginId + "_") && name.endsWith(".jar");
				}
			});
    }
    
    public static void removeSignature(File jar) {
        File unsignedJar = new File(jar.getParent(), jar.getName() + ".tmp");
        try {
            if (unsignedJar.exists()) {
                FileUtils.deleteQuietly(unsignedJar);
                unsignedJar = new File(jar.getParent(), jar.getName() + ".tmp");
            }
            if (!unsignedJar.createNewFile()) {
                throw new RuntimeException("Cannot create file " + unsignedJar);
            }

            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(unsignedJar));
            try {
                ZipFile zip = new ZipFile(jar);
                for (Enumeration list = zip.entries(); list.hasMoreElements(); ) {
                    ZipEntry entry = (ZipEntry) list.nextElement();
                    String name = entry.getName();
                    if (entry.isDirectory()) {
                        continue;
                    } else if (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".SF")) {
                        continue;
                    }

                    InputStream zipInputStream = zip.getInputStream(entry);
                    zipOutputStream.putNextEntry(entry);
                    try {
                        IOUtils.copy(zipInputStream, zipOutputStream);
                    } finally {
                        zipInputStream.close();
                    }
                }
                IOUtils.closeQuietly(zipOutputStream);
                FileUtils.copyFile(unsignedJar, jar);
            } finally {
                IOUtils.closeQuietly(zipOutputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtils.deleteQuietly(unsignedJar);
        }
    }

    public static boolean containsSignature(File jarToUnsign) {
        try {
            ZipFile zip = new ZipFile(jarToUnsign);
            try {
                for (Enumeration list = zip.entries(); list.hasMoreElements(); ) {
                    ZipEntry entry = (ZipEntry) list.nextElement();
                    String name = entry.getName();
                    if (!entry.isDirectory() && (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".SF"))) {
                        return true;
                    }
                }
                return false;
            } finally {
                zip.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createJar(File directory, File destJar) throws IOException {
    
		
		//we must be generating the feature file from the pom
		FileOutputStream fos = new FileOutputStream(destJar);
		Manifest mf = new Manifest();
		JarOutputStream jar = new JarOutputStream(fos, mf);
		addToJar(jar, directory);
		jar.close();
    }
    
	private static void addToJar(JarOutputStream jar, File content) throws IOException
	{
		for (File f : FileUtils.listFiles(content, null, true) ) {
			String fname = f.getPath().replace("\\", "/");
			if (f.isDirectory()) {
				if (!fname.endsWith("/")) {
					fname = fname + "/";
				}
				JarEntry entry = new JarEntry(fname);
				entry.setTime(f.lastModified());
				jar.putNextEntry(entry);
				jar.closeEntry();
			} else {
				//must be a file
				JarEntry entry = new JarEntry(fname);
				entry.setTime(f.lastModified());
				jar.putNextEntry(entry);
				jar.write( IOUtils.toByteArray(new FileInputStream(f)) );
				jar.closeEntry();
			}
			

		}
	}
}
