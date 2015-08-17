package easyLinking;

//TODO: find least common words, remove trivial links, aka linked because of ./the/a...

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
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

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public class Linking {
	
	// {note: {id:mentionspan}}
	private static HashMap<String, HashMap<String, MentionSpan>> _coref = 
			new HashMap<String, HashMap<String, MentionSpan>>();
	private static HashMap<String, MentionSpan> _annotation = new HashMap<String, MentionSpan>();
	// {doc:[[text, m_id/""]...]}
	private static HashMap<String, ArrayList<String[]>> _cluster = new HashMap<String, ArrayList<String[]>>();
	private static StanfordCoreNLP _pipeline;
	private static HashSet<String> _G = new HashSet<String>();
	private static HashSet<String> _H = new HashSet<String>();
	private static boolean _hasChain = false;
	
	public static void main(String[] args) throws Exception {
		
		//readCorpus("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ToyCorpus/");
		//LocalChain lc = new LocalChain(_cluster.get("1_1ecbplus.xml"));
		//LocalChain lc = new LocalChain("He likes the dog . He is Sam . He roams on the streets where nobody stands .");
		//writeLinkedPercentage("/Users/Qiheng/Desktop/Summer 2015/linkedPercentage2.txt");
		/*
		init();
		int i = 1;
		while (i==1) {
			readCorpus("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/" + i);
			linkMentions();
			eval();
			writeGH("/Users/Qiheng/Desktop/Summer 2015/experiment/G1-noChain.txt", 
					"/Users/Qiheng/Desktop/Summer 2015/experiment/H1-noChain.txt");
			//analyzeResults("/Users/Qiheng/Desktop/Summer 2015/experiment/analysis2.txt");
			_G.clear();
			_H.clear();
			_coref.clear();
			_cluster.clear();
			i++;
		}*/
		System.out.println(tokenize("I'm *UNK* when he's . . ."));
	}
	

	// read tokens and annotations from XML files, as per cluster
	public static void readCorpus(String dirPath) throws ParserConfigurationException, SAXException, IOException {
		File folder = new File(dirPath + "/");
		for (File file : folder.listFiles()) {
			System.out.println(file.getName());
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
			}
			String[] textArray = text.trim().split(" ");
			
			// 
			ArrayList<String[]> tempIds = new ArrayList<String[]>();
			
			// find all annotations -- "pred" or "arg"
			NodeList annotations = doc.getElementsByTagName("Markables").item(0).getChildNodes();
			for (int i=1; i<annotations.getLength(); i=i+2) {
				// if nodes without TAG_DESCRIPTOR
				Node node = annotations.item(i);
				if (node.getChildNodes().getLength() > 1) {
					// String content = "";
					String id = "";
					String m_id = file.getName() + "/" + node.getAttributes().item(0).getTextContent();
					NodeList anchors = node.getChildNodes();
					for (int j=1; j<anchors.getLength(); j=j+2) {
						Integer t_id = Integer.parseInt(anchors.item(j).getAttributes().item(0).getTextContent());
						// content += (textArray[t_id-1] + " ");
						id += (t_id + " ");
						// id += (file.getName() + "/" + t_id + " ");
					}
					if (node.getNodeName().contains("ACTION") || node.getNodeName().contains("NEG")) {
						//String anchor = node.getChildNodes().item(1).getAttributes().item(0).getTextContent();
						_annotation.put(m_id, new MentionSpan(m_id, "pred"));
						tempIds.add(new String[] {id.trim(), m_id});
						//System.out.format("check tempIds addition -- id: %s  m_id: %s%n", id.trim(), m_id);
					} else {
						_annotation.put(m_id, new MentionSpan(m_id, "arg"));
						tempIds.add(new String[] {id.trim(), m_id});
					}
				}
			}
			
			Collections.sort(tempIds, new IdComparator());
			
			// List[[segment of tokens, m_id/""]...]
			ArrayList<String[]> temp = new ArrayList<String[]>();
			int m=0;
			for (String[] tempId : tempIds) {
				int min = Integer.parseInt(tempId[0].split(" ")[0]), 
					max = Integer.parseInt(tempId[0].split(" ")[tempId[0].split(" ").length-1]);
				if (min-1-m > 0) {
					temp.add(new String[] {Joiner.on(" ").join(Arrays.copyOfRange(textArray,m,min-1)), ""});
				}
				temp.add(new String[] {Joiner.on(" ").join(Arrays.copyOfRange(textArray,min-1,max)), tempId[1]});
				m = max;
			}
			if (m<textArray.length) {
				temp.add(new String[] {Joiner.on(" ").join(Arrays.copyOfRange(textArray,m,textArray.length)), ""});
			}
			/*
			for (String[] s : temp) {
				System.out.println(s[0]);
				System.out.println(s[1]);
				System.out.println("------------------");
			}*/
			
			// tokenize, update mentionSpan
			for (String[] segment : temp) {
				segment[0] = tokenize(segment[0]);
				if (!segment[1].equals("")) {
					_annotation.get(segment[1]).setContent(segment[0]);
				}
			}
			
			/*for (MentionSpan ms : _annotation.values()) {
				if (ms.getAttribute().equals("pred")) {
					System.out.format("pred -- %s   id:  %s%n", ms.getContent(), ms.getMId());
				}
			}*/
			
			_cluster.put(file.getName(), temp);
			
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
	
	private static String tokenize(String text) {
		if (_pipeline == null) {
			init();
		}
		Annotation document = new Annotation(text);
	    _pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    String res = "";
	    for(CoreMap sentence: sentences) {
	        for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	            String word = token.get(TextAnnotation.class);
	            res += (word + " ");
	        }
	    }
	    return res.trim();
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
		HashMap<String, LocalChain> chains = new HashMap<String, LocalChain>();
		for (String docName : _cluster.keySet()) {
			System.out.println(docName);
			
			String text = "";
			for (String[] s : _cluster.get(docName)) {
				text += (s[0] + " ");
			}
			text = text.trim();
			
			
			String[] lemmas = lemmatize(text).split(" ");
			if (text.split(" ").length != lemmas.length) {
				System.err.format("before: %d   after: %d%n", text.split(" ").length, lemmas.length);
				System.exit(0);
			}
		    // set mention content to lemmatized tokens, _cluster unchanged
			int index = 0;
		    for (String[] segment : _cluster.get(docName)) {
		    	if (!segment[1].equals("")) {
		    		_annotation.get(segment[1]).setContent(Joiner.on(" ").
		    				join(Arrays.copyOfRange(lemmas,index,index+segment[0].split(" ").length)));
		    	}
		    	index += segment[0].split(" ").length;
		    }
		    
		    
		    if (_hasChain) {
		    	chains.put(docName, new LocalChain(_cluster.get(docName)));
		    }
		 	
		    
		}
		
		//TODO: align
		// align if content overlaps or in the same local chain
		System.out.println(_annotation.size() + "  annotation size !! ");
		for (MentionSpan mention : _annotation.values()) {
			if (mention.getMId().contains("_1ecb")) { // && mention.getAttribute().equals("pred")) {
				
				for (MentionSpan ms : _annotation.values()) {
					if (!ms.getMId().contains("_1ecb") && mention.getAttribute().equals(ms.getAttribute())) {
						// align if content overlaps
						if (mention.equals(ms)) {
							_H.add(mention.getMId() + " -> " + ms.getMId());
							
							if (_hasChain) {
								// align if in the same local chain
								String docName  = ms.getMId().substring(0, ms.getMId().indexOf("/"));
								System.out.println(docName);
								String reprm = chains.get(docName).isChained(ms.getMId());
								System.out.println(reprm);
								
								if (!reprm.equals("")) {
									System.out.println("reprm is annotated");
									for (String chainedMId : chains.get(docName).getLocalChain(reprm)) {
										_H.add(mention.getMId() + " -> " + chainedMId);
									}
								} else {
									System.out.println();
								}
								System.out.println("----------");
							}
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
				
				if (m_id.contains("_1ecb")) { // && mentions.get(m_id).getAttribute().equals("pred")) {
					for (String m_id2 : mentions.keySet()) {
						if (!m_id2.contains("_1ecb")){ // && mentions.get(m_id2).getAttribute().equals("pred")) {
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
			
			for (ArrayList<String[]> doc : _cluster.values()) {
				String text = "";
				for (String[] s : doc) {
					text += (s[0] + " ");
				}
				text = text.trim();
				totalT += text.split(" ").length;
			}
			for (MentionSpan ms : _annotation.values()) {
				if (ms.getMId().contains(clusterNum + "_")) {
					totalM += 1;
					annotatedT += ms.getMId().split(" ").length;
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
