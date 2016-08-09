package com.lazan.gradlemavenshare;

import org.apache.maven.model.Dependency
import org.apache.maven.model.Exclusion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.GradleException

public class DependencyShareAction implements ShareAction {
	@Override
	public void execute(ResolvedPom pom, Project subproject, ProjectResolver resolver) {
		MavenShareRootModel rootModel = subproject.rootProject.mavenShareRoot
		MavenShareModel msm = subproject.mavenShare
		ConfigurationResolver configResolver = msm.configurationResolver ?: new DefaultConfigurationResolver()
		for (Dependency dep : pom.dependencies) {
			String depGav = "${dep.groupId}:${dep.artifactId}:${dep.version}".toString()
			if (msm.excludes.find { Map attributes -> dependencyMatches(dep, attributes) }) {
				continue
			}
			Closure depClosure = null
			if (dep.exclusions) {
				depClosure = {
					for (Exclusion exclusion : dep.exclusions) {
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
			Configuration config = configResolver.getConfiguration(subproject, dep)
			Map resolveConfig = msm.resolvers.find { dependencyMatches(dep, it.attributes) }
			
			Object depNotation
			if (resolveConfig) {
				// custom DependencyResolver
				DependencyResolver depResolver = resolveConfig.resolver
				depNotation = depResolver.resolve(subproject, dep, resolver)
			} else if (resolver.isProject(dep)) {
				// local project dependency
				Project depProject = resolver.getProject(dep)
				validateProjectDependency(rootModel, dep, depProject)
				depNotation = subproject.project(depProject.path)
			} else {
				// external dependency
				validateExternalDependency(rootModel, dep)
				depNotation = [group: dep.groupId, name: dep.artifactId, version: dep.version]
				if (dep.classifier) {
				    depNotation['classifier'] = dep.classifier
				}
			}
			Object gradleDep = subproject.dependencies.create(depNotation, depClosure)
			config.dependencies.add(gradleDep)
		}
	}

	protected boolean dependencyMatches(Dependency dep, Map attributes) {
		if (attributes.groupId != null && attributes.groupId.toString() != dep.groupId) {
			return false
		}
		if (attributes.artifactId != null && attributes.artifactId.toString() != dep.artifactId) {
			return false
		}
		if (attributes.version != null && attributes.version.toString() != dep.version) {
			return false
		}
		if (attributes.scope != null && attributes.scope.toString() != (dep.scope ?: 'compile')) {
			return false
		}
		if (attributes.type != null && attributes.type.toString() != dep.type) {
			return false
		}
		if (attributes.classifier != null && attributes.classifier.toString() != dep.classifier) {
			return false
		}
		return true
	}
	
	private static final Set<String> INVALID_EXTERNAL_DEPENDENCY_PROPERTIES = ['type', 'systemPath'] as Set
	
	protected void validateExternalDependency(MavenShareRootModel rootModel, Dependency dep) {
		if (!rootModel.allowUnsupportedDependencyProperties) {
			INVALID_EXTERNAL_DEPENDENCY_PROPERTIES.each { propName ->
				String propValue = propName == 'type' && dep.type == 'jar' ? null : dep[propName]
				if (propValue) {
					throw new GradleException("$propName=$propValue not supported for external dependency $dep.groupId:$dep.artifactId:$dep.version. Either exclude the dependency or provide a custom DependencyResolver")
				}
			}
		}
	}

	private static final Set<String> INVALID_PROJECT_DEPENDENCY_PROPERTIES = ['type', 'systemPath', 'classifier'] as Set

	protected void validateProjectDependency(MavenShareRootModel rootModel, Dependency dep, Project depProject) {
		if (!rootModel.allowUnsupportedDependencyProperties) {
			INVALID_PROJECT_DEPENDENCY_PROPERTIES.each { propName ->
				String propValue = propName == 'type' && dep.type == 'jar' ? null : dep[propName]
				if (propValue) {
					throw new GradleException("$propName=$propValue not supported for local project depdendency $depProject.path. Either exclude the dependency or provide a custom DependencyResolver")
				}
			}
		}
	}
}
