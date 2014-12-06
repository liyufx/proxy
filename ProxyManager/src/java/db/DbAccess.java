package db;

//Loading required libraries
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import util.Home;
import util.Util;

import java.sql.*;

public class DbAccess {

	static {
		// Register JDBC driver
		try {
			Class.forName("com.mysql.jdbc.Driver");
			prepareDb();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	static void prepareDb() {
		try {
			File configHome = new File(Home.getTomcatHome(), "proxies");
			File dbConfigFile = new File(configHome, "db.config");
			if (dbConfigFile.isFile()) {
				String[] lines = Util.readFileLines(dbConfigFile);
				
				for (String line : lines) {
					line=line.trim();
					String[] vals = line.split("=");
					if (vals.length==2) {
						String key = vals[0].trim();
						String value = vals[1].trim();
						if (value.endsWith(";")) {
							value = value.substring(0, value.length()-1);
							value = value.trim();
						}
						
						if (key.equalsIgnoreCase("db.url")) {
							DB_URL = value;
						} else if (key.equalsIgnoreCase("db.user")) {
							USER = value;
						} else if (key.equalsIgnoreCase("db.password")) {
							PASS = value;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Connection conn=null;
		Statement stmt=null;
		String sqls[] = new String[]{};
		try {
			// Open a connection
			if (sqls.length>0) {
				conn = DriverManager.getConnection(DB_URL, USER, PASS);
				stmt = conn.createStatement();
			
				for (String sql : sqls) {
					stmt.execute(sql);
				}
			}
		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		} catch (Exception e) {
			// Handle errors for Class.forName
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt, null);
		} // end try
	
	}
	
	// JDBC driver name and database URL
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static String DB_URL = "jdbc:mysql://ec2-54-221-85-14.compute-1.amazonaws.com/ywb";

	// Database credentials
	private static String USER = "ybn";
	private static String PASS = "Alley1fx";

	public static Connection getDbConnection() throws SQLException {
		return DriverManager.getConnection(DB_URL, USER, PASS);
	}
	
	public static void cleanup(Connection conn, Statement stmt, ResultSet rs) {
		// finally block used to close resources
		try {
			if (rs!=null) {
				rs.close();
			}
		} catch (SQLException se1) {
		}// nothing we can do
		try {
			if (stmt != null)
				stmt.close();
		} catch (SQLException se2) {
		}// nothing we can do
		try {
			if (conn != null)
				conn.close();
		} catch (SQLException se3) {
			se3.printStackTrace();
		}// end finally try
	}
}