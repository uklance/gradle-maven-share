package com.lazan.gradlemavenshare;

import org.apache.maven.model.Dependency;
import org.gradle.api.Project;

public interface ProjectResolver {
	boolean isProjectDependency(Dependency dependency);
	Project getProject(Dependency dependency);
}
