package weibo;


interface TaskFactory<T extends Task> {
	T newInstance();
	String getTableName();
	String[] getExtraColumns();
}