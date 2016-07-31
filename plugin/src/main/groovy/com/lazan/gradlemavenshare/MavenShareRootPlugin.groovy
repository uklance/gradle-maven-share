package com.lazan.gradlemavenshare

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.apache.maven.model.Model
import org.apache.maven.model.Dependency
import org.apache.maven.model.Exclusion
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
			addMavenDependencies(subModelsByGav)
			//substituteProjects(project, subModelsByGav)
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
				String depGav = "${dep.groupId}:${dep.artifactId}:${dep.version}"
				SubProjectModel depSubModel = subModelsByGav[depGav]
				Object depNotation = depGav
				if (depSubModel) {
					//subproject.evaluationDependsOn(depSubModel.project.path)
					depNotation = subproject.project(depSubModel.project.path)
				}
				Object gradleDep = (depClosure != null) ?
					subproject.dependencies.create(depNotation, depClosure) : subproject.dependencies.create(depNotation)
				config.dependencies.add(gradleDep)
			}
		}
	}
	
	/*
	protected void substituteProjects(Project project, Map<String, SubProjectModel> subModelsByGav) {
		project.configurations.all { Configuration config -> 
			config.resolutionStrategy.dependencySubstitution {
				subModelsByGav.each { String gav, SubProjectModel subModel ->
					println "$config.name substitute module $gav  with project $subModel.project.path)"
					substitute module(gav) with project(subModel.project.path)	
				}
			}
		}
	}
	*/	
}


