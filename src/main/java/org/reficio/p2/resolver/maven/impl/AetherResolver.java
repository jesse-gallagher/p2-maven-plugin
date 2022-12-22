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
package org.reficio.p2.resolver.maven.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.reficio.p2.logger.Logger;
import org.reficio.p2.resolver.maven.Artifact;
import org.reficio.p2.resolver.maven.ArtifactResolutionRequest;
import org.reficio.p2.resolver.maven.ArtifactResolutionResult;
import org.reficio.p2.resolver.maven.ArtifactResolver;
import org.reficio.p2.resolver.maven.ResolvedArtifact;
import org.reficio.p2.resolver.maven.impl.facade.AetherFacade;

/**
 * @author Tom Bujok (tom.bujok@gmail.com)<br>
 *         Reficio (TM) - Reestablish your software!<br>
 *         http://www.reficio.org
 * @since 1.0.0
 */
public class AetherResolver implements ArtifactResolver {
	
	public static final String DEFAULT_SCOPE = "compile"; //$NON-NLS-1$

    private final Object repositorySystem;
    private final Object repositorySystemSession;
    private final List<?> remoteRepositories;
    private final String scope;
    private final AetherFacade aether;
    
    public AetherResolver(Object repositorySystem, Object repositorySystemSession, List<?> repos) {
		this(repositorySystem, repositorySystemSession, repos, DEFAULT_SCOPE);
	}
    
    public AetherResolver(Object repositorySystem, Object repositorySystemSession, List<?> repos, String scope) {
		this.repositorySystem = repositorySystem;
		this.repositorySystemSession = repositorySystemSession;
		this.remoteRepositories = repos;
		this.scope = scope;
		this.aether = Aether.facade(repositorySystemSession);
	}
    

	@Override
	public ArtifactResolutionResult resolve(ArtifactResolutionRequest request) {
		List<ResolvedArtifact> result = new ArrayList<>();
        List<Artifact> resolvedBinaries = resolveBinaries(request);
        for (Artifact resolvedBinary : resolvedBinaries) {
            Artifact resolvedSource = null;
            if (request.isResolveSource()) {
                try {
                    resolvedSource = resolveSourceForArtifact(resolvedBinary);
                } catch (Exception ex) {
                    // will not fail if the source not resolved
                }
            }
            ResolvedArtifact resolvedArtifact = new ResolvedArtifact(resolvedBinary, resolvedSource, isRoot(request, resolvedBinary));
            result.add(resolvedArtifact);
        }
        return new ArtifactResolutionResult(result);
	}
	
	private static boolean isRoot(ArtifactResolutionRequest request, Artifact artifact) {
        String rootId = request.getRootArtifactId();
        return rootId == artifact.getShortId() || rootId == artifact.getExtendedId() || rootId == artifact.getLongId();
    }

    private List<Artifact> resolveBinaries(ArtifactResolutionRequest request) {
        if (request.isResolveTransitive()) {
            return translateArtifactsAetherToGeneric(resolveWithTransitive(request.getRootArtifactId(), request.getExcludes()));
        } else {
            return translateArtifactsAetherToGeneric(Arrays.asList(resolveNoTransitive(request.getRootArtifactId())));
        }
    }

    private Artifact resolveSourceForArtifact(Artifact artifact) {
        Object artifactRequest = populateSourceRequest(artifact);
        Object artifactResult = aether.resolveArtifact(repositorySystem, repositorySystemSession, artifactRequest);
        return aether.translateArtifactAetherToGeneric(artifactResult);
    }

    private Object resolveNoTransitive(String artifact) {
        Object artifactRequest = populateArtifactRequest(artifact);
        return aether.resolveArtifact(repositorySystem, repositorySystemSession, artifactRequest);
    }

    private List<?> resolveWithTransitive(String artifact, List<String> excludes) {
        Object collectRequest = populateCollectRequest(artifact);
        Object dependencyNode = aether.getRootDependencyNode(repositorySystem, repositorySystemSession, collectRequest);
        Object dependencyRequest = aether.newDependencyRequest(dependencyNode, null);
        aether.setFilter(dependencyRequest, getFilter(artifact, transformExcludes(artifact, excludes)));
        aether.resolveDependencies(repositorySystem, repositorySystemSession, dependencyRequest);
        Object preorderNodeListGenerator = aether.newPreorderNodeListGenerator();
        aether.acceptDependencyNodeGenerator(dependencyNode, preorderNodeListGenerator);
        return aether.getGeneratorArtifacts(preorderNodeListGenerator);
    }

    private Object getFilter(final String artifactName, List<String> excludes) {
        BiPredicate<Object, List<?>> filter = aether.newPatternExclusionsDependencyFilter(excludes);
        BiPredicate<Object, List<?>> filterClosure = (node, parents) -> {
            boolean accepted = filter.test(node, parents);
            if (!accepted) {
                Object artifact = aether.getDependencyNodeArtifact(node);
                Artifact a = aether.translateArtifactAetherToGeneric(artifact);
                String pattern = MessageFormat.format("{0}:{1}:{2}", a.getGroupId(), a.getArtifactId(), a.getBaseVersion()); //$NON-NLS-1$
                if (pattern == artifactName) {
                    return true;
                }
            }
            return accepted;
        };
        return aether.newDependencyFilter(filterClosure);
    }

    private static List<String> transformExcludes(String artifact, List<String> excludes) {
        List<String> transformedExcludes = new ArrayList<>();
        for (String exclude : excludes) {
            if (StringUtils.isBlank(exclude)) {
                // aether bug fix
                Logger.getLog().warn(MessageFormat.format("Empty exclude counts as exclude-all wildcard ''*'' {0}", artifact));
                transformedExcludes.add("*"); //$NON-NLS-1$
            } else {
            	transformedExcludes.add(exclude);
            }
        }
        return transformedExcludes;
    }


    private Object populateCollectRequest(String artifact) {
        Object collectRequest = aether.newCollectRequest();
        for (Object remoteRepository : remoteRepositories) {
        	aether.addDependency(collectRequest, remoteRepository);
        }
        aether.addDependency(collectRequest, aether.newDependency(aether.newDefaultArtifact(artifact), scope));
        return collectRequest;
    }

    private Object populateArtifactRequest(String artifact) {
        Object artifactRequest = populateRepos(aether.newArtifactRequest());
        aether.setArtifactRequestArtifact(artifactRequest, artifact);
        return artifactRequest;
    }

    private Object populateSourceRequest(Artifact artifact) {
        Object artifactRequest = populateRepos(aether.newArtifactRequest());
        Object aetherArtifact = aether.translateArtifactGenericToAether(artifact);
        Object sourceArtifact = aether.newSubArtifact(aetherArtifact, "sources", "jar"); //$NON-NLS-1$ //$NON-NLS-2$
        aether.setArtifactRequestArtifact(artifactRequest, sourceArtifact);
        return artifactRequest;
    }

    private Object populateRepos(Object artifactRequest) {
        for (Object  remoteRepository : remoteRepositories) {
        	aether.addArtifactRequestRepository(artifactRequest, remoteRepository);
        }
        return artifactRequest;
    }

    private List<Artifact> translateArtifactsAetherToGeneric(List<?> artifacts) {
    	return artifacts.stream()
    		.map(aether::translateArtifactAetherToGeneric)
    		.collect(Collectors.toList());
    }

}
