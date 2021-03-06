package weibo;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import proxy.ManagedProxy;
import proxy.ProxyServlet;

import util.Util;
import weibo.WeiboAccount.AccountStatus;


public class WeiboAccountsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();

		try {
			String function = request.getParameter("fn");
			
			if (StringUtils.isEmpty(function)) {
				out.println(WeiboAccount.getTotalAccounts());
			} else if (function.equalsIgnoreCase("help")) {
				printHelp(out);
			} else if (function.equalsIgnoreCase("reload")) {
				WeiboAccount.reload();
				out.println(WeiboAccount.getTotalAccounts());
			} else if (function.equalsIgnoreCase("reset")) {
				Integer count = Util.getInt(request.getParameter("count"));
				
				WeiboAccount.reset();
				WeiboAccount.trimTo(count);
				out.println(WeiboAccount.getTotalAccounts());
			} else if (function.equalsIgnoreCase("getbulk")) {
				int count = Util.getInt(request.getParameter("count"), 1);
				WeiboAccount[] accounts = WeiboAccount.getRandomAccounts(count);
				for (WeiboAccount acc : accounts) {
					out.println("\"" + acc.getEmail() + "\", \"" + acc.getPwd() + 
							"\", \"" + acc.getProxy() + "\"");
				}
			} else if (function.equalsIgnoreCase("get")) {
				WeiboAccount acc = WeiboAccount.getActiveAccount();
				if (acc!=null) {
					out.print("\"" + acc.getEmail() + "\", \"" + acc.getPwd() + 
								"\", \"" + acc.getProxy() + "\"");
				}
			} else if (function.equalsIgnoreCase("getNormal")) {
				String blocker = request.getParameter("blocker");
				WeiboAccount acc = WeiboAccount.getNormalAccount(blocker);
				if (acc!=null) {
					out.print("\"" + acc.getEmail() + "\", \"" + acc.getPwd() + 
								"\", \"" + acc.getProxy() + "\"");
				}
			} else if (function.equalsIgnoreCase("getProxy")) {
				String blocker = request.getParameter("blocker");
				String proxy = WeiboAccount.getNormalProxy(blocker);
				if (proxy!=null) {
					out.print(proxy);
				}
			} else if (function.equalsIgnoreCase("getAltProxies")) {
				String proxy = request.getParameter("proxy");
				List<String> altProxies = getAltProxies(proxy);
				int startingPoint = (int)(Math.random()*altProxies.size());
				for (int i=0; i<altProxies.size(); i++) {
					out.println(altProxies.get(startingPoint));
					startingPoint++;
					if (startingPoint>=altProxies.size()) {
						startingPoint=0;
					}
				}
			} else if (function.equalsIgnoreCase("getAllProxy")) {
				Set<String> proxies = WeiboAccount.getInUseProxies();
				String zone = request.getParameter("zone");
				proxies.addAll(ProxyServlet.getProxies("weibo", StringUtils.isEmpty(zone)? "chn" : zone));
				for (String proxy : proxies) {
					out.println(proxy);
				}
			} else if (function.equalsIgnoreCase("getNew")) {
				WeiboAccount acc = WeiboAccount.getUninitializedAccount();
				if (acc!=null) {
					out.print("\"" + acc.getEmail() + "\", \"" + acc.getPwd() + 
								"\", \"" + acc.getProxy() + "\"");
				}
			} else if (function.equalsIgnoreCase("new")) {
				String email = request.getParameter("email");
				String pwd = request.getParameter("pwd");
				String proxy = request.getParameter("proxy");
				newAccount(email, pwd, proxy);
				out.write("success");
//			} else if (function.equalsIgnoreCase("init")) {
				
			} else if (function.equalsIgnoreCase("login")) {
				String email = request.getParameter("email");
				String proxy = request.getParameter("proxy");
				String level = request.getParameter("level");
				if (!StringUtils.isEmpty(email)) {
					registerAccountLogin(email, proxy, level);
					out.write("success");
				} else {
					out.write("Error: no account");
				}
			} else if (function.equalsIgnoreCase("loginfailure")) {
				String email = request.getParameter("email");
				String proxy = request.getParameter("proxy");
				String reason = request.getParameter("reason");
				if (!StringUtils.isEmpty(email)) {
					registerAccountLoginFailure(email, proxy, reason);
					out.write("success");
				} else {
					out.write("Error: no account");
				}
			} else if (function.equalsIgnoreCase("update")) {
				String email = request.getParameter("email");
				String proxy = request.getParameter("proxy");
				String weiboId = request.getParameter("weiboid");
				String nick = request.getParameter("nick");
				if (!StringUtils.isEmpty(email)) {
					updateAccount(email, proxy, weiboId, nick);
					out.write("success");
				} else {
					out.write("error: no account");
				}
			} else if (function.equalsIgnoreCase("stats")) {
				WeiboAccount.writeStats(out);
			}
		} catch (Exception e) {
			out.write("error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			out.close();
		}
	}

	private List<String> getAltProxies(String proxy) throws ServletException {
		try {
			ManagedProxy p = ManagedProxy.loadProxy(proxy);
			List<String> results = new ArrayList<String>();
			if (p!=null & p.getCountry()!=null) {
				List<ManagedProxy> altProxies;
				if (p.getCountry().equalsIgnoreCase("China")) {
					if (p.getCity()!=null) {
						altProxies = ManagedProxy.getActiveProxies(p.getCountry(), p.getCity());
					} else {
						altProxies = new ArrayList<ManagedProxy>();
					}
				} else {
					altProxies = ManagedProxy.getActiveProxies(p.getCountry(), null);
				}
				for (ManagedProxy ap : altProxies) {
					if (!ap.getProxy().equals(proxy) && 
							!WeiboAccount.isProxyBlocked(ap.getProxy())) {
						results.add(ap.getProxy());
					}
				}
			}
			return results;
		} catch (SQLException e) {
			throw new ServletException(e);
		}
	}

	private void printHelp(PrintWriter out) {
		out.println("fn=");
		out.println("help");
		out.println("stats");
		out.println("reload");
		out.println("reset?count");
		out.println("getbulk?count");
		out.println("get");
		out.println("getNew");
		out.println("getNormal?blocker");
		out.println("getProxy?blocker");
		out.println("getAllProxy");
		out.println("new?email&pwd&proxy");
		out.println("login?email&proxy");
		out.println("loginfailure?email&proxy&reason");
		out.println("update?email&proxy&weiboid&nick");
	}

	private void registerAccountLoginFailure(String email, String proxy, String reason) throws SQLException {
		WeiboAccount acc = WeiboAccount.getAccount(email);
		
		if (acc!=null) {
			if (proxy.equals(acc.getProxy()) || AccountStatus.FROZEN.toString().equals(reason) || 
					AccountStatus.SEMIAUTO.toString().equals(reason)) {
				acc.setLastFailureDate(new Timestamp(System.currentTimeMillis()));
				acc.updateStatus(reason);
				WeiboAccount.updateWeiboAccount(acc);
			}
			
			if (reason.equalsIgnoreCase("PROXY_ERROR")) {
				ProxyServlet.reportProxyStatus(proxy, false, true);
			} else {
				ProxyServlet.reportProxyStatus(proxy, true, true);
			}
		} else {
			throw new IllegalArgumentException("Not account found for " + email);
		}
	}

	private void registerAccountLogin(String email, String proxy, String level) throws SQLException {
		WeiboAccount acc = WeiboAccount.getAccount(email);
		
		if (acc!=null) {
			acc.updateProxy(proxy);
			acc.setLastLoginDate(new Timestamp(System.currentTimeMillis()));
			acc.setStatus(AccountStatus.NORMAL);
			if (level!=null) {
				level = level.trim();
			}
			if (!StringUtils.isEmpty(level)) {
				acc.setLevel(level);
			}
			WeiboAccount.updateWeiboAccount(acc);
			WeiboAccount.blockAccount(acc);
			
			ProxyServlet.reportProxyStatus(proxy, true, true);
		} else {
			throw new IllegalArgumentException("Not account found for " + email);
		}
	}

	private void updateAccount(String email, String proxy, String weiboId, String nick) throws SQLException {
		WeiboAccount acc = WeiboAccount.getAccount(email);
		
		if (acc!=null) {
			if (!StringUtils.isEmpty(weiboId)) {
				acc.setWeiboId(weiboId);
			}
			if (!StringUtils.isEmpty(nick)) {
				acc.setNick(nick);
			}
			if (acc.getInitDate()==null) {
				acc.setInitDate(new Timestamp(System.currentTimeMillis()));
			}
			acc.updateProxy(proxy);
			acc.setStatus(AccountStatus.NORMAL);
			WeiboAccount.updateWeiboAccount(acc);
		} else {
			throw new IllegalArgumentException("Not account found for " + email);
		}
	}

	private void newAccount(String email, String pwd, String proxy) throws SQLException {
		WeiboAccount acc = new WeiboAccount(email, pwd, proxy);
		acc.setCreateDate(new Timestamp(System.currentTimeMillis()));
		acc.setStatus(AccountStatus.CREATED);
		WeiboAccount.insertWeiboAccount(acc);
		ProxyServlet.reportProxyStatus(proxy, true, true);
	}
}
