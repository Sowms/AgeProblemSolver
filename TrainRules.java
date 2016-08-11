import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jpl.Atom;
import jpl.Compound;
import jpl.Query;
import jpl.Term;
import jpl.Variable;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;


public class TrainRules {
	
	public static ArrayList<String> actors = new ArrayList<>();
    
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
						return "V1" + op[j] + "V2" + op[k] +"V3";
				}
			}
		    
		}
		return ans;
	}
	public static String convertProblem(String problem, String ans, StanfordCoreNLP pipeline) throws IOException, ScriptException, NumberFormatException, InterruptedException {
		actors = new ArrayList<>();
		problem = WordProblemSolver.coref(problem, pipeline);
		System.out.println(problem);
		problem = WordProblemSolver.convertNumberNames(problem, pipeline);
		problem = WordProblemSolver.substitute(problem, pipeline);
		problem = ConjunctionResolver.parse(problem, pipeline);
		Annotation document = new Annotation(problem);
		pipeline.annotate(document);
		int[] times = new int[10];
		int size = 0;
		int timer = 0;
		times[0] = 0;
		size++;
		boolean eventFlag = false;
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    String program = "";
		char base = 'X';
		int count = 0;
		String actor = "";
	    for (CoreMap sentence : sentences) {
	    	List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
	    	boolean quesFlag = false, adjFlag = false;
	    	ArrayList<String> arguments = new ArrayList<>();
	    	String predicate = "";
	    	String adj = "";
	    	timer = 0;
	    	for (CoreLabel token: tokens) {
	    		String pos = token.tag();
	    		if (pos.startsWith("W") || token.originalText().toLowerCase().contains("find") || token.originalText().toLowerCase().contains("calculate")) {
	    			quesFlag = true;
	    			break;
	    		}
	    		WordNetInterface.seen = new ArrayList<>();
	    		if (!pos.contains("NN") && !adj.isEmpty() && !adjFlag) 
	    			adj = "";
	    		else if (!pos.contains("NN") && !adj.isEmpty() && adjFlag) {
	    			String arg = adj;
	    			if (!arguments.contains(arg)) {
	    				arguments.add(arg);
	    				actors.add(arg);
	    			}
	    			adj = "";
	    			adjFlag = false;
	    		}
	    		if (pos.contains("NNP")) {
	    			arguments.add(token.originalText().toLowerCase());
	    			actors.add(token.originalText().toLowerCase());
	    		}	
	    		else if (pos.contains("NN") && WordNetInterface.isActor(token.originalText().toLowerCase())) {
	    			String arg = "";
	    			if (!adj.isEmpty()) {
	    				adjFlag = true;
	    				String replace = new String(adj);
	    				adj = adj+token.originalText().toLowerCase();
	    				predicate = predicate.replace(adj, "");
	    				predicate = predicate.replace(replace, "");
	    				replace = replace.toUpperCase().substring(0, 1) + replace.substring(1);
	    				predicate = predicate.replace(replace, "");
		    			replace = adj.toUpperCase().substring(0, 1) + adj.substring(1);
		    			predicate = predicate.replace(replace, "");
		    			replace = token.originalText().toLowerCase().toUpperCase().substring(0, 1) + token.originalText().toLowerCase().substring(1);
		    			predicate = predicate.replace(replace, "");
	    			}
	    			else
	    				arg = token.originalText().toLowerCase();
	    			if (!arg.isEmpty() && !arguments.contains(arg)) {
	    				arguments.add(arg);
	    				actors.add(arg);
	    			}
	    		}
	    		else if (pos.contains("CD")) {
	    			//is it timer?
	    			int prev = timer;
	    			if (sentence.toString().toLowerCase().contains("in "+token.originalText()+" year"))
	    				timer = Integer.parseInt(token.originalText());
	    			else if (sentence.toString().toLowerCase().contains(token.originalText()+" year")) {
	    				if (sentence.toString().toLowerCase().contains(token.originalText()+" years from now"))
	    					timer = Integer.parseInt(token.originalText());
	    				else if (sentence.toString().toLowerCase().contains(token.originalText()+" years ago"))
	    					timer = Integer.parseInt("-"+token.originalText());
	    				else if (sentence.toString().toLowerCase().contains(token.originalText()+" years before"))
		    				timer = Integer.parseInt("-"+token.originalText());
		    			else
		    				arguments.add(token.originalText().toLowerCase());
	    			}
	    			else
	    				arguments.add(token.originalText().toLowerCase());
	    			if (prev != timer) {
	    				times[size] = timer;
	    				size++;
	    			}
	    		}
	    		else if (pos.contains("VB")) {
	    			 if (!token.lemma().equals("be")) {
	    				 eventFlag = true;
	    				 timer++;
	    				 times[size] = timer;
		    			 size++;
	    			 }
	    		}
	    		else if (!pos.contains("PRP") && !pos.contains("CC") && !pos.contains("RB") && !pos.contains("TO") && !pos.contains("MD") && !pos.contains("DT") && !pos.contains(".")) {
	    			if (!pos.contains("IN") && !pos.contains(",")) {
	    				if (!predicate.isEmpty() && !token.originalText().contains("year")) {
	    					predicate = predicate + token.originalText().substring(0,1).toUpperCase();
	    					predicate = predicate + token.originalText().substring(1, token.originalText().length());
	    				}
	    				else if (predicate.isEmpty() && !token.originalText().contains("year")) 
	    					predicate = predicate + token.originalText();
	    				if (pos.contains("NN") || pos.contains("JJ"))
	    					adj = token.originalText();
	    			}
	    		}
	    	}
	    	if (quesFlag)
	    		continue;
	    	String begin = "";
	    	if (eventFlag) 
	    		begin = "happensAt";
	    	else
	    		begin = "holdsAt";
	    	
	    	if (predicate.equals("old")) {
	    		predicate = "age";
	    		for (String s : arguments) {
	    			if (actors.contains(s))
	    				actor = s;
	    		}
	    	}
	    	actors.removeAll(Collections.singleton(actor));
	    	String stmt = begin + "(" + predicate + "(";
	    	System.out.println(actors);
	    	ArrayList<String> temp = new ArrayList<>();
	    	if (sentence.toString().contains(" their "))
	    		temp.addAll(actors);
	    	temp.addAll(arguments);
	    	arguments = temp;
	    	if (predicate.equals("age") && arguments.size() == 1)
	    		arguments.add("K");
	    	for (String s : arguments) {
	    		stmt = stmt + s +",";
	    	}
	    	if (timer < 0)
	    		stmt = stmt + ")," + "(" + timer + ")" + ").";
	    	else
	    		stmt = stmt + ")," + timer + ").";
	    	stmt = stmt.replaceAll(",\\)", ")");
	    	System.out.println(stmt);
	    	program = program+stmt+"\n";
	    	if (predicate.equals("age")) {
	    		char change = (char) (base+count);
	    		if (timer != 0) {
	    			stmt = stmt.replace("),"+timer+")", "-"+timer+"+"+change+"), "+change+")");
	    			count++;
		    		program = program+stmt+"\n";
	    		}
	    	}
	    	
		    	
	    }
	    program = ans+"\n"+program;
	    if (!ans.isEmpty())
	    	addRules(program,size,times);
	    return program;
	}
	
	public static void addRules(String program, int size, int[] times) throws IOException, ScriptException {
	    FileWriter fw = new FileWriter(new File("foo.pl"));
    	BufferedWriter bw = new BufferedWriter(fw);
    	bw.write(program);
    	bw.close();
    	Query q1 = new Query("consult('foo.pl')");
    	System.out.println( "consult " + (q1.hasSolution() ? "succeeded" : "failed"));
		
    	fw = new FileWriter(new File("rules.pl"), true);
    	bw = new BufferedWriter(fw);
    	
    	for (int i=0; i<size; i++) {
    		Query q4 = new Query(new Compound("holdsAt", new Term[] {new Variable("X"), new jpl.Integer(times[i])}));
    		ArrayList<String> antecedents = new ArrayList();
    		while (q4.hasMoreSolutions()) {
    			String match = q4.nextSolution().get("X").toString();
    			if (match.contains("+(")) { 
    				Pattern wordPattern = Pattern.compile("\\+\\((\\+|-)*\\d+\\,\\s(\\+|-)*\\d+\\)");
    				Matcher matcher = wordPattern.matcher(match);
					if (matcher.find()) {
						String cal = matcher.group();
						Pattern numPattern = Pattern.compile("(\\+|-)*\\d+");
						Matcher numMatcher = numPattern.matcher(cal);
						String eval = "";
						while (numMatcher.find()) {
							eval = eval + numMatcher.group()+"+";
						}
						eval = eval.substring(0,eval.length()-1);
					//	System.out.println(eval);
						ScriptEngineManager mgr = new ScriptEngineManager();
					    ScriptEngine engine = mgr.getEngineByName("JavaScript");
					    double a = (Double) engine.eval(eval);
					  
					    match = match.replace(cal, (a+"").replace(".0", ""));
					}
				}
    			System.out.println(match);
    			if (!antecedents.contains(match))
    				antecedents.add(match);
    		}
    		String rule = makeRule(antecedents);
    		bw.write(rule+"\n");
    		System.out.println("___");
    		q4.close();
    	}
    	bw.close();
	}
	private static String makeRule(ArrayList<String> antecedents) throws ScriptException {
		String rule = "";
		Pattern numPattern = Pattern.compile("(\\+|-)*\\d+");
		Pattern wordPattern = Pattern.compile("\\w+");
		HashMap<String,String> vars = new HashMap<>();
		int counter = 1, argCount = 1;
		int[] numbers = new int[3];
		System.out.println(antecedents);
		for (String s : antecedents) {
			Matcher m = numPattern.matcher(s);
			while (m.find()) {
				String num = m.group();
				if (!vars.containsKey(num)) {
					vars.put(num, "V"+counter);
					numbers[counter-1] = Integer.parseInt(num);
					counter++;
				}
			}
			m = wordPattern.matcher(s);
			while (m.find()) {
				String arg = m.group();
				if (!s.contains(arg+"(")) {
					if (!vars.containsKey(arg)) {
						vars.put(arg,"X"+argCount);
						argCount++;
					}
				}
			}
		}
		System.out.println(vars);
		String expr = findRelation(numbers[0],numbers[1],numbers[2]);
		for (String s: antecedents) {
			String newAntecedent = new String(s);
			Matcher m = numPattern.matcher(s);
			while (m.find()) {
				String num = m.group();
				//System.out.println("k"+newAntecedent);
				if (newAntecedent.contains(" "+num+",") || newAntecedent.contains("("+num+" ,")) {
					newAntecedent = newAntecedent.replace(" "+num+",", " " + vars.get(num) + ",");
					newAntecedent = newAntecedent.replace("("+num+" ,", "(" + vars.get(num)+ ",");
				}
				if (newAntecedent.contains(" "+num+")"))
						newAntecedent = newAntecedent.replace(" "+num+")", " " + vars.get(num) + ")");
				//System.out.println("k"+newAntecedent);
			}
			m = wordPattern.matcher(s);
			while (m.find()) {
				String arg = m.group();
				//System.out.println("a" + arg);
				if (numPattern.matcher(arg).find())
					continue;
				if (newAntecedent.contains(arg) && vars.containsKey(arg))
					newAntecedent = newAntecedent.replace(arg, vars.get(arg));
			}
			newAntecedent = "holdsAt(" + newAntecedent + ",T)";
			rule = rule + newAntecedent + ", ";
		}
		rule = rule.substring(0, rule.length()-2);
		rule = "equation("+expr+", 0) :- " + rule + ".";
		System.out.println(rule);
		return rule;
	}
	public static void main(String[] args) throws ScriptException, IOException, NumberFormatException, InterruptedException {
		System.out.println(findRelation(20,10,2));
		System.out.println(findRelation(10,20,2));
		System.out.println(findRelation(10,2,20));
		System.out.println(findRelation(20,2,10));
		System.out.println(findRelation(2,20,10));
		System.out.println(findRelation(2,10,20));
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    FileWriter fw = new FileWriter(new File("rules.pl"));
	    BufferedWriter bw = new BufferedWriter(fw);
	    bw.write("");
	    bw.close();
	    String ans = "";
	    ans = "holdsAt(age(boy,16),0).\nholdsAt(age(brother,6),0).\nholdsAt(age(boy,16+X),X).\nholdsAt(age(brother,6+Y),Y).";
		convertProblem("A boy is 10 years older than his brother. In 4 years, he will be 2 times as old as his brother. What are their present ages?", ans, pipeline);
		ans = "holdsAt(age(carmen,25),0).\nholdsAt(age(david,13),0).\nholdsAt(age(carmen,25+X),X).\nholdsAt(age(david,13+Y),Y).";
		convertProblem("Carmen is 12 years older than David. Five years ago the sum of their ages was 28. How old are they now?", ans, pipeline);
		ans = "holdsAt(age(brandon,46),0).\nholdsAt(age(ronda,37),0).\nholdsAt(age(brandon,46+X),X).\nholdsAt(age(ronda,37+Y),Y).";
		convertProblem("Brandon is 9 years older than Ronda. In 4 years the sum of their ages will be 91. How old are they now?", ans, pipeline);
		ans = "holdsAt(age(pat,38),0).\nholdsAt(age(james,18),0).\nholdsAt(age(pat,38+X),X).\nholdsAt(age(james,18+Y),Y).";
		convertProblem("Pat is 20 years older than his son James. In two years Pat will be twice as old as James. How old are they now?", ans, pipeline);
		ans = "holdsAt(age(jason,20),0).\nholdsAt(age(mandy,15),0).\nholdsAt(age(jason,20+X),X).\nholdsAt(age(mandy,15+Y),Y).";
		convertProblem("The sum of Jason and Mandy's age is 35. Ten years ago Jason was double Mandy's age. How old are they now?", ans, pipeline);
		ans = "holdsAt(age(father,45),0).\nholdsAt(age(daughter,15),0).\nholdsAt(age(father,45+X),X).\nholdsAt(age(daughter,15+Y),Y).";
		convertProblem("A daughter is 30 years younger than the father. The father is 4 times as old as his daughter 5 years ago. Find the present age of the father.", ans, pipeline);
		ans = "holdsAt(age(father,45),0).\nholdsAt(age(daughter,15),0).\nholdsAt(age(father,45+X),X).\nholdsAt(age(daughter,15+Y),Y).";
		convertProblem("The difference between the ages of a father and daughter is 30 years. The father is 4 times as old as his daughter 5 years ago. Find the present age of the father.", ans, pipeline);
		ans = "holdsAt(age(father,45),0).\nholdsAt(age(daughter,15),0).\nholdsAt(age(father,45+X),X).\nholdsAt(age(daughter,15+Y),Y).";
		convertProblem("The ratio between the ages of a father and daughter is 3. The father is 4 times as old as his daughter 5 years ago. Find the present age of the father.", ans, pipeline);
		
	}

}
