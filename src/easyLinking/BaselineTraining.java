package easyLinking;

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
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.collect.Sets;

public class BaselineTraining {
	
	// {note: {m_id:ms}}
	private static Map<String, HashMap<String, MentionSpan>> _coref = new HashMap<String, HashMap<String, MentionSpan>>();
	private static Map<String, MentionSpan> _annotation = new HashMap<String, MentionSpan>();
	private static boolean _hasChain=false;
	private static Set<String> _G = new HashSet<String>(), _H = new HashSet<String>();
	private static List<HashSet<String>> _chains = new ArrayList<HashSet<String>>();
	private static IdenticalWordsAlignmentModel _model;
	private static PhraseLengthCapping _plc;
	private static Synonyms _syno;
	private static double _threshold;
	private final static double _same=10299.0/(10299+20204);
	private final static double _notSame=20204.0/(10299+20204);
	
	static void train(IdenticalWordsAlignmentModel model, PhraseLengthCapping plc, Synonyms syno) throws ParserConfigurationException, SAXException, IOException {
		_model = model;
		_plc = plc;
		_syno = syno;
		_hasChain = true;
		_threshold = 0.7;
		int i=1;
		while(i<=36) {
			if (i==15 || i==17) {i++; continue;}
			System.out.println("CLUSTER:  " + i);
			System.out.print("ECBPlus --  ");
			clusterWrapper(i, true);
			System.out.print("ECB --  ");
			clusterWrapper(i, false);
			System.out.println();
			i++;
		}
		_model.trainOn(1, 36);  // wipe out punctuations
		_plc.computeProb();
	}
	
	private static void clusterWrapper(int clusterNum, boolean isECBPlus) throws ParserConfigurationException, SAXException, IOException {	
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {return !name.equals(".DS_Store");}
		};
		
		String path = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/NewCorpus/" + clusterNum;
		if (isECBPlus) {path += "/ecbplus";} else {path += "/ecb";}
		File folder = new File(path);
		for (File file : folder.listFiles(filter)) {readData(file);}

		String corefPath = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/MyCorpus/" + clusterNum;
		if (isECBPlus) {corefPath += "/ecbplus/cross_doc_coref.xml";} else {corefPath += "/ecb/cross_doc_coref.xml";}
		readCoref(new File(corefPath));
		
		align();
		extractG();
		eval();
		
