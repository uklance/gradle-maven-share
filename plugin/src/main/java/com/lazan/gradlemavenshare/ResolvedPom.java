package com.lazan.gradlemavenshare;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;

public class ResolvedPom {
	private final File pomFile;
	private final ResolvedPom parent;
	private final Model model;
	private final Map<String, String> properties;
	
	public ResolvedPom(File pomFile, ResolvedPom parent, Model model) {
		super();
		this.pomFile = pomFile;
		this.parent = parent;
		this.model = model;
		this.properties = Collections.unmodifiableMap(resolveProperties());
	}

	private Map<String, String> resolveProperties() {
		final Map<String, String> properties = new LinkedHashMap<>();
		properties.put("project.groupId", getGroupId());
		properties.put("project.artifactId", getArtifactId());
		properties.put("project.version", getVersion());
		ResolvedPomVisitor<Void> visitor = new ResolvedPomVisitor<Void>() {
			@Override
			public Void visit(ResolvedPom pom) {
				Model unresolved = pom.getModel();
				if (unresolved.getProperties() != null) {
					for (Map.Entry<Object, Object> entry : unresolved.getProperties().entrySet()) {
						String key = (String) entry.getKey();
						String value = (String) entry.getValue();
						
						if (!properties.containsKey(key)) {
							properties.put(key, value);
						}
					}
				}
				return null;
			}
		};
		visitHierarchy(visitor);
		
		boolean dirtied = true;
		while (dirtied) {
			dirtied = false;
			for (Map.Entry<String, String> entry : properties.entrySet()) {
				String value = entry.getValue();
				String result = substituteProperties(properties, value);
				if (!result.equals(value)) {
					dirtied = true;
					entry.setValue(result);
				}
			}
		}
		return properties;
	}
	
	private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{(.*?)}");

	public String substituteProperties(String value) {
		return substituteProperties(properties, value);
	}
	protected String substituteProperties(Map<String, String> properties, String value) {
		if (value == null) return null;
		
		Matcher matcher = PROPERTY_PATTERN.matcher(value);
		if (!matcher.find()) return value;
		
		StringBuffer buffer = new StringBuffer();
		do {
			String name = matcher.group(1);
			String replacement = properties.get(name);
			if (replacement == null) {
				// leave it as it was
				replacement = matcher.group(0);
			}
			matcher.appendReplacement(buffer,  Matcher.quoteReplacement(replacement));
		} while (matcher.find());
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	public File getPomFile() {
		return pomFile;
	}
	
	public ResolvedPom getParent() {
		return parent;
	}
	
	public String getArtifactId() {
		return model.getArtifactId();
	}
	
	public Model getModel() {
		return model;
	}
	
	public String getGroupId() {
		ResolvedPomVisitor<String> visitor = new ResolvedPomVisitor<String>() {
			public String visit(ResolvedPom pom) {
				return pom.getModel().getGroupId();
			}
		};
		return visitHierarchy(visitor);
	}
	
	public String getVersion() {
		ResolvedPomVisitor<String> visitor = new ResolvedPomVisitor<String>() {
			public String visit(ResolvedPom pom) {
				return pom.getModel().getVersion();
			}
		};
		return visitHierarchy(visitor);
	}
	
	protected <T> T visitHierarchy(ResolvedPomVisitor<T> visitor) {
		ResolvedPom current = this;
		while (current != null) {
			T result = visitor.visit(current);
			if (result != null) {
				return result;
			}
			current = current.getParent();
		}
		return null;
	}
	
	public String getProperty(String name) {
		return properties.get(name);
	}
	
	public Set<String> getPropertynames() {
		return properties.keySet();
	}
	
	
}
