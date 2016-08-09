package com.lazan.gradlemavenshare

import org.apache.maven.model.Dependency
import org.apache.maven.model.Exclusion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

import com.lazan.gradlemavenshare.MavenShareRootPlugin.SubProjectModel;

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
			ProjectResolver resolver = createProjectResolver(subModelsByGav)
			for (SubProjectModel subModel : subModelsByGav.values()) {
				MavenShareModel shareModel = subModel.project.mavenShare
				for (ShareAction shareAction : shareModel.shareActions) {
					shareAction.execute(subModel.pom, subModel.project, resolver)
				}
			}
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
	
	
	protected String getGav(Dependency dependency) {
		return "$dependency.groupId:$dependency.artifactId:$dependency.version".toString()
	}
	
	protected ProjectResolver createProjectResolver(Map<String, SubProjectModel> subModelsByGav) {
		return new ProjectResolver() {
			boolean isProjectDependency(Dependency dependency) {
				return subModelsByGav.containsKey(getGav(dependency))
			}
			Project getProject(Dependency dependency) {
				String gav = getGav(dependency)
				SubProjectModel subModel= subModelsByGav[gav]
				if (subModel == null) {
					throw new RuntimeException("$gav is not a project dependency")
				}
				return subModel.project
			}
		}
	}
}