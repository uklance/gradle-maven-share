package com.lazan.gradlemavenshare;

import org.gradle.api.Project;

public interface ShareAction {
	void execute(ResolvedPom pom, Project project, ProjectResolver resolver);
}
