package weibo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class RepostTask extends NumTask implements TaskFactory<RepostTask> {
	private String[] helperPosts;
	
	static List<RepostTask> loadActiveReposts() throws SQLException {
		List<RepostTask> results = new ArrayList<RepostTask>();
		loadActive(new RepostTask(), results);
		return results;
	}
	
	static RepostTask loadReposts(String id) throws SQLException {
		return load(new RepostTask(), id);
	}
	
	static boolean saveRepost(RepostTask repost) throws SQLException {
		return save(repost, repost);
	}
	
	static boolean updateProgress(String id, int progress) throws SQLException {
		return updateProgress(new RepostTask(), id, progress);
	}
	
	@Override
	public String getTableName() {
		return "weibo_repost_task";
	}

	@Override
	public RepostTask newInstance() {
		return new RepostTask();
	}

	@Override
	public String[] getExtraColumns() {
		return new String[] {"helper_posts"};
	}

	@Override
	protected void extractExtraFields(ResultSet rs, int startColIndex) throws SQLException {
		setHelperPosts(rs.getString(startColIndex));
	}

	@Override
	protected void setExtraFields(PreparedStatement stmt, int startColIndex) throws SQLException {
		stmt.setString(startColIndex, getHelperPostStr());
	}

	private String getHelperPostStr() {
		StringBuffer buf = new StringBuffer();
		
		for (String helper : helperPosts) {
			buf.append(helper.trim()).append(",");
		}
		return buf.toString();
	}

	public String[] getHelperPosts() {
		return helperPosts==null? new String[0] : helperPosts;
	}

	public void setHelperPosts(String[] helperPosts) {
		this.helperPosts = helperPosts;
	}

	public void setHelperPosts(String helperPosts) {
		if (!StringUtils.isEmpty(helperPosts)) {
			this.helperPosts = helperPosts.split("[,;]");
		} else {
			this.helperPosts = new String[0];
		}
	}
}