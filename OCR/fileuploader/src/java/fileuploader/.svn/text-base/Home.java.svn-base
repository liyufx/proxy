package com.claytablet.intel.fileuploader;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class Home {
	private static final AtomicReference<String> HOME_PATH = new AtomicReference<String>("tmsuploader");
	private static final ThreadLocal<File> HOME_DIR = new ThreadLocal<File>();
	
	public static File init(String homePath) {
		boolean init = false;
		
		synchronized (HOME_PATH) {
			homePath = homePath.trim();
			String originalHome = HOME_PATH.get();
			if (!StringUtils.isEmpty(homePath) && !homePath.equals(originalHome) && 
					HOME_PATH.compareAndSet(originalHome, homePath)) {
				init = true; 
			}
			File home = new File(HOME_PATH.get());
			if (!home.isAbsolute()) {
				home = new File(System.getProperty("user.home"), HOME_PATH.get());
			}
			if (!home.isDirectory() || init) {
				try {
					FileUtils.forceMkdir(home);
				} catch (IOException e) {
					throw new RuntimeException(
							"Cannot initialize home directory at " + home.getAbsolutePath(), e); 
				}
				try {
					FileUtils.deleteDirectory(new File(home, "tmp"));
				} catch (IOException e) {
					// ignore
				}
			}
			HOME_DIR.set(home);
			return home;
		}
	}
	
	public static File getFile(String path) {
		if (HOME_DIR.get()==null) {
			HOME_DIR.set(new File(HOME_PATH.get()));
		}
		return new File(new File(HOME_DIR.get(), path).getAbsolutePath());
	}

}
