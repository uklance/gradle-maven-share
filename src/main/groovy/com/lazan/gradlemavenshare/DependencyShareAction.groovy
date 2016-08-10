package com.lazan.gradlemavenshare;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.maven.model.Dependency
import org.apache.maven.model.Exclusion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public class DependencyShareAction implements ShareAction {
	@Override
	public void execute(ResolvedPom pom, Project subproject, ProjectResolver resolver) {
		MavenShareRootModel rootModel = subproject.getRootProject().getExtensions().getByType(MavenShareRootModel.class);
		MavenShareModel msm = subproject.getExtensions().getByType(MavenShareModel.class);
		ConfigurationResolver configResolver = msm.getConfigurationResolver();
		for (Dependency dep : pom.getDependencies()) {
			if (msm.excludes.find { Map attributes -> dependencyMatches(dep, attributes) }) {
				continue
			}
			Closure depClosure = null;
			if (dep.exclusions) {
				depClosure = {
					for (Exclusion exclusion : dep.exclusions) {
						Map exMap = [:]
						if (exclusion.groupId) {
							exMap['group'] = exclusion.groupId
						}
						if (exclusion.artifactId) {
							exMap['module'] = exclusion.artifactId
						}
						exclude exMap
					}    
				}
			}
			Configuration config = configResolver.getConfiguration(subproject, dep)
			Map resolveConfig = msm.resolvers.find { dependencyMatches(dep, it.attributes) }
			
			Object depNotation
			if (resolveConfig) {
				// custom DependencyResolver
				DependencyResolver depResolver = resolveConfig.resolver
				depNotation = depResolver.resolve(subproject, dep, resolver)
			} else if (resolver.isProject(dep)) {
				// local project dependency
				Project depProject = resolver.getProject(dep)
				validateProjectDependency(rootModel, dep, depProject)
				depNotation = subproject.project(depProject.path)
			} else {
				// external dependency
				validateExternalDependency(rootModel, dep)
				depNotation = [group: dep.groupId, name: dep.artifactId, version: dep.version]
				if (dep.classifier) {
				    depNotation['classifier'] = dep.classifier
				}
			}
			Object gradleDep = subproject.dependencies.create(depNotation, depClosure)
			config.dependencies.add(gradleDep)
		}
	}
	
	protected boolean dependencyMatches(Dependency dep, Map<String, String> attributes) {
		Map<String, String> depMap = [:]
		if (attributes.containsKey('groupId')) depMap['groupId'] = dep.groupId
		if (attributes.containsKey('artifactId')) depMap['artifactId'] = dep.artifactId
		if (attributes.containsKey('version')) depMap['version'] = dep.version
		if (attributes.containsKey('classifier')) depMap['classifier'] = dep.classifier
		if (attributes.containsKey('systemPath')) depMap['systemPath'] = dep.systemPath
		if (attributes.containsKey('type')) depMap['type'] = dep.type ?: 'jar'
		if (attributes.containsKey('scope')) depMap['scope'] = dep.scope ?: 'compile'
		return depMap.equals(attributes);
	}
	
	private static final String VALIDATE_MSG_TEMPLATE = "%s='%s' not supported for %s. Please exclude the dependency or provide a custom DependencyResolver";
	
	protected void validateExternalDependency(MavenShareRootModel rootModel, Dependency dep) {
		if (!rootModel.isAllowUnsupportedDependencyProperties()) {
			String type = "jar".equals(dep.getType()) ? null : dep.getType();
			if (type != null) {
				throw new RuntimeException(String.format(VALIDATE_MSG_TEMPLATE, "type", type, createGav(dep)));
			}
			if (dep.systemPath != null) {
				throw new RuntimeException(String.format(VALIDATE_MSG_TEMPLATE, "systemPath", dep.systemPath, createGav(dep)));
			}
		}
	}

	protected void validateProjectDependency(MavenShareRootModel rootModel, Dependency dep, Project depProject) {
		if (!rootModel.isAllowUnsupportedDependencyProperties()) {
			String type = "jar".equals(dep.getType()) ? null : dep.getType();
			if (type != null) {
				throw new RuntimeException(String.format(VALIDATE_MSG_TEMPLATE, "type", type, createGav(dep)));
			}
			if (dep.systemPath != null) {
				throw new RuntimeException(String.format(VALIDATE_MSG_TEMPLATE, "systemPath", dep.systemPath, createGav(dep)));
			}
			if (dep.classifier != null) {
				throw new RuntimeException(String.format(VALIDATE_MSG_TEMPLATE, "classifier", dep.classifier, createGav(dep)));
			}
		}
	}
	
	protected String createGav(Dependency dep) {
		String gav = String.format("%s:%s:%s", dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
		if (dep.getGroupId() == null || dep.getArtifactId() == null || dep.getVersion() == null) {
			throw new RuntimeException("Illegal gav " + gav);
		}
		return gav;
	}
}
