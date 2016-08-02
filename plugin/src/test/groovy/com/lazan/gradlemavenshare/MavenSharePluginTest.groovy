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
	List<File> pluginClasspath
	String classpathString

	def setup() {
		settingsFile = testProjectDir.newFile('settings.gradle')
		buildFile = testProjectDir.newFile('build.gradle')
		testProjectDir.newFolder('project1')
		pomFile1 = testProjectDir.newFile('project1/pom.xml')
		
		def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
		if (pluginClasspathResource == null) {
			throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
		}

		pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }
		
		classpathString = pluginClasspath
			.collect { it.absolutePath.replace('\\', '\\\\') } // escape backslashes in Windows paths
			.collect { "'$it'" }
			.join(", ")
	}

	@Ignore
	def "Maven dependency is shared with gradle"() {
		given:
		settingsFile << "include ':project1'"
		buildFile << """
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
			.withArguments(':project1:dependencies')
			.build()

		then:
		result.task(":project1:dependencies").outcome == SUCCESS
		result.output.contains("net.sourceforge.saxon:saxon:9.1.0.8")
	}
}