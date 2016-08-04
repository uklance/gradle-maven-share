package com.lazan.gradlemavenshare;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public class DefaultConfigurationResolver implements ConfigurationResolver {
	private static final Map<String, String> mappings = Collections.unmodifiableMap(initMappings());
	
	private static Map<String, String> initMappings() {
		Map<String, String> mappings = new HashMap<>();
		mappings.put("test",  "testCompile");
		mappings.put("compile", "compile");
		mappings.put("provided", "compileOnly");
		mappings.put("runtime", "runtime");
		return mappings;
	}
	
	@Override
	public Configuration getConfiguration(Project project, Dependency dependency) {
		String mavenScope = dependency.getScope() == null ? "compile" : dependency.getScope();
		String gradleScope = mappings.containsKey(mavenScope) ? mappings.get(mavenScope) : mavenScope;
		return project.getConfigurations().maybeCreate(gradleScope);
	}
}
