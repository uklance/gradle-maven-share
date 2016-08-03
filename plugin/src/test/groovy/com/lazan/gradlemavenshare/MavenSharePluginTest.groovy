package com.lazan.gradlemavenshare

import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Ignore;
import spock.lang.Specification

class MavenSharePluginTest extends Specification {

	@Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
	String classpathString

	def setup() {
		URL classpathUrl = getResourceUrl("testkit-classpath.txt")
		List<File> classpathFiles = classpathUrl.readLines().collect { new File(it) }
		
		classpathString = classpathFiles
			.collect { it.absolutePath.replace('\\', '/') } // escape backslashes in Windows paths
			.collect { "'$it'" }
			.join(", ")
			
		writeFile('gradle.properties', getResourceUrl("testkit-gradle.properties").text)
	}

	def "Maven dependency is shared with gradle"() {
		given:
		writeFile("settings.gradle", "include ':project1'")
		writeFile("build.gradle", """
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
        """)
		
		writeFile("project1/pom.xml", """
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
        """)

		when:
		def result = GradleRunner.create()
			.withProjectDir(testProjectDir.root)
			.withArguments(':project1:dependencies', '--stacktrace')
			.build()

		then:
		result.task(":project1:dependencies").outcome == SUCCESS
		result.output.contains("net.sourceforge.saxon:saxon:9.1.0.8")
	}
	
	URL getResourceUrl(String path) {
		URL url = getClass().classLoader.getResource(path)
		if (url == null) throw new RuntimeException("No such resource $path")
		return url
	}
	
	void writeFile(String path, String text) {
		File file = new File(testProjectDir.root, path)
		file.parentFile.mkdirs()
		file.text = text
	}
}