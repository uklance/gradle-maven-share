package com.lazan.gradlemavenshare

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Ignore;
import spock.lang.Specification

class MavenSharePluginTest extends Specification {

	@Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
	File buildFile
	File pomFile1
	File settingsFile
	File gradlePropertiesFile
	List<File> pluginClasspath
	String classpathString

	def setup() {
		settingsFile = testProjectDir.newFile('settings.gradle')
		buildFile = testProjectDir.newFile('build.gradle')
		gradlePropertiesFile = testProjectDir.newFile('gradle.properties')
		testProjectDir.newFolder('project1')
		pomFile1 = testProjectDir.newFile('project1/pom.xml')
		
		def testkitClasspathUrl = getClass().classLoader.findResource("testkit-classpath.txt")
		if (testkitClasspathUrl == null) {
			throw new IllegalStateException("Could not find testkit-classpath.txt")
		}

		pluginClasspath = testkitClasspathUrl.readLines().collect { new File(it) }
		
		classpathString = pluginClasspath
			.collect { it.absolutePath.replace('\\', '/') } // escape backslashes in Windows paths
			.collect { "'$it'" }
			.join(", ")
			
		def testkitGradlePropsResource = getClass().classLoader.findResource("testkit-gradle.properties")
		if (testkitGradlePropsResource == null) {
			throw new IllegalStateException("Could not find testkit-gradle.properties")
		}
		//gradlePropertiesFile.text = testkitGradlePropsResource.text
	}

	def "Maven dependency is shared with gradle"() {
		given:
		settingsFile << "include ':project1'"
		buildFile << """
			buildscript {
        		dependencies {
            		classpath files($classpathString)
        		}
    		}
			subprojects {
				buildscript {
            		dependencies {
                		classpath files($classpathString)
            		}
        		}
				apply plugin: 'java'
				apply plugin: 'com.lazan.gradlemavenshare'
			}
        """
		
		pomFile1 << """
			<project>
				<modelVersion>4.0.0</modelVersion>
				<groupId>com.foo</groupId>
				<artifactId>test</artifactId>
				<version>1.0-SNAPSHOT</version>
				<dependencies>
					<dependency>
						<groupId>net.sourceforge.saxon</groupId>
						<artifactId>saxon</artifactId>
						<version>9.1.0.8</version>
						<classifier>dom</classifier>
					</dependency>		
				</dependencies>
			</project>
        """

		when:
		def result = GradleRunner.create()
			.withProjectDir(testProjectDir.root)
			.withArguments(':project1:dependencies', '--stacktrace')
			.build()

		then:
		result.task(":project1:dependencies").outcome == SUCCESS
		result.output.contains("net.sourceforge.saxon:saxon:9.1.0.8")
	}
}