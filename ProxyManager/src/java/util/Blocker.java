package util;

import java.util.List;
import java.util.Set;

public interface Blocker {
	void setBlockingTicks(int ticks);
	int getBlockingTicks();
	void block(String key);
	boolean isBlocked(String key);
	void unblock(String key);
	void expire(int tick);
	Set<String> getAllBlocked();
	List<Set<String>> getBlockedBuckets();
}
