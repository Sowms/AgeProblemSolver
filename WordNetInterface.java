import java.util.ArrayList;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;


public class WordNetInterface {

	static ArrayList<String> seen = new ArrayList<>();
	public static ArrayList<String> getParents(String current) throws InterruptedException {
		NounSynset nounSynset;
	    WordNetDatabase database = WordNetDatabase.getFileInstance();
	    Synset[] synsets = database.getSynsets(current, SynsetType.NOUN); 
	    ArrayList<String> parents = new ArrayList<String>();
        //for (Synset synset : synsets) {
        	nounSynset = (NounSynset)(synsets[0]);
        	for (NounSynset hypernym : nounSynset.getHypernyms()) {
        		for (String sense : hypernym.getWordForms()) {
        			if (!parents.contains(sense))
        				parents.add(sense);
        		}
        	}
        //}
        return parents;
	}
	public static boolean isActor(String word) throws InterruptedException {
		ArrayList<String> parents = new ArrayList<String>();
		//System.out.println(word);
		//Thread.sleep(1000);
	    parents = getParents(word);
	    for (String candidate : parents) {
	    	if (candidate.equals("entity"))
	    		return false;
	    	else if (candidate.equals("person")) {
	    		//System.out.println(candidate+"|"+word);
	    		return true;
	    	}
	    }
	    for (String candidate : parents) {
	    	//System.out.println("c"+counter+candidate);
	    	if (!seen.contains(candidate)) {
	    		seen.add(candidate);
	    			if (isActor(candidate))
	    				return true;
	    	} 
	    }
	    return false; 
	}
	public static void main(String[] args) throws InterruptedException {
		System.out.println(isActor("brother"));
		seen = new ArrayList<>();
		System.out.println(isActor("boy"));
		seen = new ArrayList<>();
		System.out.println(isActor("times"));
		seen = new ArrayList<>();
		System.out.println(isActor("sum"));
	}
}
