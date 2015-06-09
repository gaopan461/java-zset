package zset;
import java.util.ArrayList;
import java.util.List;

public class SkipList<K extends Comparable<K>, V extends Comparable<V>> {
	private final static int SKIPLIST_MAXLEVEL = 32;
	private final static double SKIPLIST_P = 0.25;
	
	private SLNode<K,V> header = null;
	private SLNode<K,V> tail = null;
	private int length = 0;
	private int level = 1;
	
	public SkipList(V zero) {
		this.header = new SLNode<K,V>(SKIPLIST_MAXLEVEL, zero, null);
	}
	
	private int randomLevel() {
		int level = 1;
		while(Math.random() < SKIPLIST_P) {
			level++;
		}
		return Math.min(level, SKIPLIST_MAXLEVEL);
	}

	public void insert(V score, K member) {
		List<SLNode<K,V>> update = new ArrayList<>(SKIPLIST_MAXLEVEL);
		for(int i = 0; i < SKIPLIST_MAXLEVEL; ++i) {
			update.add(null);
		}
		
		SLNode<K,V> x = header;
		int rank[] = new int[SKIPLIST_MAXLEVEL];
		
		for(int i = level - 1; i >= 0; --i) {
			/* store rank that is crossed to reach the insert position */
			if(i == level - 1){
				rank[i] = 0;
			} else {
				rank[i] = rank[i+1];
			}
			
			SLNodeLevel<K,V> nodeLevelX = x.getLevels().get(i);
			while(nodeLevelX.getForward() != null 
					&& nodeLevelX.getForward().compareTo(score, member) < 0) {
				rank[i] += nodeLevelX.getSpan();
				x = nodeLevelX.getForward();
				nodeLevelX = x.getLevels().get(i);
			}
		
			update.set(i, x);
		}
		
		/* we assume the key is not already inside, since we allow duplicated
	     * scores, and the re-insertion of score and redis object should never
	     * happen since the caller of slInsert() should test in the hash table
	     * if the element is already inside or not. */
		int randLevel = randomLevel();
		if(randLevel > level) {
			for(int i = level; i < randLevel; ++i) {
				rank[i] = 0;
				update.set(i, header);
				header.getLevels().get(i).setSpan(length);
			}
			level = randLevel;
		}
		
		x = new SLNode<K,V>(randLevel, score, member);
		for(int i = 0; i < randLevel; ++i) {
			SLNodeLevel<K,V> nodeLevelX = x.getLevels().get(i);
			SLNodeLevel<K,V> nodeLevelUpdate = update.get(i).getLevels().get(i);
			nodeLevelX.setForward(nodeLevelUpdate.getForward());
			nodeLevelUpdate.setForward(x);
			
			/* update span covered by update[i] as x is inserted here */
			nodeLevelX.setSpan(nodeLevelUpdate.getSpan() - (rank[0] - rank[i]));
			nodeLevelUpdate.setSpan(rank[0] - rank[i] + 1);
		}
		
		/* increment span for untouched levels */
		for(int i = randLevel; i < level; ++i) {
			SLNodeLevel<K,V> nodeLevelUpdate = update.get(i).getLevels().get(i);
			nodeLevelUpdate.setSpan(nodeLevelUpdate.getSpan() + 1);
		}
		
		if(update.get(0) == header) {
			x.setBackward(null);
		} else {
			x.setBackward(update.get(0));
		}
		
		if(x.getLevels().get(0).getForward() != null) {
			x.getLevels().get(0).getForward().setBackward(x);
		} else {
			tail = x;
		}
		
		length++;
	}
	
	/* Internal function used by slDelete, slDeleteByScore */
	private void deleteNode(SLNode<K,V> x, List<SLNode<K,V>> update) {
		for(int i = 0; i < level; ++i) {
			SLNodeLevel<K,V> nodeLevelUpdate = update.get(i).getLevels().get(i);
			if(nodeLevelUpdate.getForward() == x) {
				SLNodeLevel<K,V> nodeLevelX = x.getLevels().get(i);
				nodeLevelUpdate.setSpan(nodeLevelUpdate.getSpan() + nodeLevelX.getSpan() - 1);
				nodeLevelUpdate.setForward(nodeLevelX.getForward());
			} else {
				nodeLevelUpdate.setSpan(nodeLevelUpdate.getSpan() - 1);
			}
		}
		
		if(x.getLevels().get(0).getForward() != null) {
			x.getLevels().get(0).getForward().setBackward(x.getBackward());
		} else {
			tail = x.getBackward();
		}
		
		while(level > 1 && header.getLevels().get(level - 1).getForward() == null) {
			level--;
		}
		
		length--;
	}
	
