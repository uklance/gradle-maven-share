package com.lazan.gradlemavenshare

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Rule
import org.junit.rules.TemporaryFolder

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

	def "Maven dependencies are shared with gradle"() {
		given:
		writeFile("settings.gradle", "include ':project1'")
		writeGradle("build.gradle")
		writePom("project1/pom.xml", "project1", """
			<dependency>
				<groupId>net.sourceforge.saxon</groupId>
				<artifactId>saxon</artifactId>
				<version>9.1.0.8</version>
				<classifier>dom</classifier>
			</dependency>
			<dependency>
				<groupId>org.springframework</groupId>
				<artifactId>spring-context</artifactId>
				<version>4.3.2.RELEASE</version>
			</dependency>"""
		)

		when:
		def result = GradleRunner.create()
			.withProjectDir(testProjectDir.root)
			.withArguments(':project1:dependencies', '--stacktrace')
			.build()

		then:
		result.task(":project1:dependencies").outcome == TaskOutcome.SUCCESS
		result.output.contains("net.sourceforge.saxon:saxon:9.1.0.8")
		result.output.contains("org.springframework:spring-context:4.3.2.RELEASE")
		result.output.contains("commons-logging")
	}

	def "Maven dependencies can be excluded"() {
		given:
		writeFile("settings.gradle", "include ':project1'")

		writePom("project1/pom.xml", "project1", """
			<dependency>
				<groupId>net.sourceforge.saxon</groupId>
				<artifactId>saxon</artifactId>
				<version>9.1.0.8</version>
				<classifier>dom</classifier>
			</dependency>
			<dependency>
				<groupId>org.springframework</groupId>
				<artifactId>spring-context</artifactId>
				<version>4.3.2.RELEASE</version>
			</dependency>"""
		)
		
		writeGradle("build.gradle", """
			mavenShare {
				exclude([artifactId: 'saxon'])
			}"""
		)

		when:
		def result = GradleRunner.create()
			.withProjectDir(testProjectDir.root)
			.withArguments(':project1:dependencies', '--stacktrace')
			.build()

		then:
		result.task(":project1:dependencies").outcome == TaskOutcome.SUCCESS
		!result.output.contains("net.sourceforge.saxon:saxon:9.1.0.8")
		result.output.contains("org.springframework:spring-context:4.3.2.RELEASE")
		result.output.contains("commons-logging")
	}

	def "Maven exclude is shared with gradle"() {
		given:
		writeFile("settings.gradle", "include ':project1'")
		writeGradle("build.gradle")
		
		writePom("project1/pom.xml", "project1", """
			<dependency>
				<groupId>net.sourceforge.saxon</groupId>
				<artifactId>saxon</artifactId>
				<version>9.1.0.8</version>
				<classifier>dom</classifier>
			</dependency>
			<dependency>
				<groupId>org.springframework</groupId>
				<artifactId>spring-context</artifactId>
				<version>4.3.2.RELEASE</version>
				<exclusions>
					<exclusion>
						<groupId>commons-logging</groupId>
						<artifactId>commons-logging</artifactId>
					</exclusion>
				</exclusions>
			</dependency>"""
		)

		when:
		def result = GradleRunner.create()
			.withProjectDir(testProjectDir.root)
			.withArguments(':project1:dependencies', '--stacktrace')
			.build()

		then:
		result.task(":project1:dependencies").outcome == TaskOutcome.SUCCESS
		result.output.contains("net.sourceforge.saxon:saxon:9.1.0.8")
		result.output.contains("org.springframework:spring-context:4.3.2.RELEASE")
		!result.output.contains("commons-logging")
	}
	
	def "Local project dependency is shared with gradle"() {
		given:
		writeFile("settings.gradle", "include ':project1', ':project2'")
		writeGradle("build.gradle")
		
		writePom("project1/pom.xml", "project1", """
			<dependency>
				<groupId>org.springframework</groupId>
				<artifactId>spring-context</artifactId>
				<version>4.3.2.RELEASE</version>
			</dependency>"""
		)
		writePom("project2/pom.xml", "project2", """
			<dependency>
				<groupId>com.foo</groupId>
				<artifactId>project1</artifactId>
				<version>1.0-SNAPSHOT</version>
			</dependency>"""
		)

		when:
		def result = GradleRunner.create()
			.withProjectDir(testProjectDir.root)
			.withArguments(':project2:dependencies', '--stacktrace')
			.build()

		then:
		result.task(":project2:dependencies").outcome == TaskOutcome.SUCCESS
		result.output.contains("project :project1")
		result.output.contains("spring-context")
	}
	
	def "Unsupported test-jar type throws exception"() {
		given:
		writeFile("settings.gradle", "include ':project1'")
		writeGradle("build.gradle")
		
		writePom("project1/pom.xml", "project1", """
			<dependency>
				<groupId>org.springframework</groupId>
				<artifactId>spring-context</artifactId>
				<version>4.3.2.RELEASE</version>
				<type>test-jar</type>
			</dependency>"""
		)

		when:
		BuildResult result = GradleRunner.create()
			.withProjectDir(testProjectDir.root)
			.withArguments(':project1:dependencies', '--stacktrace')
			.buildAndFail()

		then:
		result.output.contains("type=test-jar not supported for external dependency org.springframework:spring-context:4.3.2.RELEASE")
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
	
	String writePom(String path, String artifactId, String dependencies) {
		String xml =  """
			<project>
				<modelVersion>4.0.0</modelVersion>
				<groupId>com.foo</groupId>
				<artifactId>${artifactId}</artifactId>
				<version>1.0-SNAPSHOT</version>
				<dependencies>
					${dependencies}
				</dependencies>
			</project>
		"""
		writeFile(path, xml)
	}
	
	String writeGradle(String path, String additional="") {
		String script = """
			buildscript {
				dependencies {
					classpath files($classpathString)
				}
			}
			subprojects {
				repositories {
					mavenCentral()
				}
				apply plugin: 'java'
				apply plugin: 'com.lazan.gradlemavenshare'

				$additional
			}
		"""
		writeFile(path, script) 
	}
}