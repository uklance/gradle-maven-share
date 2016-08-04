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
	
	String pom1 =  """
		<project>
			<modelVersion>4.0.0</modelVersion>
			<groupId>com.foo</groupId>
			<artifactId>project1</artifactId>
			<version>1.0-SNAPSHOT</version>
			<dependencies>
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
				</dependency>
			</dependencies>
		</project>"""
	
	String pom1Exclude =  """
		<project>
			<modelVersion>4.0.0</modelVersion>
			<groupId>com.foo</groupId>
			<artifactId>project1</artifactId>
			<version>1.0-SNAPSHOT</version>
			<dependencies>
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
				</dependency>
			</dependencies>
		</project>"""

	String pom2 =  """
		<project>
			<modelVersion>4.0.0</modelVersion>
			<groupId>com.foo</groupId>
			<artifactId>project2</artifactId>
			<version>1.0-SNAPSHOT</version>
			<dependencies>
				<dependency>
					<groupId>com.foo</groupId>
					<artifactId>project1</artifactId>
					<version>1.0-SNAPSHOT</version>
				</dependency>
			</dependencies>
		</project>"""

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
		writeFile("build.gradle", """
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
			}
        """)
		
		writeFile("project1/pom.xml", pom1)

		when:
		def result = GradleRunner.create()
			.withProjectDir(testProjectDir.root)
			.withArguments(':project1:dependencies', '--stacktrace')
			.build()

		then:
		result.task(":project1:dependencies").outcome == SUCCESS
		result.output.contains("net.sourceforge.saxon:saxon:9.1.0.8")
		result.output.contains("org.springframework:spring-context:4.3.2.RELEASE")
		result.output.contains("commons-logging")
	}

	def "Maven dependencies can be excluded"() {
		given:
		writeFile("settings.gradle", "include ':project1'")
		writeFile("build.gradle", """
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

				mavenShare {
					exclude([artifactId: 'saxon'])
				}
			}
        """)
		
		writeFile("project1/pom.xml", pom1)

		when:
		def result = GradleRunner.create()
			.withProjectDir(testProjectDir.root)
			.withArguments(':project1:dependencies', '--stacktrace')
			.build()

		then:
		result.task(":project1:dependencies").outcome == SUCCESS
		!result.output.contains("net.sourceforge.saxon:saxon:9.1.0.8")
		result.output.contains("org.springframework:spring-context:4.3.2.RELEASE")
		result.output.contains("commons-logging")
	}

	def "Maven exclude is shared with gradle"() {
		given:
		writeFile("settings.gradle", "include ':project1'")
		writeFile("build.gradle", """
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
			}
        """)
		
		writeFile("project1/pom.xml", pom1Exclude)

		when:
		def result = GradleRunner.create()
			.withProjectDir(testProjectDir.root)
			.withArguments(':project1:dependencies', '--stacktrace')
			.build()

		then:
		result.task(":project1:dependencies").outcome == SUCCESS
		result.output.contains("net.sourceforge.saxon:saxon:9.1.0.8")
		result.output.contains("org.springframework:spring-context:4.3.2.RELEASE")
		!result.output.contains("commons-logging")
	}
	
	def "Local project dependency is shared with gradle"() {
		given:
		writeFile("settings.gradle", "include ':project1', ':project2'")
		writeFile("build.gradle", """
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
			}
        """)
		
		writeFile("project1/pom.xml", pom1)
		writeFile("project2/pom.xml", pom2)
		
		when:
		def result = GradleRunner.create()
			.withProjectDir(testProjectDir.root)
			.withArguments(':project2:dependencies', '--stacktrace')
			.build()

		then:
		result.task(":project2:dependencies").outcome == SUCCESS
		result.output.contains("project :project1")
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