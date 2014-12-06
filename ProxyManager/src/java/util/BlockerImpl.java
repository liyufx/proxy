package util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BlockerImpl implements Blocker {
	private final ArrayList<ConcurrentHashMap<String, String>> mBlockedKeys;
	private int mTicks = 0;
	
	public BlockerImpl(int ticks) {
		mTicks = ticks;
		mBlockedKeys = new ArrayList<ConcurrentHashMap<String, String>>(ticks);
		
		for (int i=0; i<ticks; i++) {
			mBlockedKeys.add(new ConcurrentHashMap<String, String>());
		}
	}
	
	@Override
	public synchronized void setBlockingTicks(int ticks) {
		mTicks = ticks;
		
		while (mBlockedKeys.size()>mTicks) {
			mBlockedKeys.remove(0);
		}
		
		while (mBlockedKeys.size()<mTicks) {
			mBlockedKeys.add(0, new ConcurrentHashMap<String, String>());
		}
	}

	@Override
	public int getBlockingTicks() {
		return mTicks;
	}

	@Override
	public void block(String key) {
		ArrayList<ConcurrentHashMap<String, String>> newList = 
				new ArrayList<ConcurrentHashMap<String, String>>(mBlockedKeys);
		
		newList.get(newList.size()-1).put(key, key);
	}

	@Override
	public boolean isBlocked(String key) {
		ArrayList<ConcurrentHashMap<String, String>> newList = 
				new ArrayList<ConcurrentHashMap<String, String>>(mBlockedKeys); 
		
		for (ConcurrentHashMap<String, String> blocked : newList) {
			if (blocked.containsKey(key)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void unblock(String key) {
		ArrayList<ConcurrentHashMap<String, String>> newList = 
				new ArrayList<ConcurrentHashMap<String, String>>(mBlockedKeys); 
		
		for (ConcurrentHashMap<String, String> blocked : newList) {
			blocked.remove(key);
		}
	}

	@Override
	public synchronized void expire(int tick) {
		for (int i=0; i<tick; i++) {
			mBlockedKeys.remove(0);
			mBlockedKeys.add(new ConcurrentHashMap<String, String>());
		}
		
	}

	@Override
	public Set<String> getAllBlocked() {
		Set<String> results = new HashSet<String>();
		
		ArrayList<ConcurrentHashMap<String, String>> newList = 
				new ArrayList<ConcurrentHashMap<String, String>>(mBlockedKeys); 
		
		for (ConcurrentHashMap<String, String> blocked : newList) {
			results.addAll(blocked.keySet());
		}
		return results;
	}

	@Override
	public List<Set<String>> getBlockedBuckets() {
		ArrayList<ConcurrentHashMap<String, String>> newList = 
				new ArrayList<ConcurrentHashMap<String, String>>(mBlockedKeys); 
		ArrayList<Set<String>> results = new ArrayList<Set<String>>(newList.size()); 
		
		for (ConcurrentHashMap<String, String> blocked : newList) {
			results.add(new HashSet<String>(blocked.keySet()));
		}
		return results;
	}

}
