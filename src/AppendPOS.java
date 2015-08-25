import java.io.File;
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
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;


public class AppendPOS {
	
	private static StanfordCoreNLP _pipeline;
	private static Map<String, MentionSpan> _annotationsInDoc = new HashMap<String, MentionSpan>();
	private static List<HashSet<String>> _localChain = new ArrayList<HashSet<String>>();
	
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		_pipeline = new StanfordCoreNLP(props);
		
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {return !name.equals(".DS_Store");}
		};
		
		Integer cluster = 1;
		while (cluster <= 36) {
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
			
			File sourceFolder = new File("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/OldNewCorpus/"+cluster+"/ecb");
			//System.out.println(sourceFolder.getPath());
			//System.out.println(sourceFolder.exists());
			for (File file : sourceFolder.listFiles(filter)) {
				System.out.println("file: " + file.getName());
				wrapper(file, cluster.toString(), false);
			}
			File sourceFolderPlus = new File("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/OldNewCorpus/"+cluster+"/ecbplus");
			for (File file : sourceFolderPlus.listFiles(filter)) {
				System.out.println("file: " + file.getName());
				wrapper(file, cluster.toString(), true);
			}
			cluster++;
		}
	}
	
	
	static void wrapper(File file, String cluster, boolean isECBPlus) throws ParserConfigurationException, SAXException, IOException {
		// read tokens as text; add annotations to global var
		String text = readText(file);
		
		String[] tokens_POS = findPOS(text);
		addPOSToMentions(tokens_POS);
		
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
		w.println(text);
		w.println("</text>\n");
		w.println("<mentions>");
		for (MentionSpan ms : _annotationsInDoc.values()) {
			w.format("  <span m_id=\"%s\" id=\"%s\" attr=\"%s\" pos=\"%s\">", ms.getMId(), ms.getTId(), ms.getAttribute(), ms.getPOS());
			w.print(ms.getContent());
			w.println("</span>");
		}
		w.println("</mentions>\n");
		w.println("<lemmas>");
		for (MentionSpan ms : _annotationsInDoc.values()) {
			w.format("  <span m_id=\"%s\" id=\"%s\" attr=\"%s\" pos=\"%s\">", ms.getMId(), ms.getTId(), ms.getAttribute(), ms.getPOS());
			w.print(ms.getLemma());
			w.println("</span>");
		}
		w.println("</lemmas>\n");
		w.println("<local_chains>");
		for (HashSet<String> chain : _localChain) {
			w.println("  <chain>");
			for (String m_id : chain) {
				MentionSpan ms = _annotationsInDoc.get(m_id);
				w.format("    <span m_id=\"%s\" id=\"%s\" attr=\"%s\" pos=\"%s\">", ms.getMId(), ms.getTId(), ms.getAttribute(), ms.getPOS());
				w.print(ms.getContent());
				w.println("</span>");
			}
			w.println("  </chain>");
		}
		w.println("</local_chains>");
		w.println("</document>");
		w.close();
		
		_annotationsInDoc.clear();
		_localChain.clear();
	}
	
	static String readText(File file) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		
		String text = doc.getElementsByTagName("text").item(0).getTextContent();
		
		NodeList annotations = doc.getElementsByTagName("mentions").item(0).getChildNodes();
		for (int i=1; i<annotations.getLength(); i=i+2) {
			Element span = (Element) annotations.item(i);
			String t_ids = span.getAttribute("id"),
					m_id = span.getAttribute("m_id"),
					attr = span.getAttribute("attr"),
					content = span.getTextContent().trim();
			_annotationsInDoc.put(m_id, new MentionSpan(m_id, t_ids, content, attr));
		}
		
		annotations = doc.getElementsByTagName("lemmas").item(0).getChildNodes();
		for (int i=1; i<annotations.getLength(); i=i+2) {
			Element span = (Element) annotations.item(i);
			String m_id = span.getAttribute("m_id"),
					lemma = span.getTextContent().trim();
			_annotationsInDoc.get(m_id).setLemma(lemma);
		}
		
		NodeList chains = doc.getElementsByTagName("local_chains").item(0).getChildNodes();
		for (int i=1; i<chains.getLength(); i+=2) {
			HashSet<String> chainedMentions = new HashSet<String>();
			NodeList mentions = chains.item(i).getChildNodes();
			for (int j=1; j<mentions.getLength(); j+=2) {
				Element span = (Element) mentions.item(j);
				String m_id = span.getAttribute("m_id");
				chainedMentions.add(m_id);
			}
			_localChain.add(chainedMentions);
		}
		
		return text.trim();
	}
	
	static String[] findPOS(String text) {
		String[] words = text.split(" ");
		String[] res = new String[words.length*2];
		int ind=0;
		Annotation document = new Annotation(text);
		_pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				res[ind]=token.originalText(); ind++;
				res[ind]=token.tag(); ind++;
			}
		}
		return res;
	}
	
	static void addPOSToMentions(String[] tokens_POS) {
		for (MentionSpan ms : _annotationsInDoc.values()) {
			String pos = "";
			for (String id : ms.getTId().split(" ")) {
				pos += (tokens_POS[Integer.parseInt(id)*2-1] + " ");
			}
			ms.setPOS(pos.trim());
		}
	}

}
