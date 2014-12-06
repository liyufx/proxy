package weibo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import db.DbAccess;

abstract class NumTask extends Task {
	private int targetNum;
	private int progress;
	private Timestamp progressDate;
	
	public int getTargetNum() {
		return targetNum;
	}
	public void setTargetNum(int targetNum) {
		this.targetNum = targetNum;
	}
	public int getProgress() {
		return progress;
	}
	public void setProgress(int progress) {
		this.progress = progress;
	}
	public Timestamp getProgressDate() {
		return progressDate;
	}
	public void setProgressDate(Timestamp progressDate) {
		this.progressDate = progressDate;
	}
	
	static public <T extends NumTask> void loadActive(TaskFactory<T> factory, List<T > results) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "SELECT id, name, target_num, create_date, expiry_date, progress, progress_date, active";
			
			for (String colName : factory.getExtraColumns()) {
				sql += " ," + colName;
			}
			sql += " FROM " + factory.getTableName() + " WHERE (NOT active=0) AND " +
					"(expiry_date is null OR expiry_date>=?";
			
			stmt = conn.prepareStatement(sql);
			stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
			rs = stmt.executeQuery();
			
			while (rs.next()) {
				T task = factory.newInstance();
				extractTask(rs, task);
				results.add(task);
			}
		} finally {
			DbAccess.cleanup(conn, stmt, rs);
		}
	}
	
	static public <T extends NumTask> T load(TaskFactory<T> factory, String id) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "SELECT id, name, target_num, create_date, expiry_date, progress, progress_date, active";
			
			for (String colName : factory.getExtraColumns()) {
				sql += " ," + colName;
			}
			sql += " FROM " + factory.getTableName() + " WHERE id=?";
			
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, id);
			rs = stmt.executeQuery();
			
			if (rs.next()) {
				T task = factory.newInstance();
				extractTask(rs, task);
				return task;
			} else {
				return null;
			}
		} finally {
			DbAccess.cleanup(conn, stmt, rs);
		}
	}
	
	static public <T extends NumTask> boolean save(TaskFactory<T> factory, T task) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		PreparedStatement insertStmt = null;
		
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "UPDATE " + factory.getTableName()  + 
					" SET id=?, name=?, target_num=?, create_date=?, expiry_date=?, progress=?, progress_date=?, active=?";
			
			for (String colName : factory.getExtraColumns()) {
				sql += " ," + colName + "=?";
			}
			sql += " WHERE id=?";
			
			stmt = conn.prepareStatement(sql);
			setTaskFields(stmt, task);
			stmt.setString(9 + factory.getExtraColumns().length, task.getId());
			int count = stmt.executeUpdate();
			
			if (count<=0) {
				sql = "INSERT INTO " + factory.getTableName()  + 
						" (id, name, target_num, create_date, expiry_date, progress, progress_date, active";
				
				for (String colName : factory.getExtraColumns()) {
					sql += " ," + colName;
				}
				
				sql += ") VALUES (?, ?, ?, ?, ?, ?, ?, ?";
				
				for (String colName : factory.getExtraColumns()) {
					sql += ", ?";
				}
				
				sql += ")";
				
				stmt = conn.prepareStatement(sql);
				setTaskFields(insertStmt, task);
				count = stmt.executeUpdate();
			}
			return count==1;
		} finally {
			DbAccess.cleanup(null, stmt, null);
		}
	}
	
	static public <T extends NumTask> boolean updateProgress(TaskFactory<T> factory, String id, int progress) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "UPDATE " + factory.getTableName() + " SET progress=?, progress_date=? WHERE id=?";
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, progress);
			stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
			stmt.setString(3, id);
			return stmt.executeUpdate()==1;
		} finally {
			DbAccess.cleanup(conn, stmt, rs);
		}
	}
	
	static private <T extends NumTask> void extractTask(ResultSet rs, T task) throws SQLException {
		// todo
		task.setId(rs.getString(1));
		task.setName(rs.getString(2));
		task.setTargetNum(rs.getInt(3));
		task.setCreatedDate(rs.getTimestamp(4));
		task.setExpiryDate(rs.getTimestamp(5));
		task.setProgress(rs.getInt(6));
		task.setProgressDate(rs.getTimestamp(7));
		task.setEnabled(rs.getInt(8)>0);
		task.extractExtraFields(rs, 9);
	}
	
	protected abstract void extractExtraFields(ResultSet rs, int startColIndex) throws SQLException;
	
	static private <T extends NumTask> void setTaskFields(PreparedStatement stmt, T task) throws SQLException {
		stmt.setString(1, task.getId());
		stmt.setString(2, task.getName());
		stmt.setInt(3, task.getTargetNum());
		stmt.setTimestamp(4,task.getCreatedDate());
		stmt.setTimestamp(5, task.getExpiryDate());
		stmt.setInt(6, task.getProgress());
		stmt.setTimestamp(7, task.getProgressDate());
		stmt.setInt(8, task.isEnabled()? 1: 0);
		task.setExtraFields(stmt, 9);
	}
	
	protected abstract void setExtraFields(PreparedStatement stmt, int startColIndex) throws SQLException;
}