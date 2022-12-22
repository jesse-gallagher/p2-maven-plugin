package org.reficio.p2.resolver.eclipse.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import com.ibm.commons.util.io.StreamUtil;

public class FileBinaryCategory {
	public static void leftShift(File file, URL url) throws IOException {
		try(
			InputStream is = url.openStream();
			OutputStream os = new FileOutputStream(file)
		) {
			StreamUtil.copyStream(is, os);
		}
    }
}
