package com.ttd;

/**
 * 
 * Represents a function that selects some cache entry from a cache set
 * according to some predefined rules. The selected cache entry is to be
 * evicted from the cache set.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
@FunctionalInterface
public interface ReplacementPolicy<K, V> {
	/**
	 * Applies the function to the given cache set and return a selected cache entry.
	 * 
	 * @param set the input cache set to select an entry from
	 * @return a cache entry to be evicted from the cache set
	 */
	Cache.Entry<K, V> select(CacheSet<K, V> set);
}
