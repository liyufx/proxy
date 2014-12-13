package proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import util.Blocker;
import util.BlockerImpl;
import util.Home;
import util.Util;

import db.DbAccess;

public class ProxyServlet extends HttpServlet {

	private static final Blocker tempProxyBlocker;
	private static final Map<String, Blocker> siteBlockers;
	private static final Map<String, Blocker> realtimeSiteBlockers;
	private static final Blocker weiboLongBlocker = new BlockerImpl(24);
	private static final Map<String, AtomicInteger> siteAccessCounters;
	private static final Map<String, AtomicInteger> siteAccessFailureCounters;
	private static final Map<String, AtomicInteger> siteRealtimeAccessCounters;
	private static final Map<String, AtomicInteger> siteRealtimeAccessFailureCounters;
	
	static {
		sRawProxies = new AtomicReference<String[]>();
		sRealtimeProxies = new AtomicReference<String[]>();
		siteRealtimeAccessCounters = new HashMap<String, AtomicInteger>();
		siteRealtimeAccessFailureCounters = new HashMap<String, AtomicInteger>();
		siteAccessCounters = new HashMap<String, AtomicInteger>();
		siteAccessFailureCounters = new HashMap<String, AtomicInteger>();
		
		tempProxyBlocker = new BlockerImpl(2);
		siteBlockers = new HashMap<String, Blocker>();
		siteBlockers.put("weibo", new BlockerImpl(40));
		siteBlockers.put("yaris", new BlockerImpl(100));
		siteBlockers.put("baidu", new BlockerImpl(30));
		
		realtimeSiteBlockers = new HashMap<String, Blocker>();
		realtimeSiteBlockers.put("weibo", new BlockerImpl(10));
		realtimeSiteBlockers.put("yaris", new BlockerImpl(10));
		
		siteAccessCounters.put("weibo",  new AtomicInteger());
		siteAccessCounters.put("yaris",  new AtomicInteger());
		siteAccessCounters.put("baidu",  new AtomicInteger());
		siteAccessFailureCounters.put("weibo",  new AtomicInteger());
		siteAccessFailureCounters.put("yaris",  new AtomicInteger());
		siteAccessFailureCounters.put("baidu",  new AtomicInteger());
		siteRealtimeAccessCounters.put("weibo",  new AtomicInteger());
		siteRealtimeAccessCounters.put("yaris",  new AtomicInteger());
		siteRealtimeAccessFailureCounters.put("weibo",  new AtomicInteger());
		siteRealtimeAccessFailureCounters.put("yaris",  new AtomicInteger());
		loadProxies();
	}
	
	static void loadProxies() {
		sProxies = new ConcurrentHashMap<String, List<String>>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			// Open a connection
			conn = DbAccess.getDbConnection();

			String sql = "SELECT site, zone, active_proxies FROM active_proxies";
			// Execute SQL query
			stmt = conn.createStatement();
			// Set response content type
			rs = stmt.executeQuery(sql);
			
			while (rs.next()) {
				String site = rs.getString(1);
				String zone = rs.getString(2);
				String rawProxies = rs.getString(3);
				String key = site + ":" + zone;
				String [] proxies = rawProxies.split("\\s");
				List<String> ps = new ArrayList<String>(proxies.length); 
				for (String proxy : proxies) {
					proxy = proxy.trim();
					if (!StringUtils.isEmpty(proxy)) {
						ps.add(proxy);
					}
				}
				sProxies.put(key, ps);
			}
		} catch (Exception e) {
			// Handle errors for Class.forName
			e.printStackTrace();
		} finally {
			DbAccess.cleanup(conn, stmt, rs);
		}

