

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class WolframTester {
	public static boolean checkAns(String sysAns, String ans) {
		if (sysAns.contains(ans))
			return true;
		Pattern wordPattern = Pattern.compile("\\d+\\.\\d+|\\d+");
		Matcher matcher = wordPattern.matcher(sysAns);
		if(matcher.find()) {
			double num1 = Math.round(Double.parseDouble(matcher.group()));
			double num2 = Math.round(Double.parseDouble(ans));
			return (num1 == num2);
		}
		return false;
	}
	static String appid = "XKXW7Q-RUTH97KAHA";
    
	public static String solveProblem(String question) {
		String input = question;
		WAEngine engine = new WAEngine();
		engine.setAppID(appid);
		engine.addFormat("plaintext");
		WAQuery query = engine.createQuery();
		query.setInput(input);
		try {
			WAQueryResult queryResult = engine.performQuery(query);
			if (queryResult.isError()) {
				return "";
			} else if (!queryResult.isSuccess()) {
				System.out.println("Query was not understood; no results available.");
				return "";
			} else {
				System.out.println("Successful query. Pods follow:\n");
				for (WAPod pod : queryResult.getPods()) {
					if (!pod.isError() && pod.getTitle().equals("Solution")) {
						System.out.println(pod.getTitle());
						System.out.println("------------");
						for (WASubpod subpod : pod.getSubpods()) {
							for (Object element : subpod.getContents()) {
								if (element instanceof WAPlainText) {
									String ans = ((WAPlainText) element).getText();
									System.out.println(ans);
									return ans;
								}
							}	
						}
						
					}
				}
			}
		}
		catch (WAException e) {
			e.printStackTrace();
		}
		return "";
	}
	public static String convert(String name) {
		String ans = null;
		WAEngine engine = new WAEngine();
		engine.setAppID(appid);
		engine.addFormat("plaintext");
		WAQuery query = engine.createQuery();
		query.setInput(name);
		try {
			WAQueryResult queryResult = engine.performQuery(query);
			if (queryResult.isError()) {
				System.out.println("Query error");
				System.out.println(" error code: " + queryResult.getErrorCode());
				System.out.println(" error message: " + queryResult.getErrorMessage());
			} else if (!queryResult.isSuccess()) {
				System.out.println("Query was not understood; no results available.");
			} else {
				System.out.println("Successful query. Pods follow:\n");
				for (WAPod pod : queryResult.getPods()) {
					if (!pod.isError() && pod.getTitle().equals("Input")) {
						WASubpod subpod = pod.getSubpods()[0];
						for (Object element : subpod.getContents()) {
							if (element instanceof WAPlainText) {
								ans = ((WAPlainText) element).getText();
								System.out.println(ans);
							}
						}
						break;
					}
				}
			}
		}
		catch (WAException e) {
			e.printStackTrace();
		}
		return ans;
	}
	/*public static void main(String[] args) {
		BufferedReader br1 = null, br2 = null;
		int count = 0, total = 0;
	    
		
		try {
 			String sCurrentLine;
 			br1 = new BufferedReader(new FileReader("q3.txt"));
 			br2 = new BufferedReader(new FileReader("ans3.txt"));
 			while ((sCurrentLine = br1.readLine()) != null) {
 				String ques = sCurrentLine, ans = br2.readLine();
				if (checkProblem(ques,ans))
					count++;
				total++;
			}
 			System.out.println(count+"|"+total);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br1 != null)
					br1.close();
				if (br2 != null)
					br2.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}*/
}
