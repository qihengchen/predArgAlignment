package easyLinking;

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
	
	// {instance_id:([token, doc],...)}
	private static HashMap<String, HashSet<String[]>> _coref = new HashMap<String, HashSet<String[]>>();
	// {doc:{t_id:[token, feature],...}}   feature: markables node name. ACTION...
	private static HashMap<String, HashMap<String, String[]>> _cluster = 
			new HashMap<String, HashMap<String, String[]>>();
	private static StanfordCoreNLP _pipeline;
	private static HashSet<String> _G = new HashSet<String>();
	private static HashSet<String> _H = new HashSet<String>();
	
	public static void main(String[] args) throws Exception {
		writeLinkedPercentage("/Users/Qiheng/Desktop/Summer 2015/linkedPercentage.txt");
		/*
		int i = 1;
		while (i<15) {
			readCorpus("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/", Integer.toString(i));
			eval();
			_G.clear();
			_H.clear();
			_coref.clear();
			_cluster.clear();
			i++;
		}*/
	}
	
	private static void writeLinkedPercentage(String output) throws ParserConfigurationException, 
	SAXException, IOException {
		PrintWriter writer = new PrintWriter(output);
		int clusterNum = 1;
		while (clusterNum <=45) {
			readCorpus("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/", Integer.toString(clusterNum));
			int count = 0;
			for (HashSet<String[]> hs : _coref.values()) {
				for (String[] s : hs) {
					if (s[1].contains(clusterNum + "_1ecb.xml") || s[1].contains(clusterNum + "_1ecbplus.xml")) {
						count += 1;
						break;
					}
				}
			}
			writer.format("cluster %d:     %d     %d       %f%n", clusterNum, count, _coref.keySet().size(),
					(double) count/_coref.keySet().size());
			_coref.clear();
			_cluster.clear();
			clusterNum += 1;
			if (clusterNum == 15 || clusterNum == 17) {
				clusterNum += 1;
			}
		}
		writer.close();
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
	
	@SuppressWarnings("resource")
	// read dependencies from XML files, per cluster
	public static void readCorpus(String dirPath, String clusterNum) throws ParserConfigurationException, 
	SAXException, IOException {
		File folder = new File(dirPath + clusterNum + "/");
		for (File file : folder.listFiles()) {
			//System.out.println(file.getName());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			HashMap<String, String[]> fileTokens = new HashMap<String, String[]>();
			NodeList tokenList = doc.getElementsByTagName("token");
			// get token text
			for (int i=0; i<tokenList.getLength(); i++) {
				Element token = (Element) tokenList.item(i);
				String[] temp = {token.getTextContent(), ""};
				fileTokens.put(token.getAttribute("t_id"), temp);
			}
			// annotate Pred/Arg/""
			NodeList annotations = doc.getElementsByTagName("Markables").item(0).getChildNodes();
			for (int i=1; i<annotations.getLength(); i=i+2) {
				// if not nodes with TAG_DESCRIPTOR
				Node node = annotations.item(i);
				if (node.getChildNodes().getLength() > 1) {
					String name = node.getNodeName();
					if (name.contains("ACTION") || name.contains("NEG")) {
						// anchor = t_id of annotated token
						String anchor = node.getChildNodes().item(1).getAttributes().item(0).getTextContent();
						fileTokens.get(anchor)[1] = "pred";
					} else {
						NodeList anchors = node.getChildNodes();
						for (int j=1; j<anchors.getLength(); j=j+2) {
							String anchor = anchors.item(j).getAttributes().item(0).getTextContent();
							fileTokens.get(anchor)[1] = "arg";
						}
					}
				}
				//System.out.println(annotations.item(i).getNodeName());
			}
			_cluster.put(file.getName(), fileTokens);
			
			// get coref
			NodeList CDCs = doc.getElementsByTagName("CROSS_DOC_COREF");
			for (int i=0; i<CDCs.getLength(); i++) {
				HashSet<String[]> instances = new HashSet<String[]>();
				NodeList refs = CDCs.item(i).getChildNodes();
				for (int j=1; j<refs.getLength(); j=j+2) {
					// ref = source/target
					Node ref = refs.item(j);
					String m_id = ref.getAttributes().item(0).getNodeValue();
					if (ref.getNodeName().equals("source")) {
						NodeList elements = doc.getElementsByTagName("Markables").item(0).getChildNodes();
						for (int k=1; k<elements.getLength(); k=k+2) {
							Element element = (Element) elements.item(k);
							if (element.getAttribute("m_id").equals(m_id)) {
								// find token anchor(s)
								String phrase = "", id = "";
								for (int l=1; l<element.getChildNodes().getLength(); l=l+2) {
									Element anchor = (Element) element.getChildNodes().item(l);
									String t_id = anchor.getAttribute("t_id");
									phrase += (_cluster.get(file.getName()).get(t_id)[0] + " ");
									id += (file.getName() + "/" + t_id + " ");
								}
								phrase.trim();
								id.trim();
								String[] temp = {phrase, id};
								instances.add(temp);
							}
						}
					} /*else {
						// find tag_descriptor
						NodeList elements = doc.getElementsByTagName("Markables").item(0).getChildNodes();
						for (int k=1; k<elements.getLength(); k=k+2) {
							Element element = (Element) elements.item(k);
							//System.out.print(element.getAttribute("m_id") + " " + m_id + " ");
							if (element.getAttribute("m_id").equals(m_id)) {
								_coref.put(element.getAttribute("TAG_DESCRIPTOR"), tags);
							}
						}
						//System.out.println();
					}*/
				}
				String note = CDCs.item(i).getAttributes().item(0).getNodeValue();
				if (_coref.containsKey(note)) {
					HashSet<String[]> temp = _coref.get(note);
					temp.addAll(instances);
					_coref.put(note, temp);
				} else {
					_coref.put(note, instances);
				}
			}
		}
	}
	
	public static void eval() {
		   // "file/t_id -> file/t_id"
		for (HashSet<String[]> hs : _coref.values()) {   // [token, doc]
			for (String[] mention : hs) {
				if (mention[1].contains("_1ecb.xml") || mention[1].contains("_1ecbplus.xml")) {
					for (String[] m : hs) {
						if (!m[1].contains("_1ecb.xml") && !m[1].contains("_1ecbplus.xml")) {
							_G.add(mention[1] + "-> " + m[1].trim());
						}
					}
				}
			}
		}
		for (String docName : _cluster.keySet()) {
			String text = "";
			HashMap<String, String[]> doc = _cluster.get(docName);
			for (int i=1; i<=doc.keySet().size(); i++) {
				text += (doc.get(Integer.toString(i))[0] + " ");
			}
			//text = lemmatize(text);
			String[] lemmatizedText = text.split(" ");
			int j= 0;  // . . . lemmatized as ... shrinking the size of returned list
			for (int i=1; i<=doc.keySet().size(); i++) {
				doc.get(Integer.toString(i))[0] = lemmatizedText[j];
				j += 1;
				if (lemmatizedText[j-1].equals("...")) {
					i += 2;
				}
			}
		}
		// _cluster already lemmatized
		// TODO: for annotated words, find x_1...xml -> x_y...xml
		for (String docName : _cluster.keySet()) {
			if (docName.contains("_1ecb")) {
				//System.out.println(docName);
				for (String t_id : _cluster.get(docName).keySet()) {
					String[] wordfeature = _cluster.get(docName).get(t_id);
					if (!wordfeature[1].equals("")) {
						//System.out.println(wordfeature[0] + "  is annotated");
						for (String dn : _cluster.keySet()) {
							if (!dn.contains("_1ecb")) {
								for (String id : _cluster.get(dn).keySet()) {
									String[] wf = _cluster.get(dn).get(id);
									if (!wf[1].equals("") && wf[0].equals(wordfeature[0])) {
										//System.out.println(wf[0] + "  matched");
										_H.add(docName + "/" + t_id + " -> " + dn + "/" + id);
									}
								}
							}
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
}
