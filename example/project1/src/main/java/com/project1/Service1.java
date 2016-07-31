package com.project1;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class Service1 {
	private final ApplicationContext context = new AnnotationConfigApplicationContext(Example1Context.class);
	
	public ApplicationContext getContext() {
		return context;
	}
	
	@Configuration
	public static class Example1Context {
		@Bean
		public String string1() {
			return "foo";
		}
		
		@Bean
		public String string2() {
			return "bar";
		}
	}
}
