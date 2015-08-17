package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;

import easyLinking.MentionSpan;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.OriginalTextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.MorphaAnnotator;
import edu.stanford.nlp.pipeline.POSTaggerAnnotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.pipeline.TokenizerAnnotator;
import edu.stanford.nlp.pipeline.WordsToSentencesAnnotator;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.TypesafeMap.Key;

public class cleanseData {
	
	// {note: {id:mentionspan}}
	private static HashMap<String, HashMap<String, MentionSpan>> _coref = 
					new HashMap<String, HashMap<String, MentionSpan>>();
	// {doc:{m_id:mentionSpan}...}
	private static HashMap<String, HashMap<String, MentionSpan>> _annotation = 
			new HashMap<String, HashMap<String, MentionSpan>>();
	// {doc:[[text, m_id/""]...]}
	private static HashMap<String, ArrayList<String>> _cluster = new HashMap<String, ArrayList<String>>();
	private static StanfordCoreNLP _pipeline;
	private static HashSet<String> _errors = new HashSet<String>();
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		//manuallyClean();
		String s = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/2_10ecbplus.xml";
		readCorpus(new File(s));
		rewriteCorpus(s, "2_10ecbplus.xml");
		addMId(s, s, "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/2/2_10ecbplus.xml");
		
		/*
		String path = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/MyCorpus/";
		File folder = new File(path);
		
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {return !name.equals(".DS_Store");}
		};

		for (File cluster : folder.listFiles(filter)) {
			System.out.println(cluster.getName());
			if (cluster.isDirectory() && !cluster.getName().equals("tempFolder")) {
				for (File subCluster : cluster.listFiles(filter)) {
					System.out.println(subCluster.getName());
					for (File file : subCluster.listFiles(filter)) {
						if (file.getName().equals("cross_doc_coref.xml")) {
							String source = file.getPath();
							Scanner scanner = new Scanner(new File(source));
							String text = scanner.useDelimiter("\\Z").next();
							scanner.close();
							text = text.replace("-lrb-", "(")
									.replace("-rrb-", ")")
									.replace("-lsb-", "[")
									.replace("-rsb-", "]")
									.replace("�", "*UNK*")
									.replace("&", "*AND*")
									.replace("</document>", "\n</document>")
									.replace("instance_id:", "instance_id=");
							String heading = String.format("<document doc_id=\"%s\">%n", source.split("/")[source.split("/").length-1]);
							if (!text.startsWith(heading)) {
								text = heading + text;
							}
							if (!text.endsWith("</document>")) {
								text += "\n</document>";
							}
							PrintWriter w1 = new PrintWriter(source);
							w1.println(text);
							w1.close();
						} else {
							System.out.println(file.getName());
							String source = file.getPath();
							String origin = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/"+cluster.getName()+"/";
							if (file.getName().contains("Lemmas")) {
								origin += (file.getName().substring(0, file.getName().length()-10)+".xml");
							} else {
								origin += file.getName();
							}
							addMId(source, source, origin);
						}
					}
				}
			}
		}*/
		
