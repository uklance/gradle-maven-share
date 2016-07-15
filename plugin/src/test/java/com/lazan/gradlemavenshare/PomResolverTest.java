package com.lazan.gradlemavenshare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PomResolverTest {
	private PomResolver resolver = new PomResolver();

	@Mock private PomSource pomSource;
	
	@Test
	public void testResolvePom() {
		File implFile = getFile("maven-example-1/impl/pom.xml");

		PomResolveCache cache = new PomResolveCache();
		ResolvedPom implPom = resolver.resolvePom(implFile, cache, null);
		
		assertEquals("1.0-SNAPSHOT", implPom.getVersion());
		assertEquals("impl", implPom.getArtifactId());
		assertEquals("com.foo", implPom.getGroupId());
		assertEquals("parentOne-parentTwo-implTwo", implPom.getProperty("impl1"));
		assertEquals("overrideOneImpl", implPom.getProperty("override1"));
		assertEquals("overrideOneParent", implPom.getParent().getProperty("override1"));
		assertEquals("com.foo:impl:1.0-SNAPSHOT", implPom.getProperty("gav"));
		
		File interfaceFile = getFile("maven-example-1/interface/pom.xml");
		ResolvedPom interfacePom = resolver.resolvePom(interfaceFile, cache, null);
		assertEquals("1.0-SNAPSHOT", interfacePom.getVersion());
		assertEquals("interface", interfacePom.getArtifactId());
		assertEquals("com.foo", interfacePom.getGroupId());
	}

	private File getFile(String path) {
		URL url = PomResolverTest.class.getClassLoader().getResource(path);
		assertNotNull(url);
		return new File(url.getFile());
	}

}
