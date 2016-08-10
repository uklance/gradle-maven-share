package com.lazan.gradlemavenshare;

import java.util.Collections;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class MavenSharePlugin implements Plugin<Project> {
	public void apply(Project project) {
		project.getExtensions().create("mavenShare", MavenShareModel.class);
		project.getRootProject().apply(Collections.singletonMap("plugin", "com.lazan.gradlemavenshareroot"));
	}
}