		/*
		String source = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/MyCorpus/4_1ecbplus.xml";
		String dest = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/MyCorpus/4_1ecbplusNew.xml";
		String origin = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/4/4_1ecbplus.xml";
		addMId(source, source, origin);
		/*DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new File(path));
		Node textNode = doc.getElementsByTagName("text").item(0);
		//System.out.println(textNode.getNodeName());
		//System.out.println(textNode.getTextContent());
		// list of all mentions
		NodeList mts = doc.getElementsByTagName("mentions");
		System.out.println(mts.item(0));
		System.out.println(mts.item(1));
		// #text parent, child1, #text child1, ...
		NodeList spans = mts.item(0).getChildNodes();
		Node span = spans.item(0);
		System.out.println(span.getNodeName());
		Node span2 = spans.item(1);
		System.out.println(span2.getNodeName());
		System.out.println(span2.getAttributes().item(0));
		System.out.println(span2.getAttributes().item(1));
		System.out.println(spans.getLength());
		System.out.println(spans.item(2).getNodeName());
		// after casting, able to get attribute by attribute name
		Element span2Ele = (Element) span2;
		System.out.println(span2Ele.getAttribute("m_id"));
		
		/*init();
		String filename = "14_5ecb.xml";
		File file = new File("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/14/" + filename);
		readCorpus(file);
		String text = Joiner.on(" ").join(_cluster.get(filename));
		for (int i=0; i<text.split(" ").length; i++) {
			System.out.print(text.split(" ")[i] + " " + i + "  ");
		}
		System.out.println();
		//System.out.println("text length = " + text.split(" ").length);
		String l = lemmatize(text);
		for (int i=0; i<l.split(" ").length; i++) {
			System.out.print(l.split(" ")[i] + " " + i + "  ");
		}
		System.out.println();
		//System.out.println("lemmas length = " + l.split(" ").length);
		
		init();
		File dir = new File("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/MyCorpus");
		if (!dir.exists()) {dir.mkdir();}
		
		for (int i=2; i<=30; i++) {
			if (i==15 || i==17) {
				continue;
			}
			mainWrapper(i);
		}
		printErrorFiles("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/MyCorpus/ErrorsLog3.txt");*/
	}
	
	private static void addMId(String source, String dest, String origin) throws SAXException, IOException, ParserConfigurationException {
		Scanner scanner = new Scanner(new File(source));
		String text = scanner.useDelimiter("\\Z").next();
		scanner.close();
		text = text.replace("-lrb-", "(")
				.replace("-rrb-", ")")
				.replace("-lsb-", "[")
				.replace("-rsb-", "]")
				.replace("�", "*UNK*")
				.replace("&", "*AND*");
		String heading = String.format("<document doc_id=\"%s\">%n", source.split("/")[source.split("/").length-1]);
		if (!text.startsWith(heading)) {
			text = heading + text;
		}
		if (!text.endsWith("</document>")) {
			text += "</document>";
		}
		PrintWriter w1 = new PrintWriter(source);
		w1.println(text);
		w1.close();
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new File(source));
		Document originalDoc = db.parse(new File(origin));
		NodeList originalMentions = originalDoc
				.getElementsByTagName("Markables")
				.item(0)
				.getChildNodes();
		
		PrintWriter w2 = new PrintWriter(dest);
		w2.print(text.substring(0, text.indexOf("<mentions>")));
		w2.println("<mentions>");
		NodeList spans = doc.getElementsByTagName("span");
		for (int i=0; i<spans.getLength(); i++) {
			Element span = (Element) spans.item(i);
			String m_id = span.getAttribute("m_id");
			String id = span.getAttribute("id");
			String attr = "";
			for (int j=1; j<originalMentions.getLength(); j+=2) {
				Element node = (Element) originalMentions.item(j);
				if (node.getChildNodes().getLength() > 1 && 
						node.getAttribute("m_id").equals(m_id.split("/")[1])) {
					attr = node.getNodeName();
				}
			}
			w2.format("  <span m_id=\"%s\" id=\"%s\" attr=\"%s\">%s</span>%n", m_id, id, attr, span.getTextContent());
		}
		w2.println("</mentions>");
		w2.println("</document>");
		w2.close();
	}
	
	private static void manuallyClean() throws IOException, ParserConfigurationException, SAXException {
		File errors = new File("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/MyCorpus/ErrorsLog.txt");
		BufferedReader r = new BufferedReader(new FileReader(errors));
		String sourcePath = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/MyCorpus/tempFolder/";
		String origPath = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/";
		String destPath = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/MyCorpus/";
		String line = "";
		while ((line=r.readLine()) != null) {
			System.out.println(line);
			readCorpus(new File(origPath + line.split("_")[0] + "/" + line));
			@SuppressWarnings("resource")
			String lemma = new Scanner(new File(sourcePath+line.substring(0, line.length()-4)+"Lemmas.xml"))
								.useDelimiter("\\Z").next();
			String[] lemmas = lemma.split(" ");
			int index = 0;
			ArrayList<String> temp = new ArrayList<String>();
			for (String segment : _cluster.get(line)) {
				int len = segment.split(" ").length;
				if (len==1) {
					temp.add(lemmas[index]);
				} else {
					temp.add(Joiner.on(" ").join(Arrays.copyOfRange(lemmas, index, index+len)));
				}
				index += len;
			}
			if (index != lemmas.length) {
				System.err.println("ERROR: lemmas was not read till the end");
				System.exit(0);
			}
			_cluster.put(line, temp);
			
			// update mentions
			for (MentionSpan ms : _annotation.get(line).values()) {
				if (!ms.getTId().contains(" ")) {
					ms.setContent(temp.get(Integer.parseInt(ms.getTId())-1));
				} else {
					String[] ids = ms.getTId().split(" ");
					ms.setContent(Joiner.on(" ").join(temp.subList(Integer.parseInt(ids[0])-1, 
							Integer.parseInt(ids[ids.length-1])-1)));
				}
			}
			//destPath/clusterNum/ecb/filename
			String path = destPath+line.split("_")[0]+"/";
			if (line.contains("plus")) {
				path += ("ecb/" + line.substring(0, line.length()-4)+"Lemmas.xml");
			} else {
				path += ("ecbplus/" + line.substring(0, line.length()-4)+"Lemmas.xml");
			}
			rewriteCorpus(path, line);
		}
		r.close();
		
		
		/*BufferedReader r2 = new BufferedReader(new FileReader(errors));
		while ((line=r2.readLine()) != null) {
			//System.out.println(line);
			tempLemmatizeWrapper(path + line.substring(0, line.length()-4) + "Lemmas.xml", line);
		}
		r2.close();*/
	}
	
	private static void tempLemmatizeWrapper(String outputpath, String filename) throws FileNotFoundException {
		System.out.println(filename + "    in LWrapper");
		// doc text
		String text = Joiner.on(" ").join(_cluster.get(filename));
		// lemma array
		String[] lemmas = lemmatize(text).split(" ");
		PrintWriter w = new PrintWriter(outputpath);
		for (String s : lemmas) {
			w.print(s + " ");
		}
		w.println();
		w.println("text -- " + text.split(" ").length);
		w.println("lemmas -- " + lemmas.length);
		w.close();
	}
	
	private static void mainWrapper(int cluster) throws ParserConfigurationException, SAXException, IOException {
		String path = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/MyCorpus/" + cluster;
		File clusterDir = new File(path);
		if (!clusterDir.exists()) {clusterDir.mkdir();}
		
		File subDir1 = new File(path + "/ecb");
		if (!subDir1.exists()) {subDir1.mkdir();}
		File subDir2 = new File(path + "/ecbplus");
		if (!subDir2.exists()) {subDir2.mkdir();}
		
		File sourceFolder = new File("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/" + cluster);
		for (File file : sourceFolder.listFiles()) {
			if (file.getName().endsWith("ecb.xml")) {
				readCorpus(file);
				rewriteCorpus(subDir1.getPath() + "/" + file.getName(), file.getName());
			}
		}
		rewriteCoref(subDir1.getPath() + "/" + "cross_doc_coref.xml");
		lemmatizeWrapper();
		for (File file : sourceFolder.listFiles()) {
			if (file.getName().endsWith("ecb.xml")) {
				rewriteCorpus(subDir1.getPath() + "/" + file.getName().substring(0, file.getName().length()-4) 
						+ "Lemmas.xml", file.getName());
			}
		}
		_cluster.clear();
		_annotation.clear();
		_coref.clear();
		
		for (File file : sourceFolder.listFiles()) {
			if (!file.getName().endsWith("ecb.xml")) {
				readCorpus(file);
				rewriteCorpus(subDir2.getPath() + "/" + file.getName(), file.getName());
			}
		}
		rewriteCoref(subDir2.getPath() + "/" + "cross_doc_coref.xml");
		lemmatizeWrapper();
		for (File file : sourceFolder.listFiles()) {
			if (!file.getName().endsWith("ecb.xml")) {
				
				rewriteCorpus(subDir2.getPath() + "/" + file.getName().substring(0, file.getName().length()-4) 
						+ "Lemmas.xml", file.getName());
			}
		}
		_cluster.clear();
		_annotation.clear();
		_coref.clear();
	}
	
	// read tokens and annotations from XML files, as per cluster
	private static void readCorpus(File file) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		
		//String text = "";
		NodeList tokenList = doc.getElementsByTagName("token");
		ArrayList<String> docTokens = new ArrayList<String>();
		// get text tokens
		for (int i=0; i<tokenList.getLength(); i++) {
			Element token = (Element) tokenList.item(i);
			docTokens.add(tokenize(token.getTextContent()));
			//text += (token.getTextContent() + " ");
		}
		_cluster.put(file.getName(), docTokens);
		
		
		// find all annotations -- "pred" or "arg"
		HashMap<String, MentionSpan> annotationsInDoc = new HashMap<String, MentionSpan>();
		NodeList annotations = doc.getElementsByTagName("Markables").item(0).getChildNodes();
		for (int i=1; i<annotations.getLength(); i=i+2) {
			// if nodes without TAG_DESCRIPTOR
			Node node = annotations.item(i);
			if (node.getChildNodes().getLength() > 1) {
				String t_ids = "", content = "";
				String m_id = file.getName() + "/" + node.getAttributes().item(0).getTextContent();
				NodeList anchors = node.getChildNodes();
				for (int j=1; j<anchors.getLength(); j=j+2) {
					Integer t_id = Integer.parseInt(anchors.item(j).getAttributes().item(0).getTextContent());
					t_ids += (t_id + " ");
					content += (docTokens.get(t_id-1) + " ");
				}
				if (node.getNodeName().contains("ACTION") || node.getNodeName().contains("NEG")) {
					annotationsInDoc.put(m_id, new MentionSpan(m_id, t_ids.trim(), content.trim(), "pred"));
				} else {
					annotationsInDoc.put(m_id, new MentionSpan(m_id, t_ids.trim(), content.trim(), "arg"));
				}
			}
		}
		_annotation.put(file.getName(), annotationsInDoc);
		
		// find coref
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
						_coref.get(note).put(m_id, _annotation.get(m_id.split("/")[0]).get(m_id));
					} else {
						_coref.put(note, new HashMap<String, MentionSpan>());
						_coref.get(note).put(m_id, _annotation.get(m_id.split("/")[0]).get(m_id));
					}
				}
			}
		}
	}
	
	private static String tokenize(String text) throws FileNotFoundException {
		String temp = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/MyCorpus/temp.txt";
		PrintWriter w = new PrintWriter(temp);
		w.print(text);
		w.close();
		String config = "ptb3Ellipsis=true,normalizeParentheses=false,normalizeOtherBrackets=false,untokenizable=allKeep";
		PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(new FileReader(temp),
	              new CoreLabelTokenFactory(), config);
		String res = "";
	    while (ptbt.hasNext()) {
	        res += (ptbt.next() + " ");
	    }
	    //System.out.println(res);
	    return res.trim();
	}
	
	//deprecated
	private static String simpleTokenize(String text) {
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
	
	private static void lemmatizeWrapper() throws FileNotFoundException {
		for (String filename : _cluster.keySet()) {
			System.out.println(filename + "    in LWrapper");
			String text = Joiner.on(" ").join(_cluster.get(filename));
			
			String[] lemmas = lemmatize(text).split(" ");
			if (text.split(" ").length != lemmas.length) {
				_errors.add(filename);
				continue;
			}
			
			// update _cluster
			int index = 0;
			ArrayList<String> temp = new ArrayList<String>();
			for (String segment : _cluster.get(filename)) {
				int len = segment.split(" ").length;
				if (len==1) {
					temp.add(lemmas[index]);
				} else {
					temp.add(Joiner.on(" ").join(Arrays.copyOfRange(lemmas, index, index+len)));
				}
				index += len;
			}
			if (index != lemmas.length) {
				System.err.println("ERROR: lemmas was not read till the end");
				System.exit(0);
			}
			_cluster.put(filename, temp);
			
			// update mentions
			for (MentionSpan ms : _annotation.get(filename).values()) {
				if (!ms.getTId().contains(" ")) {
					ms.setContent(temp.get(Integer.parseInt(ms.getTId())-1));
				} else {
					String[] ids = ms.getTId().split(" ");
					ms.setContent(Joiner.on(" ").join(temp.subList(Integer.parseInt(ids[0])-1, 
							Integer.parseInt(ids[ids.length-1])-1)));
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
	private static String lemmatize(String text) throws FileNotFoundException {
		Annotation document = new Annotation(text);
		_pipeline.annotate(document);
		String res = ""; 
		for (CoreLabel sentence : document.get(TokensAnnotation.class)) {
			//System.out.println(sentence);
			res += (sentence.get(LemmaAnnotation.class) + " ");
		}
		//System.out.println(res);
		return res.trim();
	}
	
	private static void rewriteCorpus(String path, String filename) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(path); //.substring(0, filename.length()-4) + "xml");
		writer.println("<text>");
		for (int i=0; i<_cluster.get(filename).size(); i++) {
			for (String token : _cluster.get(filename).get(i).split(" ")) {
				writer.print(token + " " + (i+1) + " ");
			}
		}
		writer.println("\n</text>\n");
		writer.println("<mentions>");
		for (MentionSpan ms : _annotation.get(filename).values()) {
			writer.format("    <span m_id=\"%s\" id=\"%s\" attr=\"%s\">", ms.getMId(), ms.getTId(), ms.getAttribute());
			writer.print(ms.getContent());
			writer.println("</span>");
		}
		writer.println("</mentions>");
		writer.close();
	}
	
	private static void rewriteCoref(String path) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(path);
		for (String note : _coref.keySet()) {
			writer.format("<note instance_id:\"%s\">%n", note);
			for (MentionSpan ms : _coref.get(note).values()) {
				writer.format("  <span m_id=\"%s\" id=\"%s\" attr=\"%s\">", ms.getMId(), ms.getTId(), ms.getAttribute());
				writer.print(ms.getContent());
				writer.println("</span>");
			}
			writer.println("</note>");
		}
		writer.close();
	}
	
	private static void printErrorFiles(String path) throws FileNotFoundException {
		PrintWriter w = new PrintWriter(path);
		for (String s : _errors) {
			w.println(s);
		}
		w.close();
	}
}
