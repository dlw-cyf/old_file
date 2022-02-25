public class B {

	int a = 0;

	@Override
	public String toString() {
		return "B{" +
				"a=" + a +
				'}';
	}

	public int getA() {
		return a;
	}

	public void setA(int a) {
		this.a = a;
	}

	public B(int a) {
		this.a = a;
	}
}
