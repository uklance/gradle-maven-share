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

	def "Single project maven dependencies are shared with gradle"() {
		given:
		writeGradleSingle("build.gradle")
		writePom("pom.xml", "project1", """
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
			.withArguments('dependencies', '--stacktrace')
			.build()

		then:
		result.task(":dependencies").outcome == TaskOutcome.SUCCESS
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
		
		writeGradleMulti("build.gradle", """
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
		writeGradleMulti("build.gradle")
		
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
		writeGradleMulti("build.gradle")
		
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

	def "Multi project dependencies compiles"() {
		given:
		writeFile("settings.gradle", "include ':clientTest', ':server', ':client'")
		writeGradleMulti("build.gradle", """
			test {
				afterTest { descriptor, result ->
					println String.format("Test result %s = %s", descriptor.className, result.resultType)
				}
			}"""
		)
		
		writePom("server/pom.xml", "server", """
			<dependency>
				<groupId>org.springframework</groupId>
				<artifactId>spring-context</artifactId>
				<version>4.3.2.RELEASE</version>
			</dependency>"""
		)
		writeFile("server/src/main/java/com/foo/Server.java", """
			package com.foo;
			public class Server {
				public String getGreeting(String name) throws Exception {
					Class.forName("org.springframework.context.ApplicationContext");
					return "Hello " + name;
				}
			}
		""")
		
		writePom("client/pom.xml", "client", """
			<dependency>
				<groupId>com.foo</groupId>
				<artifactId>server</artifactId>
				<version>1.0-SNAPSHOT</version>
			</dependency>"""
		)
		writeFile("client/src/main/java/com/foo/Client.java", """
			package com.foo;
			public class Client {
				public String helloWorld() throws Exception {
					return new Server().getGreeting("world");
				}
			}"""
		)

		writePom("clientTest/pom.xml", "clientTest", """
			<dependency>
				<groupId>com.foo</groupId>
				<artifactId>client</artifactId>
				<version>1.0-SNAPSHOT</version>
			</dependency>
			<dependency>
			    <groupId>junit</groupId>
			    <artifactId>junit</artifactId>
			    <version>4.11</version>
				<scope>test</scope>
			</dependency>"""
		)

		writeFile("clientTest/src/test/java/com/foo/ClientTest.java", """
			package com.foo;
			import org.junit.*;
			public class ClientTest {
				@Test
				public void testGetGreeting() throws Exception {
					Assert.assertEquals("Hello world", new Client().helloWorld());
				}
			}
		""")

		when:
		def result = GradleRunner.create()
			.withProjectDir(testProjectDir.root)
			.withArguments('test', '--stacktrace')
			.build()

		then:
		result.task(":clientTest:test").outcome == TaskOutcome.SUCCESS
		result.output.contains("Test result com.foo.ClientTest = SUCCESS")
	}

	def "Unsupported test-jar type throws exception"() {
		given:
		writeFile("settings.gradle", "include ':project1'")
		writeGradleMulti("build.gradle")
		
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
	
	String writeGradleSingle(String path, String additional="") {
		String script = """
			buildscript {
				dependencies {
					classpath files($classpathString)
				}
			}
			repositories {
				mavenCentral()
			}
			apply plugin: 'java'
			apply plugin: 'com.lazan.gradlemavenshare'

			$additional
		"""
		writeFile(path, script)
	}

	String writeGradleMulti(String path, String additional="") {
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