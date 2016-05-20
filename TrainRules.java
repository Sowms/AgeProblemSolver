import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


public class TrainRules {
	
	public static String findRelation(int a, int b, int c) throws ScriptException {
		String ans = "";
		int[] numbers = new int[3];
		numbers[0] = a;
		numbers[1] = b;
		numbers[2] = c;
		//http://stackoverflow.com/questions/3422673/evaluating-a-math-expression-given-in-string-form
		ScriptEngineManager mgr = new ScriptEngineManager();
	    ScriptEngine engine = mgr.getEngineByName("JavaScript");
		char[] op = {'+','-','*','/'};
		for (int i=0; i<3; i++) {
			int num1 = numbers[i], num2 = numbers[(i+1)%3], num3 = numbers[(i+2)%3];
			for (int j=0; j<4; j++) {
				for (int k=0; k<4; k++) {
					String foo = num1 + "" + op[j] + "" + num2 + op[k] + num3 ;
					double val = (Double) engine.eval(foo);
					if (val == 0)
						return foo;
				}
			}
		    
		}
		return ans;
	}
	
	public static void main(String[] args) throws ScriptException {
		System.out.println(findRelation(20,10,2));
		System.out.println(findRelation(10,20,2));
		System.out.println(findRelation(10,2,20));
		System.out.println(findRelation(20,2,10));
		System.out.println(findRelation(2,20,10));
		System.out.println(findRelation(2,10,20));
	}

}
