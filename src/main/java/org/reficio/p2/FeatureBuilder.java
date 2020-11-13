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
package org.reficio.p2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;
import org.reficio.p2.bundler.ArtifactBundlerInstructions;
import org.reficio.p2.logger.Logger;
import org.reficio.p2.utils.JarUtils;
import org.reficio.p2.utils.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Multimap;
import com.google.common.io.Files;

public class FeatureBuilder {


	public FeatureBuilder(P2FeatureDefinition p2FeatureDefintion, Multimap<P2Artifact, 
			ArtifactBundlerInstructions>  bundlerInstructions, boolean generateSourceFeature, boolean unpack, String timestamp,
			MavenProject mavenProject) {
		this.p2FeatureDefintion = p2FeatureDefintion;
		this.bundlerInstructions = bundlerInstructions;
		this.generateSourceFeature = generateSourceFeature;
		this.unpack = unpack;
		this.featureTimeStamp = timestamp;
		this.mavenProject = mavenProject;
	}

	private Multimap<P2Artifact, ArtifactBundlerInstructions>  bundlerInstructions;
	private P2FeatureDefinition p2FeatureDefintion;
	private boolean generateSourceFeature;
	private boolean unpack;
	//cache this so that the same timestamp is used
	private String featureTimeStamp;
	/**
	 * @since 1.4.1
	 */
	private final MavenProject mavenProject;
	
	public void generate(File destinationFolder) {
		try {
			File featureContent = new File(destinationFolder, this.getFeatureFullName());
			featureContent.mkdir();
			Document xmlDoc = this.buildXml(destinationFolder);
			
			XmlUtils.writeXml(xmlDoc, new File(featureContent, "feature.xml"));
			
			File destJar = new File(destinationFolder, this.getFeatureFullName()+".jar");
			JarUtils.createJar(featureContent, destJar);
			
		} catch (Exception e) {
			throw new RuntimeException("Cannot generate feature", e);
		}
	}
	
	
	String getQualifiedFeatureVersion() {
		String v = this.p2FeatureDefintion.getVersion();
		return v.replace("qualifier",featureTimeStamp);
	}
	
	
	String getFeatureFullName() {
		String id = this.p2FeatureDefintion.getId();
		if (generateSourceFeature) {
			id = id + ".source";
		}
		return id + "_" + this.getQualifiedFeatureVersion();
	}

	private Logger log() {
		return Logger.getLog();
	}

	Document buildXml(File destinationFolder) throws ParserConfigurationException, IOException {
		Document xmlDoc = this.fetchOrCreateXml();
		Element featureElement = XmlUtils.fetchOrCreateElement(xmlDoc, xmlDoc, "feature");
	
		// feature ID & Version are read from POM/featureDefinition and if not present in POM from the XML-template.
		// then we have to write back the found value to featureDefinition
		computeFeatureId(featureElement);
		computeFeatureVersion(featureElement);
		
		// set additonal attributes: label, providerName, description, copyright, license
		computeFeatureAttributes(xmlDoc, featureElement);
	
		// handle source feature renaming
		if (generateSourceFeature) {
			String id = featureElement.getAttribute("id");
			if (!id.endsWith(".source")) {
				// we assume, if ID does not end with .source, also label has no note that this is the developer resources
				featureElement.setAttribute("id", id + ".source");
				featureElement.setAttribute("label", featureElement.getAttribute("label") + " (Developer Resources)");
			}
		}
		// add the <plugin> tags of the defined artifacts
		generateFeatureContent(xmlDoc, featureElement);
		resolveExistingP2Artifacts(xmlDoc, featureElement, destinationFolder);
	
		return xmlDoc;
	}
	
	/**
	 * @since 1.4.1
	 */
	private void resolveExistingP2Artifacts(Document xmlDoc, Element featureElement, File destinationFolder) throws IOException {
		File pluginsDir = new File(destinationFolder.getParentFile(), "plugins");
		for(EclipseArtifact artifact : p2FeatureDefintion.getP2()) {
			String[] idparts = StringUtils.split(artifact.getId(), ':');
			if(idparts.length != 2) {
				throw new IllegalArgumentException("Illegal artifact ID; expected format is bundleId:version - " + artifact.getId());
			}
			File plugin = JarUtils.findPlugin(pluginsDir, mavenProject, idparts[0]);
			if(plugin == null) {
				throw new IllegalStateException("Unable to resolve P2 artifact " + artifact.getId());
			}
			
			// Copy the plugin into our workspace
			File pluginDest = new File(pluginsDir, plugin.getName());
			if(!pluginDest.equals(plugin)) {
				Files.copy(plugin, pluginDest);
			}
			
			// Read the true info from the manifest
			try(JarFile f = new JarFile(pluginDest)) {
				Manifest manifest = f.getManifest();
				String version = manifest.getMainAttributes().getValue("Bundle-Version");
				
				Element pluginElement = XmlUtils.createElement(xmlDoc,featureElement,"plugin");
				pluginElement.setAttribute("id", idparts[0]);
				pluginElement.setAttribute("download-size", "0"); //TODO How can we get the JAR-size from the artifact?
				pluginElement.setAttribute("install-size", "0");  //TODO 
				pluginElement.setAttribute("version", version);
				pluginElement.setAttribute("unpack", unpack ? "true" : "false");
			}
		}
	}

