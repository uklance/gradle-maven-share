# gradle-maven-share [![Build Status](https://travis-ci.org/uklance/gradle-maven-share.svg?branch=master)](https://travis-ci.org/uklance/gradle-maven-share) [![Coverage Status](https://coveralls.io/repos/github/uklance/gradle-maven-share/badge.svg?branch=master)](https://coveralls.io/github/uklance/gradle-maven-share?branch=master)

### Custom DependencyResolver

```groovy
subprojects {
	apply plugin: 'java'
	apply plugin: 'com.lazan.gradlemavenshare'

	configurations {
		testOutput
	}

	dependencies {
		testOutput sourceSets.test.output
	}

	mavenShare {
		def testJarResolver = { Project containingProject, org.apache.maven.model.Dependency mavenDep, Project dependencyProject ->
			if (dependencyProject == null) {
				return [group: mavenDep.groupId, name: mavenDep.artifactId, version: mavenDep.version, classifier: 'tests']
			}
			return containingProject.dependencies.project([path: dependencyProject.path, configuration: 'testOutput'])
		} as com.lazan.gradlemavenshare.DependencyResolver
		
		resolve([groupId: 'com.foo', type: 'test-jar'], testJarResolver)
	}
}
```
