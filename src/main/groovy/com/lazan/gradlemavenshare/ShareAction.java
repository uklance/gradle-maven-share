package com.lazan.gradlemavenshare;

import org.gradle.api.Project;

public interface ShareAction {
	void execute(ResolvedPom mavenPom, Project gradleProject);
}
