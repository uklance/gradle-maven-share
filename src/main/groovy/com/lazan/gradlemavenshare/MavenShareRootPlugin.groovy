package com.lazan.gradlemavenshare

import org.gradle.api.Plugin
import org.gradle.api.Project

class MavenShareRootPlugin implements Plugin<Project> {
	void apply(Project project) {
		project.tasks.create(name: 'mavenShareRoot', type: MavenShareRootTask)
	}
}