package com.lazan.gradlemavenshare

import java.util.HashMap
import java.util.Iterator
import java.util.Map

import org.apache.maven.model.Dependency
import org.apache.maven.model.Exclusion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.publish.maven.MavenDependency

public class DependencyShareAction implements ShareAction {
	private final DependencyResolver DEFAULT_DEPENDENCY_RESOLVER = new DefaultDependencyResolver()
	
	@Override
	public void execute(ResolvedPom pom, Project project, ProjectResolver resolver) {
		MavenShareRootModel rootModel = project.getRootProject().getExtensions().getByType(MavenShareRootModel.class)
		MavenShareModel msm = project.getExtensions().getByType(MavenShareModel.class)
		ConfigurationResolver configResolver = msm.getConfigurationResolver()
		for (Dependency dep : pom.getDependencies()) {
			if (msm.excludes.find { Map attributes -> dependencyMatches(dep, attributes) }) {
				continue
			}
			Configuration config = configResolver.getConfiguration(project, dep)
			Map resolveConfig = msm.resolvers.find { dependencyMatches(dep, it.attributes) }
			DependencyResolver dependencyResolver = resolveConfig?.resolver ?: DEFAULT_DEPENDENCY_RESOLVER
			Object depNotation = dependencyResolver.resolve(project, dep, resolver)
			Object gradleDep = project.dependencies.create(depNotation, createExclusionClosure(dep))
			config.dependencies.add(gradleDep)
		}
	}
	
	protected boolean dependencyMatches(Dependency dep, Map<String, String> attributes) {
		Map<String, String> depMap = [:]
		if (attributes.containsKey('groupId')) depMap['groupId'] = dep.groupId
		if (attributes.containsKey('artifactId')) depMap['artifactId'] = dep.artifactId
		if (attributes.containsKey('version')) depMap['version'] = dep.version
		if (attributes.containsKey('classifier')) depMap['classifier'] = dep.classifier
		if (attributes.containsKey('systemPath')) depMap['systemPath'] = dep.systemPath
		if (attributes.containsKey('type')) depMap['type'] = dep.type ?: 'jar'
		if (attributes.containsKey('scope')) depMap['scope'] = dep.scope ?: 'compile'
		return depMap.equals(attributes)
	}
	
	protected Closure createExclusionClosure(Dependency mavenDep) {
		if (!mavenDep.exclusions) return null
		return { 
			for (Exclusion exclusion : mavenDep.exclusions) {
				Map exMap = [:]
				if (exclusion.groupId) {
					exMap['group'] = exclusion.groupId
				}
				if (exclusion.artifactId) {
					exMap['module'] = exclusion.artifactId
				}
				exclude exMap
			}    
		}
	}
}
