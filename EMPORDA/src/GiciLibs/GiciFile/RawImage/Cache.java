/*
 * GICI Library -
 * Copyright (C) 2012  Group on Interactive Coding of Images (GICI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Group on Interactive Coding of Images (GICI)
 * Department of Information and Communication Engineering
 * Autonomous University of Barcelona
 * 08193 - Bellaterra - Cerdanyola del Valles (Barcelona)
 * Spain
 *
 * http://gici.uab.es
 * gici-info@deic.uab.es
 */
package GiciFile.RawImage;

import java.util.*;

/**
 * Represents a cache with blocks of data and LRU policy.
 */
public class Cache<T> {

	/**
	 * This representations allows quick get and set operations.
	 */
	private Map<Integer,Block> pages;
	
	/**
	 * The elements are saved in order according the last time that were used.
	 * This list allows apply policy quickly.
	 */
	private DoubleLinkedList<Integer> policy;
	
	/**
	 * The max number of elements.
	 */
	private int numElements;

	/**
	 * Construct a cache wit numElements elements.
	 * @param numElements is max number of elements that cache can contain.
	 */
	public Cache(int numElements) {
		this.numElements = numElements;

		pages = new HashMap<Integer, Block>();
		policy = new DoubleLinkedList<Integer>();
	}

	/**
	 * Return the block that contains this position.
	 * @param position of the block that we want.
	 * @return block that contains this position.
	 */
	public Block get(int position) {
		Block page = pages.get(position);
		if(page != null) {
			LinkedElement<Integer> element = page.element;
			policy.remove(element);
			page.element = policy.add(element.key);
			return page;
		}
		return null;
	}

	/**
	 * Add a newPage to cache and if it is full, return least recently used block.
	 * @param newPage is a block that we want add to cache.
	 * @return least recently used block if cache is full or null in another case.
	 */
	public Block set(Block newPage) {
		Block block = get(newPage.position);
		if(block != null) {
			block.data = newPage.data;
			block.dirty = true;
			return null;
		}
		pages.put(newPage.position, newPage);
		newPage.element = policy.add(newPage.position);
		Block deletedPage = null;
		if(policy.size() > numElements) {
			int key = policy.poll().key;
			deletedPage = pages.remove(key);
		}
		return deletedPage;
	}

	/**
	 * Mark as dirty the block that represents this position.
	 * @param position of block that we want mark as dirty.
	 * @throws NoSuchElementException if any block contains this position.
	 */
	public void markDirty(int position) throws NoSuchElementException {
		Block page = pages.get(position);
		if(page == null) {
			throw new NoSuchElementException("Can't mark as dirty element "+position+" because it's not cached");
		}
		page.dirty = true;
	}

	/**
	 * Return all dirty blocks and mark them as not dirty.
	 * @return all dirty blocks.
	 */
	public ArrayList<Block> flush() {
		ArrayList<Block> dirtyPages = new ArrayList<Block>();
		Iterator<Integer> iterator = policy.iterator();
		while(iterator.hasNext()) {
			Block page = pages.get(iterator.next());
			if(page.dirty) {
				dirtyPages.add(page);
				page.dirty = false;
			}
		}
		return dirtyPages;
	}

	/**
	 * Is a cache block.
	 */
	public class Block {
		/**
		 * An element that represents the position of the block in
		 * double linked list.
		 */
		protected LinkedElement<Integer> element;
		
		/**
		 * The data of this block.
		 */
		public T data;
		
		/**
		 * The position at this block starts.
		 */
		public int position;
		
		/**
		 * Says if the data of this block has been modified.
		 */
		public boolean dirty;

		/**
		 * Construct a block of cache.
		 * @param data is the data of this block.
		 * @param position is the position at this block starts.
		 * @param dirty says if the data of this block has been modified.
		 */
		public Block(T data, int position, boolean dirty) {
			element = null;
			this.data = data;
			this.position = position;
			this.dirty = dirty;
		}
	}
}

/**
 * An implementation of double linked list.
 */
class DoubleLinkedList<T> {

	/**
	 * Represents a iterator over linked list.
	 */
	class DoubleLinkedListIterator implements Iterator<T> {
		/**
		 * The next element to be returned.
		 */
		LinkedElement<T> element;

		/**
		 * Construct a iterator starting at element 'element'
		 * @param element is an element of linked list.
		 */
		DoubleLinkedListIterator(LinkedElement<T> element) {
			this.element = element;
		}

		/**
		 * Return true if there are more elements.
		 * @return true if there are more elements.
		 */
		public boolean hasNext() {
			return element != null;
		}

		/**
		 * Return the next element of this list.
		 * @throws NoSuchElementException if there are no more elements.
		 */
		public T next() throws NoSuchElementException {
			if(element == null) {
				throw new NoSuchElementException();
			}
			T key = element.key;
			element = element.next;
			return key;
		}

		/**
		 * Remove operation is not supported.
		 * @throws UnsupportedOperationException if is called.
		 */
		public void remove() throws UnsupportedOperationException {
			throw new UnsupportedOperationException("remove operation is not implemented");
		}
	}
			
	/**
	 * First element of this list.
	 */
	private LinkedElement<T> head;
	
	/**
	 * Last element of this list.
	 */
	private LinkedElement<T> tail;
	
	/**
	 * Number of elements of the list.
	 */
	private int size;

	/**
	 * Construct a double linked list.
	 */
	public DoubleLinkedList() {
		head = null;
		tail = null;
		size = 0;
	}

	/**
	 * Add at the end an element with data key.
	 * @param key is the data of the element to add.
	 * @return an element added to this list.
	 */
	public LinkedElement<T> add(T key) {
		LinkedElement<T> le = new LinkedElement<T>(key);

		if(head == null) {
			head = le;
		}else {
			tail.next = le;
		}
		le.prev = tail;
		le.next = null;
		tail = le;

		size++;
		return le;
	}

	/**
	 * Remove and return the first element of the list.
	 * @return the first element of the list.
	 * @throws NoSuchElementException if the list is empty.
	 */
	public LinkedElement<T> poll() throws NoSuchElementException {
		if(head == null) {
			throw new NoSuchElementException("Can't do poll operation in empty list");
		}
		
		LinkedElement<T> t = head;
		head = t.next;
		if(head == null) {
			tail = null;
		} else {
			head.prev = null;
		}
		size--;
		return t;
	}

	/**
	 * Remove the element e of this list.
	 * @param e is the element to remove. Must be not null.
	 */
	public void remove(LinkedElement<T> e) {
		if (e.prev != null) {
			e.prev.next = e.next;
		} else {
			head = e.next;
		}

		if (e.next != null) {
			e.next.prev = e.prev;
		} else {
			tail = e.prev;
		}
		size--;
	}

	/**
	 * Return the number of the elements of this list.
	 * @return the number of elements.
	 */
	public int size() {
		return size;
	}

	/**
	 * Return a iterator over this list.
	 * @return iterator over this list.
	 */
	public Iterator<T> iterator() {
		return new DoubleLinkedListIterator(head);
	}
}

/**
 * Represents an element of double linked list.
 */
class LinkedElement<T> {

	/**
	 * Constructs an element of double linked list.
	 * @param key is the data of this element.
	 */
	public LinkedElement(T key) {
		this.key=key;
	}

	/**
	 * Next element in the list.
	 */
	LinkedElement<T> next;
	
	/**
	 * Previous element in the list.
	 */
	LinkedElement<T> prev;
	
	/**
	 * Data of the element.
	 */
	T key;
}
