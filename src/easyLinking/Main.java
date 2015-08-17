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
		IdenticalWordsAlignmentModel model = new IdenticalWordsAlignmentModel();
		PhraseLengthCapping plc = new PhraseLengthCapping();
		Synonyms syno = new Synonyms();
		WordNet wn = new WordNet();
		BaselineTraining.train(model, plc, syno);
		System.out.println("\n\n==============================\n\n");
		Testing.testing(model,plc,syno);
		model.report("/Users/Qiheng/Desktop/Summer 2015/experiment/model_report_allFiles.txt");
		plc.report("/Users/Qiheng/Desktop/Summer 2015/experiment/plc_report_allFiles.txt");
		System.out.println(syno);
	}
}

/*
 * next step:
 * 1. 
 * 2. stratify _model as per POS tags
 * 3. design a computational model
 */
