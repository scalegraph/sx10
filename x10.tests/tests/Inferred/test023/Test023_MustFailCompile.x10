//OPTIONS: -STATIC_CHECKS=true -CONSTRAINT_INFERENCE=true -VERBOSE_INFERENCE=true



import harness.x10Test;

public class Test023_MustFailCompile extends x10Test {

    public def run(): boolean {
	Test023.f(true, 1, 1, 1, 1);
        return true;
    }

    public static def main(Rail[String]) {
    	new Test023_MustFailCompile().execute();
    }

}
