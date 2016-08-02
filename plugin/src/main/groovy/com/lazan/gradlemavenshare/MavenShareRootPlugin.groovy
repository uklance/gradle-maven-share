package com.lazan.gradlemavenshare

import org.apache.maven.model.Dependency
import org.apache.maven.model.Exclusion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

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
			addMavenDependencies(subModelsByGav)
			afterShare(subModelsByGav)
		}
	}
	
	protected Map<String, SubProjectModel> getSubModelsByGav(Project project) {
		PomResolver pomResolver = new PomResolver()
		PomResolveCache cache = new PomResolveCache()
		Map<String, SubProjectModel> subModelsByGav = [:]
		project.subprojects { Project subproject ->
			if (subproject.plugins.hasPlugin(MavenSharePlugin)) {
				SubProjectModel subModel = new SubProjectModel()
				MavenShareModel msm = subproject.mavenShare
				File pomFile = subproject.file(msm.pomFile == null ? 'pom.xml' : msm.pomFile)
				ResolvedPom pom = pomResolver.resolvePom(pomFile, cache, project.mavenShareRoot.pomSource)
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
	
	protected void addMavenDependencies(Map<String, SubProjectModel> subModelsByGav) { 
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
					depNotation = resolveConfig.resolver.resolve(subproject, dep, depSubModel?.project)
				} else if (depSubModel) {
					depNotation = subproject.project(depSubModel.project.path)
				} else {
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

	protected void beforeShare(Map<String, SubProjectModel> subModelsByGav) { 
		subModelsByGav.values().each { SubProjectModel subModel ->
			subModel.project.mavenShare.beforeShare.each { ShareAction action ->
				action.execute(subModel.pom, subModel.project)
			}
		}
	}

	protected void afterShare(Map<String, SubProjectModel> subModelsByGav) { 
		subModelsByGav.values().each { SubProjectModel subModel ->
			subModel.project.mavenShare.afterShare.each { ShareAction action ->
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