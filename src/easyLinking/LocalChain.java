package easyLinking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.IntPair;

public class LocalChain {
	
	// {reprWordMId : (chainedWordsMIds)}
	private HashMap<String, HashSet<String>> _chain = new HashMap<String, HashSet<String>>();
	
	public LocalChain(String text) {
		Annotation document = new Annotation(text);
	    Linking.getPipeline().annotate(document);
	    
	    Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
	    System.out.println(graph.entrySet().size() + "  is the size");
	    for(Map.Entry<Integer, CorefChain> entry : graph.entrySet()) {
	    	CorefChain c = entry.getValue();
	    	//CorefMention cm = c.getRepresentativeMention();
	    	//System.out.println(cm.mentionSpan);
	    	//System.out.println(cm.startIndex);
	    	
	    	//TODO: figure out mention id from text
	    	
	    	HashSet<String> temp = new HashSet<String>();
	    	
	    	for (Entry<IntPair, Set<CorefMention>> cms : c.getMentionMap().entrySet()) {
	    		for (CorefMention m : cms.getValue()) {
	    			System.out.println(m.mentionSpan);
	    			
	    		}
	    		
	    	}
	    	
	    	System.out.println("________");
	    }
	    //TODO: kick out one-entry chain
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
