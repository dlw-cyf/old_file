import java.util.concurrent.locks.ReentrantLock;

public class Test {

	private static void testTryFinally(int a){
		boolean flag = true;
		try{

			if(a == 0){
				flag = false;
				System.out.println("aaaaaaaaaaaaaaaaa");
			}

		}finally {
			if(flag){
				System.out.println("ffffffffffffffffffffffffffff");
			}
		}
	}

	public static void main(String[] args) {
		testTryFinally(0);

	}

}
