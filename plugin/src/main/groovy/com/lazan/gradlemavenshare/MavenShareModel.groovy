package com.lazan.gradlemavenshare

import org.gradle.api.Action

class MavenShareModel {
	Object pomFile
	ConfigurationResolver configurationResolver
	List<Action<ResolvedPom>> beforeShare = []
	List<Action<ResolvedPom>> afterShare = []
	
	void beforeShare(Action<ResolvedPom> action) {
		beforeShare << action
	}

	void afterShare(Action<ResolvedPom> action) {
		afterShare << action
	}
}