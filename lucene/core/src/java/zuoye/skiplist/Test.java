package zuoye.skiplist;

public class Test {
	public static void main(String[] args) {
		
		int a, b;
		
		a = b = 1;
		
		System.out.println(((2^(4-1))));
		
		SkipList S = new SkipList();

		S.printHorizontal();
		System.out.println("------");
		// S.printVertical();
		// System.out.println("======");

		S.put("ABC", 123);
		S.printHorizontal();
		System.out.println("------");
		// S.printVertical();
		// System.out.println("======");

		S.put("DEF", 123);
		S.printHorizontal();
		System.out.println("------");
		// S.printVertical();
		// System.out.println("======");

		S.put("KLM", 123);
		S.printHorizontal();
		System.out.println("------");
		// S.printVertical();
		// System.out.println("======");

		S.put("HIJ", 123);
		S.printHorizontal();
		System.out.println("------");
		// S.printVertical();
		// System.out.println("======");

		S.put("GHJ", 123);
		S.printHorizontal();
		System.out.println("------");
		// S.printVertical();
		// System.out.println("======");

		S.put("AAA", 123);
		S.printHorizontal();
		System.out.println("------");
		// S.printVertical();
		// System.out.println("======");

	}
}