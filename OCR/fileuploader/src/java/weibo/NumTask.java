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
			
			String sql = "SELECT id, name, target_num, create_date, expiry_date, progress, progress_date, active" +
			
			for (String colName : factory.getExtraColumns()) {
				sql += " ," + colName;
			}
			sql += " FROM " + factory.getTableName() + " where not active=0";
			
			stmt = conn.prepareStatement(sql);
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
	
	static public <T extends NumTask> void save(TaskFactory<T> factory, List<T > results) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			conn = DbAccess.getDbConnection();
			
			String sql = "SELECT id, name, target_num, create_date, expiry_date, progress, progress_date, active " +
					" FROM " + factory.getTableName() + " where not active=0";
			stmt = conn.prepareStatement(sql);
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
	
	protected abstract void extractExtraFields(ResultSet rs, int startColIndex);
	protected abstract void setExtraFields(PreparedStatement stmt, int startColIndex);
}