package zuoye.skiplist;

public class SkipListEntry {

	public String key;
	public Integer value;

	public int pos; // ��ҪΪ�˴�ӡ������
	public SkipListEntry up, down, left, right; // ���������ĸ�ָ��

	public static String negInf = new String("-oo"); // ������
	public static String posInf = new String("+oo"); // ������

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
