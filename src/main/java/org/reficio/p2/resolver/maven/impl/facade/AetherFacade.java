package org.reficio.p2.resolver.maven.impl.facade;

import java.util.List;
import java.util.function.BiPredicate;

import org.reficio.p2.resolver.maven.Artifact;

/**
 * @author Tom Bujok (tom.bujok@gmail.com)<br>
 *         Reficio (TM) - Reestablish your software!<br>
 *         http://www.reficio.org
 * @since 1.1.0
 */
public interface AetherFacade {

	Object newDependencyRequest(Object dependencyNode, Object dependencyFilter);

	Object newPreorderNodeListGenerator();

	Object newCollectRequest();

    Object newDependency(Object defaultArtifact, String scope);

    Object newDefaultArtifact(String artifact);

    Object newArtifactRequest();

    Object newSubArtifact(Object artifact, String classifier, String extension);

    BiPredicate<Object, List<?>> newPatternExclusionsDependencyFilter(List<String> excludes);

    Object newDependencyFilter(BiPredicate<Object, List<?>> filterClosure);

    Artifact translateArtifactAetherToGeneric(Object artifact);

    Object translateArtifactGenericToAether(Artifact artifact);
    
    void addRepository(Object collectRequest, Object repository);
    
    void addDependency(Object collectRequest, Object dependency);
    
    void addArtifactRequestRepository(Object artifactRequest, Object repository);

    void setArtifactRequestArtifact(Object artifactRequest, Object artifact);
    
    Object resolveArtifact(Object repositorySystem, Object repositorySystemSession, Object artifactRequest);
    
    void setFilter(Object dependencyRequest, Object dependencyFilter);
    
    Object getRootDependencyNode(Object repositorySystem, Object repositorySystemSession, Object collectRequest);
    
    Object resolveDependencies(Object repositorySystem, Object repositorySystemSession, Object dependencyRequest);
    
    void acceptDependencyNodeGenerator(Object dependencyNode, Object generator);
    
    List<?> getGeneratorArtifacts(Object generator);
    
    Object getDependencyNodeArtifact(Object dependencyNode);
    
    String getRepositoryType(Object repository);
    
    String getRepositoryUrl(Object repository);
}
