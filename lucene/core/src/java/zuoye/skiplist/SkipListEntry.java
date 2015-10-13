package zuoye.skiplist;

public class SkipListEntry {

	public String key;
	public Integer value;

	public int pos; // 主要为了打印链表用
	public SkipListEntry up, down, left, right; // 上下左右四个指针

	public static String negInf = new String("-oo"); // 负无穷
	public static String posInf = new String("+oo"); // 正无穷

	public SkipListEntry(String key, Integer value) {
		this.key = key;
		this.value = value;
		up = down = left = right = null;
	}
	
	public Integer getValue() {
		return value;
	}
	
	public String getKey() {
		return key;
	}
	
	public Integer setValue(Integer value) {
		Integer oldValue = this.value;
		this.value = value;
		return oldValue;
	}
	
	@Override
	public boolean equals(Object o) {
		SkipListEntry entry;
		try {
			entry = (SkipListEntry) o;
		} catch (Exception e) {
			return false;
		}
		return entry.getKey() == this.key && entry.getValue() == value;
	}
	
	@Override
	public String toString() {
		return "(" + key + "," + value + ")";
	}

}
