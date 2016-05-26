import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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
						return foo;
				}
			}
		    
		}
		return ans;
	}
	public static void convertProblem(String problem, StanfordCoreNLP pipeline) {
		
		problem = WordProblemSolver.solveWordProblems(problem, pipeline);
		Annotation document = new Annotation(problem);
		pipeline.annotate(document);
		int[] times = new int[10];
		int timer = 0;
		boolean eventFlag = false;
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
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
	    		}
	    		else if (pos.contains("VB")) {
	    			 if (!token.lemma().equals("be")) {
	    				 eventFlag = true;
	    				 timer++;
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
	    }
	}
	public static void main(String[] args) throws ScriptException {
		System.out.println(findRelation(20,10,2));
		System.out.println(findRelation(10,20,2));
		System.out.println(findRelation(10,2,20));
		System.out.println(findRelation(20,2,10));
		System.out.println(findRelation(2,20,10));
		System.out.println(findRelation(2,10,20));
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		convertProblem("A boy is 10 years older than his brother. In 4 years, he will be twice as old as his brother. What are their present ages?", pipeline);
	}

}
