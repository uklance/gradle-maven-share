package com.lazan.gradlemavenshare

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.apache.maven.model.Model
import org.apache.maven.model.Dependency
import org.gradle.api.artifacts.Configuration

class MavenShareRootPlugin implements Plugin<Project> {
	void apply(Project project) {
		project.afterEvaluate {
			Map<String, ResolvedPom> pomsByPath = [:]
			PomResolver pomResolver = new PomResolver()
			PomResolveCache cache = new PomResolveCache()
			
			project.subprojects { Project subproject ->
				if (subproject.plugins.hasPlugin(MavenSharePlugin)) {
					MavenShareModel msm = subproject.mavenShare
					File pomFile = subproject.file(msm.pomFile == null ? 'pom.xml' : msm.pomFile)
					pomsByPath[subproject.path] = pomResolver.resolvePom(pomFile, cache)
				}
			}
			project.subprojects { Project subproject ->
				if (subproject.plugins.hasPlugin(MavenSharePlugin)) {
					ResolvedPom pom = pomsByPath[subproject.path]
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
	}
}
