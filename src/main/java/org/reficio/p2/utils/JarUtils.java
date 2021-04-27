/**
 * Copyright (c) 2012-2020 Reficio (TM), Jesse Gallagher All Rights Reserved.
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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;

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
	        Resource res = jar.getResource("feature.xml"); //$NON-NLS-1$
	        Document featureSpec = XmlUtils.parseXml(res.openInputStream());
	        
	        adjustFeatureQualifierVersionWithTimestamp(featureSpec, timestamp);
	        adjustFeaturePluginData(featureSpec, pluginDir, log, mavenProject);
            
	        File temp = new File(outputFile.getParentFile(),"temp"); //$NON-NLS-1$
	        temp.mkdir();
	        newXml = new File(temp,"feature.xml"); //$NON-NLS-1$
	        XmlUtils.writeXml(featureSpec, newXml);

            FileResource newRes = new FileResource(newXml);
            jar.putResource("feature.xml", newRes, true); //$NON-NLS-1$
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
	        String version = featureSpec.getDocumentElement().getAttributeNode("version").getValue(); //$NON-NLS-1$
	        String newVersion = Utils.eclipseQualifierToTimeStamp(version, timestamp); 
	        featureSpec.getDocumentElement().getAttributeNode("version").setValue(newVersion); //$NON-NLS-1$
    }

    static Comparator<File> fileComparator = new Comparator<File>() {
		@Override
		public int compare(File arg0, File arg1) {
			return arg0.getName().compareTo(arg1.getName());
		}
	};
	static Comparator<File> pluginComparator = Comparator.comparing((File plugin) -> {
		try(JarFile f = new JarFile(plugin)) {
			Manifest m = f.getManifest();
			return new Version((String)m.getMainAttributes().getValue("Bundle-Version")); //$NON-NLS-1$
		} catch(IOException e) {
			throw new UncheckedIOException(e);
		}
	});
	private static class Version implements Comparable<Version> {
		public int major;
		public int minor;
		public int patch;
		public String qualifier;
		
		public Version(String spec) {
			if(spec == null) {
				major = minor = patch = 0;
				qualifier = "0"; //$NON-NLS-1$
			} else {
				String[] bits = StringUtils.split(spec, ".", 4); //$NON-NLS-1$
				major = Integer.parseInt(bits[0]);
				minor = bits.length > 1 ? Integer.parseInt(bits[1]) : 0;
				patch = bits.length > 2 ? Integer.parseInt(bits[2]) : 0;
				qualifier = bits.length > 3 ? bits[3] : "0"; //$NON-NLS-1$
			}
		}
		@Override
		public int compareTo(Version o) {
			return Comparator.comparingInt((Version v) -> v.major)
				.thenComparing(v -> v.minor)
				.thenComparing(v -> v.patch)
				.thenComparing(v -> v.qualifier)
				.compare(this, o);
		}
		@Override
		public String toString() {
			return MessageFormat.format("[Version: {0}.{1}.{2}.{3}]", major, minor, patch, qualifier);
		}
	}
    
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
    	
	        NodeList plugins = featureSpec.getElementsByTagName("plugin"); //$NON-NLS-1$
	        for(int i=0; i<plugins.getLength(); ++i) {
	        	Node n = plugins.item(i);
	        	if (n instanceof Element) {
		        	Element el = (Element)n;
		        	String pluginId = el.getAttribute("id"); //$NON-NLS-1$
		        	File pluginFile = findPlugin(pluginDir, mavenProject, pluginId, el.getAttribute("version")); //$NON-NLS-1$
		        	
		        	if (pluginFile == null) {
		        		log.error("Cannot find plugin "+pluginId);
		        	} else {
		        		//String firstVersion = BundleUtils.INSTANCE.getBundleVersion(new Jar(firstFile));
		        		String lastVersion = BundleUtils.INSTANCE.getBundleVersion(new Jar(pluginFile)); //may throw IOException
		        		log.info("Adjusting version for plugin "+pluginId+" to "+lastVersion + " from " + pluginFile);
		        		el.setAttribute("version", lastVersion); //$NON-NLS-1$
		        	}
	        	}
	        }
    }
    
    /**
     * @since 2.0.0
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
        		Collections.sort(files,pluginComparator);
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
     * @since 2.0.0
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
    		if(StringUtils.isEmpty(featureVersion) || "0.0.0".equals(featureVersion)) { //$NON-NLS-1$
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
					return name.startsWith(pluginId + "_") && name.endsWith(".jar"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
    }
    
    public static void removeSignature(File jar) {
        File unsignedJar = new File(jar.getParent(), jar.getName() + ".tmp"); //$NON-NLS-1$
        try {
            if (unsignedJar.exists()) {
                FileUtils.deleteQuietly(unsignedJar);
                unsignedJar = new File(jar.getParent(), jar.getName() + ".tmp"); //$NON-NLS-1$
            }
            if (!unsignedJar.createNewFile()) {
                throw new RuntimeException("Cannot create file " + unsignedJar);
            }

            try(ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(unsignedJar))) {
                try(ZipFile zip = new ZipFile(jar)) {
	                for (Enumeration<? extends ZipEntry> list = zip.entries(); list.hasMoreElements(); ) {
	                    ZipEntry entry = (ZipEntry) list.nextElement();
	                    String name = entry.getName();
	                    if (entry.isDirectory()) {
	                        continue;
	                    } else if (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".SF")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	                FileUtils.copyFile(unsignedJar, jar);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            FileUtils.deleteQuietly(unsignedJar);
        }
    }

    public static boolean containsSignature(File jarToUnsign) {
        try {
            ZipFile zip = new ZipFile(jarToUnsign);
            try {
                for (Enumeration<? extends ZipEntry> list = zip.entries(); list.hasMoreElements(); ) {
                    ZipEntry entry = (ZipEntry) list.nextElement();
                    String name = entry.getName();
                    if (!entry.isDirectory() && (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".SF"))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
			String fname = f.getPath().replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
			if (f.isDirectory()) {
				if (!fname.endsWith("/")) { //$NON-NLS-1$
					fname = fname + "/"; //$NON-NLS-1$
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
