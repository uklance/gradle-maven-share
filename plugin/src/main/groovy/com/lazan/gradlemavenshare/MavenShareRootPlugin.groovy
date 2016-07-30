package com.lazan.gradlemavenshare

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.apache.maven.model.Model
import org.apache.maven.model.Dependency
import org.apache.maven.model.Exclusion
import org.gradle.api.artifacts.Configuration

class MavenShareRootPlugin implements Plugin<Project> {
	void apply(Project project) {
		MavenShareRootModel rootModel = project.extensions.create('mavenShareRoot', MavenShareRootModel)
		project.afterEvaluate {
			PomResolver pomResolver = new PomResolver()
			PomResolveCache cache = new PomResolveCache()
			
			Map<String, SubProjectModel> subModelsByGav = [:]
			project.subprojects { Project subproject ->
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
			subModelsByGav.each { String key, SubProjectModel subModel ->
				Project subproject = subModel.project
				ResolvedPom pom = subModel.pom
				MavenShareModel msm = subproject.mavenShare
				ConfigurationResolver configResolver = msm.configurationResolver
				if (!configResolver) {
					configResolver = { Dependency dep ->
						return subproject.configurations.maybeCreate(dep.scope ?: 'compile')
					} as ConfigurationResolver
				}
				for (Dependency dep : pom.dependencies) {
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
								println "   exclude ${exMap}"
							}    
						}
					}
					Configuration config = configResolver.getConfiguration(dep)
					String gav = "${dep.groupId}:${dep.artifactId}:${dep.version}"
					SubProjectModel gavModel = subModelsByGav[gav]
					String depNotation
					if (gavModel) {
						depNotation = subproject.dependencies.project(path: gavModel.project.path)
					} else {
						depNotation = gav
					}

					println "${subproject.name} Adding ${dep.groupId}:${dep.artifactId}:${dep.version} to ${config.name} [$gradleDep]"
					Object gradleDep
					if (depClosure != null) {
						gradleDep = subproject.dependencies.create(depNotation, depClosure)
					} else {
						gradleDep = subproject.dependencies.create(depNotation)
					}						 
					config.dependencies.add(gradleDep)
				}
			}
		}
	}

	static class SubProjectModel {
		Project project
		ResolvedPom pom
	}
}


