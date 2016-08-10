package com.lazan.gradlemavenshare;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class MavenShareRootPlugin implements Plugin<Project> {
	private static class SubProjectModel {
		Project project;
		ResolvedPom pom;
		public SubProjectModel(Project project, ResolvedPom pom) {
			super();
			this.project = project;
			this.pom = pom;
		}
	}

	public void apply(Project project) {
		project.getExtensions().create("mavenShareRoot", MavenShareRootModel.class);
		project.afterEvaluate(new Action<Project>() {
			@Override
			public void execute(Project arg0) {
				List<SubProjectModel> subModels = parsePoms(project);
				ProjectResolver resolver = createProjectResolver(subModels);
				for (SubProjectModel subModel : subModels) {
					MavenShareModel shareModel = subModel.project.getExtensions().getByType(MavenShareModel.class);
					for (ShareAction shareAction : shareModel.getShareActions()) {
						shareAction.execute(subModel.pom, subModel.project, resolver);
					}
				}
			}
		});
	}
	
	protected List<SubProjectModel> parsePoms(Project rootProject) {
		PomResolver pomResolver = new PomResolver();
		PomResolveCache cache = new PomResolveCache();
		List<SubProjectModel> subModels = new ArrayList<>();
		MavenShareRootModel rootModel = rootProject.getExtensions().getByType(MavenShareRootModel.class);
		for (Project subproject : rootProject.getAllprojects()) {
			if (subproject.getPlugins().hasPlugin(MavenSharePlugin.class)) {
				MavenShareModel msm = subproject.getExtensions().getByType(MavenShareModel.class);
				File pomFile = subproject.file(msm.getPomFile());
				ResolvedPom pom = pomResolver.resolvePom(pomFile, cache, rootModel.getPomSource());
				subModels.add(new SubProjectModel(subproject, pom));
			}
		}
		return subModels;
	}
	
	protected ProjectResolver createProjectResolver(List<SubProjectModel> subModels) {
		Map<String, Project> map = new LinkedHashMap<>();
		for (SubProjectModel subModel : subModels) {
			ResolvedPom pom = subModel.pom;
			String gav = createGav(pom.getGroupId(), pom.getArtifactId(), pom.getVersion());
			if (map.containsKey(gav)) throw new RuntimeException("Duplicate GAV " + gav);
			map.put(gav, subModel.project);
		}
		return new ProjectResolver() {
			public boolean isProject(Dependency dependency) {
				String gav = createGav(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
				return map.containsKey(gav);
			}
			public Project getProject(Dependency dependency) {
				String gav = createGav(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
				Project project = map.get(gav);
				if (gav == null) {
					throw new RuntimeException(gav + " is not a project dependency");
				}
				return project;
			}
		};
	}

	protected String createGav(String group, String artifact, String version) {
		String gav = String.format("%s:%s:%s", group, artifact, version);
		if (group == null || artifact == null || version == null) {
			throw new RuntimeException("Illegal gav " + gav);
		}
		return gav;
	}
}