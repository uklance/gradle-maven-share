package com.lazan.gradlemavenshare;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.gradle.api.Project;

public class DefaultDependencyResolver implements DependencyResolver {
	@Override
	public Object resolve(Project project, Dependency dep, ProjectResolver resolver) {
		MavenShareRootModel rootModel = project.getRootProject().getExtensions().getByType(MavenShareRootModel.class);
		if (resolver.isProject(dep)) {
			// local project dependency
			Project depProject = resolver.getProject(dep);
			validateProjectDependency(rootModel, dep, depProject);
			return depProject;
		} else {
			// external dependency
			validateExternalDependency(rootModel, dep);
			Map<String, String> depNotation = new LinkedHashMap<>();
			depNotation.put("group", dep.getGroupId());
			depNotation.put("name", dep.getArtifactId());
			depNotation.put("version", dep.getVersion());
			if (dep.getClassifier() != null) {
			    depNotation.put("classifier", dep.getClassifier());
			}
			return depNotation;
		}
	}

	private static final String VALIDATE_MSG_TEMPLATE = "%s='%s' not supported for %s. Please exclude the dependency or provide a custom DependencyResolver";

	protected void validateExternalDependency(MavenShareRootModel rootModel, Dependency dep) {
		if (!rootModel.isAllowUnsupportedDependencyProperties()) {
			String type = "jar".equals(dep.getType()) ? null : dep.getType();
			if (type != null) {
				throw new RuntimeException(String.format(VALIDATE_MSG_TEMPLATE, "type", type, createGav(dep)));
			}
			if (dep.getSystemPath() != null) {
				throw new RuntimeException(String.format(VALIDATE_MSG_TEMPLATE, "systemPath", dep.getSystemPath(), createGav(dep)));
			}
		}
	}

	protected void validateProjectDependency(MavenShareRootModel rootModel, Dependency dep, Project depProject) {
		if (!rootModel.isAllowUnsupportedDependencyProperties()) {
			String type = "jar".equals(dep.getType()) ? null : dep.getType();
			if (type != null) {
				throw new RuntimeException(String.format(VALIDATE_MSG_TEMPLATE, "type", type, createGav(dep)));
			}
			if (dep.getSystemPath() != null) {
				throw new RuntimeException(String.format(VALIDATE_MSG_TEMPLATE, "systemPath", dep.getSystemPath(), createGav(dep)));
			}
			if (dep.getClassifier() != null) {
				throw new RuntimeException(String.format(VALIDATE_MSG_TEMPLATE, "classifier", dep.getClassifier(), createGav(dep)));
			}
		}
	}

	protected String createGav(Dependency dep) {
		String gav = String.format("%s:%s:%s", dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
		if (dep.getGroupId() == null || dep.getArtifactId() == null || dep.getVersion() == null) {
			throw new RuntimeException("Illegal GAV " + gav);
		}
		return gav;
	}
}
