package com.lazan.gradlemavenshare

import org.gradle.api.*
import org.gradle.api.tasks.compile.JavaCompile

class MavenSharePlugin implements Plugin<Project> {
	void apply(Project project) {
		MavenShareModel shareModel = project.extensions.create('mavenShare', MavenShareModel)
		Project rootProject = project.rootProject 
		rootProject.apply(plugin: 'com.lazan.gradlemavenshareroot')
		
		project.afterEvaluate {
			MavenShareRootTask rootTask = rootProject.tasks.getByName('mavenShareRoot')
			for (String taskName : shareModel.dependentTasks) {
				Task compileTask = project.tasks.findByName(taskName)
				if (!compileTask) {
					project.logger.warn("Could not wire mavenShare into the DAG for $project.name (No such task '$taskName')")
				} else {
					compileTask.dependsOn rootTask
				}
			}
		}
	}
}