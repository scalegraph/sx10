import harness.x10Test;

/**
 * Check that info from the real clause of a type is propagated down
 * to the use of the type.
 */
public class FieldPropFromThis extends x10Test {
	class  Foo(int(:self==0) i) {
		public Foo() {
			this.i = 0;
		}
	}
	
	                   
	public boolean run() {
		Foo f = new Foo();
		int(:self==0) s = f.i;
		return true;
	}

	public static void main(String[] args) {
		new FieldPropFromThis().execute();
	}


}

