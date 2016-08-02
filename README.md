# gradle-maven-share [![Build Status](https://travis-ci.org/uklance/gradle-maven-share.svg?branch=master)](https://travis-ci.org/uklance/gradle-maven-share) [![Coverage Status](https://coveralls.io/repos/github/uklance/gradle-maven-share/badge.svg?branch=master)](https://coveralls.io/github/uklance/gradle-maven-share?branch=master)

The `gradle-maven-share` plugin parses maven `pom.xml` files and adds the dependencies into the gradle project. It also provides hooks to perform custom [ShareAction](https://github.com/uklance/gradle-maven-share/blob/master/plugin/src/main/groovy/com/lazan/gradlemavenshare/ShareAction.java)s

### Custom pom file
```groovy
apply plugin: 'com.lazan.gradlemavenshare'
mavenShare {
	pomFile 'custom-pom.xml'
}
```

### Custom ShareAction
```groovy
import com.lazan.gradlemavenshare.*
def shareAction = { ResolvedPom mavenPom, Project gradleProject ->
	println "Sharing $mavenPom.artifactId with $gradleProject.name"
} as ShareAction

subprojects {
	apply plugin: 'com.lazan.gradlemavenshare'
	mavenShare {
		beforeShare shareAction
		afterShare shareAction
	}
}
```

### Excluding maven dependencies
```groovy
apply plugin: 'com.lazan.gradlemavenshare'
mavenShare {
	exclude(groupId: 'com.foo', artifactId: 'bar')
	exclude(classifier: 'tests')
	exclude(type: 'test-jar')
}
```

### Custom ConfigurationResolver
```groovy
import com.lazan.gradlemavenshare.*

apply plugin: 'com.lazan.gradlemavenshare'
mavenShare {
	configurationResolver =  { Project project, org.apache.maven.model.Dependency dependency ->
		String scope = dependency.scope ?: 'compile'
		String configName = "foo${scope}"
		return project.configurations.maybeCreate(configName)
	} as ConfigurationResolver
}
```

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
