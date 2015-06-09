import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import zset.ZSet;
import junit.framework.TestCase;


public class ZSetTest extends TestCase {
	public void testZSet() {
		final int total = 100;
		List<Long> all = new ArrayList<>(total);
		for(int i = 1; i <= total; ++i) {
			all.add(Long.valueOf(i));
		}
		
		ZSet zset = new ZSet();
		
		while(true) {
			Long score = randomChoose(all);
			if(score == null) {
				break;
			}
			
			String name = "a" + score.toString();
			zset.add(score, name);
		}
		
		assertEquals(total, zset.count());
		
		System.out.printf("rank 28:%s\n", zset.rank("a28"));
		System.out.printf("rev rank 28:%s\n", zset.revRank("a28"));
		
		List<String> t = zset.range(1, 10);
		System.out.println("rank 1-10:");
		for(int i = 0; i < t.size(); ++i) {
			System.out.println(t.get(i));
		}
		
		t = zset.revRange(1, 10);
		System.out.println("rev rank 1-10:");
		for(int i = 0; i < t.size(); ++i) {
			System.out.println(t.get(i));
		}
		
		System.out.println("------------------ dump ------------------");
		System.out.println(zset.toString());
		
		System.out.println("------------------ dump after limit 10 ------------------");
		zset.limit(10);
		System.out.println(zset.toString());

		System.out.println("------------------ dump after rev limit 5 ------------------");
		zset.revLimit(5);
		System.out.println(zset.toString());
	}
	
	private static Long randomChoose(List<Long> t) {
		if(t.isEmpty()) {
			return null;
		}
		
		Random random = new Random();
		int i = random.nextInt(t.size());
		return t.remove(i);
	}
}
