package easyLinking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.*;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;


public class Linking {
	
	// {tag:([token, doc],...)}
	private static HashMap<String, HashSet<String[]>> _coref = new HashMap<String, HashSet<String[]>>();
	// {doc:{t_id:[token, feature],...}}   feature: markables node name. ACTION...
	private static HashMap<String, HashMap<String, String[]>> _cluster = 
			new HashMap<String, HashMap<String, String[]>>();
	private static StanfordCoreNLP _pipeline;
	
	public static void main(String[] args) throws Exception {
		readCorpus("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/");
		eval();
	}

	private static void init() {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		_pipeline = new StanfordCoreNLP(props);
	}
	
	// return lemmatized string of text
	private static String lemmatize(String text) {
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
			/*Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props); */
			
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
	public static void readCorpus(String dirPath) throws ParserConfigurationException, SAXException, IOException {
		String clusterNum = "1";  // may be generalized as parameter
		File folder = new File(dirPath + clusterNum + "/");
		for (File file : folder.listFiles()) {
			System.out.println(file.getName());
			
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
				HashSet<String[]> tags = new HashSet<String[]>();
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
									phrase += (_cluster.get(file.getName()).get(t_id) + " ");
									id += (file.getName() + "/" + t_id + " ");
								}
								phrase.trim();
								id.trim();
								String[] temp = {phrase, id};
								tags.add(temp);
							}
						}
					} else {
						// find tag_descriptor
						
						NodeList elements = doc.getElementsByTagName("Markables").item(0).getChildNodes();
						for (int k=1; k<elements.getLength(); k=k+2) {
							Element element = (Element) elements.item(k);
							System.out.print(element.getAttribute("m_id") + " " + m_id + " ");
							if (element.getAttribute("m_id").equals(m_id)) {
								
								_coref.put(element.getAttribute("TAG_DESCRIPTOR"), tags);
							}
						}
						System.out.println();
					}
				}
			}
		}
	}
	
	public static void eval() {
		// evaluate F-Score on stored data
		ArrayList<String> G = new ArrayList<String>();   // "file/t_id -> file/t_id"
		for (String key : _coref.keySet()) {
			System.out.println();
			System.out.println(key);
			for (String[] s : _coref.get(key)) {
				System.out.println(s[1]);
			}
		}
		// TODO: coref println results seem not correct
		
		ArrayList<String> H = new ArrayList<String>();
		// TODO: call lemmatize()
		
	}
}
