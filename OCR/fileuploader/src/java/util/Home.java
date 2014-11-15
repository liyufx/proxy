package util;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class Home {
	private static Log log = LogFactory.getLog(Home.class);
	private static AtomicReference<File> sHome = new AtomicReference<File>();

	public static File getHome() {
		return sHome.get();
	}
	
	public static void init(File home) {
		if (!sHome.compareAndSet(null, home)) {
			throw new IllegalStateException("Home already set to " + sHome.get() + 
					" , cannot reset to " + home);
		}
	}

	private static final File getFileFromUrl(URL fileUrl) {
		String path = fileUrl.getPath();
		try {
			path = URLDecoder.decode(path, "UTF-8");
		} catch (Exception e) {
			log.error("Could not decode path URL: " + path, e);
		}

		return new File(path.replace('/', File.separatorChar));
	}

	public static File getTomcatHome() throws IOException {
		return getClassHomeDir(Home.class).
				getParentFile().getParentFile().getParentFile().getParentFile();
	}
	public static File getClassHomeDir(Class<?> clazz) throws IOException {
		/**
		 * Return the classpath root (dir that contains the JAR file or classpath root dir) 
		 * from which a given class was loaded.
		 */
		// Get the resource URL for the class, from it's class loader
		ClassLoader cl = clazz.getClassLoader();
		String className = clazz.getName();
		URL classFileUrl = cl.getResource(className.replace(".", "/") + ".class");
		String protocol = classFileUrl.getProtocol();

		// If this is a developer build, the classResUrl may be a .class
		// file in the build/classes dir.
		if (protocol.equals("file")) {
			File classFile = getFileFromUrl(classFileUrl);
			// Walk up the package structure to the classes dir
			File classesDir = classFile.getParentFile();
			for (int i = 0; i < className.length(); ++i) {
				if (className.charAt(i) == '.') {
					classesDir = classesDir.getParentFile();
				}
			}
			return classesDir;
		} else if (protocol.equals("jar")) {
			// Get the URL to the jar file itself
			URL jarUrl = ((JarURLConnection) classFileUrl.openConnection()).getJarFileURL();
			return getFileFromUrl(jarUrl).getParentFile();
		} else {
			throw new IllegalStateException(
					"Unexpected URL for class resource: " + classFileUrl);
		}
	}

	public static File getFile(String relativePath) {
		return new File(new File(sHome.get(), relativePath).getAbsolutePath());
	}
}
