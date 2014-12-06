package util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Util {
	private static Log log = LogFactory.getLog(Util.class);
	
	public static long dumpStream(InputStream is,OutputStream os) throws IOException {
	    byte[] buffer = new byte[1024 * 16];
	    int bytesRead = 0;
	    long total = 0;

	    while(true) {
	        bytesRead = is.read(buffer,0,buffer.length);

	        if(bytesRead == -1) {
	            break;
	        }
	        os.write(buffer,0,bytesRead);
	        total += bytesRead;
	    }
	    return total;
	}

	public static void closeStream(Closeable s) {
		try {
			s.close();
		} catch (IOException e) {
			log.warn("Failed to close stream " + s, e);
		}
	}

	public static void unzip(ZipInputStream zis, File dest) throws IOException {
		ZipEntry entry = zis.getNextEntry();
		if (entry==null) {
			throw new IOException("Input is not ZIP");
		} else {
			do {
				String entryName = entry.getName();
				entryName = replaceFileSeparator(entryName);
	
				File outFile = new File(dest,entryName);
	
		        if(entry.isDirectory()) {
		            if (!outFile.isDirectory() && !outFile.mkdirs()) {
		            	throw new IOException("Cannot create directory: " + outFile);
		            }
		        }
		        else {
		        	File parentFile = outFile.getParentFile();
		            if (!parentFile.isDirectory() && !parentFile.mkdirs()) {
		            	throw new IOException("Cannot create directory: " + parentFile);
		            }
		            OutputStream os = new FileOutputStream(outFile);
					try {
						dumpStream(zis,os);
					} finally {
						os.close();
					}
				}
		    	entry = zis.getNextEntry();
			} while(entry != null);
		}
	}

	private static String replaceFileSeparator(String filename) {
		char sep = System.getProperty("file.separator").charAt(0);
		if (sep=='/') {
			filename = filename.replace('\\', sep);
		} else {
			filename = filename.replace('/', sep);
		}
		return filename;
	}
	
	static String getRelativePath(File file, File base) {
		String prefix = base.getAbsolutePath() + System.getProperty("file.separator");
		String path = file.getAbsolutePath();
		if (path.startsWith(prefix)) {
			return path.substring(prefix.length());
		} else {
			return path;
		}
	}
	
	public static String[] readFileLines(File file) throws IOException {
		List<String>lines = new ArrayList<String>();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
			
			String line;
			while ((line=reader.readLine()) != null) {
				lines.add(line);
			}
			reader.close();
			return lines.toArray(new String[lines.size()]);
		} finally {
			if (fis!=null) {
				fis.close();
			}
		}
	}

	public static Integer getInt(String str, Integer defVal) {
		try {
			return Integer.valueOf(str.trim());
		} catch (Exception e) {
			return defVal;
		}
	}
	
	public static Integer getInt(String str) {
		return getInt(str, null);
	}

	public static void writeBlockerStats(PrintWriter out, Blocker blocker) {
		out.print(blocker.getAllBlocked().size() + "\n");
		int i=0;
		for (Set<String> blocked : blocker.getBlockedBuckets()) {
			out.println("    bucket " + i + ": " + blocked.size());
			i++;
		}
	}
}
