import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

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
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;


public class syncIDs {
	
	private static StanfordCoreNLP _pipeline;
	private static Annotation _document;
	private static Map<String, MentionSpan> _annotationsInDoc = new HashMap<String, MentionSpan>();
	private static Map<Integer, List<Integer>> _map = new HashMap<Integer, List<Integer>>();
	private static List<HashSet<String>> _localChain = new ArrayList<HashSet<String>>();
	private static Set<String> _log = new HashSet<String>();
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		init();
		
		File source3 = new File("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/4/4_8ecbplus.xml");
		File source4 = new File("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/2/2_9ecbplus.xml");
		File source5 = new File("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/14/14_5ecb.xml");
		
		System.out.println(3);
		wrapper(source3, "4", true);
		System.out.println(4);
		wrapper(source4, "2", true);
		System.out.println(5);
		wrapper(source5, "14", false);

		/*
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {return !name.equals(".DS_Store");}
		};
		
		Integer cluster = 16;
		while (cluster <= 45) {
			if (cluster == 15 || cluster == 17) {
				cluster++;
				continue;
			}
			String path = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/NewCorpus/" + cluster;
			File clusterDir = new File(path);
			if (!clusterDir.exists()) {clusterDir.mkdir();}
			File subDir1 = new File(path + "/ecb");
			if (!subDir1.exists()) {subDir1.mkdir();}
			File subDir2 = new File(path + "/ecbplus");
			if (!subDir2.exists()) {subDir2.mkdir();}
			
			File sourceFolder = new File("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/" + cluster);
			for (File file : sourceFolder.listFiles(filter)) {
				System.out.println(file.getName());
				if (file.getName().endsWith("ecb.xml")) {
					wrapper(file, cluster.toString(), false);
				} else {
					wrapper(file, cluster.toString(), true);
				}
			}
			cluster++;
		}
		log();*/
	}
	
	static void wrapper(File file, String cluster, boolean isECBPlus) throws ParserConfigurationException, SAXException, IOException {
		// read tokens as text; add annotations to global var
		String text = readText(file);
		synchronized (text) {
			text = text.replace("\"", "``")
					.replace("'", "`")
					.replace("‘", "`")
					.replace("’", "`")
					.replace("“", "``")
					.replace("”", "``")
					.replace("(", "-LRB-")
					.replace(")", "-RRB-")
					.replace("[", "-LSB-")
					.replace("]", "-RSB-")
					.replace("—", "--")
					.replace("…", "...");
					
		}
		// split text into original tokens
		String[] orig = text.split(" ");
		// annotate
		_document = new Annotation(text);
		_pipeline.annotate(_document);
		// result of tokenization
		String[] tokens = tokenize();
		String[] lemmas = lemmatize();
		if (tokens.length != lemmas.length) {
			System.err.println("ERROR: tokens len != lemmas len");
			StringBuilder b = new StringBuilder();
			for (int i=0; i<tokens.length; i++) {
				b.append(tokens[i] + " " + i + " ");
			}
			System.out.println(tokens.length);
			System.out.println(b.toString());
			StringBuilder b1 = new StringBuilder();
			for (int i=0; i<lemmas.length; i++) {
				b1.append(lemmas[i] + " " + i+ " ");
			}
			System.out.println(lemmas.length);
			System.out.println(b1.toString());
			_log.add(file.getName() + "--tokens len != lemmas len");
			_annotationsInDoc.clear();
			_map.clear();
			_localChain.clear();
			log();
			return;
		}
		_map = sync(orig, tokens);
		updateMentions(tokens, lemmas);
		localChain();
		symplifyChain();
		// write output
		String path = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/NewCorpus/";
		PrintWriter w;
		if (isECBPlus) {
			w = new PrintWriter(path+cluster+"/ecbplus/"+file.getName());
		} else {
			w = new PrintWriter(path+cluster+"/ecb/"+file.getName());
		}
		w.format("<document doc_id=\"%s\">%n", file.getName());
		w.println("<text>");
		w.println(Joiner.on(" ").join(tokens));
		w.println("</text>\n");
		w.println("<mentions>");
		for (MentionSpan ms : _annotationsInDoc.values()) {
			w.format("  <span m_id=\"%s\" id=\"%s\" attr=\"%s\">", ms.getMId(), ms.getTId(), ms.getAttribute());
			w.print(ms.getContent());
			w.println("</span>");
		}
		w.println("</mentions>\n");
		w.println("<lemmas>");
		for (MentionSpan ms : _annotationsInDoc.values()) {
			w.format("  <span m_id=\"%s\" id=\"%s\" attr=\"%s\">", ms.getMId(), ms.getTId(), ms.getAttribute());
			w.print(ms.getLemma());
			w.println("</span>");
		}
		w.println("</lemmas>\n");
		w.println("<local_chains>");
		for (HashSet<String> chain : _localChain) {
			w.println("  <chain>");
			for (String m_id : chain) {
				MentionSpan ms = _annotationsInDoc.get(m_id);
				w.format("    <span m_id=\"%s\" id=\"%s\" attr=\"%s\">", ms.getMId(), ms.getTId(), ms.getAttribute());
				w.print(ms.getContent());
				w.println("</span>");
			}
			w.println("  </chain>");
		}
		w.println("</local_chains>");
		w.println("</document>");
		w.close();
		
		_annotationsInDoc.clear();
		_map.clear();
		_localChain.clear();
	}
	
	static String readText(File file) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		
		StringBuilder b = new StringBuilder();
		NodeList tokenList = doc.getElementsByTagName("token");
		for (int i=0; i<tokenList.getLength(); i++) {
			Element token = (Element) tokenList.item(i);
			b.append(token.getTextContent().trim() + " ");
		}
		
		NodeList annotations = doc.getElementsByTagName("Markables").item(0).getChildNodes();
		for (int i=1; i<annotations.getLength(); i=i+2) {
			// if nodes without TAG_DESCRIPTOR
			Node node = annotations.item(i);
			if (node.getChildNodes().getLength() > 1) {
				String t_ids = "";
				String m_id = file.getName() + "/" + node.getAttributes().item(0).getTextContent();
				NodeList anchors = node.getChildNodes();
				for (int j=1; j<anchors.getLength(); j=j+2) {
					Integer t_id = Integer.parseInt(anchors.item(j).getAttributes().item(0).getTextContent());
					t_ids += (t_id + " ");
				}
				if (node.getNodeName().contains("ACTION") || node.getNodeName().contains("NEG")) {
					_annotationsInDoc.put(m_id, new MentionSpan(m_id, t_ids.trim(), node.getNodeName()));
				} else {
					_annotationsInDoc.put(m_id, new MentionSpan(m_id, t_ids.trim(), node.getNodeName()));
				}
			}
		}
		
		return b.toString().trim();
	}
	
	
	// 0 1 2 3
	// 0 0 1 3 4
	// mapping original id to new id
	// return: [(0,0),(0,0),(1,1),(2,-1),(3,3),(3,4)] -- a dict
	static Map<Integer, List<Integer>> sync(String[] orig, String[] tokens) {
		Map<Integer, List<Integer>> map = new HashMap<Integer, List<Integer>>();
		int index=1;
		int cap = 0;
		for (int i=1; i<=orig.length; i++) {
			List<Integer> tokenIDs = new ArrayList<Integer>();
			if (tokens[index-1].equals(orig[i-1])) {
				//System.out.println(orig[i-1] + " -> " + tokens[index-1]);
				tokenIDs.add(index); index++;
			} else if (index<tokens.length && (tokens[index-1]+tokens[index]).equals(orig[i-1])) {
				//System.out.println(orig[i-1] + " -> " + tokens[index-1] + " " + tokens[index]);
				tokenIDs.add(index); tokenIDs.add(index+1); index+=2;
				
			} else if (index+1<tokens.length && (tokens[index-1]+tokens[index]+tokens[index+1]).equals(orig[i-1])){
				//System.out.println(orig[i-1] + " -> " + tokens[index-1] + " " + tokens[index] + " " + tokens[index+1]);
				tokenIDs.add(index); tokenIDs.add(index+1); tokenIDs.add(index+2); index+=3;
				
			} else if (index+2<tokens.length && (tokens[index-1]+tokens[index]+tokens[index+1]+tokens[index+2]).equals(orig[i-1])) {
				tokenIDs.add(index); tokenIDs.add(index+1); tokenIDs.add(index+2); tokenIDs.add(index+3); index+=4;
			} else if (tokens[index-1].equals("...")) {
				//System.out.println(tokens[index-1]);
				if (orig[i-1].equals(".")) {
					tokenIDs.add(index); index++;
					while (i<orig.length && orig[i].equals(".")) {i++;}
				}
			} else {
				cap += 1;
				if (cap < 7) {
					System.out.println(index + " in tokens:  " + tokens[index-2] + "   " + tokens[index-1] + "  " + tokens[index] + "  " + tokens[index+1]);
					System.out.println("     in orig:  " + orig[i-2] + "  " + orig[i-1] + "   " + orig[i] + "   " + orig[i+1]);
				}
			}
			//if else, unrecognized by tokenizer
			map.put(i, tokenIDs);
		}
		return map;
	}
	
	// change t_id in ms and update ms content
	static void updateMentions(String[] tokens, String[] lemmas) {
		Map<String, MentionSpan> temp = new HashMap<String, MentionSpan>();
		for (MentionSpan ms : _annotationsInDoc.values()) {
			String newID = "", content="", lemma="";
			for (String oldID : ms.getTId().split(" ")) {
				for (Integer i : _map.get(Integer.parseInt(oldID))) {
					newID += (i + " ");
					content += (tokens[i-1] + " ");
					lemma += (lemmas[i-1] + " ");
				}
			}
			ms.setTId(newID.trim());
			ms.setContent(content);
			ms.setLemma(lemma);
			if (!ms.getTId().equals("")) {
				temp.put(ms.getMId(), ms);
			}
		}
		_annotationsInDoc = temp;
	}
	
	static void init() {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		_pipeline = new StanfordCoreNLP(props);
	}
	
	static String[] tokenize() throws FileNotFoundException {
		StringBuilder b = new StringBuilder();
		List<CoreMap> sentences = _document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				//System.out.println(token.index() + token.originalText() + " is the token " + token.lemma());
				for (String s : token.word().split(" ")) {
					b.append(s + " ");
				}
			}
		}
	    return b.toString().trim().split(" ");
	}
	
	static String[] lemmatize() throws FileNotFoundException {
		StringBuilder b = new StringBuilder();
		List<CoreMap> sentences = _document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				//System.out.println(token.index() + token.originalText() + " is the token " + token.lemma());
				for (String s : token.lemma().split(" ")) {
					b.append(s + " ");
				}
			}
		}
	    return b.toString().trim().split(" ");
	}
	
	static void localChain() {
		// {sentNum:{startInd:id}}
		Map<Integer, HashMap<Integer, Integer>> sents = new HashMap<Integer, HashMap<Integer, Integer>>();
	    int i=1;
	    List<CoreMap> sentences = _document.get(SentencesAnnotation.class);
	    for(int index=1; index<=sentences.size(); index++) {
	    	HashMap<Integer, Integer> temp = new HashMap<Integer, Integer>();
	    	//String s = sentences.get(index-1).toString();
	    	//System.out.println(s);
	    	for (CoreLabel token : sentences.get(index-1).get(TokensAnnotation.class)) {
				//System.out.println(token.index() + token.originalText() + " is the token " + token.lemma());
	    		temp.put(token.index(), i);
	    		i++;
			}	
	    	sents.put(index, temp);
	    }
	    
	    Map<Integer, CorefChain> graph = _document.get(CorefChainAnnotation.class);
	    //System.out.println(graph.entrySet().size() + "  is the size");
	    for(Map.Entry<Integer, CorefChain> entry : graph.entrySet()) {
	    	CorefChain c = entry.getValue();
	    	//CorefMention reprm = c.getRepresentativeMention();
	    	//System.out.println(reprm.mentionSpan + "  " + reprm.startIndex + "-" + reprm.endIndex + "  REPR");
	    	//int[] repr = new int[] {reprm.sentNum, reprm.startIndex, reprm.endIndex};
	    	if (c.getMentionMap().size() > 1) {
	    		HashSet<String> temp = new HashSet<String>();
		    	for (Entry<IntPair, Set<CorefMention>> cms : c.getMentionMap().entrySet()) {
		    		for (CorefMention cm : cms.getValue()) {
		    			//System.out.println(cm.mentionSpan + "  " + cm.startIndex + "-" + cm.endIndex + "  sent: " + cm.sentNum + " id: " + sents.get(cm.sentNum).get(cm.startIndex));
			    		for (int j=cm.startIndex; j<cm.endIndex; j++) {
			    			Integer id = sents.get(cm.sentNum).get(j);
			    			//System.out.println(cm.sentNum + "  " + j);
			    			for (MentionSpan ms : _annotationsInDoc.values()) {
			    					
			    				if (ms.containsID(id.toString())) {
			    					temp.add(ms.getMId());
			    				}
			    			}
			   			}    
		    		}
		    	}
		    	if (temp.size() > 1) {
		    		_localChain.add(temp);
		    	}
		    	//System.out.println("________");
	    	}
	    }
	    //System.out.println("reduced: " + _localChain.size());
	}
	
	static void symplifyChain() {
		List<HashSet<String>> tempLocalChain = new ArrayList<HashSet<String>>();
		for (HashSet<String> chain : _localChain) {
			HashSet<String> tempChain = new HashSet<String>();
			Map<String, Integer> count = new HashMap<String, Integer>();
			for (String m_id : chain) {
				MentionSpan ms = _annotationsInDoc.get(m_id);
				String attr = ms.getAttribute();
				if (count.containsKey(attr)) {
					count.put(attr, count.get(attr)+1);
				} else {
					count.put(attr, 1);
				}
			}
			String maxAttr="";
			int max=0;
			for (String key : count.keySet()) {
				if (count.get(key) > max) {
					maxAttr = key;
					max = count.get(key);
				}
			}
			for (String m_id : chain) {
				MentionSpan ms = _annotationsInDoc.get(m_id);
				if (ms.getAttribute().equals(maxAttr)) {
					tempChain.add(ms.getMId());
				}
			}
			if (chain.size() > 1) {
				tempLocalChain.add(tempChain);
			}
		}
		_localChain = tempLocalChain;
	}
	
	static void log() throws FileNotFoundException {
		String path = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/NewCorpus/log.txt";
		PrintWriter w = new PrintWriter(path);
		for (String s: _log) {
			w.println(s);
		}
		w.close();
	}
	
}
