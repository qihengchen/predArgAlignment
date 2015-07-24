package easyLinking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;

public class LocalChain {
	
	// {reprWordMId : (chainedWordsMIds)}
	private HashMap<String, HashSet<String>> _chain = new HashMap<String, HashSet<String>>();
	
	public LocalChain(ArrayList<String[]> doc) {
		String text = "";
		for (String[] segment : doc) {
			text += (segment[0] + " ");
		}
		Annotation document = new Annotation(text.trim());
	    Linking.getPipeline().annotate(document);
	    
	    HashMap<Integer, String> sents = new HashMap<Integer, String>();
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    for(int index=1; index<=sentences.size(); index++) {
	    	String s = sentences.get(index-1).toString();
	    	sents.put(index, s);
	    	//System.out.println(s);
	    }
	    
	    HashMap<int[], HashSet<int[]>> tempChain = new HashMap<int[], HashSet<int[]>>();
	    
	    Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
	    System.out.println(graph.entrySet().size() + "  is the size");
	    for(Map.Entry<Integer, CorefChain> entry : graph.entrySet()) {
	    	CorefChain c = entry.getValue();
	    	CorefMention reprm = c.getRepresentativeMention();
	    	//System.out.println(reprm.mentionSpan + "  " + reprm.startIndex + "-" + reprm.endIndex + "  REPR");
	    	int[] repr = new int[] {reprm.sentNum, reprm.startIndex, reprm.endIndex};
	    	
	    	HashSet<int[]> temp = new HashSet<int[]>();
	    	
	    	for (Entry<IntPair, Set<CorefMention>> cms : c.getMentionMap().entrySet()) {
	    		for (CorefMention cm : cms.getValue()) {
	    			//System.out.println(cm.mentionSpan + "  " + cm.startIndex + "-" + cm.endIndex + "  sent: " + cm.sentNum);
	    			temp.add(new int[] {cm.sentNum, cm.startIndex, cm.endIndex});
	    		}
	    	}
	    	tempChain.put(repr, temp);
	    	//System.out.println("________");
	    }

	    HashMap<int[], HashSet<int[]>> reducedChain = new HashMap<int[], HashSet<int[]>>();
	    for (int[] key : tempChain.keySet()) {
	    	if (tempChain.get(key).size() > 1) {
	    		reducedChain.put(key, tempChain.get(key));
	    	}
	    }
	    System.out.println(reducedChain.size() + "   is the reduced size");
	    
	    _chain = indexToMId(doc, sents, reducedChain);
	    
	}
	
	// this constructor is for testing
	public LocalChain(String text) {
		Annotation document = new Annotation(text);
	    Linking.getPipeline().annotate(document);
	    
	    HashMap<Integer, String> sents = new HashMap<Integer, String>();
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    for(int index=1; index<=sentences.size(); index++) {
	    	String s = sentences.get(index-1).toString();
	    	sents.put(index, s);
	    	System.out.println(s);
	    }
	    
	    HashMap<int[], HashSet<int[]>> tempChain = new HashMap<int[], HashSet<int[]>>();
	    
	    Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
	    System.out.println(graph.entrySet().size() + "  is the size");
	    for(Map.Entry<Integer, CorefChain> entry : graph.entrySet()) {
	    	CorefChain c = entry.getValue();
	    	CorefMention reprm = c.getRepresentativeMention();
	    	System.out.println(reprm.mentionSpan + "  " + reprm.startIndex + "-" + reprm.endIndex + "  REPR");
	    	int[] repr = new int[] {reprm.sentNum, reprm.startIndex, reprm.endIndex};
	    	
	    	HashSet<int[]> temp = new HashSet<int[]>();
	    	
	    	for (Entry<IntPair, Set<CorefMention>> cms : c.getMentionMap().entrySet()) {
	    		for (CorefMention cm : cms.getValue()) {
	    			System.out.println(cm.mentionSpan + "  " + cm.startIndex + "-" + cm.endIndex + "  " + cm.sentNum);
	    			temp.add(new int[] {cm.sentNum, cm.startIndex, cm.endIndex});
	    		}
	    	}
	    	tempChain.put(repr, temp);
	    	System.out.println("________");
	    }

	    HashMap<int[], HashSet<int[]>> reducedChain = new HashMap<int[], HashSet<int[]>>();
	    for (int[] key : tempChain.keySet()) {
	    	if (tempChain.get(key).size() > 1) {
	    		reducedChain.put(key, tempChain.get(key));
	    	}
	    }
	    System.out.println(reducedChain.size());
	}
	
	// return the m_id; "" if not a mention
	private HashMap<String, HashSet<String>> indexToMId(ArrayList<String[]> doc, HashMap<Integer, String> sents, 
			HashMap<int[], HashSet<int[]>> reducedChain) {
		HashMap<String, HashSet<String>> res = new HashMap<String, HashSet<String>>();
		
		HashSet<String> temp = new HashSet<String>();
		
		for (int[] reprm : reducedChain.keySet()) {
			for (int[] a : reducedChain.get(reprm)) {
				int[] stringIndex = toStringIndex(sents, a[0], a[1], a[2]);
				//System.out.format("sent: %d   start: %d   end: %d%n", a[0], a[1], a[2]);
				String m_id = toMId(doc, stringIndex);
				System.out.println(m_id);
				// omit if not annotated
				if (!m_id.equals("")) {
					temp.add(m_id);
				}
			}
			if (!temp.isEmpty()) {
				String headOfChain = toMId(doc, toStringIndex(sents, reprm[0], reprm[1], reprm[2]));
				if (headOfChain.equals("")) {
					//System.out.println("head of chain is empty");
				}
				res.put(headOfChain, temp);
			}
		}

		return res;
	}
	
	// sentNum, startIndex, endIndex all index from 1; endIndex exclusive
	private int[] toStringIndex(HashMap<Integer, String> sents, int sentNum, int startIndex, int endIndex) {
		int i=1, index=0;
		while(i<sentNum) {
			index += sents.get(i).split(" ").length;
			System.out.println(index);
			i++;
		}
		System.out.format("sent: %d   start: %d   end: %d  ->  %d, %d%n", sentNum, startIndex, endIndex,
				index+startIndex-1, index+endIndex-1);
		return new int[] {index+startIndex-1, index+endIndex-1};
	}
	
	// indexed from 0
	private String toMId(ArrayList<String[]> doc, int[] stringIndex) {
		int startIndex = stringIndex[0], endIndex = stringIndex[1];
		int index = 0;
		for (String[] segment : doc) {
			index += segment[0].split(" ").length;
			if (index >= endIndex) {
				return segment[1];
			}
			/*if (index > startIndex) {
				if (index >= endIndex) {
					System.out.println("segment[1]:  " + segment[1]);
					return segment[1];
				} else {
					System.out.println(segment[0]);
					System.out.println("index is: " + index);
					System.out.println("chained entity crosses mention span");
					return "";
				}
			}*/
		}
		System.err.println("ERROR: this line should never be reached");
		System.out.println("index= " + index + " startindex= " + startIndex + " endindex= " + endIndex);
		System.exit(0);
		return "";
	}
	
	// return the key of the chain that m_id is in
	public String isChained(String m_id) {
		for (String reprMention : _chain.keySet()) {
			if (_chain.get(reprMention).contains(m_id)) {
				return reprMention;
			}
		}
		return "";
	}
	
	public HashSet<String> getLocalChain(String reprMention) {
		return _chain.get(reprMention);
	}
}
