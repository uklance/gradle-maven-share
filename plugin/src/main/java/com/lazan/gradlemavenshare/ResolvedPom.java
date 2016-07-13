package com.lazan.gradlemavenshare;

import java.io.File;

import org.apache.maven.model.Model;

public class ResolvedPom {
	private final File pomFile;
	private final ResolvedPom parent;
	private final Model model;
	
	public ResolvedPom(File pomFile, ResolvedPom parent, Model model) {
		super();
		this.pomFile = pomFile;
		this.parent = parent;
		this.model = model;
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
}
