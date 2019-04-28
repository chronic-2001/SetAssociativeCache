package com.ttd;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * An N-way, set-associative cache. Sets are distributed based on the hash codes
 * of keys, so make sure the type of keys has a well-distributed hash function.
 * Sets are lazily initialized on first put. Default replacement policy is
 * LRU(least recently used), MRU(most recently used) is also provided. The
 * elements in each set is ordered by access time(from least recent to most), so
 * operations with default replacement policy(LRU or MRU) have O(1) time
 * complexity. Users can also provide their own replacement algorithms.
 * 
 * This cache is thread-safe. Synchronization is done in each set separately so
 * operations in one set won't block operations in other sets.
 * 
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 */
public class SetAssociativeCache<K, V> implements Cache<K, V> {

	private static final ReplacementPolicy<?, ?> LRU = set -> set.getFirst();
	private static final ReplacementPolicy<?, ?> MRU = set -> set.getLast();

	/**
	 * @return the LRU(least recently used) replacement policy
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> ReplacementPolicy<K, V> lru() {
		return (ReplacementPolicy<K, V>) LRU;
	}

	/**
	 * @return the MRU(most recently used) replacement policy
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> ReplacementPolicy<K, V> mru() {
		return (ReplacementPolicy<K, V>) MRU;
	}

	private final int setCapacity;
	private final AtomicReferenceArray<Set<K, V>> sets;
	private ReplacementPolicy<K, V> policy;

	/**
	 * Constructs an empty cache with the specified set capacity， number of sets,
	 * and the default replacement policy(LRU).
	 * 
	 * @param setCapacity the maximum number of elements in each set
	 * @param numOfSets   total number of sets in this cache
	 */
	public SetAssociativeCache(int setCapacity, int numOfSets) {
		this(setCapacity, numOfSets, lru());
	}

	/**
	 * Constructs an empty cache with the specified set capacity, number of sets and
	 * replacement policy. Set capacity and number of sets must be greater than 0.
	 * 
	 * @param setCapacity the maximum number of elements in each set
	 * @param numOfSets   total number of sets in this cache
	 * @param policy      the replacement policy used by each cache set
	 * 
	 * @throws IllegalArgumentException if the set capacity or number of sets is
	 *                                  non-positive.
	 * @throws NullPointerException     if the replacement policy is null.
	 */
	public SetAssociativeCache(int setCapacity, int numOfSets, ReplacementPolicy<K, V> policy) {
		if (setCapacity <= 0) {
			throw new IllegalArgumentException("Set capacity must be greater than 0");
		}
		if (numOfSets <= 0) {
			throw new IllegalArgumentException("Number of sets must be greater than 0");
		}
		if (policy == null) {
			throw new NullPointerException("Replacement policy cannot be null");
		}
		this.setCapacity = setCapacity;
		this.sets = new AtomicReferenceArray<>(numOfSets);
		this.policy = policy;
	}

	@Override
	public V get(K key) {
		Set<K, V> set = sets.get(key.hashCode() % sets.length());
		if (set == null) {
			return null;
		}
		return set.get(key);
	}

	@Override
	public void put(K key, V value) {
		if (key == null || value == null) {
			throw new NullPointerException();
		}
		int index = key.hashCode() % sets.length();
		Set<K, V> set = sets.get(index);
		if (set == null) {
			sets.compareAndSet(index, null, new Set<>(setCapacity, policy));
			// if succeeded, then get the newly created set; otherwise the set has already
			// been created by another thread, so just use it.
			set = sets.get(index);
		}
		set.put(key, value);
	}

	/**
	 * @return the replacement policy used by this cache
	 */
	public ReplacementPolicy<K, V> getPolicy() {
		return policy;
	}

	/**
	 * Sets the replacement policy of this cache.
	 * 
	 * @param policy the replacement policy to be used by this cache.
	 */
	public void setPolicy(ReplacementPolicy<K, V> policy) {
		this.policy = policy;
	}

	private static class Set<K, V> implements CacheSet<K, V>, Cache<K, V> {
		final Map<K, Node<K, V>> nodes = new HashMap<>();
		final int capacity;
		final ReplacementPolicy<K, V> policy;
		final Node<K, V> head;
		final Node<K, V> tail;

		Set(int capacity, ReplacementPolicy<K, V> policy) {
			this.capacity = capacity;
			this.policy = policy;
			// dummy node to avoid a lot of boundary checks
			head = tail = new Node<>();
			head.next = tail;
			tail.prev = head;
		}

		@Override
		public synchronized Entry<K, V> getFirst() {
			return head.next == tail ? null : head.next;
		}

		@Override
		public synchronized Entry<K, V> getLast() {
			return tail.prev == head ? null : tail.prev;
		}

		@Override
		public Iterator<Entry<K, V>> iterator() {
			return new Iterator<Entry<K, V>>() {
				Node<K, V> current = head;

				@Override
				public boolean hasNext() {
					return current.next != tail;
				}

				@Override
				public Entry<K, V> next() {
					if (!hasNext()) {
						throw new NoSuchElementException();
					}
					current = current.next;
					return current;
				}
			};
		}

		@Override
		public Iterator<Entry<K, V>> descendingIterator() {
			return new Iterator<Entry<K, V>>() {
				Node<K, V> current = tail;

				@Override
				public boolean hasNext() {
					return current.prev != head;
				}

				@Override
				public Entry<K, V> next() {
					if (!hasNext()) {
						throw new NoSuchElementException();
					}
					current = current.prev;
					return current;
				}
			};
		}

		@Override
		public synchronized int size() {
			return nodes.size();
		}

		@Override
		public synchronized V get(K key) {
			Node<K, V> node = nodes.get(key);
			if (node == null) {
				return null;
			}
			touch(node);
			return node.value;
		}

		@Override
		public synchronized void put(K key, V value) {
			Node<K, V> node = nodes.get(key);
			if (node == null) {
				node = new Node<>(key, value);
				add(node);
			} else {
				node.value = value;
				touch(node);
			}
		}

		private void add(Node<K, V> node) {
			ensureCapacity();
			nodes.put(node.key, node);
			linkLast(node);
		}

		private void touch(Node<K, V> node) {
			node.accessTime = System.currentTimeMillis();
			node.frequency++;
			unlink(node);
			linkLast(node);
		}

		private void ensureCapacity() {
			if (nodes.size() == capacity) {
				Node<K, V> node = (Node<K, V>) policy.select(this);
				unlink(node);
				nodes.remove(node.key);
			}
		}

		private void unlink(Node<K, V> node) {
			node.prev.next = node.next;
			node.next.prev = node.prev;
		}

		private void linkLast(Node<K, V> node) {
			node.prev = tail.prev;
			node.next = tail;
			tail.prev.next = node;
			tail.prev = node;
		}
	}

	private static class Node<K, V> implements Entry<K, V> {
		K key;
		V value;
		long createTime;
		long accessTime;
		int frequency;

		Node<K, V> next;
		Node<K, V> prev;

		/*
		 * constructor for dummy nodes
		 */
		Node() {
		}

		Node(K key, V value) {
			this.key = key;
			this.value = value;
			this.accessTime = this.createTime = System.currentTimeMillis();
			this.frequency = 1;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public long getCreateTime() {
			return createTime;
		}

		@Override
		public long getAccessTime() {
			return accessTime;
		}

		@Override
		public int getFrequency() {
			return frequency;
		}

	}

}
