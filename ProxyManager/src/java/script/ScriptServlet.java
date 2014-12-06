package script;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import util.Home;
import weibo.WeiboAccount;

public class ScriptServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final String SCRIPTS_HOME = "proxies/scripts";
	private final static String SUFFIX = ".js";

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		PrintWriter out = null;
		try {
			String function = request.getParameter("fn");
			String id = request.getParameter("id");
			
			if (function.equalsIgnoreCase("help")) {
				printHelp(out);
			} else if (function.equalsIgnoreCase("check")) {
				response.setContentType("text/plain;charset=utf-8");
				out = response.getWriter();
				out.write(getLatestScriptVersion(id));
			} else if (function.equalsIgnoreCase("get")) {
				String version = request.getParameter("version");
				response.setContentType("application/octet-stream");
				
				if (StringUtils.isEmpty(version)) {
					version = getLatestScriptVersion(id);
				}
				
				response.addHeader("Content-Disposition",
						"attachment; filename=" + id + "." + version + SUFFIX);
				
				FileInputStream input = null;
				try {
					input = new FileInputStream(getScriptFile(id, version));
					BufferedInputStream buf = new BufferedInputStream(input);
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					
					int readBytes = 0;
				
					//read from the file; write to the ServletOutputStream
					while((readBytes = buf.read()) != -1)
					{
						bos.write(readBytes);
					}
					
					if (bos.toString().contains("/* " + id + " " + version + " - END OF SCRIPT */") &&
							bos.toString().contains("var script_version = \"" + version + "\";")) {
						OutputStream outStream = response.getOutputStream();
						outStream.write(bos.toByteArray());
						outStream.close();
					} else {
						throw new ServletException("Invalid script");
					}
					buf.close();
				} finally {
					if (input!=null) {
						input.close();
					}
				}
			}
		} finally {
			if (out!=null) { out.close(); }
		}
	}

	private void printHelp(PrintWriter out) {
		out.println("fn=");
		out.println("help");
		out.println("check");
		out.println("get?version");
	}

	private String getScript(String id, String version) throws IOException {
		if (version.length()==8) {
			File script = getScriptFile(id, version);
			if (script.exists()) {
				return readScript(id, version, script);
			}
		}
		return "";
	}

	private File getScriptFile(String id, String version) throws IOException {
		return new File(Home.getTomcatHome(), "proxies/scripts/"+id+"."+version+SUFFIX);
	}

	private String getLatestScript(String id) throws IOException {
		final String prefix = id + ".";
		
		File scriptsHome = new File(Home.getTomcatHome(), SCRIPTS_HOME);
		if (scriptsHome.isDirectory()) {
			File scripts[] = scriptsHome.listFiles(
					new FilenameFilter() {
						@Override
						public boolean accept(File file, String name) {
							return name.startsWith(prefix) && name.endsWith(SUFFIX);
						}
					} );
			
			
			String version = "";
			File latestVersion = null;
			for (File file : scripts) {
				String name = file.getName();
				if (name.startsWith(prefix)) {
					name = name.substring(prefix.length());
					if (name.endsWith(SUFFIX)) {
						name = name.substring(0, name.length()-SUFFIX.length());
					}
					if (name.length()==8 && name.compareTo(version)>0) {
						version = name;
						latestVersion = file;
					}
				}
			}
			if (latestVersion!=null && latestVersion.exists()) {
				return readScript(id, version, latestVersion);
			}
		}
		return "";
	}

	private String readScript(String id, String version, File file)
			throws IOException {
		String script = FileUtils.readFileToString(file);
		if (script.contains("/* " + id + " " + version + " - END OF SCRIPT */")) {
			return script;
		} else {
			return "";
		}
	}

	private String getLatestScriptVersion(String id) throws IOException {
		final String prefix = id + ".";
		
		File scriptsHome = new File(Home.getTomcatHome(), SCRIPTS_HOME);
		String version = "";
		
		if (scriptsHome.isDirectory()) {
			String scripts[] = scriptsHome.list(
					new FilenameFilter() {
						@Override
						public boolean accept(File file, String name) {
							return name.startsWith(prefix) && name.endsWith(SUFFIX);
						}
					} );
			
			
			for (String name : scripts) {
				if (name.startsWith(prefix)) {
					name = name.substring(prefix.length());
					if (name.endsWith(SUFFIX)) {
						name = name.substring(0, name.length()-SUFFIX.length());
					}
					if (name.length()==8 && name.compareTo(version)>0) {
						version = name;
					}
				}
			}
		}
		return version;
	}
}
