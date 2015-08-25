package easyLinking;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

public class WordNet {
	
	private IDictionary _dict;
	
	public static void main(String[] args) throws IOException {
		String path = "/Users/Qiheng/Desktop/Summer 2015/WordNet-3.0/dict";
		URL url = new URL("file", null, path);
		IDictionary dict = new Dictionary(url);
		dict.open();
		
		// must feed in lemma + wn's pos tagging
		IIndexWord idxWord = dict.getIndexWord("purchase", POS.NOUN);
		IWordID wordID = idxWord.getWordIDs().get(0);  // 1st meaning
		IWord word = dict.getWord(wordID);
		System.out.println("Id = " + wordID);
		System.out.println("Lemma = " + word.getLemma());
		System.out.println("Gloss = " + word.getSynset().getGloss());
		
		ISynset synset = word.getSynset();
		for (IWord w : synset.getWords()) {
			System.out.println(w.getLemma());
		}
	}
	
	public WordNet() throws IOException {
		String path = "/Users/Qiheng/Desktop/Summer 2015/WordNet-3.0/dict";
		URL url = new URL("file", null, path);
		_dict = new Dictionary(url);
		_dict.open();
	}
	
	public String getMeaning(String token, POS pos) {
		IIndexWord idxWord = _dict.getIndexWord(token, pos);
		List<IWordID> wordIDs = idxWord.getWordIDs();  // 1st meaning
		if (wordIDs == null) {return "NOT FOUND";}
		IWord word = _dict.getWord(wordIDs.get(0));
		return word.getSynset().getGloss().toString();
	}
	
	public List<String> getSynonyms(String token, POS pos) {
		List<String> res = new ArrayList<String>();
		IIndexWord idxWord = _dict.getIndexWord(token, pos);
		if (idxWord == null) {return res;}
		List<IWordID> wordIDs = idxWord.getWordIDs();  // 1st meaning
		IWord word = _dict.getWord(wordIDs.get(0));
		ISynset synset = word.getSynset();
		for (IWord w : synset.getWords()) {
			res.add(w.getLemma());
		}
		return res;
	}

}









