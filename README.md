# gradle-maven-share [![Build Status](https://travis-ci.org/uklance/gradle-maven-share.svg?branch=master)](https://travis-ci.org/uklance/gradle-maven-share) [![Coverage Status](https://coveralls.io/repos/github/uklance/gradle-maven-share/badge.svg?branch=master)](https://coveralls.io/github/uklance/gradle-maven-share?branch=master)

The `gradle-maven-share` plugin helps with migrating your build from Maven to Gradle. It allows you to have a Maven and Gradle build working in parallel by importing Maven dependencies by into a Gradle build. It's not only dependencies that can be shared, there's hooks to share any configuration from your maven pom.xml's with Gradle via custom [ShareActions](https://github.com/uklance/gradle-maven-share/blob/master/plugin/src/main/groovy/com/lazan/gradlemavenshare/ShareAction.java)

It's recommended to use this plugin as a 'stepping stone' to migrate from Maven to Gradle with the end goal of removing Maven entirely. 

### Custom pom file
By default, `pom.xml` will be used in the project directory but this can be configured
```groovy
apply plugin: 'com.lazan.gradlemavenshare'
apply plugin: 'java'

mavenShare {
	pomFile 'custom-pom.xml'
}
```

### Custom ShareAction
You can mutate the Gradle project model based on the Maven project model using `ShareAction`s. These can be applied before or after the dependencies are shared
```groovy
import com.lazan.gradlemavenshare.*
def shareAction = { ResolvedPom mavenPom, Project gradleProject ->
	println "Sharing $mavenPom.artifactId with $gradleProject.name"
} as ShareAction

subprojects {
	apply plugin: 'java'
	apply plugin: 'com.lazan.gradlemavenshare'
	
	mavenShare {
		beforeShare shareAction
		afterShare shareAction
	}
}
```

### Excluding maven dependencies
You may not wish to share all maven dependencies with gradle, dependencies can be excluded via any maven dependency attributes (groupId, artifactId, version, classifier, type)
```groovy
apply plugin: 'java'
apply plugin: 'com.lazan.gradlemavenshare'

mavenShare {
	exclude(groupId: 'com.foo', artifactId: 'bar')
	exclude(classifier: 'tests')
	exclude(type: 'test-jar')
}
```

### Custom ConfigurationResolver
By default, Gradle will use the maven dependency's `scope` to decide which Gradle Configuration to add the dependecy to

| Maven Scope | Gradle Configuration |
| --- | --- |
| test | testCompile | 
| compile | compile |
| provided | compileOnly |
| runtime | runtime |

For custom behaviour you can configure a `ConfigurationResolver`
```groovy
import com.lazan.gradlemavenshare.*

apply plugin: 'java'
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
