package weibo;

class RepostTask extends Task implements TaskFactory<RepostTask> {
	@Override
	public String getTableName() {
		return "weibo_repost_task";
	}

	@Override
	public RepostTask newInstance() {
		return new RepostTask();
	}
}