	/* Delete an element with matching score/object from the skiplist. */
	public boolean delete(V score, K member) {
		List<SLNode<K,V>> update = new ArrayList<>(SKIPLIST_MAXLEVEL);
		for(int i = 0; i < SKIPLIST_MAXLEVEL; ++i) {
			update.add(null);
		}
		
		SLNode<K,V> x = header;
		for(int i = level - 1; i >= 0; --i) {
			SLNodeLevel<K,V> nodeLevelX = x.getLevels().get(i);
			while(nodeLevelX.getForward() != null 
					&& nodeLevelX.getForward().compareTo(score, member) < 0) {
				x = nodeLevelX.getForward();
				nodeLevelX = x.getLevels().get(i);
			}
			update.set(i, x);
		}
		
		/* We may have multiple elements with the same score, what we need
	     * is to find the element with both the right score and object. */
		x = x.getLevels().get(0).getForward();
		if(x != null && x.compareTo(score, member) == 0) {
			deleteNode(x, update);
			return true;
		}
		
		/* not found */
		return false;
	}
	
	public int deleteByRank(int start, int end, IDeleteCallback cb) {
		if(start > end) {
			int tmp = start;
			start = end;
			end = tmp;
		}
		
		List<SLNode<K,V>> update = new ArrayList<>(SKIPLIST_MAXLEVEL);
		for(int i = 0; i < SKIPLIST_MAXLEVEL; ++i) {
			update.add(null);
		}
		
		int traversed = 0;
		int removed = 0;
		
		SLNode<K,V> x = header;
		for(int i = level - 1; i >= 0; --i) {
			SLNodeLevel<K,V> nodeLevelX = x.getLevels().get(i);
			while(nodeLevelX.getForward() != null
					&& (traversed + nodeLevelX.getSpan()) < start) {
				traversed += nodeLevelX.getSpan();
				x = nodeLevelX.getForward();
				nodeLevelX = x.getLevels().get(i);
			}
			
			update.set(i, x);
		}
		
		traversed++;
		x = x.getLevels().get(0).getForward();
		while(x != null && traversed <= end) {
			SLNode<K,V> next = x.getLevels().get(0).getForward();
			deleteNode(x, update);
			cb.process(x.getMember());
			removed++;
			traversed++;
			x = next;
		}
		return removed;
	}
	
	/* Find the rank for an element by both score and key.
	 * Returns 0 when the element cannot be found, rank otherwise.
	 * Note that the rank is 1-based due to the span of sl->header to the
	 * first element. */
	public int getRank(V score, K member) {
		int rank = 0;
		SLNode<K,V> x = header;
		for(int i = level - 1; i >= 0; --i) {
			SLNodeLevel<K,V> nodeLevel = x.getLevels().get(i);
			while(nodeLevel.getForward() != null
					&& nodeLevel.getForward().compareTo(score, member) <= 0) {
				rank += nodeLevel.getSpan();
				x = nodeLevel.getForward();
				nodeLevel = x.getLevels().get(i);
			}
			
			/* x might be equal to sl->header, so test if obj is non-NULL */
			if(x.getMember() != null && x.getMember().equals(member)) {
				return rank;
			}
		}
		return 0;
	}
	
	/* Finds an element by its rank. The rank argument needs to be 1-based. */
	public SLNode<K,V> getNodeByRank(long rank) {
		if(rank == 0 || rank > length) {
			return null;
		}
		
		int traversed = 0;
		SLNode<K,V> x = header;
		for(int i = level - 1; i >= 0; --i) {
			SLNodeLevel<K,V> nodeLevel = x.getLevels().get(i);
			while(nodeLevel.getForward() != null
					&& (traversed + nodeLevel.getSpan()) <= rank) {
				traversed += nodeLevel.getSpan();
				x = nodeLevel.getForward();
				nodeLevel = x.getLevels().get(i);
			}
			
			if(traversed == rank) {
				return x;
			}
		}
		
		return null;
	}
	
