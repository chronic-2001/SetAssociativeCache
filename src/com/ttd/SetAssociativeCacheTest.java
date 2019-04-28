package com.ttd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import com.ttd.Cache.Entry;

public class SetAssociativeCacheTest {

	@Test
	public void testConstructors() {
		SetAssociativeCache<String, String> cache = new SetAssociativeCache<>(3, 15);
		assertEquals(SetAssociativeCache.lru(), cache.getPolicy()); // default replacement policy is LRU
		assertThrows(NullPointerException.class, () -> new SetAssociativeCache<String, String>(3, 15, null));
		// Exceptions should be thrown for non-positive values
		assertThrows(IllegalArgumentException.class, () -> new SetAssociativeCache<String, String>(0, 16));
		assertThrows(IllegalArgumentException.class, () -> new SetAssociativeCache<String, String>(-1, 16));
		assertThrows(IllegalArgumentException.class, () -> new SetAssociativeCache<String, String>(4, 0));
		assertThrows(IllegalArgumentException.class, () -> new SetAssociativeCache<String, String>(4, -1));
	}

	@Test
	public void testBasicPutAndGet() {
		SetAssociativeCache<String, String> cache = new SetAssociativeCache<>(4, 64);
		cache.put("a", "b");
		cache.put("c", "d");
		cache.put("e", "f");
		cache.put("g", "h");
		assertEquals("b", cache.get("a"));
		assertEquals("d", cache.get("c"));
		assertNull(cache.get("k")); // return null on cache miss
		cache.put("a", "z"); // override an existing entry
		assertEquals("z", cache.get("a"));
		// do not accept null keys or values
		assertThrows(NullPointerException.class, () -> cache.put(null, "null"));
		assertThrows(NullPointerException.class, () -> cache.put("null", null));
	}

	@Test
	public void testLRUAndMRU() {
		// use integers for easier prediction of their hashCodes (An integer's hashCode
		// is the same as its value.)
		// LRU
		SetAssociativeCache<Integer, Integer> cache = new SetAssociativeCache<>(4, 16);
		cache.put(0, 0);
		cache.put(16, 1);
		cache.put(32, 2);
		cache.put(48, 3);
		cache.put(64, 4);
		// the entry with key 0 should be evicted
		assertNull(cache.get(0));
		assertSame(1, cache.get(16)); // the entry with key 16 has just been accessed
		// now the entry with key 32 is the oldest and should be evicted
		cache.put(0, 0);
		assertNull(cache.get(32));

		// MRU
		cache.setPolicy(SetAssociativeCache.mru());
		cache.put(1, 1);
		cache.put(17, 2);
		cache.put(33, 3);
		cache.put(49, 4);
		cache.put(65, 5);
		assertNull(cache.get(49));
		cache.get(1);
		cache.put(49, 4);
		assertNull(cache.get(1));
	}

	@Test
	public void testCustomReplacementAlgorithms() {
		ReplacementPolicy<Integer, Integer> lfu = set -> {
			Entry<Integer, Integer> candidate = set.getFirst();
			for (Entry<Integer, Integer> e : set) {
				if (e.getFrequency() < candidate.getFrequency()) {
					candidate = e;
				}
			}
			return candidate;
		};
		SetAssociativeCache<Integer, Integer> cache = new SetAssociativeCache<>(4, 4, lfu);
		cache.put(0, 0);
		cache.put(4, 1);
		cache.put(8, 2);
		cache.get(0);
		cache.get(4);
		cache.get(8);
		cache.put(12, 3);
		cache.put(16, 4);
		assertNull(cache.get(12));

		SetAssociativeCache<Integer, String> nameCache = new SetAssociativeCache<>(4, 4, set -> {
			Iterator<Entry<Integer, String>> iterator = set.descendingIterator();
			Entry<Integer, String> candidate = iterator.next();
			while (iterator.hasNext()) {
				Entry<Integer, String> e = iterator.next();
				if (e.getKey() == 0) {
					return e;
				}
				if (e.getValue().compareToIgnoreCase(candidate.getValue()) > 0) {
					candidate = e;
				}
			}
			return candidate;
		});

		nameCache.put(4, "mark");
		nameCache.put(0, "amy");
		nameCache.put(8, "frank");
		nameCache.put(12, "john");
		nameCache.put(16, "bob");
		assertNull(nameCache.get(0));
		nameCache.put(20, "daisy");
		assertNull(nameCache.get(4));
	}

	@Test
	public void testThreadSafety() throws Exception {
		SetAssociativeCache<Integer, Integer> cache = new SetAssociativeCache<>(4, 16);
		ExecutorService executor = Executors.newFixedThreadPool(8);
		List<Future<?>> futures = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			int n = i;
			futures.add(executor.submit(() -> cache.put(n, n)));
		}
		for (Future<?> future : futures) {
			future.get();
		}
		int count = 0;
		for (int i = 0; i < 1000; i++) {
			if (cache.get(i) != null) {
				count++;
			}
		}
		assertEquals(4 * 16, count);
	}

}
