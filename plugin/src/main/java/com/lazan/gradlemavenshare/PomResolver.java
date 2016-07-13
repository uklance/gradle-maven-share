package com.lazan.gradlemavenshare;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

public class PomResolver {
	private MavenXpp3Reader mavenReader = new MavenXpp3Reader();

	public ResolvedPom resolvePom(File pomFile, PomResolveCache cache, PomSource pomSource) {
		try (InputStream in = new FileInputStream(pomFile)) {
			return resolvePom(in, pomFile, cache, pomSource);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected ResolvedPom resolvePom(InputStream in, File pomFile, PomResolveCache cache, PomSource pomSource) throws Exception {
		ResolvedPom resolved = null;
		if (pomFile != null) {
			resolved = cache.getResolvedPom(pomFile);
		}
		if (resolved == null) {
			Model model = mavenReader.read(in);
			ResolvedPom parent = resolveParent(model, pomFile, cache, pomSource);
			resolved = new ResolvedPom(pomFile, parent, model);
			cache.addResolvedPom(resolved);
		}
		return resolved;
	}
	
	protected ResolvedPom resolveParent(Model model, File pomFile, PomResolveCache cache, PomSource pomSource) throws Exception {
		Parent rawParent = model.getParent();
		if (rawParent != null) {
			if (rawParent.getGroupId() == null || rawParent.getArtifactId() == null || rawParent.getVersion() == null) {
				throw new RuntimeException("Invalid parent for artifact " + createGav(model));
			}
			ResolvedPom parent = cache.getResolvedPom(rawParent.getGroupId(), rawParent.getArtifactId(), rawParent.getVersion());
			if (parent == null) {
				if (pomFile != null && rawParent.getRelativePath() == null) {
					File parentPomFile = new File(pomFile.getParentFile(), rawParent.getRelativePath());
					if (!parentPomFile.isFile()) {
						parentPomFile = new File(parentPomFile, "pom.xml");
					}
					parent = resolvePom(parentPomFile, cache, pomSource);
				} else {
					try (
						InputStream parentIn = pomSource.getPom(rawParent.getGroupId(), rawParent.getArtifactId(), rawParent.getVersion());
					) {
						parent = resolvePom(parentIn, null, cache, pomSource);
					}
				}
				cache.addResolvedPom(parent);
			}
			return parent;
		}
		return null;
	}

	protected String createGav(Parent parent) {
		return String.format("%s:%s:%s", parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
	}

	protected String createGav(Model model) {
		return String.format("%s:%s:%s", model.getGroupId(), model.getArtifactId(), model.getVersion());
	}
}
