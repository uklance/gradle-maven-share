package com.lazan.gradlemavenshare;

import java.io.InputStream;

public interface PomSource {
	InputStream getPom(String group, String artifact, String version);
}
