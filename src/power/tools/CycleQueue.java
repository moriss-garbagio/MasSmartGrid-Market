package power.tools;

public class CycleQueue<T> {
	private int start = 0;
	private int end = 0;
	private int size = 0;
	private Object[] list;

	public CycleQueue() {
		this(16);
	}

	public CycleQueue(int capacity) {
		list = new Object[capacity];
	}

	public boolean add(T element) {
		if (size == list.length) {
			if (!increaseCapacity()) return false;
		}
		list[end] = element;
		end = (end + 1) % list.length;
		size++;
		return true;
	}

	@SuppressWarnings("unchecked")
	public T remove() {
		if (size > 0) {
			T object = (T)list[start];
			start = (start + 1) % list.length;
			size --;
			return object;
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public T get(int index) {
		if (index < size()) {
			return (T)list[(start + index) % list.length];
		} else {
			throw new IndexOutOfBoundsException();
		}
	}

	public int size() {
		return size;
	}

	private boolean increaseCapacity() {
		Object[] newList = null;
		try {
			newList = new Object[2 * list.length];
		} catch(Exception e) {
			return false;
		}
		for (int index = 0; index < size; index++) {
			newList[index] = list[(start + index) % list.length];
		}
		list = newList;
		
		end = size;
		start = 0;
		return true;
	}
}
