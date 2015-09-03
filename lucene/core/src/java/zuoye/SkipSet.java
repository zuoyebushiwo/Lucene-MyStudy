package zuoye;

/**
 *	����ڵ����ݴ洢�ṹ
 */
class SkipNode<E extends Comparable<? super E>> {
	public final E value; //�ڵ�洢������
	public final SkipNode<E>[] forward; //�ڵ��ָ������
	
	/**
	 * ���ݽڵ�Ĳ㼶����һ���ڵ�
	 * @param level �ڵ�㼶
	 * @param value �ڵ�洢ֵ
	 */
	@SuppressWarnings("unchecked")
	public SkipNode(int level, E value) {
		forward = new SkipNode[level + 1];//level���Ԫ�غ������level+1��ָ������
		this.value = value;
	}

}

public class SkipSet<E extends Comparable<? super E>> {
	
	/**
	 * �������ӣ�ʵ��֤��p=1/e��p=0.5Ҫ�ã�e�Ǹ���������֣�
	 */
//	public static final double P = 0.5;
	public static final double P = 1/Math.E;
	/**
	 *  ���㼶
	 */
	public static final int MAX_LEVEL = 6;
	
	/**
	 * ��ʼ�ڵ㣬����ֵ���ᴩ���в�
	 */
	public final SkipNode<E> header = new SkipNode<E>(MAX_LEVEL, null);
	/**
	 * ��ǰ�������߲㼶
	 */
	public int level = 0;
	
	/**
	 * ����һ��Ԫ��
	 * @param value ������ֵ
	 */
	@SuppressWarnings("unchecked")
	public void insert(E value) {
		SkipNode<E> x = header;
		SkipNode<E>[] update = new SkipNode[MAX_LEVEL + 1];
		//����Ԫ�ص�λ�ã�������ʵ����һ��contain������ע�ͼ�contain
		for (int i = level; i >= 0; i--) {
			while (x.forward[i] != null
					&& x.forward[i].value.compareTo(value) < 0) {
				x = x.forward[i];
			}
			//update[i]�Ǳ�valueС�����������ģ���value��ǰ�ýڵ�
			update[i] = x;
		}
		x = x.forward[0];

		//�˴������������ͬԪ�أ�Ϊһ��set
		if (x == null || !x.value.equals(value)) {//�����в�������Ҫ���Ԫ��
			//�����������Ĳ㼶
			int lvl = randomLevel();
			//����������㼶�ȵ�ǰ�������߲㼶����Ҫ�����Ӧ�Ĳ㼶����������߲㼶
			if (lvl > level) {
				for (int i = level + 1; i <= lvl; i++) {
					update[i] = header;
				}
				level = lvl;
			}
			
			//�����½ڵ�
			x = new SkipNode<E>(lvl, value);
			//�����ڵ��ָ�룬��ָ������ָ��
			for (int i = 0; i <= lvl; i++) {
				x.forward[i] = update[i].forward[i];
				update[i].forward[i] = x;
			}

		}
	}
	/**
	 * ɾ��һ��Ԫ��
	 * @param value ��ɾ��ֵ
	 */
	@SuppressWarnings("unchecked")
	public void delete(E value) {
		SkipNode<E> x = header;
		SkipNode<E>[] update = new SkipNode[MAX_LEVEL + 1];
		//����Ԫ�ص�λ�ã�������ʵ����һ��contain������ע�ͼ�contain
		for (int i = level; i >= 0; i--) {
			while (x.forward[i] != null
					&& x.forward[i].value.compareTo(value) < 0) {
				x = x.forward[i];
			}
			update[i] = x;
		}
		x = x.forward[0];
		//ɾ��Ԫ�أ�����ָ��
		if (x.value.equals(value)) {
			for (int i = 0; i <= level; i++) {
				if (update[i].forward[i] != x)
					break;
				update[i].forward[i] = x.forward[i];
			}
			//���Ԫ��Ϊ�������һ��Ԫ�أ���ɾ��ͬʱ���͵�ǰ�㼶
			while (level > 0 && header.forward[level] == null) {
				level--;
			}

		}
	}
	/**
	 * �����Ƿ������Ԫ��
	 * @param searchValue ������ֵ
	 * @return true��������false:������
	 */
	public boolean contains(E searchValue) {
		SkipNode<E> x = header;
		//�ӿ�ʼ�ڵ����߲㼶��ʼ����
		for (int i = level; i >= 0; i--) {
			//�����ﱾ�㼶��NULL�ڵ���������Ȳ���ֵ��Ľڵ�ʱ��ת����һ�㼶����
			while (x.forward[i] != null
					&& x.forward[i].value.compareTo(searchValue) < 0) {
				x = x.forward[i];
			}
		}
		x = x.forward[0];
		//��ʱx�����ֿ��ܣ�1.x=null,2.x.value=searchValue,3.x.value>searchValue
		return x != null && x.value.equals(searchValue);
	}
	/**
	 * ����������ľ������ڣ�ͨ������������жϽڵ�Ĳ㼶
	 * @return �ڵ�Ĳ㼶
	 */
	public static int randomLevel() {
		int lvl = (int) (Math.log(1. - Math.random()) / Math.log(1. - P));
		return Math.min(lvl, MAX_LEVEL);
	}

	/**
	 * ������������Ԫ��
	 * ������ײ��Ԫ�ؼ���
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		SkipNode<E> x = header.forward[0];
		while (x != null) {
			sb.append(x.value);
			x = x.forward[0];
			if (x != null)
				sb.append(",");
		}
		sb.append("}");
		return sb.toString();
	}
	
	public static void main(String[] args) {
		SkipSet<Integer> skipSet = new SkipSet<Integer>();
		skipSet.insert(1);
		skipSet.insert(2);
		skipSet.insert(3);
		skipSet.insert(4);
		skipSet.insert(5);
		skipSet.insert(6);
		skipSet.insert(7);
		System.out.println();
	}
	
}

