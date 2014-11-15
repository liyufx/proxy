package weibo;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class VoteTask extends NumTask implements TaskFactory<VoteTask> {
	
	static List<VoteTask> loadActiveVotes() throws SQLException {
		List<VoteTask> results = new ArrayList<VoteTask>();
		loadActive(new VoteTask(), results);
		return results;
	}
	
	static List<VoteTask> loadVote(String id) {
		return null;
	}
	
	static VoteTask saveVote(VoteTask vote) {
		return null;
		
	}
	
	static VoteTask updateProgress(String id, int progress) {
		return null;
		
	}
	
	@Override
	public String getTableName() {
		return "weibo_vote_task";
	}

	@Override
	public VoteTask newInstance() {
		return new VoteTask();
	}
}