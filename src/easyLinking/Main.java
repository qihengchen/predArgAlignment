package easyLinking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.stanford.nlp.dcoref.CorefChain;
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

public class Main {
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		
		MentionSpan m1 = new MentionSpan("a b c d", "1", "10", "P");
		MentionSpan m2 = new MentionSpan("c k l", "2", "20", "A");
		System.out.println(m1.equals(m2));	
		
		/*
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
	    Properties props = new Properties();
	    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    
	    @SuppressWarnings("resource")
	    String inputFile = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/2/2_9ecb.xml";
		//String text = new Scanner(new File(inputFile)).useDelimiter("//A").next();
		String text = "";
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new File(inputFile));
	    NodeList tokenList = doc.getElementsByTagName("token");
	    for (int i=0; i<tokenList.getLength(); i++) {
			Element token = (Element) tokenList.item(i);
			text += (token.getTextContent() + " ");	
		}
	    System.out.println(text);
	    System.out.println(text.split(" ").length);
		
	    // create an empty Annotation just with the given text
	    Annotation document = new Annotation(text);
	    
	    // run all Annotators on this text
	    pipeline.annotate(document);
	    
	    // these are all the sentences in this document
	    // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    
	    String res = "";
	    for(CoreMap sentence: sentences) {
	        // traversing the words in the current sentence
	        // a CoreLabel is a CoreMap with additional token-specific methods
	        for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    	    String word = token.get(LemmaAnnotation.class);
	    	    res += (word + " ");
	        }
	    }
	    System.out.println();
	    System.out.println(res);
	    System.out.println(res.split(" ").length);
	    
	    String[] old = text.split(" ");
	    String[] now = res.split(" ");
	    String[] propre = new String[old.length];
	    int j=0;
	    for (int i=0; i<old.length; i++) {
	    	String s = old[i];
	    	if (!old[i].equals(now[j])) {
	    		System.out.println("i= " + i);
	    		System.out.println("j= " + j);
	    		System.out.println("old  " + old[i]);
	    		System.out.println("now  " + now[j]);
	    		//System.out.println("propre last  " + propre[i-1]);
	    		System.out.println("***********");
	    	}
	    	if (s.startsWith("\"")) {
	    		if (s.length()==1) {
	    			propre[i] = old[i];
	    			j+=1;
	    		} else {
	    			if (s.endsWith("'s") || s.endsWith("'d") || s.endsWith("'m") || s.endsWith("s'")) {
	    				propre[i] = old[i];
	    				j+=3;
	    			} else {
	    				propre[i] = old[i];
	    				j+=2;
	    			}
	    		}
	    	} else if (s.endsWith("'s") || s.endsWith("'d") || s.endsWith("'m") || s.endsWith("s'")) {
	    		propre[i] = old[i];
	    		j+=2;
	    	} else if (now[j].equals("...")) {
	    		propre[i] = ".";
	    		propre[i+1] = ".";
	    		propre[i+2] = ".";
	    		i+=2;
	    		j+=1;
	    	} else {
	    		propre[i] = now[j] + " " + j;
	    		j+=1;
	    	}
	    }
	    for (String s : propre) {
	    	System.out.print(s + " ");
	    }
	    System.out.println();
	    
	    for (int i=0; i<text.split(" ").length; i++) {
	    	System.out.print(text.split(" ")[i] + " " + i + " ");
	    }
	    System.out.println();
	    System.out.println(propre.length);
		*/
	}
}
