package easyLinking;

//TODO: build in-document reference chain
//TODO: try on old ecb.xml data
//TODO: compute F-score for P, A, and overall.
//TODO: find least common words, remove trivial links, aka linked because of ./the/a...
//TODO: unanchored entity disambiguation

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import com.google.common.collect.Sets;

public class Linking {
	
	// {note: {id:mentionspan}}
	private static HashMap<String, HashMap<String, MentionSpan>> _coref = 
			new HashMap<String, HashMap<String, MentionSpan>>();
	private static HashMap<String, MentionSpan> _annotation = new HashMap<String, MentionSpan>();
	private static HashMap<String, String> _cluster = new HashMap<String, String>();
	private static StanfordCoreNLP _pipeline;
	private static HashSet<String> _G = new HashSet<String>();
	private static HashSet<String> _H = new HashSet<String>();
	private static boolean _doLemmatize = false;
	
	public static void main(String[] args) throws Exception {
		_doLemmatize = true;
		readCorpus("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/" + 1);
		LocalChain lc = new LocalChain(_cluster.get("1_2ecbplus.xml"));
		//writeLinkedPercentage("/Users/Qiheng/Desktop/Summer 2015/linkedPercentage2.txt");
		/*
		int i = 1;
		while (i==1) {
			readCorpus("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/" + i);
			linkMentions();
			eval();
			writeGH("/Users/Qiheng/Desktop/Summer 2015/experiment/GL1P.txt", 
					"/Users/Qiheng/Desktop/Summer 2015/experiment/HL1P.txt");
			//analyzeResults("/Users/Qiheng/Desktop/Summer 2015/experiment/analysis2.txt");
			_G.clear();
			_H.clear();
			_coref.clear();
			_cluster.clear();
			i++;
		}*/
	}
	

	// read tokens and annotations from XML files, as per cluster
	public static void readCorpus(String dirPath) throws ParserConfigurationException, SAXException, IOException {
		File folder = new File(dirPath + "/");
		for (File file : folder.listFiles()) {
			
			//TODO: get it work on ecb.xml data
			if (file.getName().endsWith("ecb.xml")) {
				continue;
			}
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			String text = "";
			NodeList tokenList = doc.getElementsByTagName("token");
			
			// get text tokens
			for (int i=0; i<tokenList.getLength(); i++) {
				Element token = (Element) tokenList.item(i);
				text += (token.getTextContent() + " ");
				//String[] temp = {token.getTextContent(), ""};
				//fileTokens.put(token.getAttribute("t_id"), temp);
			}
			_cluster.put(file.getName(), text.trim());
			
			// find all annotations -- "pred" or "arg"
			NodeList annotations = doc.getElementsByTagName("Markables").item(0).getChildNodes();
			for (int i=1; i<annotations.getLength(); i=i+2) {
				// if nodes without TAG_DESCRIPTOR
				Node node = annotations.item(i);
				if (node.getChildNodes().getLength() > 1) {
					String content = "";
					String id = "";
					String m_id = file.getName() + "/" + node.getAttributes().item(0).getTextContent();
					NodeList anchors = node.getChildNodes();
					for (int j=1; j<anchors.getLength(); j=j+2) {
						Integer t_id = Integer.parseInt(anchors.item(j).getAttributes().item(0).getTextContent());
						content += (_cluster.get(file.getName()).split(" ")[t_id-1] + " ");
						id += (file.getName() + "/" + t_id + " ");
					}
					if (node.getNodeName().contains("ACTION") || node.getNodeName().contains("NEG")) {
						//String anchor = node.getChildNodes().item(1).getAttributes().item(0).getTextContent();
						_annotation.put(m_id, new MentionSpan(content, id, m_id, "pred"));
					} else {
						_annotation.put(m_id, new MentionSpan(content, id, m_id, "arg"));
					}
				}
			}
			
			// get cross_doc_coref
			NodeList CDCs = doc.getElementsByTagName("CROSS_DOC_COREF");
			for (int i=0; i<CDCs.getLength(); i++) {
				String note = CDCs.item(i).getAttributes().item(0).getNodeValue();
				NodeList refs = CDCs.item(i).getChildNodes();
				for (int j=1; j<refs.getLength(); j=j+2) {
					// ref = source/target
					Node ref = refs.item(j);
					String m_id = file.getName() + "/" + ref.getAttributes().item(0).getNodeValue();
					if (ref.getNodeName().equals("source")) {
						if (_coref.containsKey(note)) {
							_coref.get(note).put(m_id, _annotation.get(m_id));
						} else {
							_coref.put(note, new HashMap<String, MentionSpan>());
							_coref.get(note).put(m_id, _annotation.get(m_id));
						}
					}
				}
			}
		}
	}
	

