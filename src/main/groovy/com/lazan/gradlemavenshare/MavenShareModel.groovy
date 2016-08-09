package com.lazan.gradlemavenshare

class MavenShareModel {
	private static final Set<String> ATTRIBUTE_NAMES = ['groupId', 'artifactId', 'version', 'type', 'scope', 'classifier', 'systemPath'] as Set
	
	Object pomFile
	ConfigurationResolver configurationResolver
	List<ShareAction> shareActions = [new DependencyShareAction()]
	List<Map> excludes = []
	List<Map> resolvers = []
	
	void doFirst(ShareAction action) {
		shareActions.add(0, action)
	}

	void doLast(ShareAction action) {
		shareActions << action
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