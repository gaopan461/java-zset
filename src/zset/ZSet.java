package zset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZSet {
	private SkipList<String,Long> skipList = new SkipList<String,Long>(0L);
	private Map<String,Long> map = new HashMap<String,Long>();
	
	public void add(Long score, String member) {
		Long oldScore = map.get(member);
		if(oldScore != null) {
			if(oldScore.equals(score)) {
				return;
			}
			
			skipList.delete(score, member);
		}
		
		skipList.insert(score, member);
		map.put(member, score);
	}
	
	public void rem(String member) {
		Long score = map.get(member);
		if(score != null) {
			skipList.delete(score, member);
			map.remove(member);
		}
	}
	
	public int count() {
		return skipList.getCount();
	}
	
	private int reverseRank(int rank) {
		return skipList.getCount() - rank + 1;
	}
	
	public int limit(int count) {
		int total = skipList.getCount();
		if(total <= count) {
			return 0;
		}
		
		return skipList.deleteByRank(count+1, total, new IDeleteCallback() {
			
			@Override
			public void process(Object member) {
				ZSet.this.map.remove(member);
			}
		});
	}
	
	public int revLimit(int count) {
		int total = skipList.getCount();
		if(total <= count) {
			return 0;
		}
		
		int from = reverseRank(count+1);
		int to = reverseRank(total);
		return skipList.deleteByRank(from, to, new IDeleteCallback() {
			
			@Override
			public void process(Object member) {
				ZSet.this.map.remove(member);
			}
		});
	}
	
	public List<String> revRange(int r1, int r2) {
		r1 = reverseRank(r1);
		r2 = reverseRank(r2);
		return range(r1, r2);
	}
	
	public List<String> range(int r1, int r2) {
		r1 = Math.max(r1, 1);
		r2 = Math.max(r2, 1);
		return skipList.getRankRange(r1, r2);
	}
	
	public int revRank(String member) {
		int rank = rank(member);
		if(rank > 0) {
			return reverseRank(rank);
		}
		
		return -1;
	}
	
	public int rank(String member) {
		Long score = map.get(member);
		if(score == null) {
			return -1;
		}
		
		return skipList.getRank(score, member);
	}
	
	public List<String> rangeByScore(Long s1, Long s2) {
		return skipList.getScoreRange(s1, s2);
	}
	
	public Long score(String member) {
		return map.get(member);
	}
	
	@Override
	public String toString() {
		return skipList.toString();
	}
}