	private static void init() {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		_pipeline = new StanfordCoreNLP(props);
	}
	
	// return lemmatized string of text
	private static String lemmatize(String text) {
		if (_pipeline == null) {
			init();
		}
		Annotation document = new Annotation(text);
		_pipeline.annotate(document);
		String lemmatizedText = "";
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String word = token.get(LemmaAnnotation.class);
				lemmatizedText += (word + " ");
			}
		}
		return lemmatizedText;
	}
	
	// lemmatizeFile inputFile, function similar to lemmatize which acts on string
	public static void lemmatizeFile(String inputFile, String outputFile) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(outputFile);
		if (_pipeline==null) {
			init();
		}
		@SuppressWarnings("resource")
		String text = new Scanner(new File(inputFile)).useDelimiter("//A").next();
		Annotation document = new Annotation(text);
		_pipeline.annotate(document);
		// all sentences in this document
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String word = token.get(LemmaAnnotation.class);
				writer.print(word + " ");
			}
		}
		writer.close();
	}
	
	
	private static void linkMentions() {
		for (String docName : _cluster.keySet()) {
			System.out.println(docName);
			String text = _cluster.get(docName);
			
			String[] old = text.split(" ");           // tokens
			String[] res = new String[old.length];    // results
			String[] now = text.split(" ");           // lemmatized tokens
			
			if (_doLemmatize == true) {
				now = lemmatize(text).split(" ");
			}
			
			// deal with inconsistency caused by lemmatizer. ecb.xml excluded
		    int j=0;
		    for (int i=0; i<old.length; i++) {
		    	String s = old[i];
		    	if (s.startsWith("\"")) {
		    		if (s.length()==1) {
		    			res[i] = old[i];
		    			j+=1;
		    		} else {
		    			if (s.endsWith("'s") || s.endsWith("'d") || s.endsWith("'m") || s.endsWith("s'")) {
		    				res[i] = old[i];
		    				j+=3;
		    			} else {
		    				res[i] = old[i];
		    				j+=2;
		    			}
		    		}
		    	} else if (s.endsWith("'s") || s.endsWith("'d") || s.endsWith("'m") || s.endsWith("s'")) {
		    		res[i] = old[i];
		    		j+=2;
		    	} else if (now[j].equals("...")) {
		    		res[i] = ".";
		    		res[i+1] = ".";
		    		res[i+2] = ".";
		    		i+=2;
		    		j+=1;
		    	} else {
		    		res[i] = now[j];
		    		j+=1;
		    	}
		    }
		    
		    // set mentions in _annotation to lemmatized tokens
		    for (MentionSpan ms : _annotation.values()) {
		    	if (ms.getId().contains(docName)) {
		    		String[] ids = ms.getId().split(" ");
		    		String newContent = "";
		    		for (String id : ids) {
		    			newContent += (res[Integer.parseInt(id.substring(id.indexOf("/")+1, id.length()))-1] + " ");
		    		}
		    		ms.setContent(newContent.trim());
		    	}
		    }
		}
		
		//TODO: alignment!
		// align if content overlaps
		System.out.println(_annotation.size() + "  anno size !! ");
		for (MentionSpan mention : _annotation.values()) {
			if (mention.getId().contains("_1ecb") && mention.getAttribute().equals("pred")) {
				for (MentionSpan ms : _annotation.values()) {
					if (!ms.getId().contains("_1ecb") && mention.getAttribute().equals(ms.getAttribute())) {
						if (mention.equals(ms)) {
							_H.add(mention.getMId() + " -> " + ms.getMId());
						}
					}
				}
			}
		}
	}
	

	public static void eval() {
		// ground truth -- [doc/m_id -> doc/m_id]
		for (HashMap<String, MentionSpan> mentions : _coref.values()) {   // [token, doc]
			for (String m_id : mentions.keySet()) {
				if (m_id.contains("_1ecb") && mentions.get(m_id).getAttribute().equals("pred")) {
					for (String m_id2 : mentions.keySet()) {
						if (!m_id2.contains("_1ecb") && mentions.get(m_id2).getAttribute().equals("pred")) {
							_G.add(m_id + " -> " + m_id2);
						}
					}
				}
			}
		}
		
		int intersection = Sets.intersection(_G, _H).size();
		System.out.println(intersection);
		double precision = (double) intersection / _H.size();
		double recall = (double) intersection / _G.size();
		double F1 = 2*precision*recall / (precision+recall);
		System.out.format("precision: %f   recall: %f   F1: %f%n", precision, recall, F1);
	}
	

	// analyse results for "host"
	private static void analyzeResults(String output) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(output);
		writer.println("overlap:");
		for (String g : _G) {
			if (g.contains("2_1ecbplus.xml/36") && _H.contains(g)) {
				writer.println(g);
			}
		}
		writer.println("overlap:");
		writer.println("ground truth:");
		for (String g : _G) {
			if (g.contains("2_1ecbplus.xml/36")) {
				writer.println(g);
			}
		}
		writer.println("overlap:");
		writer.println("identified:");
		for (String h : _H) {
			if (h.contains("2_1ecbplus.xml/36")) {
				writer.println(h);
			}
		}
		writer.close();
	}
	
	private static void writeGH(String GFile, String HFile) throws FileNotFoundException {
		PrintWriter writerG = new PrintWriter(GFile);
		PrintWriter writerH = new PrintWriter(HFile);
		for (String g : _G) {
			writerG.print(g);
			writerG.println("  " + _annotation.get(g.split(" ")[0]).getContent() + " -> " 
			+ _annotation.get(g.split(" ")[2]).getContent());
		}
		for (String h : _H) {
			writerH.print(h);
			writerH.println("  " + _annotation.get(h.split(" ")[0]).getContent() + " -> " 
					+ _annotation.get(h.split(" ")[2]).getContent());
		}
		writerG.close();
		writerH.close();
	}
	
	private static void writeLinkedPercentage(String output) throws ParserConfigurationException, 
	SAXException, IOException {
		PrintWriter writer = new PrintWriter(output);
		int clusterNum = 1;
		while (clusterNum <=45) {
			int annotatedT = 0, totalT = 0;
			int alignedM = 0, totalM = 0;
			
			readCorpus("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/" + clusterNum);

			for (String text : _cluster.values()) {
				totalT += text.split(" ").length;
			}
			for (MentionSpan ms : _annotation.values()) {
				if (ms.getId().contains(clusterNum + "_")) {
					totalM += 1;
					annotatedT += ms.getId().split(" ").length;
				}
			}
			HashSet<String> temp = new HashSet<String>();
			for (HashMap<String, MentionSpan> alignment : _coref.values()) {
				for (String m_id : alignment.keySet()) {
					if (m_id.contains(clusterNum + "_")) {
						temp.add(m_id);
					}
				}
			}
			alignedM = temp.size();
			
			writer.format("cluster %2d: %-5d annotated      %-5d tokens       %f%n", clusterNum, annotatedT, totalT,
					(double) annotatedT/totalT);
			writer.format("            %-5d aligned        %-5d mentions     %f%n%n", alignedM, totalM, 
					(double) alignedM/totalM);
			
			_cluster.clear();
			_annotation.clear();
			_coref.clear();
			
			clusterNum += 1;
			if (clusterNum == 15 || clusterNum == 17) {
				clusterNum += 1;
			}
		}
		writer.close();
	}
	
	public static StanfordCoreNLP getPipeline() {
		if (_pipeline == null) {
			init();
		}
		return _pipeline;
	}
	
	public static HashMap<String, MentionSpan> getAnnotation() {
		return _annotation;
	}
}
