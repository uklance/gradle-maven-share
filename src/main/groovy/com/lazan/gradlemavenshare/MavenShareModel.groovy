package com.lazan.gradlemavenshare

class MavenShareModel {
	private static final Set<String> ATTRIBUTE_NAMES = ['groupId', 'artifactId', 'version', 'type', 'scope', 'classifier'] as Set
	
	Object pomFile
	ConfigurationResolver configurationResolver
	List<ShareAction> beforeShare = []
	List<ShareAction> afterShare = []
	List<Map> excludes = []
	List<Map> resolvers = []
	
	void beforeShare(ShareAction action) {
		beforeShare << action
	}

	void afterShare(ShareAction action) {
		afterShare << action
	}
	
	void exclude(Map attributes) {
		validateAttributes(attributes)
		excludes << attributes
	}
	
	void resolve(Map attributes, DependencyResolver resolver) {
		validateAttributes(attributes)
		resolvers << [attributes: attributes, resolver: resolver]
	}
	
	private void validateAttributes(Map attributes) {
		attributes.keySet().each { String key ->
			if (!ATTRIBUTE_NAMES.contains(key)) {
				throw new RuntimeException("Unsupported attribute $key $ATTRIBUTE_NAMES")
			}
		}
	}
}