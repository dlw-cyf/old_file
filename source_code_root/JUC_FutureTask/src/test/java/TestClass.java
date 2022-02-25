public class TestClass {

	private String a = "aa";

	@Override
	public String toString() {
		return "TestClass{" +
				"a='" + a + '\'' +
				'}';
	}

	public String getA() {
		return a;
	}

	public void setA(String a) {
		this.a = a;
	}
}
