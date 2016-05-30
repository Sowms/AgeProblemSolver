
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
                if (tks.get(m.startIndex-1).get(PartOfSpeechAnnotation.class).startsWith("P") || clust2.toLowerCase().contains("the")) {
                	if (clust.contains("his ") || clust.contains("her ") || clust.contains("His ") || clust.contains("Her ") || clust.toLowerCase().equals("she") || clust.toLowerCase().equals("he")) {
                		////System.out.println("check!"+clust);
                		if (!coref.isEmpty()) {
                			coref.put(clust2, coref.entrySet().iterator().next().getValue());
                		}
                		continue;
                	}
                	if (clust.matches("\\d+\\.\\d*")||clust.matches(".*\\d.*"))
                		continue;
                	////////System.err.println(clust+clust2);
                	if (clust.toLowerCase().contains("they") && clust2.toLowerCase().contains("their"))
                		continue;
                	if (clust.toLowerCase().contains("their") && clust2.toLowerCase().contains("they"))
                		continue;
                	if (clust.contains("'s")) {
                		String root = clust.replace("'s", "").trim();
                		////System.out.println(root+"|"+clust+"|"+clust2);
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
	/*public static String convertInfix (String prefix) {
		String[] stack = new String[100];
		int top = -1;
		String expr = ""; int pos = 0;
		while (!prefix.isEmpty()) {
			String s = "";
			while (prefix.charAt(0) != ) {
				
			}
			prefix = prefix.substring(1,prefix.length());
			if (c == ',')
				continue;
			if (c == ')') {
				char pop = ' ';
				while (pop != '(')
				
			}
		}
		return expr;
	}*/
	public static String solveWordProblems(String problem, StanfordCoreNLP pipeline) throws IOException, ScriptException {
		String corefProblem = coref(problem,pipeline);
		// change number names to numbers
		String conjFreeProblem = ConjunctionResolver.parse(corefProblem, pipeline);
		System.out.println(conjFreeProblem);
		String p = TrainRules.convertProblem(conjFreeProblem, "", pipeline);
		Annotation document = new Annotation(conjFreeProblem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    ArrayList<String> actors = new ArrayList<>();
	    for (CoreMap sentence : sentences) {
	    	List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
	    	for (CoreLabel token: tokens) {
	    		String pos = token.tag();
	    		if (pos.contains("NN") && !token.originalText().contains("years")) {
	    			if (!actors.contains(token.originalText().toLowerCase()))
	    				actors.add(token.originalText().toLowerCase());
	    		}
	    		if (pos.startsWith("W") || token.originalText().contains("find") || token.originalText().contains("calculate")) {
	    			if (sentence.toString().contains("now") || sentence.toString().contains("present")) {
	    				int counter = 0; char base = 'x', varBase = 'X';
	    				for (String actor : actors) {
	    					p = "holdsAt(age("+actor+","+(char)(base+counter)+"+"+(char)(varBase+counter)+"),"+(char)(varBase+counter)+").\n" + p;
	    					p = "holdsAt(age("+actor+","+(char)(base+counter)+"),0).\n" + p;
	    					counter++;
	    				}
	    				FileWriter fw = new FileWriter(new File("problem.pl"));
	    				FileReader fr = new FileReader(new File("rules.pl"));
	    				BufferedReader br = new BufferedReader(fr);
	    				String rules = "";
	    				String s;
						while ((s = br.readLine()) != null)
	    					rules = rules + s + "\n";
						br.close();
	    				BufferedWriter bw = new BufferedWriter(fw);
	    		    	bw.write(p);
	    		    	bw.write(rules);
	    		    	bw.close();
	    		    	Query q1 = new Query("consult('problem.pl')");
	    		    	System.out.println( "consult " + (q1.hasSolution() ? "succeeded" : "failed"));
	    		    	Query q4 = new Query(new Compound("equation", new Term[] {new Variable("A"), new Variable("B")}));
	    	    		while (q4.hasMoreSolutions()) {
	    	    			String match1 = q4.nextSolution().get("A").toString();
	    	    			String match2 = q4.nextSolution().get("B").toString();
	    	    			System.out.println(match1+"="+match2);
	    	    		}
	    			}
	    		}
	    		
	    	}
	    }	
		pipeline.annotate(document);
		
		return conjFreeProblem;
	}
	public static void main(String[] main) throws IOException, ScriptException {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		solveWordProblems("A boy is 6 years older than his brother. In 4 years, he will be 2 as old as his brother. What are their present ages?", pipeline);
	}

	        
}