	private void computeFeatureId(Element featureElement) {
		if (this.p2FeatureDefintion.getId() != null) {
			featureElement.setAttribute("id", this.p2FeatureDefintion.getId());
		} else if (featureElement.hasAttribute("id")) {
			// ID in XML, but not in POM. Write back
			this.p2FeatureDefintion.setId(featureElement.getAttribute("id"));
		} else {
			throw new RuntimeException("No id defined for feature in pom or featureFile");
		}
	}

	private void computeFeatureVersion(Element featureElement) {
		if (this.p2FeatureDefintion.getVersion() != null) {
			featureElement.setAttribute("version", this.p2FeatureDefintion.getVersion());
		} else if (featureElement.hasAttribute("version")) {
			// Version in XML, but not in POM. Write back
			this.p2FeatureDefintion.setVersion(featureElement.getAttribute("version"));
		} else {
			throw new RuntimeException("No version defined for feature in pom or featureFile");
		}
		
		//update qualified version if need be
		String xmlVersion = featureElement.getAttribute("version");
		if (xmlVersion.contains("qualifier")) {
			featureElement.setAttribute("version", xmlVersion.replace("qualifier", featureTimeStamp));
		}
	}
	
	private void computeFeatureAttributes(Document xmlDoc, Element featureElement) {
		if (this.p2FeatureDefintion.getLabel() != null) {
			featureElement.setAttribute("label", this.p2FeatureDefintion.getLabel());
		}
		if (this.p2FeatureDefintion.getProviderName() != null) {
			featureElement.setAttribute("provider-name", this.p2FeatureDefintion.getProviderName());
		}
		if (this.p2FeatureDefintion.getDescription() != null) {
			Element descriptionElement = XmlUtils.fetchOrCreateElement(xmlDoc, featureElement, "description");
			descriptionElement.setTextContent(this.p2FeatureDefintion.getDescription());
		}
		if (this.p2FeatureDefintion.getCopyright() != null) {
			Element copyrightElement = XmlUtils.fetchOrCreateElement(xmlDoc, featureElement, "copyright");
			copyrightElement.setTextContent(this.p2FeatureDefintion.getCopyright());
		}
		if (this.p2FeatureDefintion.getLicense() != null) {
			Element licenceElement = XmlUtils.fetchOrCreateElement(xmlDoc, featureElement, "license");
			licenceElement.setTextContent(this.p2FeatureDefintion.getLicense());
		}
		
	}

	private void generateFeatureContent(Document xmlDoc, Element featureElement) {
		for(P2Artifact artifact: this.p2FeatureDefintion.getArtifacts()) {
			Collection<ArtifactBundlerInstructions> abis = this.bundlerInstructions.get(artifact);
			for (ArtifactBundlerInstructions abi : abis) {
				String version = abi.getProposedVersion();
				
				String id;
				if (generateSourceFeature) {
					// 2015-05-12/RPr: A Source feature contains only sources.
					id = abi.getSourceSymbolicName();
					if (StringUtils.isBlank(id)) {
						log().info("\t [WARN] No source found for " + abi.getSymbolicName());
						continue;
					}
				} else {
					id = abi.getSymbolicName();
				}

				Element pluginElement = XmlUtils.createElement(xmlDoc,featureElement,"plugin");
				pluginElement.setAttribute("id", id);
				pluginElement.setAttribute("download-size", "0"); //TODO How can we get the JAR-size from the artifact?
				pluginElement.setAttribute("install-size", "0");  //TODO 
				pluginElement.setAttribute("version", version);
				pluginElement.setAttribute("unpack", unpack ? "true" : "false");
			}

		}	
	}

	Document fetchOrCreateXml() throws ParserConfigurationException, FileNotFoundException {
		if (null == this.p2FeatureDefintion.getFeatureFile()) {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} else {
			return XmlUtils.parseXml(new FileInputStream(this.p2FeatureDefintion.getFeatureFile()));
		}
	}

}
