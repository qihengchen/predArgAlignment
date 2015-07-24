import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;


public class CountOverlappingWords {
	
	private static HashMap<String, Integer> _count = new HashMap<String, Integer>(),
											_mistakes = new HashMap<String, Integer>();
	
	public static void main(String[] args) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(
				"/Users/Qiheng/Desktop/Summer 2015/experiment/H1-noChain.txt"))) {
			String line;
			while((line = reader.readLine()) != null) {
				String a = line.split("  ")[1];
				String s1 = a.split(" -> ")[0], s2 = a.split(" -> ")[1];
				findOverlap(s1, s2);
			}
			reader.close();
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(
				"/Users/Qiheng/Desktop/Summer 2015/experiment/mistakes-noChain.txt"))) {
			String line;
			while((line = reader.readLine()) != null) {
				String a = line.split("  ")[1];
				String s1 = a.split(" -> ")[0], s2 = a.split(" -> ")[1];
				findMistakeOverlap(s1, s2);
			}
			reader.close();
		}
		PrintWriter writer = new PrintWriter("/Users/Qiheng/Desktop/Summer 2015/experiment/H1-noChain-commonWords.txt");
		for (String key : _count.keySet()) {
			int m = 0;
			if (_mistakes.containsKey(key)) {
				m = _mistakes.get(key);
			}
			writer.format("%-15s   err: %-3d   all: %-3d   err%%: %-4f%n", key, m, _count.get(key), (double) m/_count.get(key));
		}
		writer.close();
	}
	
	private static void findOverlap(String s1, String s2) {
		String[] a1 = s1.split(" "), a2 = s2.split(" ");
		for (String s : a1) {
			for (String d : a2) {
				if (s.equals(d)) {
					if (_count.containsKey(s)) {
						_count.put(s, _count.get(s)+1);
					} else {
						_count.put(s, 1);
					}
				}
			}
		}
	}
	
	private static void findMistakeOverlap(String s1, String s2) {
		String[] a1 = s1.split(" "), a2 = s2.split(" ");
		for (String s : a1) {
			for (String d : a2) {
				if (s.equals(d)) {
					if (_mistakes.containsKey(s)) {
						_mistakes.put(s, _mistakes.get(s)+1);
					} else {
						_mistakes.put(s, 1);
					}
				}
			}
		}
	}

}