		try {
			File proxyHome = new File(Home.getTomcatHome(), "proxies");
			
			File proxyFiles[] = proxyHome.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File file, String name) {
					return name.toLowerCase().matches(".*-proxy\\.txt");
				}
			});

			for (File file : proxyFiles) {
				Set<String> ips = new HashSet<String>();
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(file);
					readProxies(ips, fis);
					
					sProxies.put(file.getName().split("-")[0] + ":", new ArrayList<String>(ips));
				} finally {
					IOUtils.closeQuietly(fis);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			loadRawProxies();
			loadRealtimeProxies();
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					do {
						try {
							for (int j=0; j<10; j++) {
								for (int i=0; i<6; i++) {
									Thread.sleep(60000);
									tempProxyBlocker.expire(1);
									ProxyServlet.loadRealtimeProxies();
								}
								ProxyServlet.loadRawProxies();
								for (Blocker blocker : siteBlockers.values()) {
									blocker.expire(1);
								}
								for (Blocker blocker : realtimeSiteBlockers.values()) {
									blocker.expire(1);
								}
								siteAccessCounters.get("weibo").set(0);
								siteAccessCounters.get("yaris").set(0);
								siteAccessCounters.get("baidu").set(0);
								siteAccessFailureCounters.get("weibo").set(0);
								siteAccessFailureCounters.get("yaris").set(0);
								siteAccessFailureCounters.get("baidu").set(0);
								siteRealtimeAccessCounters.get("weibo").set(0);
								siteRealtimeAccessCounters.get("yaris").set(0);
								siteRealtimeAccessFailureCounters.get("weibo").set(0);
								siteRealtimeAccessFailureCounters.get("yaris").set(0);
							}
							weiboLongBlocker.expire(1);
						} catch (Exception e) {
						}
					} while(true);
				}
				
			}, "RawProxyLoader");
			t.setDaemon(true);
			t.start();
		} catch (Exception e) {
			// Handle errors for Class.forName
			e.printStackTrace();
		}
	}
	
	static int reloadSiteProxies(String site) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int count = 0;
		
		try {
			// Open a connection
			conn = DbAccess.getDbConnection();

			String sql = "SELECT site, zone, active_proxies FROM active_proxies WHERE site=?";
			// Set response content type
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, site);
			rs = stmt.executeQuery();
			
			while (rs.next()) {
				String zone = rs.getString(2);
				String rawProxies = rs.getString(3);
				String key = site + ":" + zone;
				String [] proxies = rawProxies.split("\\s");
				List<String> ps = new ArrayList<String>(proxies.length); 
				for (String proxy : proxies) {
					proxy = proxy.trim();
					if (!StringUtils.isEmpty(proxy)) {
						ps.add(proxy);
					}
					count++;
				}
				sProxies.put(key, ps);
			}
		} catch (Exception e) {
			// Handle errors for Class.forName
			e.printStackTrace();
		} finally {
			DbAccess.cleanup(conn, stmt, rs);
		}
		
		try {
			File file = new File(new File(Home.getTomcatHome(), "proxies"),
					site + "-proxy.txt");
			
			if (file.isFile()) {
				Set<String> ips = new HashSet<String>();
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(file);
					readProxies(ips, fis);
					
					sProxies.put(site + ":", new ArrayList<String>(ips));
				} finally {
					IOUtils.closeQuietly(fis);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return count;
	}
	
	static AtomicReference<String[]> sRawProxies;
	static Map<String, List<String>> sProxies;
	static AtomicReference<String[]> sRealtimeProxies;
	
	
	private static final long serialVersionUID = 1L;

	private static void loadRealtimeProxies() {
		Set<String> loadedProxies = new HashSet<String>();
		try {
			File proxyHome = new File(Home.getTomcatHome(), "proxies");
			File proxyServiceFile = new File(proxyHome, "RealtimeProxyServices.txt");
			
			if (proxyServiceFile.isFile()) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(proxyServiceFile);
					BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
					
					String url;
					while ((url=reader.readLine()) != null) {
						try {
							readProxyFromUrl(loadedProxies, url);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					reader.close();
				} finally {
					IOUtils.closeQuietly(fis);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		sRealtimeProxies.set(loadedProxies.toArray(new String[loadedProxies.size()]));
	}
	
	private static int loadRawProxies() {
		Set<String> loadedProxies = new HashSet<String>();
		
		for (List<String> existingProxies : sProxies.values()) {
			loadedProxies.addAll(existingProxies);
		}
		
		try {
			File proxyHome = new File(Home.getTomcatHome(), "proxies");
			
			File proxyFiles[] = proxyHome.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File file, String name) {
					return name.toLowerCase().matches("proxy*.txt");
				}
			});

			for (File file : proxyFiles) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(file);
					readProxies(loadedProxies, fis);
				} finally {
					IOUtils.closeQuietly(fis);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		try {
			File proxyHome = new File(Home.getTomcatHome(), "proxies");
			File proxyServiceFile = new File(proxyHome, "ProxyServices.txt");
			
			if (proxyServiceFile.isFile()) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(proxyServiceFile);
					BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
					
					String url;
					while ((url=reader.readLine()) != null) {
						try {
							readProxyFromUrl(loadedProxies, url);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					reader.close();
				} finally {
					IOUtils.closeQuietly(fis);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		sRawProxies.set(loadedProxies.toArray(new String[loadedProxies.size()]));
		return loadedProxies.size();
	}

	private static final String USER_AGENT = "Mozilla/5.0";
	
	private static void readProxyFromUrl(Set<String> loadedProxies, String url) throws Exception {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
 
		// optional default is GET
		con.setRequestMethod("GET");
 
		//add request header
		con.setRequestProperty("User-Agent", USER_AGENT);
 
		int code = con.getResponseCode();
 
		if (code>=200 && code<300) {
			InputStream is = con.getInputStream();
			try {
				readProxies(loadedProxies, is);
			} finally {
				is.close();
			}
		}
	}

	private static void readProxies(Set<String> loadedProxies,
			InputStream fis) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
		
		String line;
		while ((line=reader.readLine()) != null) {
			line = line.replace('"', ' ');
			line = line.replace(',', ' ');
			line = line.trim();
			loadedProxies.add(line);
		}
		reader.close();
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();

		try {
			String site = request.getParameter("site");
			String zone = request.getParameter("zone");
			String strCount = request.getParameter("count");
			String block = request.getParameter("block");
			String fn = request.getParameter("fn");
			
			int defaultCount = "raw".equalsIgnoreCase(site)? Integer.MAX_VALUE : 1;
			int count = defaultCount;
	
			if (site==null) site="";
			if (zone==null) zone="";
			
			if (StringUtils.isEmpty(fn)) {
				if (strCount!=null) {
					try {
						count = Integer.parseInt(strCount);
					} catch (Exception e) {
						count = defaultCount;
					}
				}
				
				List<String> proxies = getProxies(site, zone);
				
				if (proxies!=null) {
					List<String> results;
					if (count>1) {
						if (proxies.size()<=count) {
							results = proxies;
						} else {
							results = new ArrayList<String>(count);
							
							for (int i=0; i<count; i++) {
								results.add(proxies.get((int)(Math.random()*proxies.size())));
							}
						}
					} else if (proxies.size()>=1) {
						results = new ArrayList<String>(1);
						
						String[] px = proxies.toArray(new String[proxies.size()]);
						int startIndex = (int)(Math.random()*px.length);
						
						for (int i=0; i<px.length; i++) {
							String proxy = px[startIndex];
							AtomicInteger counter = siteAccessCounters.get(site);
							counter.incrementAndGet();
							if (!isSiteProxyBlocked(site, proxy)) {
								if ("true".equalsIgnoreCase(block)) {
									blockProxy(site, proxy);
								} else {
									tempBlockProxy(proxy);
								}
								results.add(proxy);
								break;
							}
							startIndex++;
							AtomicInteger failureCounter = siteAccessFailureCounters.get(site);
							failureCounter.incrementAndGet();
							if (startIndex>=px.length) {
								startIndex = 0;
							}
						}
					} else {
						results = Collections.emptyList();
					}
					for (String proxy : results) {
						out.println(proxy);
					}
				}
			} else if (fn.equalsIgnoreCase("block")) {
				String proxy =  request.getParameter("proxy");
				if (!StringUtils.isEmpty(proxy)) {
					blockProxy(site, proxy);
				}
			} else if (fn.equalsIgnoreCase("unblock")) {
				String proxy =  request.getParameter("proxy");
				if (!StringUtils.isEmpty(proxy)) {
					unblockProxy(site, proxy);
				}
			} else if (fn.equalsIgnoreCase("getUnlocated")) {
				try {
					Set<String> unlocatedProxies = ManagedProxy.getUnlocatedProxies();
					for (String proxy : unlocatedProxies) {
						out.println(proxy);
					}
				} catch (SQLException e) {
					throw new ServletException(e);
				}
			} else if (fn.equalsIgnoreCase("location")) {
				String proxy =  request.getParameter("proxy");
				String country =  request.getParameter("country");
				String region =  request.getParameter("region");
				String city =  request.getParameter("city");
				String isp =  request.getParameter("isp");
				int updated = 0;
				if (!StringUtils.isEmpty(proxy) && !StringUtils.isEmpty(country)) {
					updated = updateProxyLocation(proxy, country, region, city, isp);
				}
				if (updated==1) {
					out.println("Successfully updated: " + updated);
				} else {
					out.println("Failed to update");
				}
			} else if (fn.equalsIgnoreCase("getAvailUnknown")) {
				try {
					Set<String> availUnknonwnProxies = ManagedProxy.getAvailUnknownProxies();
					for (String proxy : availUnknonwnProxies) {
						out.println(proxy);
					}
				} catch (SQLException e) {
					throw new ServletException(e);
				}
			} else if (fn.equalsIgnoreCase("avail")) {
				String proxy =  request.getParameter("proxy");
				String avail =  request.getParameter("success");
				String create =  request.getParameter("create");
				
				try {
					if (reportProxyStatus(proxy, "true".equalsIgnoreCase(avail), !"false".equalsIgnoreCase(create))) {
						out.write("Success: location unknown");
					} else if ("true".equalsIgnoreCase(avail)) {
						out.write("Success: location known");
					} else {
						out.write("Success: unusable proxy");
					}
				} catch (Exception e) {
					throw new ServletException(e);
				}
			} else if (fn.equalsIgnoreCase("stats")) {
				writeStats(site, zone, out);
			} else if (fn.equalsIgnoreCase("reload")) {
				int c = 0;
				if (site.equalsIgnoreCase("raw")) {
					c = loadRawProxies();
				} else {
					c = reloadSiteProxies(site);
				}
				out.write(String.valueOf(c));
			} else if (fn.equalsIgnoreCase("help")) {
				printHelp(out);
			}
		} finally {
			out.close();
		}
	}
	
	private int updateProxyLocation(String proxyStr, String country, String region, String city, String isp) 
			throws ServletException {
		try {
			ManagedProxy proxy = ManagedProxy.loadProxy(proxyStr);
			
			if (proxy==null) {
				proxy = new ManagedProxy(proxyStr, country, region, city, isp);
				return ManagedProxy.insertProxy(proxy);
			} else {
				proxy.updateLocation(country, region, city, isp);
				return ManagedProxy.updateProxy(proxy);
			}
		} catch (SQLException e) {
			throw new ServletException(e);
		}
	}

	private void printHelp(PrintWriter out) {
		out.println("fn=");
		out.println("?site(raw,weibo,baidu)&zone(chn,cnd)&count(1)");
		out.println("help");
		out.println("reload?site(raw)");
		out.println("stats?site(raw,weibo,baidu)&zone(chn,cnd)");
		out.println("block?proxy");
		out.println("location?proxy&country&region&city&isp");
		out.println("unblock?proxy");
		out.println("getUnlocated");
	}

	private void writeStats(String site, String zone, PrintWriter out) {
		List<String> proxies;
		if (StringUtils.isEmpty(site)) {
			for (String key : sProxies.keySet()) {
				out.println(key + ": " + sProxies.get(key).size());
			}
			out.println("----");
			out.println("raw: " + sRawProxies.get().length);
		} else {
			if (site.equalsIgnoreCase("raw")) {
				proxies = Arrays.asList(sRawProxies.get());
			} else {
				proxies = getProxies(site, zone);
			}
			
			if (proxies==null) {
				out.println("Not proxy found for " + site + ":" + zone);
			} else {
				if (site.equalsIgnoreCase("raw")) {
					out.println("Total raw proxies: " + proxies.size());
				} else {
					out.println("Total raw proxies: " + sRawProxies.get().length);
					out.print("Temporarily blocked proxies: ");
					Util.writeBlockerStats(out, tempProxyBlocker);
					
					if (site.equalsIgnoreCase("yaris")) {
						out.println("Total realtime proxies: " + sRealtimeProxies.get().length);
						out.print("Blocked realtime proxies: ");
						Util.writeBlockerStats(out, getRealtimeSiteBlocker(site));
						out.print("Site realtime access (failure/total): ");
						writeAccessStats(out, siteRealtimeAccessCounters.get(site), siteRealtimeAccessFailureCounters.get(site));
					}
					
					out.println("Stat for site: " + site + " (zone: " + zone + ")");
					out.println("Total proxies: " + proxies.size());
					Blocker blocker = getSiteBlocker(site);
					
					
					if (blocker==null) {
						out.println("No blockers");
					} else {
						out.print("Total blocked : ");
						Util.writeBlockerStats(out, blocker);
					}
					out.print("Site access (failure/total): ");
					writeAccessStats(out, siteAccessCounters.get(site), siteAccessFailureCounters.get(site));
					
					if (site.equalsIgnoreCase("weibo")) {
						out.print("Long term blocked : ");
						Util.writeBlockerStats(out, weiboLongBlocker);
					}
				}
			}
		}
	}

	private void writeAccessStats(PrintWriter out, AtomicInteger accessCounter, AtomicInteger accessFailureCounter) {
		out.println((accessFailureCounter==null? "null" : String.valueOf(accessFailureCounter.get())) 
				+ " : " + (accessCounter==null? "null" : String.valueOf(accessCounter.get())) );
	}

	private void blockProxy(String site, String proxy) {
		Blocker siteBlocker = getSiteBlocker(site);
		if (siteBlocker!=null) {
			siteBlocker.block(proxy);
		}
	}
	
	private boolean blockRealtimeProxy(String site, String proxy) {
		Blocker siteBlocker = getRealtimeSiteBlocker(site);
		if (siteBlocker!=null && !siteBlocker.isBlocked(proxy)) {
			siteBlocker.block(proxy);
			return true;
		} else {
			return false;
		}
	}


	private void unblockProxy(String site, String proxy) {
		Blocker siteBlocker = getSiteBlocker(site);
		if (siteBlocker!=null) {
			siteBlocker.unblock(proxy);
		}
	}

	private Blocker getSiteBlocker(String site) {
		if (site.equalsIgnoreCase("weibo_long")) {
			return weiboLongBlocker;
		} else {
			return siteBlockers.get(site);
		}
	}

	private Blocker getRealtimeSiteBlocker(String site) {
		return realtimeSiteBlockers.get(site);
	}

	private void tempBlockProxy(String proxy) {
		tempProxyBlocker.block(proxy);
	}

	private boolean isSiteProxyBlocked(String site, String proxy) {
		Blocker siteBlocker = getSiteBlocker(site);
		if (siteBlocker!=null && siteBlocker.isBlocked(proxy)) {
			return true;
		} else {
			if (site.equalsIgnoreCase("weibo") && weiboLongBlocker.isBlocked(proxy)) {
				return true;
			}
			return tempProxyBlocker.isBlocked(proxy);
		}
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		
		String site = request.getParameter("site");
		String zone = request.getParameter("zone");
		
//		String title = "Database Result";
//		String docType = "<!doctype html public \"-//w3c//dtd html 4.0 "
//				+ "transitional//en\">\n";
//		out.println(docType + "<html>\n" + "<head><title>" + title
//				+ "</title></head>\n" + "<body bgcolor=\"#f0f0f0\">\n"
//				+ "<h1 align=\"center\">" + title + "</h1>\n");
		Connection conn=null;
		PreparedStatement stmt=null;
		try {
			BufferedReader reader = request.getReader();
			StringBuffer buf = new StringBuffer();
			
			List<String> proxies = new ArrayList<String>();
			String proxy = null;
			do {
				proxy = reader.readLine();
				
				if (!StringUtils.isEmpty(proxy)) {
					proxies.add(proxy);
					buf.append(proxy).append("\n");
				}
			} while (proxy!=null);
			
			String key = site + ":" + zone;
			sProxies.put(key, proxies);
			
			// Open a connection
			conn = DbAccess.getDbConnection();
			
			try {
				String sql = "INSERT INTO active_proxies (site, zone, active_proxies, test_time, tester_host) " +
						"VALUES (?, ?, ?, ?, ?)";

				// Execute SQL query
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, site);
				stmt.setString(2, zone);
				stmt.setString(3, buf.toString());
				stmt.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
				stmt.setString(5, request.getRemoteHost());
				
				stmt.execute();
				// Set response content type
			} catch (SQLException e) {
				DbAccess.cleanup(null, stmt, null);
				
				String sql = "UPDATE active_proxies SET active_proxies=?, test_time=?, tester_host=? " +
						"WHERE site=? AND zone=?";

				// Execute SQL query
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, buf.toString());
				stmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
				stmt.setString(3, request.getRemoteHost());
				stmt.setString(4, site);
				stmt.setString(5, zone);
				
				stmt.execute();
			}
			
			out.println("success");
		} catch (Exception e) {
			// Handle errors for Class.forName
			e.printStackTrace();
			out.println("Error");
		} finally {
			out.close();
			DbAccess.cleanup(conn, stmt, null);
		}
	}

	public static boolean reportProxyStatus(String proxy, boolean success, boolean create) throws SQLException {
		ManagedProxy managedProxy = ManagedProxy.loadProxy(proxy);
		
		if (managedProxy ==null) {
			if (success || create) {
				managedProxy = new ManagedProxy(proxy);
				if (success) {
					managedProxy.reportSuccess();
				} else {
					managedProxy.reportFailure();
				}
				ManagedProxy.insertProxy(managedProxy);
			}
		} else {
			if (success) {
				managedProxy.reportSuccess();
			} else {
				managedProxy.reportFailure();
			}
			ManagedProxy.updateProxy(managedProxy);
		}
		if (managedProxy!=null && StringUtils.isEmpty(managedProxy.getCountry())) {
			return true;
		} else {
			return false;
		}
	}

	public static List<String> getProxies(String site, String zone) {
		List<String> proxies;
		if (site.equalsIgnoreCase("raw")) {
			if (zone.equalsIgnoreCase("reload")) {
				loadRawProxies();
			}
			proxies = Arrays.asList(sRawProxies.get());
		} else {
			String key = site + ":" + zone;
			proxies = sProxies.get(key);
		}
		return proxies;
	}
}
