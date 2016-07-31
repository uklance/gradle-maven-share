package com.project2;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.project1.Service1;

public class Service1Test {
	@Test
	public void testGetContext() {
		Service1 service1 = new Service1();
		assertEquals("foo", service1.getContext().getBean("string1"));
		assertEquals("bar", service1.getContext().getBean("string2"));
	}
}
