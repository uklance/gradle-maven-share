package com.lazan.gradlemavenshare;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class PomResolveCache {
	private final Map<String, ResolvedPom> pomsByGav = new LinkedHashMap<>();
	private final Map<File, ResolvedPom> pomsByFile = new LinkedHashMap<>();
	
	public void addResolvedPom(ResolvedPom pom) {
		if (pom.getPomFile() != null) {
			File pomFile;
			try {
				pomFile = pom.getPomFile().getCanonicalFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if (pomsByFile.containsKey(pomFile)) throw new RuntimeException("Duplicate pom file " + pomFile);
			pomsByFile.put(pomFile, pom);
		}
		String gav = createGav(pom.getGroupId(), pom.getArtifactId(), pom.getVersion());
		if (pomsByGav.containsKey(gav)) throw new RuntimeException("Duplicate gav " + gav);
		pomsByGav.put(gav, pom);
	}
	
	protected String createGav(String group, String artifact, String version) {
		String gav = String.format("%s:%s:%s", group, artifact, version);
		if (group == null || artifact == null || version == null) {
			throw new RuntimeException("Illegal gav " + gav);
		}
		return gav;
	}
	
	public ResolvedPom getResolvedPom(File pomFile) {
		try {
			return pomsByFile.get(pomFile.getCanonicalFile());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public ResolvedPom getResolvedPom(String group, String artifact, String version) {
		return pomsByGav.get(createGav(group, artifact, version));
	}
}
