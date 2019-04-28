package com.ttd;

/**
 * 
 * An object that stores key-value pairs.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 */
public interface Cache<K, V> {
	/**
	 * Returns the value associated with the specified key, or {@code null} if this
	 * cache contains no mapping for the key.
	 * 
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped, or {@code null} if
	 *         there's no mapping for the key
	 */
	V get(K key);

	/**
	 * Associates the specified value with the specified key. If the map previously
	 * contained a mapping for the key, the old value is replaced by the specified
	 * value.
	 * Neither the key nor the value can be null.
	 * 
	 * @param key key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 * @throws NullPointerException if the specified key or value is null
	 */
	void put(K key, V value);

	/**
	 * A cache entry(key-value pair). Some additional information(create time, last
	 * access time, frequency) is also provided to implement custom replacement
	 * algorithms.
	 *
	 * @param <K> the type of the key
	 * @param <V> the type of the value
	 */
	interface Entry<K, V> {
		/**
		 * @return the key of this entry
		 */
		K getKey();

		/**
		 * @return the value of this entry
		 */
		V getValue();

		/**
		 * Returns the creation time of this entry. The creation time does not change if
		 * the entry's value is overridden.
		 * 
		 * @return the creation time of this entry in milliseconds
		 */
		long getCreateTime();

		/**
		 * Returns the last access time of this entry. The access time is updated for
		 * both get and put operations.
		 * 
		 * @return the last access time of this entry in milliseconds
		 */
		long getAccessTime();

		/**
		 * Returns the access count of this entry. The access count is incremented each
		 * time this entry is accessed(including both get and put operations)
		 * 
		 * @return the number of times this entry has been accessed since creation(started from 1)
		 */
		int getFrequency();
	}
}
