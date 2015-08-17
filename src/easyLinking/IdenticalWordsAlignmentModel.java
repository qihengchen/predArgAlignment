package easyLinking;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/*
 * SAME: 10299  NOT SAME: 20204
 */

public class IdenticalWordsAlignmentModel {
	
	private Map<String,int[]> _count = new HashMap<String, int[]>();
	private Map<String,Double> _prob = new HashMap<String, Double>();
	private double alpha = 5.0;
	
	public IdenticalWordsAlignmentModel() {
		
	}
	
	public void trainOn(int start, int end) throws IOException {
		for (int i=start; i<=end; i++) {
			if (i==15 || i==17) {
				continue;
			}
			trainOn("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/Results/compareGH"+i+"+.txt");
			trainOn("/Users/Qiheng/Desktop/Summer 2015/ECB_corpus/Results/compareGH"+i+".txt");
		}
	}
	
	public void trainOn(String path) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
			String line = "";
			//System.out.println(path);
			line = reader.readLine();
			while (!(line = reader.readLine()).contains("Wrong Alignment")) {
				String a = line.split("  ")[1];
				String s1 = a.split(" -> ")[0], s2 = a.split(" -> ")[1];
				addGoodWords(s1, s2);
			}
			while (!(line = reader.readLine()).contains("Correct Alignment")) {
				String a = line.split("  ")[1];
				String s1 = a.split(" -> ")[0], s2 = a.split(" -> ")[1];
				addBadWords(s1, s2);
			}
			while ((line = reader.readLine()) != null) {
				String a = line.split("  ")[1];
				String s1 = a.split(" -> ")[0], s2 = a.split(" -> ")[1];
				addGoodWords(s1, s2);
			}
			reader.close();
		}
		for (String token : _count.keySet()) {
			_prob.put(token, (_count.get(token)[0]+alpha)/(_count.get(token)[1]+alpha*3));
		}
	}
	
	// penalize more than one wrong token alignment
	private void addBadWords(String s1, String s2) {
		String[] a1 = s1.split(" "), a2 = s2.split(" ");
		for (String s : a1) {
			for (String d : a2) {
				if (s.equals(d)) {
					if (_count.containsKey(s)) {
						_count.get(s)[0]+=1;
						_count.get(s)[1]+=1;
					} else {
						_count.put(s, new int[]{1,1});
					}
				}
			}
		}
	}
	
	private void addGoodWords(String s1, String s2) {
		String[] a1 = s1.split(" "), a2 = s2.split(" ");
		for (String s : a1) {
			for (String d : a2) {
				if (s.equals(d)) {
					if (_count.containsKey(s)) {
						_count.get(s)[1]+=1;
					} else {
						_count.put(s, new int[]{0,1});
					}
				}
			}
		}
	}
	
	// the probability that token is a bad word
	public double prob(String token) {
		if (!_prob.containsKey(token)) {
			return 0.01;
		} else {
			return _prob.get(token);
		}
	}
	
	public void report(String path) throws FileNotFoundException {
		PrintWriter w = new PrintWriter(path);
		Map<String, Double> p75_100 = new HashMap<String, Double>(), p50_75 = new HashMap<String, Double>(),
				p25_50 = new HashMap<String, Double>(), p0_25 = new HashMap<String, Double>();
		for (String key : _prob.keySet()) {
			Double p = _prob.get(key);
			if (p>0.75) {p75_100.put(key, p);}
			else if (p>0.5) {p50_75.put(key, p);}
			else if (p>0.25) {p25_50.put(key, p);}
			else {p0_25.put(key, p);}
		}
		/*for (String key : _prob.keySet()) {
			w.format("%10s  %f%n", key, _prob.get(key));
		}*/
		w.println("size: " + _prob.size());
		for (String key : p75_100.keySet()) {w.format("%15s  %f%n", key, _prob.get(key));}
		for (String key : p50_75.keySet()) {w.format("%15s  %f%n", key, _prob.get(key));}
		for (String key : p25_50.keySet()) {w.format("%15s  %f%n", key, _prob.get(key));}
		for (String key : p0_25.keySet()) {w.format("%15s  %f%n", key, _prob.get(key));}
		w.close();
	}
}
