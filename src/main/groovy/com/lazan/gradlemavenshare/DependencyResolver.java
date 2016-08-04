package com.lazan.gradlemavenshare;

import org.apache.maven.model.Dependency;
import org.gradle.api.Project;

public interface DependencyResolver {
	/**
	 * @param containingProject Gradle project to add the dependency to
	 * @param dependency Maven dependency to resolve
	 * @param dependencyProject Gradle project that matches the dependency (null for external/non-project dependencies)
	 * @return Dependency notation (Map, String)
	 */
	Object resolve(Project containingProject, Dependency dependency, Project dependencyProject);
}
