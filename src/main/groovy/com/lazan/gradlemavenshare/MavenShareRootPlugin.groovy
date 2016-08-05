package com.lazan.gradlemavenshare

import org.apache.maven.model.Dependency
import org.apache.maven.model.Exclusion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.GradleException

class MavenShareRootPlugin implements Plugin<Project> {
	static class SubProjectModel {
		Project project
		ResolvedPom pom
	}

	void apply(Project project) {
		project.extensions.create('mavenShareRoot', MavenShareRootModel)
		project.afterEvaluate {
			Map<String, SubProjectModel> subModelsByGav = getSubModelsByGav(project)
			beforeShare(subModelsByGav)
			shareMavenDependencies(project, subModelsByGav)
			afterShare(subModelsByGav)
		}
	}
	
	protected Map<String, SubProjectModel> getSubModelsByGav(Project project) {
		PomResolver pomResolver = new PomResolver()
		PomResolveCache cache = new PomResolveCache()
		Map<String, SubProjectModel> subModelsByGav = [:]
		MavenShareRootModel rootModel = project.mavenShareRoot
		project.allprojects { Project subproject ->
			if (subproject.plugins.hasPlugin(MavenSharePlugin)) {
				SubProjectModel subModel = new SubProjectModel()
				MavenShareModel msm = subproject.mavenShare
				File pomFile = subproject.file(msm.pomFile == null ? 'pom.xml' : msm.pomFile)
				ResolvedPom pom = pomResolver.resolvePom(pomFile, cache, rootModel.pomSource)
				subModel.project = subproject
				subModel.pom = pom
				String gav = "${pom.groupId}:${pom.artifactId}:${pom.version}"
				if (subModelsByGav.containsKey(gav)) {
					throw new RuntimeException("Duplicate GAV ${gav}")
				}
				subModelsByGav[gav] = subModel
			}
		}
		return subModelsByGav
	}
	
	protected void shareMavenDependencies(Project project, Map<String, SubProjectModel> subModelsByGav) { 
		MavenShareRootModel rootModel = project.mavenShareRoot
		subModelsByGav.each { String subGav, SubProjectModel subModel ->
			Project subproject = subModel.project
			ResolvedPom pom = subModel.pom
			MavenShareModel msm = subproject.mavenShare
			ConfigurationResolver configResolver = msm.configurationResolver ?: new DefaultConfigurationResolver()
			for (Dependency dep : pom.dependencies) {
				String depGav = "${dep.groupId}:${dep.artifactId}:${dep.version}"
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
				SubProjectModel depSubModel = subModelsByGav[depGav]
				Map resolveConfig = msm.resolvers.find { dependencyMatches(dep, it.attributes) }
				
				Object depNotation
				if (resolveConfig) {
					// custom DependencyResolver
					depNotation = resolveConfig.resolver.resolve(subproject, dep, depSubModel?.project)
				} else if (depSubModel) {
					// local project dependency
					dep = validateProjectDependency(rootModel, dep, depSubModel.project)
					depNotation = subproject.project(depSubModel.project.path)
				} else {
					// external dependency
					dep = validateExternalDependency(rootModel, dep)
					depNotation = [group: dep.groupId, name: dep.artifactId, version: dep.version]
					if (dep.classifier) {
					    depNotation['classifier'] = dep.classifier
					}
				}
				Object gradleDep = subproject.dependencies.create(depNotation, depClosure)
				config.dependencies.add(gradleDep)
			}
		}
	}

	private static final Set<String> INVALID_EXTERNAL_DEPENDENCY_PROPERTIES = ['type', 'systemPath'] as Set

	protected Dependency validateExternalDependency(MavenShareRootModel rootModel, Dependency dep) {
		if (!rootModel.allowUnsupportedDependencyProperties) {
			INVALID_EXTERNAL_DEPENDENCY_PROPERTIES.each { propName ->
				String propValue = propName == 'type' && dep.type == 'jar' ? null : dep[propName]
				if (propValue) {
					throw new GradleException("$propName=$propValue not supported for external dependency $dep.groupId:$dep.artifactId:$dep.version. Either exclude the dependency or provide a custom DependencyResolver")
				}
			}
		}
		return dep
	}

	private static final Set<String> INVALID_PROJECT_DEPENDENCY_PROPERTIES = ['type', 'systemPath', 'classifier'] as Set

	protected Dependency validateProjectDependency(MavenShareRootModel rootModel, Dependency dep, Project depProject) {
		if (!rootModel.allowUnsupportedDependencyProperties) {
			INVALID_PROJECT_DEPENDENCY_PROPERTIES.each { propName ->
				String propValue = propName == 'type' && dep.type == 'jar' ? null : dep[propName]
				if (propValue) {
					throw new GradleException("$propName=$propValue not supported for local project depdendency $depProject.path. Either exclude the dependency or provide a custom DependencyResolver")
				}
			}
		}
		return dep
	}

	protected void beforeShare(Map<String, SubProjectModel> subModelsByGav) { 
		subModelsByGav.values().each { SubProjectModel subModel ->
			List<ShareAction> shareActions = subModel.project.mavenShare.beforeShare
			shareActions.each { ShareAction action ->
				action.execute(subModel.pom, subModel.project)
			}
		}
	}

	protected void afterShare(Map<String, SubProjectModel> subModelsByGav) { 
		subModelsByGav.values().each { SubProjectModel subModel ->
			List<ShareAction> shareActions = subModel.project.mavenShare.afterShare
			shareActions.each { ShareAction action ->
				action.execute(subModel.pom, subModel.project)
			}
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
}