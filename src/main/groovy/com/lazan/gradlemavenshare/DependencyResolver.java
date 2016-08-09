package com.lazan.gradlemavenshare;

import org.apache.maven.model.Dependency;
import org.gradle.api.Project;

public interface DependencyResolver {
	/**
	 * @param project Gradle project to add the dependency to
	 * @param dependency Maven dependency to resolve
	 * @param resolver Resolves dependencies with Projects where applicable 
	 * @return Dependency notation (Map, String)
	 */
	Object resolve(Project project, Dependency dependency, ProjectResolver resolver);
}
