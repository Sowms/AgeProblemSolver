
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import jpl.Compound;
import jpl.Query;
import jpl.Term;
import jpl.Variable;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class WordProblemSolver {
	
	public static String coref(String problem, StanfordCoreNLP pipeline) {
		Annotation document = new Annotation(problem);
		pipeline.annotate(document);
		Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
		HashMap<String,String> coref = new HashMap<String,String>();
		//http://stackoverflow.com/questions/6572207/stanford-core-nlp-understanding-coreference-resolution
		for(Map.Entry<Integer, CorefChain> entry : graph.entrySet()) {
            CorefChain c = entry.getValue();
            //this is because it prints out a lot of self references which aren't that useful
            if(c.getMentionsInTextualOrder().size() <= 1)
                continue;
            CorefMention cm = c.getRepresentativeMention();
            String clust = "";
            List<CoreLabel> tks = document.get(SentencesAnnotation.class).get(cm.sentNum-1).get(TokensAnnotation.class);
            for(int i = cm.startIndex-1; i < cm.endIndex-1; i++)
                clust += tks.get(i).get(TextAnnotation.class) + " ";
            clust = clust.trim();
            ////System.out.println("representative mention: \"" + clust + "\" is mentioned by:");
            for(CorefMention m : c.getMentionsInTextualOrder()){
                String clust2 = "";
                tks = document.get(SentencesAnnotation.class).get(m.sentNum-1).get(TokensAnnotation.class);
                for(int i = m.startIndex-1; i < m.endIndex-1; i++)
                    clust2 += tks.get(i).get(TextAnnotation.class) + " ";
                clust2 = clust2.trim();
                //don't need the self mention
                if(clust.equals(clust2))
                    continue;
                ////System.out.println("\t" + clust2 + tks.get(m.startIndex-1).get(PartOfSpeechAnnotation.class));
                if (tks.get(m.startIndex-1).get(PartOfSpeechAnnotation.class).startsWith("P") /*|| clust2.toLowerCase().contains("the")*/) {
                	if (clust.contains("his ") || clust.contains("her ") || clust.contains("His ") || clust.contains("Her ") || clust.toLowerCase().equals("she") || clust.toLowerCase().equals("he")) {
                		////System.out.println("check!"+clust);
                		if (!coref.isEmpty()) {
                			coref.put(clust2, coref.entrySet().iterator().next().getValue());
                		}
                		continue;
                	}
                	if (clust.matches("\\d+\\.\\d*")||clust.matches(".*\\d.*"))
                		continue;
                	//System.err.println(clust+clust2);
                	if (clust.toLowerCase().contains("they") && clust2.toLowerCase().contains("their"))
                		continue;
                	if (clust.toLowerCase().contains("their") && clust2.toLowerCase().contains("they"))
                		continue;
                	if (clust.contains("'s")) {
                		String root = clust.replace("'s", "").trim();
                		//System.out.println(root+"|"+clust+"|"+clust2);
                		if (!clust2.equals("his") && !clust2.equals("theirs") && !clust2.equals("hers"))
                			coref.put(clust2, root);
                		else if (!clust.contains(clust2))
                			coref.put(clust2, clust);
                		continue;
                	}
                	if(!clust2.isEmpty())
                		coref.put(clust2, clust);
                }
            }
        }
	    /*for(CoreMap sentence: sentences) {
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    		String pos = token.get(PartOfSpeechAnnotation.class);
	    		if (pos.contains("CD"))
		    		numbers.add(token.originalText());
	    	}
	    }*/
	    
        Iterator<Entry<String, String>> it = coref.entrySet().iterator();
        while (it.hasNext()) {
        	Entry<String, String> pair = it.next();
        	if (pair.getKey().contains("his") || pair.getKey().contains("her"))
        		continue;
        	problem = problem.replace(" "+ pair.getKey()+" ", " "+pair.getValue()+" ");
        }
        return problem;
		
	}
	public static String substitute(String problem, StanfordCoreNLP pipeline) {
		String newProblem = new String(problem);
		if (newProblem.toLowerCase().contains(" double "))
			newProblem = newProblem.replace(" double ", " 2 times ");
		if (newProblem.toLowerCase().contains(" twice "))
			newProblem = newProblem.replace(" twice ", " 2 times ");
		if (newProblem.toLowerCase().contains(" thrice "))
			newProblem = newProblem.replace(" thrice ", " 3 times ");
		if (newProblem.toLowerCase().contains(" one half "))
			newProblem = newProblem.replace(" one half ", " 0.5 times ");
		if (newProblem.toLowerCase().contains(" half "))
			newProblem = newProblem.replace(" half ", " 0.5 times ");
		if (newProblem.toLowerCase().contains(" one-half "))
			newProblem = newProblem.replace(" one-half ", " 0.5 times ");
		if (newProblem.toLowerCase().contains("'s"))
			newProblem = newProblem.replace("'s", " ");
		return newProblem;
	}
	public static String convertNumberNames(String problem, StanfordCoreNLP pipeline) {
		String newProblem = new String(problem);
		Annotation document = new Annotation(problem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		ArrayList<String> names = new ArrayList<>();
    	ArrayList<String> numbers = new ArrayList<>();
	    for (CoreMap sentence : sentences) {
	    	List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
	    	String name = "";
	    	boolean isNum = false;
	    	for (CoreLabel token: tokens) {
	    		String pos = token.tag();
	    		if (pos.contains("CD")) {
	    			if (!isNum) {
	    				isNum = true;
	    				name = "";
	    			}
	    			name = name + token.originalText() + " ";
	    		}
	    		else if (isNum) {
	    			System.out.println(name);
	    			names.add(name);
	    			numbers.add(Word2Num.convert(name));
	    			isNum = false;
	    		}
	    	}
	    }
	    System.out.println(numbers);
	    System.out.println(names);
	    int counter = 0;
	    for (String name : names) {
	    	name = name.trim();
	    	newProblem = newProblem.replace(" "+name+" ", " "+numbers.get(counter)+" ");
	    	newProblem = newProblem.replace("."+name+" ", "."+numbers.get(counter)+" ");
	    	newProblem = newProblem.replace(" "+name+".", " "+numbers.get(counter)+".");
	    	newProblem = newProblem.replace("."+name+" ", ","+numbers.get(counter)+" ");
	    	newProblem = newProblem.replace(" "+name+",", " "+numbers.get(counter)+",");
	    	counter++;
	    }
	    System.out.println(newProblem);
	    return newProblem;
	}
	public static String convertInfix (String prefix) {
		String[] stack = new String[100];
		prefix = prefix.replaceAll(" ", "");
		int top = -1;
		while (!prefix.isEmpty()) {
			String s = "";
			while (prefix.charAt(0) != '(' && prefix.charAt(0) != ')' && prefix.charAt(0) != ',') {
				s = s + prefix.charAt(0);
				prefix = prefix.substring(1, prefix.length());
			}
			if (!s.isEmpty()) {
				top++;
				stack[top] = s;
			}
			if (prefix.charAt(0) == '(') {
				s = "(";
				prefix = prefix.substring(1, prefix.length());
				top++;
				stack[top] = s;
			}
			if (prefix.charAt(0) == ',') {
				prefix = prefix.substring(1,prefix.length());
				continue;
			}
			if (prefix.charAt(0) == ')') {
				String s2 = stack[top];
				top--;
				String s1 = stack[top];
				top--;
				top--;
				String op = stack[top];
				top--;
				top++;
				stack[top] = "["+s1+op+s2+"]";
				prefix = prefix.substring(1,prefix.length());
			}
		}
		return stack[top].replaceAll("\\[", "(").replaceAll("\\]", ")");
	}
	public static String solveWordProblems(String problem, StanfordCoreNLP pipeline) throws IOException, ScriptException, NumberFormatException, InterruptedException {
		//problem = coref(problem, pipeline);
		//problem = ConjunctionResolver.parse(problem, pipeline);
		//problem = substitute(problem, pipeline);
		
		String p = TrainRules.convertProblem(problem, "", pipeline);
		System.out.println(p);
		Annotation document = new Annotation(problem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    ArrayList<String> actors = TrainRules.actors;
	    System.out.println(actors);
	    for (CoreMap sentence : sentences) {
	    	List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
	    	for (CoreLabel token: tokens) {
	    		String pos = token.tag();
	    		WordNetInterface.seen = new ArrayList<>();
	    		if (pos.startsWith("W") || token.originalText().toLowerCase().contains("find") || token.originalText().toLowerCase().contains("calculate")) {
	    			boolean actorFlag = false;
	    			if (sentence.toString().toLowerCase().contains("how many")) {
	    				String p1;
	    				if (problem.contains("how"))
	    					p1 = TrainRules.convertProblem(sentence.toString().replace("how many", ""), "", pipeline);
	    				else
	    					p1 = TrainRules.convertProblem(sentence.toString().replace("How many", ""), "", pipeline);
	    				p1 = p1.replace(",0)",",k)");
	    				p = p1 + "\n" + p;
	    				
	    			} 
	    			else {
	    				actorFlag = true;
		    			int counter = 0; char base = 'x', varBase = 'A';
		    			ArrayList<String> temp = new ArrayList<String>();
		    			for (String actor : actors) {
		    				if (temp.contains(actor))
		    					continue;
		    				p = "holdsAt(age("+actor+","+(char)(base+counter)+"+"+(char)(varBase+counter)+"),"+(char)(varBase+counter)+").\n" + p;
		    				p = "holdsAt(age("+actor+","+(char)(base+counter)+"),0).\n" + p;
		    				temp.add(actor);
		    				counter++;
		    			}
	    			}
	    			FileWriter fw = new FileWriter(new File("problem.pl"));
	    			FileReader fr = new FileReader(new File("rules.pl"));
	    			BufferedReader br = new BufferedReader(fr);
	    			String rules = "";
	    			String s;
					while ((s = br.readLine()) != null)
	    				rules = rules + s + "\n";
					br.close();
					p = p + rules;
	    			BufferedWriter bw = new BufferedWriter(fw);
	    		    bw.write(p);
	    		    bw.close();
	    		    fw.close();
	    		    Query q1 = new Query("consult('problem.pl')");
	    		    System.out.println( "consult " + (q1.hasSolution() ? "succeeded" : "failed"));
	    		    //Query q4 = new Query(new Compound("equation", new Term[] {new Variable("X"), new Variable("Y")}));
	    		    Query q4 = new Query(new Compound("equation", new Term[] {new Variable("X"), new Variable("Y")}));
	    		    String equations = "";
	    		    ArrayList<String> candidates = new ArrayList<>();
	    	    	while (q4.hasMoreSolutions()) {
	    	    		String match1 = q4.nextSolution().get("X").toString();
	    	    		String match2 = q4.nextSolution().get("Y").toString();
	    	    		//System.out.println(match1+"="+match2);
	    	    		String equation = convertInfix(new String(match1))+"="+match2 +", ";
	    	    		//if (!equation.contains("+0"))
	    	    		equation = equation.replaceAll("\\+-", "-");
	    	    		equation = equation.replaceAll("\\+0", "");
	    	    		if (!candidates.contains(equation)) {
	    	    			candidates.add(equation);
	    	    			equations = equation + equations;
	    	    		}
	    	    	}
	    	    	if (!equations.isEmpty())
	    	    		equations = equations.substring(0,equations.length()-2);
	    	    	else {
	    	    		String p1 = "";
	    	    		p1 = TrainRules.convertProblem(sentence.toString().replace("What", "").replace("How many", ""), "", pipeline);
	    	    		if (p1.charAt(0) == '\n')
	    	    			p1 = p1.substring(1);
	    	    		System.out.println("a"+p1+"a");
	    	    		p1 = p1.split("\n")[0];
	    	    		p1 = p1.replace(",0)",",K)");
	    	    		System.out.println(p1);
	    	    		q1 = new Query("consult('problem.pl')");
		    		    System.out.println( "consult " + (q1.hasSolution() ? "succeeded" : "failed"));
		    		    
	    	    		q4 = new Query(p1);
	    	    		while (q4.hasMoreSolutions()) {
	    	    			System.out.println(q4.nextSolution());
	    	    		}
	    	    		return "";
	    	    	}
	    	    	System.out.println(equations);
	    	    	String ans = WolframTester.solveProblem(equations);
	    	    	Pattern numPattern = Pattern.compile("\\d+");
					Matcher numMatcher = numPattern.matcher(ans);
					String a = "";
					int counter = 0;
					if (actorFlag) {
						while (numMatcher.find()) {
							String val = numMatcher.group();
							System.out.println("Age of " + actors.get(counter) + " = " + val + " years");
							a = val + " ";
							counter++;
						}
					}
					else if (numMatcher.find()) {
						a = numMatcher.group();
						if (ans.contains("-"))
							System.out.print(a + " years ago");
						else
							System.out.print("In " + a + " years");
					}
					return a;
	    		}
	    	}
	    }	
		
		return "";
	}
	public static void main(String[] main) throws IOException, ScriptException, NumberFormatException, InterruptedException {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    //solveWordProblems("A boy is 6 years older than his brother. In 4 years, he will be 2 times as old as his brother. What are their present ages?", pipeline);
		//solveWordProblems("A father is 4 times as old as his son. In 20 years the father will be 2 times as old as his son. Find the present age of each.", pipeline);
		//solveWordProblems("Brandon is 9 years older than Ronda. In 4 years the sum of their ages will be 91. How old are they now?", pipeline);
		//solveWordProblems("Tim is 5 years older than JoAnn. 6 years from now the sum of their ages will be 79. How old are they now?",pipeline);
	    //solveWordProblems("The sum of Jason and Mandy ages is 35. Ten years ago Jason was twice as old as Mandy. How old are they now?", pipeline);
	    //solveWordProblems("Carmen is 12 years older than David. Five years ago the sum of their ages was 28. How old are they now?",pipeline);
	    //solveWordProblems("A father is 4 times as old as his son. In 20 years the father will be twice as old as his son. Find the present age of each.",pipeline);
	    //solveWordProblems("Pat is 20 years older than his son James. In two years Pat will be twice as old as James. How old are they now?", pipeline);
	    //solveWordProblems("Diane is 23 years older than her daughter Amy. In 6 years Diane will be twice as old as Amy. How old are they now?", pipeline);
	    //solveWordProblems("The sum of the ages of a father and son is 56. Four years ago the father was 3 times as old as the son. Find the present age of each.", pipeline);
	    ////solveWordProblems("The sum of the ages of a china plate and a glass plate is 16 years. Four years ago the china plate was three times the age of the glass plate. Find their present ages.", pipeline);
	    //solveWordProblems("Fred is 4 years older than Barney. Five years ago the sum of their ages was 48. How old are they now?", pipeline);
	    //solveWordProblems("John is four times as old as Martha. Five years ago the sum of their ages was 50. How old are they now?", pipeline);
	    //solveWordProblems("The sum of the ages of a china plate and a glass plate is 16 years. Four years ago the china plate was three times the age of the glass plate. Find the present age of each plate", pipeline);
	    //solveWordProblems("The sum of the ages of a wood plaque and a bronze plaque is 20 years. Four years ago, the bronze plaque was one-half the age of the wood plaque. Find the present age of each plaque.", pipeline);
	    //solveWordProblems("Angel is now 34 years old, and Betty is 4 years old. In how many years will Angel be twice as old as Betty?", pipeline);
	    //solveWordProblems("A log cabin quilt is 24 years old and a friendship quilt is 6 years old. In how many years will the log cabin quilt be three times as old as the friendship quilt?", pipeline);
	    ////solveWordProblems("The age of the older of two boys is twice that of the younger. 5 years ago it was three times that of the younger. Find the age of each.", pipeline);
	    ////solveWordProblems("A pitcher is 30 years old, and a vase is 22 years old. How many years ago was the pitcher twice as old as the vase?", pipeline);
	    solveWordProblems("Marge is twice as old as Consuelo. The sum of their ages seven years ago was 13. How old are they now?", pipeline);
	    //solveWordProblems("The sum of Jason and Mandy's age is 35. Ten years ago Jason was double Mandy's age. How old are they now?", pipeline);
	    ////solveWordProblems("A silver coin is 28 years older than a bronze coin. In 6 years, the silver coin will be twice as old as the bronze coin. Find the present age of each coin.", pipeline);
	    //solveWordProblems("A sofa is 12 years old and a table is 36 years old. In how many years will the table be twice as old as the sofa?", pipeline);
	    ////solveWordProblems("A limestone statue is 56 years older than a marble statue. In 12 years, the limestone will be three times as old as the marble statue. Find the present age of the statues.", pipeline);
	    //solveWordProblems("A pewter bowl is 8 years old, and a silver bowl is 22 years old. In how many years will the silver bowl be twice the age of the pewter bowl?", pipeline);
	    //solveWordProblems("A kerosene lamp is 95 years old, and an electric lamp is 55 years old. How many years ago was the kerosene lamp twice the age of the electric lamp?", pipeline);
	    //solveWordProblems("Ruth is 10 years old. She is 3 years younger than Sam. How old is Sam now?", pipeline);
	    ////solveWordProblems("In 56 years, Daniel will be 5 times as old as he is right now. How old is he right now?", pipeline);
	    ////solveWordProblems("Arman is 18. Diya is 2. How many years will it take for Arman to be 3 times as old as Diya?", pipeline);
	    ////solveWordProblems("In 40 years, Imran will be 11 times as old as he is right now. How old is he right now", pipeline);
	    //solveWordProblems("3 years from now Mary will be 52 years old. In 15 years, the sum of the ages of Mary and Cindy will be 95. How old is Cindy right now?", pipeline);
	    ////solveWordProblems("Aftab tells his daughter, \'Seven years ago, I was seven times as old as you were then. Also, three years from now, I shall be three times as old as you will be.\'", pipeline);
	    //solveWordProblems("5 years from now Sharon will be twice as old as Tiffany. The sum of the ages of Sharon and Tiffany is 86. How old is Tiffany right now?", pipeline);
	    //solveWordProblems("Ruth is 10 years old. Sam is 5 years older than Ruth. How old is Sam?", pipeline);
	    //solveWordProblems("Ruth is 10 years old. What is Ruth's age 5 years from now?", pipeline);
	}

	        
}
