package weibo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VoteTask extends NumTask implements TaskFactory<VoteTask> {
	private int votePos = 0;
	
	static List<VoteTask> loadActiveVotes() throws SQLException {
		List<VoteTask> results = new ArrayList<VoteTask>();
		loadActive(new VoteTask(), results);
		return results;
	}
	
	static VoteTask loadVote(String id) throws SQLException {
		return load(new VoteTask(), id);
	}
	
	static boolean saveVote(VoteTask vote) throws SQLException {
		return save(vote, vote);
	}
	
	static boolean updateProgress(String id, int progress) throws SQLException {
		return updateProgress(new VoteTask(), id, progress);
	}
	
	@Override
	public String getTableName() {
		return "weibo_vote_task";
	}

	@Override
	public VoteTask newInstance() {
		return new VoteTask();
	}

	@Override
	public String[] getExtraColumns() {
		return new String[] {"vote_pos"};
	}

	@Override
	protected void extractExtraFields(ResultSet rs, int startColIndex) throws SQLException {
		setVotePos(rs.getInt(startColIndex));
	}

	@Override
	protected void setExtraFields(PreparedStatement stmt, int startColIndex) throws SQLException {
		stmt.setInt(startColIndex, getVotePos());
	}

	public int getVotePos() {
		return votePos;
	}

	public void setVotePos(int votePos) {
		this.votePos = votePos;
	}
}