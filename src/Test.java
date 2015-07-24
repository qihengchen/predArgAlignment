import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import easyLinking.IdComparator;
import edu.stanford.nlp.ie.machinereading.common.SimpleTokenize;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;


public class Test {
	
	public static void main(String[] args) throws FileNotFoundException {
		
		
		String path = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ToyCorpus/1_15ecb.xml";
		String temp = "/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/ToyCorpus/temp.txt";
		String s = "We are (parentheses). Let's parse.";
		
		
		PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(new FileReader(temp),
	              new CoreLabelTokenFactory(), "ptb3Escaping=false");
	    while (ptbt.hasNext()) {
	        CoreLabel label = ptbt.next();
	        System.out.print(label + " ");
	    }
	    
	    
	}

}