	/* range [min, max], left & right both include */
	/* Returns if there is a part of the zset is in range. */
	private boolean isInRange(V min, V max) {
		/* Test for ranges that will always be empty. */
		if(min.compareTo(max) > 0) {
			return false;
		}
		
		SLNode<K,V> x = tail;
		if(x == null || x.getScore().compareTo(min) < 0) {
			return false;
		}
		
		x = header.getLevels().get(0).getForward();
		if(x == null || x.getScore().compareTo(max) > 0) {
			return false;
		}
		
		return true;
	}
	
	/* Find the first node that is contained in the specified range.
	 * Returns NULL when no element is contained in the range. */
	public SLNode<K,V> getFirstInRange(V min, V max) {
		/* If everything is out of range, return early. */
		if(!isInRange(min, max)) {
			return null;
		}
		
		SLNode<K,V> x = header;
		for(int i = level - 1; i >= 0; --i) {
			/* Go forward while *OUT* of range. */
			SLNodeLevel<K,V> nodeLevel = x.getLevels().get(i);
			while(nodeLevel.getForward() != null
					&& nodeLevel.getForward().getScore().compareTo(min) < 0) {
				x = nodeLevel.getForward();
				nodeLevel = x.getLevels().get(i);
			}
		}
		
		/* This is an inner range, so the next node cannot be NULL. */
		x = x.getLevels().get(0).getForward();
		return x;
	}
	
	/* Find the last node that is contained in the specified range.
	 * Returns NULL when no element is contained in the range. */
	public SLNode<K,V> getLastInRange(V min, V max) {
		/* If everything is out of range, return early. */
		if(!isInRange(min, max)) {
			return null;
		}
		
		SLNode<K,V> x = header;
		for(int i = level - 1; i >= 0; --i) {
			/* Go forward while *IN* range. */
			SLNodeLevel<K,V> nodeLevel = x.getLevels().get(i);
			while(nodeLevel.getForward() != null
					&& nodeLevel.getForward().getScore().compareTo(max) <= 0) {
				x = nodeLevel.getForward();
				nodeLevel = x.getLevels().get(i);
			}
		}
		
		/* This is an inner range, so this node cannot be NULL. */
		return x;
	}
	
	@Override
	public String toString() {
		SLNode<K,V> x = header;
		SLNodeLevel<K,V> nodeLevel0 = x.getLevels().get(0);
		StringBuffer sb = new StringBuffer();
		int i = 0;
		while(nodeLevel0.getForward() != null) {
			x = nodeLevel0.getForward();
			i++;
			sb.append("node:").append(i).append(",score:").append(x.getScore()).append(",member:").append(x.getMember()).append('\n');
			nodeLevel0 = x.getLevels().get(0);
		}
		return sb.toString();
	}
	
	//-------------------------------------------------------------------
	
	public int getCount() {
		return length;
	}
	
	public List<K> getRankRange(int r1, int r2) {
		boolean reverse = (r1 > r2);
		int rangelen = Math.abs(r1-r2) + 1;
		
		SLNode<K,V> node = getNodeByRank(r1);
		List<K> retList = new ArrayList<>(rangelen);
		int n = 0;
		while(node != null && n < rangelen) {
			n++;
			retList.add(node.getMember());
			
			if(reverse) {
				node = node.getBackward();
			} else {
				node = node.getLevels().get(0).getForward();
			}
		}
		
		return retList;
	}
	
	public List<K> getScoreRange(V s1, V s2) {
		boolean reverse = (s1.compareTo(s2) > 0);
		SLNode<K,V> node = null;
		if(reverse) {
			node = getLastInRange(s2, s1);
		} else {
			node = getFirstInRange(s1, s2);
		}
		
		List<K> retList = new ArrayList<>();
		while(node != null) {
			if(reverse) {
				if(node.getScore().compareTo(s2) < 0) {
					break;
				}
			} else {
				if(node.getScore().compareTo(s2) > 0) {
					break;
				}
			}
			
			retList.add(node.getMember());
			
			if(reverse) {
				node = node.getBackward();
			} else {
				node = node.getLevels().get(0).getForward();
			}
		}
		
		return retList;
	}
}
