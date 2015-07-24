import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;


public class Analysis {
	
	private static HashSet<String> _H = new HashSet<String>(), 
									 _G = new HashSet<String>();
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(
				"/Users/Qiheng/Desktop/Summer 2015/experiment/H1-noChain.txt"))) {
			String line;
			while((line = reader.readLine()) != null) {
				_H.add(line.split(" ")[0] + " -> " + line.split(" ")[2]);	
			}
			reader.close();
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(
				"/Users/Qiheng/Desktop/Summer 2015/experiment/G1-noChain.txt"))) {
			String line;
			while((line = reader.readLine()) != null) {
				_G.add(line.split(" ")[0] + " -> " + line.split(" ")[2]);	
			}
			reader.close();
		}
		Set<String> common = Sets.intersection(_G, _H);
		PrintWriter writer = new PrintWriter("/Users/Qiheng/Desktop/Summer 2015/experiment/mistakes-noChain.txt");
		int count = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(
				"/Users/Qiheng/Desktop/Summer 2015/experiment/H1-noChain.txt"))) {
			String line;
			while((line = reader.readLine()) != null) {
				if (!common.contains(Joiner.on(" ").join(Arrays.copyOfRange(line.split(" "), 0, 3)))) {
					count += 1;
					writer.println(line);
				}
			}
			reader.close();
		}
		writer.close();
	}

}
