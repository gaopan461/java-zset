import java.util.List;
import java.util.Random;

import junit.framework.TestCase;
import zset.IDeleteCallback;
import zset.SkipList;


public class SkipListTest extends TestCase {

	public void testSkipList() {
		SkipList<String,Long> skipList = new SkipList<String, Long>(0L);
		
		long start = System.currentTimeMillis();
		long total = 500000;
		for(long i = 1; i <= total; ++i) {
			skipList.insert(i, String.valueOf(i));
			skipList.insert(i, String.valueOf(i));
		}
		
		for(long i = 1; i <= total; ++i) {
			skipList.delete(i, String.valueOf(i));
		}
		System.out.println("Used time :" + (System.currentTimeMillis() - start));
		
		assertEquals(skipList.getRank(1L, "1"), 1);
		assertEquals(skipList.getRank(total, String.valueOf(total)), total);
		
		Random random = new Random();
		long rand = random.nextInt(((Long)total).intValue()) + 1;
		assertEquals(skipList.getRank(rand, String.valueOf(rand)), rand);
		
		int r1 = 100, r2 = 100000;
		List<String> t1 = skipList.getRankRange(r1, r2);
		List<String> t2 = skipList.getRankRange(r2, r1);
		assertEquals(t1.size(), t2.size());
		for(int i = 0; i < t1.size(); ++i) {
			assertEquals(t1.get(i), t2.get(t2.size()-i-1));
		}
		
		long s1 = 100, s2 = 100000;
		List<String> t3 = skipList.getScoreRange(s1, s2);
		List<String> t4 = skipList.getScoreRange(s2, s1);
		assertEquals(t3.size(), t4.size());
		for(int i = 0; i < t3.size(); ++i) {
			assertEquals(t3.get(i), t4.get(t4.size()-i-1));
		}
		
		dumpRankRange(skipList, 2, 5);
		dumpRankRange(skipList, 5, 2);
		
		dumpScoreRange(skipList, 10, 15);
		dumpScoreRange(skipList, 15, 10);
		
		skipList.deleteByRank(15, 10, new IDeleteCallback() {
			
			@Override
			public void process(Object member) {
				System.out.printf("delete:%s\n", member.toString());
			}
		});
	}
	
	private static void dumpRankRange(SkipList<String,Long> skipList, int r1, int r2) {
		System.out.printf("rank range:%d %d\n", r1, r2);
		List<String> t = skipList.getRankRange(r1, r2);
		for(int i = 0; i < t.size(); ++i) {
			if(r1 <= r2) {
				System.out.printf("%d %s\n", r1+i, t.get(i));
			} else {
				System.out.printf("%d %s\n", r1-i, t.get(i));
			}
		}
	}

	private static void dumpScoreRange(SkipList<String,Long> skipList, long s1, long s2) {
		System.out.printf("rank range:%d %d\n", s1, s2);
		List<String> t = skipList.getScoreRange(s1, s2);
		for(int i = 0; i < t.size(); ++i) {
			System.out.println(t.get(i));
		}
	}
}
