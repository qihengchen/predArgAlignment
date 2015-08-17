import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import easyLinking.IdComparator;
import edu.stanford.nlp.ie.machinereading.common.SimpleTokenize;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;


public class Test {
	
	
	public static void main(String[] args) throws FileNotFoundException {
		Integer i = 1;
		System.out.println(i instanceof Object);
		/*
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {return !name.equals(".DS_Store");}
		};
		
		int c = 1;
		while (c<=45) {
			System.out.println(c);
			if (c==15 || c==17) {
				c++;
				continue;
			}
			String path = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/NewCorpus/"+c;
			File ecb = new File(path+"/ecb");
			File ecbplus = new File(path+"/ecbplus");
			int ecbN = ecb.listFiles(filter).length, ecbplusN = ecbplus.listFiles(filter).length;
			File cluster = new File("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ECB+/"+c);
			int fileNum = cluster.listFiles(filter).length;
			if ((ecbN + ecbplusN) != fileNum) {
				System.out.println("ecb="+ecbN+"  ecbplus="+ecbplusN+"   should be:"+fileNum);
			}
			c++;
		}
		
		/*String annotators = "tokenize, ssplit, pos, lemma, ner, parse, dcoref";
		Properties props = new Properties();
		props.put("annotators",annotators);
		props.put("tokenize.options", "americanize=false");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		String text = "He organises this organization . . .";
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		
		
		String path = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ToyCorpus/1_15ecb.xml";
		String temp = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ToyCorpus/temp.txt";
		String s = "We are (parentheses). Let's parse.";
		
		System.out.println(String.format("%s/%s/%s/%s", 1,2,3,4));
		PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(new FileReader(temp),
	              new CoreLabelTokenFactory(), "ptb3Escaping=false");
	    while (ptbt.hasNext()) {
	        CoreLabel label = ptbt.next();
	        //System.out.print(label + " ");
	    }*/
	    
	    
	}

}
