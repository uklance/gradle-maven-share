package com.lazan.gradlemavenshare

import org.gradle.api.*

class MavenSharePlugin implements Plugin<Project> {
	void apply(Project project) {
		project.extensions.create('mavenShare', MavenShareModel)
		project.rootProject.apply(plugin: MavenShareRootPlugin)
	}
}