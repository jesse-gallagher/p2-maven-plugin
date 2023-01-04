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
package org.reficio.p2.resolver.maven.impl.facade;

import java.util.List;
import java.util.function.BiPredicate;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.filter.PatternExclusionsDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

/**
 * @author Tom Bujok (tom.bujok@gmail.com)<br>
 *         Reficio (TM) - Reestablish your software!<br>
 *         http://www.reficio.org
 * @since 1.1.0
 */
public class AetherEclipseFacade implements AetherFacade {

    @Override
    public DependencyRequest newDependencyRequest(Object dependencyNode, Object dependencyFilter) {
        return new DependencyRequest((DependencyNode) dependencyNode, (DependencyFilter) dependencyFilter);
    }

    @Override
    public PreorderNodeListGenerator newPreorderNodeListGenerator() {
        return new PreorderNodeListGenerator();
    }

    @Override
    public Dependency newDependency(Object defaultArtifact, String scope) {
        return new Dependency((DefaultArtifact) defaultArtifact, scope);
    }

    @Override
    public CollectRequest newCollectRequest() {
        return new CollectRequest();
    }

    @Override
    public DefaultArtifact newDefaultArtifact(String artifact) {
        return new DefaultArtifact(artifact);
    }

    @Override
    public ArtifactRequest newArtifactRequest() {
        return new ArtifactRequest();
    }

    @Override
    public SubArtifact newSubArtifact(Object artifact, String classifier, String extension) {
        return new SubArtifact((Artifact)artifact, classifier, extension);
    }

    @SuppressWarnings("unchecked")
	@Override
    public BiPredicate<Object, List<?>> newPatternExclusionsDependencyFilter(List<String> excludes) {
		return (a, b) -> new PatternExclusionsDependencyFilter(excludes).accept((DependencyNode)a, (List<DependencyNode>)b);
    }

    @Override
    public DependencyFilter newDependencyFilter(BiPredicate<Object, List<?>> filterClosure) {
    	return (node, parents) -> {
    		return filterClosure.test(node, parents);
    	};
//        new DependencyFilter() {
//            @Override
//            boolean accept(DependencyNode node, List<DependencyNode> parents) {
//                filterClosure(node, parents)
//            }
//        }
    }

    @Override
    public org.reficio.p2.resolver.maven.Artifact translateArtifactAetherToGeneric(Object artifact) {
    	Artifact a = (Artifact)artifact;
    	return new org.reficio.p2.resolver.maven.Artifact(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getExtension(), a.getClassifier(),
    			a.isSnapshot(), a.getVersion(), a.getFile());
    }

    @Override
    public Object translateArtifactGenericToAether(org.reficio.p2.resolver.maven.Artifact a) {
        // baseVersion and snapshot properties are internal fields calculated on the basis of the others
        return new DefaultArtifact(a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getExtension(), a.getVersion(), null, a.getFile());
    }
    
    @Override
    public void addDependency(Object collectRequest, Object dependency) {
    	((CollectRequest)collectRequest).addDependency((Dependency)dependency);
    }
    
    @Override
    public void addRepository(Object collectRequest, Object repository) {
    	((CollectRequest)collectRequest).addRepository((RemoteRepository)repository);
    }
    
    @Override
    public void addArtifactRequestRepository(Object artifactRequest, Object repository) {
    	((ArtifactRequest)artifactRequest).addRepository((RemoteRepository)repository);
    }
    
    @Override
    public void setArtifactRequestArtifact(Object artifactRequest, Object artifact) {
    	((ArtifactRequest)artifactRequest).setArtifact((Artifact)artifact);
    }
    
    @Override
    public Object resolveArtifact(Object repositorySystem, Object repositorySystemSession, Object artifactRequest) {
    	try {
			return ((RepositorySystem)repositorySystem)
				.resolveArtifact((RepositorySystemSession)repositorySystemSession, (ArtifactRequest)artifactRequest)
				.getArtifact();
		} catch (ArtifactResolutionException e) {
			throw new RuntimeException(e);
		}
    }
    
    @Override
    public void setFilter(Object dependencyRequest, Object dependencyFilter) {
    	((DependencyRequest)dependencyRequest).setFilter((DependencyFilter)dependencyFilter);
    }
    
    @Override
    public Object getRootDependencyNode(Object repositorySystem, Object repositorySystemSession,
    		Object collectRequest) {
    	try {
			return ((RepositorySystem)repositorySystem)
				.collectDependencies((RepositorySystemSession)repositorySystemSession, (CollectRequest)collectRequest)
				.getRoot();
		} catch (DependencyCollectionException e) {
			throw new RuntimeException(e);
		}
    }
    
    @Override
    public Object resolveDependencies(Object repositorySystem, Object repositorySystemSession, Object dependencyRequest) {
    	try {
			return ((RepositorySystem)repositorySystem)
				.resolveDependencies((RepositorySystemSession)repositorySystemSession, (DependencyRequest)dependencyRequest);
		} catch (DependencyResolutionException e) {
			throw new RuntimeException(e);
		}
    }
    
    @Override
    public void acceptDependencyNodeGenerator(Object dependencyNode, Object generator) {
    	((DependencyNode)dependencyNode).accept((PreorderNodeListGenerator)generator);
    }
    
    @Override
    public List<?> getGeneratorArtifacts(Object generator) {
    	return ((PreorderNodeListGenerator)generator).getArtifacts(false);
    }
    
    @Override
    public Object getDependencyNodeArtifact(Object dependencyNode) {
    	return ((DependencyNode)dependencyNode).getDependency().getArtifact();
    }
    
    @Override
    public String getRepositoryType(Object repository) {
    	return ((RemoteRepository)repository).getContentType();
    }
    
    @Override
    public String getRepositoryUrl(Object repository) {
    	return ((RemoteRepository)repository).getUrl();
    }

}