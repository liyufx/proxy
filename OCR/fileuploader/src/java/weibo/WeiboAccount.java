package weibo;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.StringUtils;

import util.Blocker;
import util.BlockerImpl;
import util.Home;
import util.Util;

import db.DbAccess;

public class WeiboAccount {
	public enum AccountStatus{
		CREATED,
		NORMAL,
		SEMIAUTO,
		FROZEN,
		RESTRICTED,
		FORBIDDEN,
		ERROR,
	};

	private final static int BLOCK_ACCOUNT_HOURS = 80;
	private final static int TEMP_BLOCK_ACCOUNT_HOURS = 20;
	private final static int BLOCK_PROXY_HOURS = 10;
	private final static int TEMP_BLOCK_PROXY_HOURS = 5;
	
	private static AtomicReference<Map<String, WeiboAccount>> sAllAccounts;
	
	public static Blocker sBlockedAccounts;
	public static Blocker sTempBlockedAccounts;
	public static Blocker sBlockedProxies;
	public static Blocker sTempBlockedProxies;
	private static Map<String, Blocker> sCustomBlocker;
	
	static {
		reset();
		
		Thread weiboAccountBlocker = new Thread(new Runnable() {
			@Override
			public void run() {
				do {
					try {
						Thread.sleep(360000);
						sBlockedAccounts.expire(1);
						sBlockedProxies.expire(1);
						sTempBlockedAccounts.expire(1);
						sTempBlockedProxies.expire(1);
						for (Blocker blocker : sCustomBlocker.values()) {
							blocker.expire(1);
						}
					} catch (Exception e) {
						
					}
				} while (true);
			}
		}, "weibo_account_blocker");
		weiboAccountBlocker.setDaemon(true);
		weiboAccountBlocker.start();
	}

