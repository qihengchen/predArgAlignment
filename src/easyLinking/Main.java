package easyLinking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;

public class Main {
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
	    Properties props = new Properties();
	    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    //String text = "Sam is a man. He's awesome. He'd like to eat. His head is red. His friends do not like him.";
	    String text = "I'm.";
	    Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    
	    for(CoreMap sentence: sentences) {
	        for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	            String word = token.get(TextAnnotation.class);
	            System.out.println(word);
	            System.out.println("-----------");
	        }
	        System.out.println("xxxxxxxx");
	    }
	    
	    
	    /*
	    Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
	    System.out.println(graph.entrySet().size() + "  is the size");
	    for(Map.Entry<Integer, CorefChain> entry : graph.entrySet()) {
	    	CorefChain c = entry.getValue();
	    	CorefMention cm = c.getRepresentativeMention();
	    	System.out.println(cm.mentionSpan + " repr mSpan");
	    	System.out.println(cm.startIndex + " repr startIndex");
	    	for (Entry<IntPair, Set<CorefMention>> cms : c.getMentionMap().entrySet()) {
	    		for (CorefMention m : cms.getValue()) {
	    			System.out.println(m.mentionSpan);
	    			
	    		}
	    		
	    	}
	    	System.out.println("________");
	    }*/
	}
}
