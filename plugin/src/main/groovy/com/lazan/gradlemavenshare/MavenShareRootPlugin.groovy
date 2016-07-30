package com.lazan.gradlemavenshare

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.apache.maven.model.Model
import org.apache.maven.model.Dependency
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
					Configuration config = configResolver.getConfiguration(dep)
					println "${subproject.name} Adding ${dep.groupId}:${dep.artifactId}:${dep.version} to ${config.name}"
				}
			}
		}
	}

	static class SubProjectModel {
		Project project
		ResolvedPom pom
	}
}


