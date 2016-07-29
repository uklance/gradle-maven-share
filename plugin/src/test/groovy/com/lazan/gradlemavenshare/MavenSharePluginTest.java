package com.lazan.gradlemavenshare;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MavenSharePluginTest {
	@Rule public final TemporaryFolder testDir = new TemporaryFolder();
	
	@Test @Ignore
	public void test() throws IOException {
		File immutableDir = getFile("gradle-sample-1/build.gradle").getParentFile();
		copyDir(immutableDir, testDir.getRoot());
		
		BuildResult buildResult = GradleRunner.create()
				.withProjectDir(testDir.getRoot())
				.withArguments("compile")
				.withPluginClasspath(getPluginClasspath())
				.build();
		
		System.out.println(buildResult.getOutput());
	}
	
	private List<File> getPluginClasspath() throws IOException {
		String resourceName = "plugin-classpath.txt";
		try (
			InputStream in = MavenSharePluginTest.class.getClassLoader().getResourceAsStream(resourceName);	
		) {
			assertNotNull("No such resource " + resourceName, in);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			List<File> files = new ArrayList<File>();
			String line;
			while ((line = reader.readLine()) != null) {
				File file = new File(line);
				if (file.isDirectory()) {
					System.out.println("Adding directory " + file);
				}
				files.add(file);
			}
			return files;
		}
	}
	
	private void copyDir(File source, File dest) throws IOException {
		File[] files = source.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					File destDir = new File(dest, file.getName());
					destDir.mkdirs();
					copyDir(file, destDir);
				} else {
					byte[] bytes = new byte[1024];
					int count;
					try (
						InputStream in = new FileInputStream(file);
						OutputStream out = new FileOutputStream(new File(dest, file.getName()));
					) {
						while((count = in.read(bytes)) > 0) {
							out.write(bytes, 0, count);
						}
					}
				}
			}
		}
	}

	private File getFile(String path) {
		URL url = PomResolverTest.class.getClassLoader().getResource(path);
		assertNotNull(url);
		return new File(url.getFile());
	}
	
}
