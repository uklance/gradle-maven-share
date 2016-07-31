package com.lazan.gradlemavenshare

class MavenShareModel {
	Object pomFile
	ConfigurationResolver configurationResolver
	List<ShareAction> beforeShare = []
	List<ShareAction> afterShare = []
	Set<String> excludes = [] as Set
	
	void beforeShare(ShareAction action) {
		beforeShare << action
	}

	void afterShare(ShareAction action) {
		afterShare << action
	}
	
	void exclude(String gav) {
		excludes.add(gav)
	}
}