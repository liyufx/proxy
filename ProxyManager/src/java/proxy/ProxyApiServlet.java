package proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.Home;

public class ProxyApiServlet extends HttpServlet {
	
	static String[] loadedApis = null;
	static Date loadTime = new Date();
	
	synchronized private static String[] loadApi() throws IOException {
		List<String> apis = new ArrayList<String>();  
		File defaultFile = new File(Home.getTomcatHome(), "proxies/proxyapi.def.txt");
		File apiFile = new File(Home.getTomcatHome(), "proxies/proxyapi.txt");
		String[] apis1 = loadedApis;
		if (apis1==null || (defaultFile.isFile() && defaultFile.lastModified()>loadTime.getTime()) ||
				(apiFile.isFile() && apiFile.lastModified()>loadTime.getTime())) {
			FileInputStream fis = null; 
			
			try {
				if (!apiFile.isFile()) {
					fis = new FileInputStream(defaultFile);
				} else {
					fis = new FileInputStream(apiFile);
				}
				BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
				String line;
				while ((line=reader.readLine()) != null) {
					line = line.trim();
					if (line.length()>0)
						apis.add(line);
				}
				reader.close();
			} catch (Exception e) {
				
			} finally {
				if (fis!=null) fis.close();
			}
			apis1 = apis.toArray(new String[apis.size()]);
			loadedApis = apis1;
		}
		return apis1;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();

		try {
			String[] apis = loadApi();
			if (apis.length>0) {
				int index = (int)(Math.random() * apis.length);
			
				out.println(apis[index]);
			}
		} catch (Exception e) {
		}
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		String api = request.getParameter("api");
		String action = request.getParameter("action");

		File apiFile = new File(Home.getTomcatHome(), "proxies/proxyapi.txt");
		if ("delete".equalsIgnoreCase(action)) {
			apiFile.delete();
			loadedApis = null;
		} else {
			if (api!=null && api.length()>0) {
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(apiFile);
					PrintWriter pw = new PrintWriter(fos);
					pw.println(api);
					pw.close();
				} catch (Exception e) {
					if (fos!=null) fos.close();
				}
			}
		}
		out.println(api);
	}
}
