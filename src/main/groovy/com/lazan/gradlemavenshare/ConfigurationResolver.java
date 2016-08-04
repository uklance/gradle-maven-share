package com.lazan.gradlemavenshare;

import org.apache.maven.model.Dependency;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public interface ConfigurationResolver {
	Configuration getConfiguration(Project project, Dependency dependency);
}
