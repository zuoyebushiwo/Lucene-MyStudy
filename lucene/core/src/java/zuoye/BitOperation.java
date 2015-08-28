package zuoye;


public class BitOperation {
	
	public static void main(String[] args) {
		System.out.println( (1 << 30));
		System.out.println( (1 << 28));
		System.out.println(31 - Integer.numberOfLeadingZeros(1 << 28));
	}

}
