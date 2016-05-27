import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
	public static String convertProblem(String problem, String ans, StanfordCoreNLP pipeline) throws IOException, ScriptException {
		
		problem = WordProblemSolver.solveWordProblems(problem, pipeline);
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
	    for (CoreMap sentence : sentences) {
	    	List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
	    	boolean quesFlag = false;
	    	ArrayList<String> arguments = new ArrayList<>();
	    	String predicate = "";
	    	for (CoreLabel token: tokens) {
	    		String pos = token.tag();
	    		if (pos.contains("W") || token.originalText().contains("find") || token.originalText().contains("calculate")) {
	    			quesFlag = true;
	    			continue;
	    		}
	    		if (pos.contains("NN") && !token.originalText().contains("years")) {
	    			arguments.add(token.originalText().toLowerCase());
	    		}
	    		else if (pos.contains("CD")) {
	    			//is it timer?
	    			int prev = timer;
	    			if (sentence.toString().toLowerCase().contains("in "+token.originalText()+" year"))
	    				timer = Integer.parseInt(token.originalText());
	    			else if (sentence.toString().toLowerCase().contains(token.originalText()+" year")) {
	    				if (sentence.toString().toLowerCase().contains(token.originalText()+" from now"))
	    					timer = Integer.parseInt(token.originalText());
	    				else
	    					arguments.add(token.originalText().toLowerCase());
	    			}
	    			else if (sentence.toString().toLowerCase().contains(token.originalText()+" ago "))
	    				timer = -Integer.parseInt(token.originalText());
	    			else if (sentence.toString().toLowerCase().contains(token.originalText()+" before "))
	    				timer = -Integer.parseInt(token.originalText());
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
	    		else if (!pos.contains("PRP") && !pos.contains("MD") && !pos.contains("DT") && !pos.contains(".")) {
	    			if (!pos.contains("IN") && !pos.contains(",")) {
	    				if (!predicate.isEmpty() && !token.originalText().contains("year")) {
	    					predicate = predicate + token.originalText().substring(0,1).toUpperCase();
	    					predicate = predicate + token.originalText().substring(1, token.originalText().length());
	    				}
	    				else if (predicate.isEmpty() && !token.originalText().contains("year")) 
	    					predicate = predicate + token.originalText();
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
	    	String stmt = begin + "(" + predicate + "(";
	    	for (String s : arguments) {
	    		stmt = stmt + s +",";
	    	}
	    	stmt = stmt + ")," + timer + ").";
	    	stmt = stmt.replaceAll(",\\)", ")");
	    	System.out.println(stmt);
	    	program = program+stmt+"\n";
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
		
    	fw = new FileWriter(new File("rules.pl"));
    	bw = new BufferedWriter(fw);
    	
    	for (int i=0; i<size; i++) {
    		Query q4 = new Query(new Compound("holdsAt", new Term[] {new Variable("X"), new jpl.Integer(times[i])}));
    		ArrayList<String> antecedents = new ArrayList();
    		while (q4.hasMoreSolutions()) {
    			String match = q4.nextSolution().get("X").toString();
    			if (match.contains("+(")) { 
    				Pattern wordPattern = Pattern.compile("\\+\\(\\d+\\,\\s\\d+\\)");
    				Matcher matcher = wordPattern.matcher(match);
					if (matcher.find()) {
						String cal = matcher.group();
					//	System.out.println(cal);
						Pattern numPattern = Pattern.compile("\\d+");
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
		Pattern numPattern = Pattern.compile("\\d+");
		Pattern wordPattern = Pattern.compile("\\w+");
		HashMap<String,String> vars = new HashMap<>();
		int counter = 1, argCount = 1;
		int[] numbers = new int[3];
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
		//System.out.println(vars);
		String expr = findRelation(numbers[0],numbers[1],numbers[2]);
		for (String s: antecedents) {
			String newAntecedent = new String(s);
			Matcher m = numPattern.matcher(s);
			while (m.find()) {
				String num = m.group();
				if (newAntecedent.contains(num))
					newAntecedent = newAntecedent.replace(num, vars.get(num));
			}
			m = wordPattern.matcher(s);
			while (m.find()) {
				String arg = m.group();
				if (newAntecedent.contains(arg) && vars.containsKey(arg))
					newAntecedent = newAntecedent.replace(arg, vars.get(arg));
			}
			newAntecedent = "holdsAt(" + newAntecedent + ",T)";
			rule = rule + newAntecedent + ", ";
		}
		rule = rule.substring(0, rule.length()-2);
		rule = "equation("+expr+", 0) :- " + rule;
		System.out.println(rule);
		return rule;
	}
	public static void main(String[] args) throws ScriptException, IOException {
		System.out.println(findRelation(20,10,2));
		System.out.println(findRelation(10,20,2));
		System.out.println(findRelation(10,2,20));
		System.out.println(findRelation(20,2,10));
		System.out.println(findRelation(2,20,10));
		System.out.println(findRelation(2,10,20));
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    String ans = "holdsAt(age(boy,16),0).\nholdsAt(age(brother,6),0).\nholdsAt(age(boy,16+X),X).\nholdsAt(age(brother,6+Y),Y).";
		convertProblem("A boy is 10 years older than his brother. In 4 years, he will be 2 times as old as his brother. What are their present ages?", ans, pipeline);
	}

}