		if (isECBPlus) {
			writeGH("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/Results/G"+clusterNum+"+.txt", 
					"/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/Results/H"+clusterNum+"+.txt",
					"/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/Results/compareGH"+clusterNum+"+.txt");
		} else {
			writeGH("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/Results/G"+clusterNum+".txt", 
					"/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/Results/H"+clusterNum+".txt",
					"/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/Results/compareGH"+clusterNum+".txt");
		}
		_coref.clear(); _annotation.clear(); _chains.clear(); _G.clear(); _H.clear();
	}
	
	// read data from MyCorpus folder
	private static void readData(File file) throws SAXException, IOException, ParserConfigurationException {
		//System.out.println(filename);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		
		// find all annotations
		NodeList mentionList = doc.getElementsByTagName("mentions").item(0).getChildNodes();
		for (int i=1; i<mentionList.getLength()-1; i+=2) {   // ?
			Element ms = (Element) mentionList.item(i);
			String m_id=ms.getAttribute("m_id"), id=ms.getAttribute("id"),
					attr=ms.getAttribute("attr"), content=ms.getTextContent().trim(), pos=ms.getAttribute("pos");
			if (attr.contains("ACTION") || attr.contains("NEG")) {
				_annotation.put(m_id, new MentionSpan(m_id, id, content, "pred", pos));
			} else {
				_annotation.put(m_id, new MentionSpan(m_id, id, content, "arg", pos));
			}
		}
		
		// get lemmas
		NodeList lemmaList = doc.getElementsByTagName("lemmas").item(0).getChildNodes();
		for (int i=1; i<lemmaList.getLength()-1; i+=2) {
			Element lemmaMS = (Element) lemmaList.item(i);
			String m_id=lemmaMS.getAttribute("m_id"), lemma=lemmaMS.getTextContent().trim();
			_annotation.get(m_id).setLemma(lemma);
		}
		
		// get local chain
		NodeList chains = doc.getElementsByTagName("chain");
		for (int i=0; i<chains.getLength(); i++) {
			HashSet<String> temp = new HashSet<String>();
			NodeList mentions = chains.item(i).getChildNodes();
			for (int j=1; j<mentions.getLength()-1; j+=2) {
				Element ms = (Element) mentions.item(j);
				temp.add(ms.getAttribute("m_id"));
			}
			_chains.add(temp);
		}
	}
	
	private static void readCoref(File file) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		NodeList notes = doc.getElementsByTagName("note");
		for (int i=0; i<notes.getLength(); i++) {
			Element note = (Element) notes.item(i);
			HashMap<String, MentionSpan> temp = new HashMap<String, MentionSpan>();
			NodeList spans = note.getChildNodes();
			for (int j=1; j<spans.getLength(); j+=2) {
				Element span = (Element) spans.item(j);
				String m_id = span.getAttribute("m_id");
				temp.put(m_id, _annotation.get(m_id));
			}
			_coref.put(note.getAttribute("instance_id"), temp);
		}
	}
	
	private static void align() {
		// align if content overlaps or in the same local chain
		System.out.println("Annotation size:   " + _annotation.size());
		for (String key : _annotation.keySet()) {
			//if (key.contains("_1ecb")) {
				MentionSpan mention = _annotation.get(key);
				for (MentionSpan ms : _annotation.values()) {
					if (/*!ms.getMId().contains("_1ecb") &&*/ mention.getAttribute().equals(ms.getAttribute())) {
						
						if (mention.equals(ms)) {
							_H.add(mention.getMId() + " -> " + ms.getMId());
							if (_hasChain) {
								for (HashSet<String> hs : _chains) {
									if (hs.contains(ms.getMId())) {
										for (String MID : hs) {
											if (mention.equals(_annotation.get(MID))) {
												_H.add(mention.getMId() + " -> " + MID);
											}
										}
									}
									if (hs.contains(mention.getMId())) {
										for (String MID : hs) {
											if (_annotation.get(MID).equals(ms)) {
												_H.add(MID + " -> " + ms.getMId());
											}
											
										}
									}
								}
							}
						}
					}
				}
			//}
		}
	}
	
	private static String findOverlap(String s1, String s2) {
		String[] a1 = s1.split(" "), a2 = s2.split(" ");
		for (String s : a1) {
			for (String d : a2) {
				if (s.equals(d)) {
					return s;
				}
			}
		}
		return "*NOT FOUND*";
	}
	
	private static void extractG() {
		// ground truth -- [doc/m_id -> doc/m_id]
		for (HashMap<String, MentionSpan> mentions : _coref.values()) {   // [token, doc]
			for (String m_id : mentions.keySet()) {
				//if (m_id.contains("_1ecb")) { // && mentions.get(m_id).getAttribute().equals("pred")) {
					for (String m_id2 : mentions.keySet()) {
						//if (!m_id2.contains("_1ecb")){ // && mentions.get(m_id2).getAttribute().equals("pred")) {
							_G.add(m_id + " -> " + m_id2);
								
								MentionSpan ms = _annotation.get(m_id);
								MentionSpan ms2 = _annotation.get(m_id2);
								int left = ms.getContent().split(" ").length;
								int right = ms2.getContent().split(" ").length;
								_plc.addEntry(left, right);
								
								if (left==1 && right==1 && !ms.getLemma().equals(ms2.getLemma()) && !ms.getPOS().equals("PRP") &&
										ms2.getPOS().equals("PRP")) {
									_syno.add(_annotation.get(m_id).getLemma(), _annotation.get(m_id2).getLemma());
								}
							
						//}
					}
				//}
			}
		}
	}
	
	private static void eval() {
		int intersection = Sets.intersection(_G, _H).size();
		System.out.println("G /\\ H :  " + intersection);
		double precision = (double) intersection / _H.size();
		double recall = (double) intersection / _G.size();
		double F1 = 2*precision*recall / (precision+recall);
		System.out.format("precision: %f   recall: %f   F1: %f%n", precision, recall, F1);
	}
	
	private static void writeGH(String GFile, String HFile, String path) throws FileNotFoundException {
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
		
		PrintWriter w = new PrintWriter(path);
		w.println("Not Inferred Ground Truth");
		for (String g : _G) {
			if (!_H.contains(g)) {
				w.print(g);
				w.println("  " + _annotation.get(g.split(" ")[0]).getLemma() + " -> " 
						+ _annotation.get(g.split(" ")[2]).getLemma());
			}
		}
		w.println("Wrong Alignment");
		for (String h : _H) {
			if (!_G.contains(h)) {
				w.print(h);
				w.println("  " + _annotation.get(h.split(" ")[0]).getLemma() + " -> " 
						+ _annotation.get(h.split(" ")[2]).getLemma());
			}
		}
		w.println("Correct Alignment");
		for (String s : Sets.intersection(_G, _H)) {
			w.print(s);
			w.println("  " + _annotation.get(s.split(" ")[0]).getLemma() + " -> " 
					+ _annotation.get(s.split(" ")[2]).getLemma());
		}
		w.close();
	}

}