	public static void reset() {
		try {
			sAllAccounts = new AtomicReference<Map<String, WeiboAccount>>();
			sAllAccounts.set(loadAllWeiboAccounts());
			
			sCustomBlocker = new HashMap<String, Blocker>();
			
			File configHome = new File(Home.getTomcatHome(), "proxies");
			File blockerConfigFile = new File(configHome, "WeiboBlocker.txt");
			
			if (blockerConfigFile.isFile()) {
				String[] lines = Util.readFileLines(blockerConfigFile);
				
				for (String line : lines) {
					String[] vals = line.split("=");
					if (vals.length==2) {
						Integer ticks = Util.getInt(vals[1]);
						if (ticks!=null) {
							sCustomBlocker.put(vals[0], new BlockerImpl(ticks));
						}
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		sBlockedAccounts = new BlockerImpl(BLOCK_ACCOUNT_HOURS);
		sTempBlockedAccounts = new BlockerImpl(TEMP_BLOCK_ACCOUNT_HOURS);
		sBlockedProxies = new BlockerImpl(BLOCK_PROXY_HOURS);
		sTempBlockedProxies = new BlockerImpl(TEMP_BLOCK_PROXY_HOURS);
	}
	
	
/* 
CREATE TABLE `weibo_accounts` (
	`email` VARCHAR(50) NOT NULL,
	`pwd` VARCHAR(20) NOT NULL,
	`proxy` VARCHAR(32) NOT NULL,
	`proxy1` VARCHAR(32) NULL DEFAULT NULL,
	`proxy2` VARCHAR(32) NULL DEFAULT NULL,
	`weibo_id` VARCHAR(12) NULL DEFAULT NULL,
	`nick` VARCHAR(32) NULL DEFAULT NULL,
	`enabled` BIT(1) NOT NULL,
	`create_date` DATETIME NOT NULL,
	`init_date` DATETIME NULL DEFAULT NULL,
	`last_login_date` DATETIME NOT NULL,
	`last_failure_date` DATETIME NOT NULL,
	PRIMARY KEY (`email`),
	INDEX `proxy` (`proxy`),
	INDEX `weibo_id` (`weibo_id`)
)
*/	
	private String mEmail;
	private String mPwd;
	private String mProxy;
	private String mProxy1;
	private String mProxy2;
	private String mWeiboId;
	private String mNick;
	private boolean mEnabled;
	private AccountStatus mStatus;
	private String mError;
	private Timestamp mCreateDate;
	private Timestamp mInitDate;
	private Timestamp mLastLoginDate;
	private Timestamp mLastFailureDate;
	private String mLevel;
	
	public WeiboAccount(String email, String pwd, String proxy) {
		mEmail = email;
		mPwd = pwd;
		mProxy = proxy;
		mEnabled = true;
	}
	
	public boolean isEnabled() {
		return mEnabled;
	}

	public void setEnabled(boolean enabled) {
		this.mEnabled = enabled;
	}

	public AccountStatus getStatus() {
		return mStatus;
	}

	public void setStatus(AccountStatus status) {
		this.mStatus = status;
	}

	public Timestamp getLastLoginDate() {
		return mLastLoginDate;
	}

	public void setLastLoginDate(Timestamp lastLoginDate) {
		this.mLastLoginDate = lastLoginDate;
	}

	public Timestamp getLastFailureDate() {
		return mLastFailureDate;
	}

	public void setLastFailureDate(Timestamp lastFailureDate) {
		this.mLastFailureDate = lastFailureDate;
	}

	public String getPwd() {
		return mPwd;
	}
	public void setPwd(String mPwd) {
		this.mPwd = mPwd;
	}
	public String getProxy() {
		return mProxy;
	}
	public void setProxy(String mProxy) {
		this.mProxy = mProxy;
	}
	public String getProxy1() {
		return mProxy1;
	}
	public void setProxy1(String mProxy1) {
		this.mProxy1 = mProxy1;
	}
	public String getProxy2() {
		return mProxy2;
	}
	public void setProxy2(String mProxy2) {
		this.mProxy2 = mProxy2;
	}
	public String getWeiboId() {
		return mWeiboId;
	}
	public void setWeiboId(String mWeiboId) {
		this.mWeiboId = mWeiboId;
	}
	public String getNick() {
		return mNick;
	}
	public void setNick(String mNick) {
		this.mNick = mNick;
	}
	public Timestamp getCreateDate() {
		return mCreateDate;
	}
	public void setCreateDate(Timestamp mCreateDate) {
		this.mCreateDate = mCreateDate;
	}
	public Timestamp getInitDate() {
		return mInitDate;
	}
	public void setInitDate(Timestamp mInitDate) {
		this.mInitDate = mInitDate;
	}
	public String getEmail() {
		return mEmail;
	}
	public String getError() {
		return mError;
	}
	public void setError(String mError) {
		this.mError = mError;
	}
	
	public String getStatusString() {
		if (mStatus==null || mStatus==AccountStatus.ERROR) {
			String result = mError;
			while (result.getBytes().length>1023) {
				result = result.substring(0, result.length()/2-1);
			}
			return result;
		} else {
			return mStatus.toString();
		}
	}
	
	public String getLevel() {
		return mLevel;
	}
	
	public WeiboAccount setLevel(String level) {
		mLevel = level;
		return this;
	}
	
	public void updateProxy(String proxy) {
		if (!StringUtils.isEmpty(proxy)) {
			if (!proxy.equals(mProxy)) {
				if (StringUtils.isEmpty(mProxy1)) {
					setProxy1(proxy);
				} else if (StringUtils.isEmpty(mProxy2)) {
					setProxy2(proxy);
				} else {
					setProxy1(mProxy2);
					setProxy2(proxy);
				}
			} 
		}
	}
	
	public void updateStatus(String reason) {
		if (!StringUtils.isEmpty(reason)) {
			try {
				AccountStatus status = AccountStatus.valueOf(reason.toUpperCase());
				if (mStatus!=AccountStatus.FROZEN || 
						status==AccountStatus.NORMAL || status==AccountStatus.RESTRICTED) {
					setStatus(status);
					setError(null);
				}
			} catch (Exception e) {
				if (mStatus!=AccountStatus.FROZEN) {
					setStatus(AccountStatus.ERROR);
					setError(reason);
				}
			}
		}
	}
	
	public static WeiboAccount loadWeiboAccount(String email) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		WeiboAccount account = null;
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "SELECT email, pwd, proxy, proxy1, proxy2, weibo_id, nick, " +
					"create_date, init_date FROM weibo_accounts, level WHERE email = ?";
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, email);
			rs = stmt.executeQuery();
			
			if (rs.next()) {
				account = extractAccount(rs);
			}
			return account;
		} finally {
			DbAccess.cleanup(conn, stmt, rs);
		}
	}

	private static WeiboAccount extractAccount(ResultSet rs)
			throws SQLException {
		WeiboAccount account;
		account = new WeiboAccount(rs.getString(1), rs.getString(2), rs.getString(3));
		account.setProxy1(rs.getString(4));
		account.setProxy2(rs.getString(5));
		account.setWeiboId(rs.getString(6));
		account.setNick(rs.getString(7));
		account.setCreateDate(rs.getTimestamp(8));
		account.setInitDate(rs.getTimestamp(9));
		account.setEnabled(rs.getBoolean(10));
		account.setLastLoginDate(rs.getTimestamp(11));
		account.setLastFailureDate(rs.getTimestamp(12));
		account.updateStatus(rs.getString(13));
		account.setLevel(rs.getString(14));
		return account;
	}
	
	public static int insertWeiboAccount(WeiboAccount account) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "INSERT INTO weibo_accounts (" +
					"pwd, proxy, proxy1, proxy2, weibo_id, nick, create_date, init_date, enabled, last_login_date, last_failure_date, status, level, email) " +
					"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			stmt = conn.prepareStatement(sql);
			setAccountParams(stmt, account);
			int result = stmt.executeUpdate();
			cacheAccount(account);
			return result;
		} finally {
			DbAccess.cleanup(conn, stmt, null);
		}
	}

	private static void cacheAccount(WeiboAccount account) {
		sAllAccounts.get().put(account.getEmail(), account);
	}
	
	public static int updateWeiboAccount(WeiboAccount account) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "UPDATE weibo_accounts SET " +
					"pwd=?, proxy=?, proxy1=?, proxy2=?, weibo_id=?, nick=?, create_date=?, init_date=?, " +
					"enabled=?, last_login_date=?, last_failure_date=?, status=?, level=? WHERE email=?";
			stmt = conn.prepareStatement(sql);
			setAccountParams(stmt, account);
			int result = stmt.executeUpdate();
			cacheAccount(account);
			return result;
		} finally {
			DbAccess.cleanup(conn, stmt, null);
		}
	}
	
	private static void setAccountParams(PreparedStatement stmt,
			WeiboAccount account) throws SQLException {
		stmt.setString(1, account.getPwd());
		stmt.setString(2, account.getProxy());
		stmt.setString(3, account.getProxy1());
		stmt.setString(4, account.getProxy2());
		stmt.setString(5, account.getWeiboId());
		stmt.setString(6, account.getNick());
		stmt.setTimestamp(7, account.getCreateDate());
		stmt.setTimestamp(8, account.getInitDate());
		stmt.setInt(9, account.isEnabled()? 1 : 0);
		stmt.setTimestamp(10, account.getLastLoginDate());
		stmt.setTimestamp(11, account.getLastFailureDate());
		stmt.setString(12, account.getStatusString());
		stmt.setString(13, account.getLevel());
		
		stmt.setString(14, account.getEmail());
	}

	private static Map<String, WeiboAccount> loadAllWeiboAccounts() throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		Map<String, WeiboAccount> accounts = new ConcurrentHashMap<String, WeiboAccount>();
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "SELECT email, pwd, proxy, proxy1, proxy2, weibo_id, nick, " +
					"create_date, init_date, enabled, last_login_date, last_failure_date, status, level FROM weibo_accounts";
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			
			while (rs.next()) {
				WeiboAccount account = extractAccount(rs);
				accounts.put(account.getEmail(), account);
			}
			return accounts;
		} finally {
			DbAccess.cleanup(conn, stmt, rs);
		}
	}

	public static WeiboAccount getAccount(String email) {
		Map<String, WeiboAccount> accounts = sAllAccounts.get();
		
		WeiboAccount acc = accounts.get(email);
		
		if (acc==null) {
			try {
				acc = loadWeiboAccount(email);
			} catch (SQLException e) {
				acc = null;
				e.printStackTrace();
			}
			if (acc!=null) {
				cacheAccount(acc);
			}
		}
		
		return acc;
	}
	
	public static int getTotalAccounts() {
		return sAllAccounts.get().size();
	}

	public static WeiboAccount getActiveAccount() {
		return getUnblockedAccount(true);
	}
	
	public static WeiboAccount getUninitializedAccount() {
		return getUnblockedAccount(false);
	}
	
	public static WeiboAccount getNormalAccount(String blocker) {
		Blocker bk = getCustomBlocker(blocker);
		List<WeiboAccount> accs = new ArrayList<WeiboAccount>(sAllAccounts.get().values());
		
		int index = (int)(Math.random()*accs.size());
		for (int i=0; i<accs.size(); i++) {
			if (index>=accs.size()) index = 0;
			WeiboAccount acc = accs.get(index);
			if (acc.isEnabled() && acc.getInitDate()!=null && acc.getStatus()==AccountStatus.NORMAL) {
				if (bk==null) {
					if (!sTempBlockedAccounts.isBlocked(acc.getEmail()) &&
							!sBlockedAccounts.isBlocked(acc.getEmail()) &&
							!sBlockedProxies.isBlocked(acc.getProxy()) ) {
						tempBlockAccount(acc);
						return acc;
					}
				} else {
					if (!bk.isBlocked(acc.getEmail())) {
						bk.block(acc.getEmail());
						return acc;
					}
				}
			}
			index ++;
		}
		return null;
	}
	
	public static String getNormalProxy(String blocker) {
		Blocker bk = getCustomBlocker(blocker);
		List<WeiboAccount> accs = new ArrayList<WeiboAccount>(sAllAccounts.get().values());
		
		int index = (int)(Math.random()*accs.size());
		for (int i=0; i<accs.size(); i++) {
			if (index>=accs.size()) index = 0;
			WeiboAccount acc = accs.get(index);
			if (acc.isEnabled() && acc.getInitDate()!=null && acc.getStatus()==AccountStatus.NORMAL) {
				if (bk==null) {
					if (!sBlockedProxies.isBlocked(acc.getProxy()) ) {
						sBlockedProxies.block(acc.getProxy());
						return acc.getProxy();
					}
					
				} else {
					if (!bk.isBlocked(acc.getProxy())) {
						bk.block(acc.getProxy());
						return acc.getProxy();
					}
				}
			}
			index ++;
		}
		return null;
	}
	
	private static Blocker getCustomBlocker(String blocker) {
		if (StringUtils.isEmpty(blocker)) {
			return null;
		} else {
			return sCustomBlocker.get(blocker);
		}
	}

	private static WeiboAccount getUnblockedAccount(boolean initialized) {
		List<WeiboAccount> accs = new ArrayList<WeiboAccount>(sAllAccounts.get().values());
		
		int index = (int)(Math.random()*accs.size());
		for (int i=0; i<accs.size(); i++) {
			if (index>=accs.size()) index = 0;
			WeiboAccount acc = accs.get(index);
			if (acc.isEnabled() && (initialized ^ (acc.getInitDate()==null))) {
				if (!sTempBlockedAccounts.isBlocked(acc.getEmail()) &&
						!sBlockedAccounts.isBlocked(acc.getEmail()) &&
						!sBlockedProxies.isBlocked(acc.getProxy()) ) {
					tempBlockAccount(acc);
					return acc;
				}
			}
			index ++;
		}
		return null;
	}
	
	private static void tempBlockAccount(WeiboAccount acc) {
		sTempBlockedAccounts.block(acc.getEmail());
		sTempBlockedProxies.block(acc.getProxy());
	}

	public static WeiboAccount[] getRandomAccounts(int count) {
		Collection<WeiboAccount> loadedAccounts = sAllAccounts.get().values();
		
		if (count>loadedAccounts.size()) {
			return loadedAccounts.toArray(new WeiboAccount[loadedAccounts.size()]);
		} else {
			List<WeiboAccount> existingAccounts = new ArrayList<WeiboAccount>(loadedAccounts); 
			List<WeiboAccount> accounts = new ArrayList<WeiboAccount>(count);

			for (int i=0; i<count; i++) {
				accounts.add(existingAccounts.get((int)(Math.random()*existingAccounts.size())));
			}
			
			return accounts.toArray(new WeiboAccount[accounts.size()]);
		}
	}

	@Override
	public String toString() {
		return "Weibo account: " + mEmail + "(" + mProxy + ")";
	}

	public static void blockAccount(WeiboAccount acc) {
		sBlockedAccounts.block(acc.getEmail());
		sBlockedProxies.block(acc.getProxy());
	}

	public static void reload() throws SQLException {
		sAllAccounts.set(loadAllWeiboAccounts());
	}

	public static void trimTo(Integer count) {
		if (count!=null && count<=sAllAccounts.get().size()) {
			Map<String, WeiboAccount> accounts = new ConcurrentHashMap<String, WeiboAccount>();
			Map<String, WeiboAccount> allAccounts = sAllAccounts.get();

			int c = count;
			for (Map.Entry<String, WeiboAccount> entry : allAccounts.entrySet()) {
				if (entry.getValue().getStatus()==AccountStatus.NORMAL) {
					accounts.put(entry.getKey(), entry.getValue());
					c --;
					if (c<=0) {
						break;
					}
				}
			}
			sAllAccounts.set(accounts);
		}
	}

	public static void writeStats(PrintWriter out) {
		out.println("Total accounts: " + getTotalAccounts() );
		out.println("Blocked accounts");
		Util.writeBlockerStats(out, sBlockedAccounts);
		out.println("Temporarily blocked accounts");
		Util.writeBlockerStats(out, sTempBlockedAccounts);
		out.println("Blocked proxies");
		Util.writeBlockerStats(out, sBlockedProxies);
		out.println("Temporarily blocked proxies");
		Util.writeBlockerStats(out, sTempBlockedProxies);
		for (Map.Entry<String, Blocker> entry : sCustomBlocker.entrySet()) {
			out.println("Custom blocker :" + entry.getKey());
			Util.writeBlockerStats(out, entry.getValue());
		}
	}

	public static Set<String> getInUseProxies() throws SQLException {
		Set<String> results = new HashSet<String>();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "SELECT DISTINCT(proxy) FROM weibo_accounts";
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			
			if (rs.next()) {
				results.add(rs.getString(1));
			}
		} finally {
			DbAccess.cleanup(conn, stmt, rs);
		}
		return results;
	}

	public static boolean isProxyBlocked(String proxy) {
		return sTempBlockedProxies.isBlocked(proxy) || sBlockedProxies.isBlocked(proxy);
	}
}
