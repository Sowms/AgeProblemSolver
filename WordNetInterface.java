import java.util.ArrayList;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;


public class WordNetInterface {

	static ArrayList<String> seen = new ArrayList<>();
	
	private static ILexicalDatabase db = new NictWordNet();
	
	private static double compute(String word1, String word2) {
		WS4JConfiguration.getInstance().setMFS(true);
		double s = new WuPalmer(db).calcRelatednessOfWords(word1, word2);
		return s;
	}
	/*public static ArrayList<String> getParents(String current) throws InterruptedException {
		NounSynset nounSynset;
	    WordNetDatabase database = WordNetDatabase.getFileInstance();
	    Synset[] synsets = database.getSynsets(current, SynsetType.NOUN); 
	    ArrayList<String> parents = new ArrayList<String>();
        for (Synset synset : synsets) {
	    	//if (synsets.length == 0)
	    		//return parents;
        	nounSynset = (NounSynset)(synset);
        	for (NounSynset hypernym : nounSynset.getHypernyms()) {
        		for (String sense : hypernym.getWordForms()) {
        			if (!parents.contains(sense)) {
        				System.out.println(sense + "|" + current);
        				parents.add(sense);
        			}
        		}
        	}
        }
        return parents;
	}*/
	public static boolean isActor(String word) throws InterruptedException {
		//System.out.println(word);
		//Thread.sleep(1000);
	    //parents = getParents(word);
		double val1 = compute(word,"person");
		double val2 = compute(word,"article");
		//System.out.println(val1+"|"+val2);
		if (val1 >= 0.6 || val2 >= 0.6)
			return true;
	    return false; 
	}
	public static void main(String[] args) throws InterruptedException {
		System.out.println(isActor("sum"));
		seen = new ArrayList<>();
		System.out.println(isActor("plate"));
		seen = new ArrayList<>();
		System.out.println(isActor("boy"));
		seen = new ArrayList<>();
		System.out.println(isActor("china"));
		seen = new ArrayList<>();
		System.out.println(isActor("glass"));
	}
}
