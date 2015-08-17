package easyLinking;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
 * this class models the probability of left length and right length in a linking
 */

public class PhraseLengthCapping {
	
	// {leftLen:{rightLen:count}}
	private Map<Integer, HashMap<Integer,Integer>> _count = new HashMap<Integer, HashMap<Integer,Integer>>();
	private Map<Integer, HashMap<Integer,Double>> _prob = new HashMap<Integer, HashMap<Integer,Double>>();
	
	public PhraseLengthCapping() {
		
	}
	
	public void computeProb() {
		int sum = 0;
		for (Map<Integer, Integer> map : _count.values()) {
			for (Integer i : map.values()) {sum += i;}
		}
		
		if (sum==0) {
			System.out.println("size: " + _count.size());
			System.exit(0);
		}
		for (Integer left : _count.keySet()) {
			_prob.put(left, new HashMap<Integer, Double>());
			for (Integer right : _count.get(left).keySet()) {
				_prob.get(left).put(right, (double) _count.get(left).get(right)/sum);
			}
		}
		//_count.clear();   //save memory
	}
	
	public void addEntry(int left, int right) {
		if (_count.containsKey(left)) {
			Integer num = _count.get(left).get(right);
			if (num != null) {_count.get(left).put(right, num+=1);}
			else {_count.get(left).put(right, 1);}
		} else {
			_count.put(left, new HashMap<Integer,Integer>());
			_count.get(left).put(right, 1);
		}
	}
	
	public void addGoldenTruth(Set<String> G) {
		
	}
	
	public double getProb(int left, int right) {
		if (_prob.containsKey(left) && _prob.get(left).containsKey(right)) {
			return _prob.get(left).get(right);
		} else {return 0.0;}
	}
	
	public void report(String path) throws FileNotFoundException {
		PrintWriter w = new PrintWriter(path);
		for (Integer i : _prob.keySet()) {
			for (Integer j : _prob.get(i).keySet()) {
				w.format("%2d -> %2d:  %f%n", i, j, _prob.get(i).get(j));
			}
		}
		/*for (int i=1; i<=7; i++) {
			for (int j=1; j<=7; j++) {
				try{
					w.format("%d -> %d:  %f%n", i, j, _prob.get(i).get(j));
				} catch (NullPointerException e) {
					w.format("%d -> %d:  %f%n", i, j, 0.0000);
				}
			}
		}*/
		w.close();
	}

}
