package zset;
import java.util.ArrayList;
import java.util.List;

public class SLNode<K extends Comparable<K>, V extends Comparable<V>>
		implements Comparable<SLNode<K, V>> {
	private K member;
	private V score;
	SLNode<K, V> backward = null;
	List<SLNodeLevel<K, V>> levels = new ArrayList<>();
	
	public SLNode(int level, V score, K member) {
		this.score = score;
		this.member = member;
		
		for(int i = 0; i < level; ++i) {
			this.levels.add(new SLNodeLevel<K, V>());
		}
	}

	public K getMember() {
		return member;
	}

	public void setMember(K member) {
		this.member = member;
	}

	public V getScore() {
		return score;
	}

	public void setScore(V score) {
		this.score = score;
	}

	public SLNode<K, V> getBackward() {
		return backward;
	}

	public void setBackward(SLNode<K, V> backward) {
		this.backward = backward;
	}

	public List<SLNodeLevel<K, V>> getLevels() {
		return levels;
	}

	public void setLevels(List<SLNodeLevel<K, V>> levels) {
		this.levels = levels;
	}

	@Override
	public int compareTo(SLNode<K, V> node2) {
		return compareTo(node2.getScore(), node2.getMember());
	}
	
	public int compareTo(V score, K member) {
		int result = this.score.compareTo(score);
		if(result == 0) {
			return this.member.compareTo(member);
		} else {
			return result;
		}
	}
}
