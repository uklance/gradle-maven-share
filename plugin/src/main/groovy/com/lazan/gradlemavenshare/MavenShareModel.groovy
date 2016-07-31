package com.lazan.gradlemavenshare

class MavenShareModel {
	Object pomFile
	ConfigurationResolver configurationResolver
	List<ShareAction> beforeShare = []
	List<ShareAction> afterShare = []
	
	void beforeShare(ShareAction action) {
		beforeShare << action
	}

	void afterShare(ShareAction action) {
		afterShare << action
	}
}