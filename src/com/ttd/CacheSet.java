package com.ttd;

import java.util.Iterator;

/**
 *
 * A set in a set-associative cache. This interface only provides read access to
 * the set and is mainly used for writing callbacks. e.g. implementing a custom
 * replacement algorithm for the set
 *
 * @param <K> the type of keys in this set
 * @param <V> the type of values in this set
 */
public interface CacheSet<K, V> extends Iterable<Cache.Entry<K, V>> {
	/**
	 * @return the first element in this cache set
	 */
	Cache.Entry<K, V> getFirst();

	/**
	 * @return the last element in this cache set
	 */
	Cache.Entry<K, V> getLast();

	/**
	 * @return an iterator of this set
	 */
	Iterator<Cache.Entry<K, V>> iterator();

	/**
	 * @return an iterator of this set in descending order.
	 */
	Iterator<Cache.Entry<K, V>> descendingIterator();

	/**
	 * @return the number of elements in this cache set
	 */
	int size();
}
