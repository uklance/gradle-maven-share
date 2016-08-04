package com.lazan.gradlemavenshare;

public interface ResolvedPomVisitor<T> {
	T visit(ResolvedPom pom);
}
