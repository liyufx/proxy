package proxy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import db.DbAccess;

public class ManagedProxy {

	private String proxy;
	private String country;
	private String region;
	private String city;
	private String isp;
	private Timestamp lastSuccessTime;
	private Timestamp lastFailureTime;

	public static ManagedProxy loadProxy(String proxy)  throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ManagedProxy managedProxy = null;
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "SELECT ip, country, region, city, ISP, is_proxy, last_success_time, last_failure_time " +
					"FROM ip_addr WHERE ip = ? and is_proxy=1";
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, proxy);
			rs = stmt.executeQuery();
			
			if (rs.next()) {
				managedProxy = extractProxy(rs);
			}
			return managedProxy;
		} finally {
			DbAccess.cleanup(conn, stmt, rs);
		}
	}

	public static Set<String> getUnlocatedProxies() throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		Set<String> results = new HashSet<String>();
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "SELECT ip FROM ip_addr WHERE country is null";
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			
			while (rs.next()) {
				results.add(rs.getString(1));
			}
			return results;
		} finally {
			DbAccess.cleanup(conn, stmt, rs);
		}
	}
	
	private static ManagedProxy extractProxy(ResultSet rs) throws SQLException {
		ManagedProxy proxy = new ManagedProxy(rs.getString(1));
		proxy.country = rs.getString(2);
		proxy.region = rs.getString(3);
		proxy.city = rs.getString(4);
		proxy.isp = rs.getString(5);
		proxy.lastSuccessTime = rs.getTimestamp(7);
		proxy.lastFailureTime = rs.getTimestamp(8);
		return proxy;
	}

	public static List<ManagedProxy> getActiveProxies(String country, String city)  throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		List<ManagedProxy> proxies = new ArrayList<ManagedProxy>();
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "SELECT ip, country, region, city, ISP, is_proxy, last_success_time, last_failure_time " +
					"FROM ip_addr WHERE country=? ";
			
			if (!StringUtils.isEmpty(city)) {
				sql += " AND city=? ";
			}
			sql += "AND last_success_time IS NOT NULL AND " +
					"(last_failure_time IS NULL OR last_success_time>last_failure_time)";
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, country);
			if (!StringUtils.isEmpty(city)) {
				stmt.setString(2, city);
			}
			rs = stmt.executeQuery();
			
			while (rs.next()) {
				proxies.add(extractProxy(rs));
			}
			return proxies;
		} finally {
			DbAccess.cleanup(conn, stmt, rs);
		}
	}

	public static Set<String> getAvailUnknownProxies() throws SQLException{
		Set<String> allProxies = new HashSet<String>(ProxyServlet.getProxies("weibo", "chn"));

		{
			Connection conn = null;
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try {
				conn = DbAccess.getDbConnection();
				
				String sql = "SELECT ip FROM ip_addr";
				
				stmt = conn.prepareStatement(sql);
				rs = stmt.executeQuery();
				
				while (rs.next()) {
					allProxies.add(rs.getString(1));
				}
			} finally {
				DbAccess.cleanup(conn, stmt, rs);
			}
		}

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "SELECT ip FROM ip_addr WHERE (last_success_time IS NOT NULL AND " +
					"(last_failure_time IS NULL or last_success_time>last_failure_time) AND " +
					"last_success_time>?) OR last_failure_time>?";
			
			Timestamp ts = new Timestamp(System.currentTimeMillis()- 12*3600000L);
			stmt = conn.prepareStatement(sql);
			stmt.setTimestamp(1, ts);
			stmt.setTimestamp(2, ts);
			rs = stmt.executeQuery();
			
			while (rs.next()) {
				allProxies.remove(rs.getString(1));
			}
			return allProxies;
		} finally {
			DbAccess.cleanup(conn, stmt, rs);
		}
	}

	public static int insertProxy(ManagedProxy proxy) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "INSERT INTO ip_addr (" +
					"country, region, city, ISP, is_proxy, last_success_time, last_failure_time, ip) " +
					"VALUES (?,?,?,?,?,?,?,?)";
			stmt = conn.prepareStatement(sql);
			setAccountParams(stmt, proxy);
			int result = stmt.executeUpdate();
			return result;
		} finally {
			DbAccess.cleanup(conn, stmt, null);
		}
	}

	public static int updateProxy(ManagedProxy proxy) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "UPDATE ip_addr SET " +
					"country=?, region=?, city=?, ISP=?, is_proxy=?, last_success_time=?, last_failure_time=? WHERE ip=?";
			stmt = conn.prepareStatement(sql);
			setAccountParams(stmt, proxy);
			int result = stmt.executeUpdate();
			return result;
		} finally {
			DbAccess.cleanup(conn, stmt, null);
		}
	}

	private static void setAccountParams(PreparedStatement stmt, ManagedProxy proxy) throws SQLException {
		stmt.setString(1, proxy.getCountry());
		stmt.setString(2, proxy.getRegion());
		stmt.setString(3, proxy.getCity());
		stmt.setString(4, proxy.getIsp());
		stmt.setInt(5, 1);
		stmt.setTimestamp(6, proxy.getLastSuccessTime());
		stmt.setTimestamp(7, proxy.getLastFailureTime());
		stmt.setString(8, proxy.getProxy());
	}

	public ManagedProxy(String proxy) {
		this.proxy = proxy;
	}

	public ManagedProxy(String proxy, String country, String region, String city, String isp) {
		this.proxy = proxy;
		this.country = country;
		this.region = region;
		this.city = city;
		this.isp = isp;
	}

	public void updateLocation(String country, String region, String city, String isp) {
		if (!StringUtils.isEmpty(country)) {
			this.country = country;
		}
		if (!StringUtils.isEmpty(region)) {
			this.region = region;
		}
		if (!StringUtils.isEmpty(city)) {
			this.city = city;
		}
		if (!StringUtils.isEmpty(isp)) {
			this.isp = isp;
		}
	}

	public void reportSuccess() {
		this.lastSuccessTime = new Timestamp(System.currentTimeMillis());
	}
	
	public void reportFailure() {
		this.lastFailureTime = new Timestamp(System.currentTimeMillis());
	}
	
	public String getProxy() {
		return proxy;
	}

	public String getCountry() {
		return country;
	}

	public String getRegion() {
		return region;
	}

	public String getCity() {
		return city;
	}

	public String getIsp() {
		return isp;
	}

	public Timestamp getLastSuccessTime() {
		return lastSuccessTime;
	}

	public Timestamp getLastFailureTime() {
		return lastFailureTime;
	}
}
