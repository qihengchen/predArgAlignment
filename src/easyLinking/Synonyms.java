package easyLinking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

// tracking non-identical singleton-singleton alignment; return true if two words are linked before
// later, this class may be changed to a probability-based class
public class Synonyms {
	
	private Map<String, HashSet<String>> _synonyms = new HashMap<String, HashSet<String>>();
	
	public Synonyms() {
		
	}
	
	public void add(String left, String right) {
		if (!_synonyms.containsKey(left)) {
			_synonyms.put(left, new HashSet<String>());
		}
		_synonyms.get(left).add(right);
	}
	
	public void compute() {
		
	}
	
	public boolean isSynonym(String left, String right) {
		if (_synonyms.containsKey(left)) {
			return _synonyms.get(left).contains(right);
		} else if (_synonyms.containsKey(right)) {
			return _synonyms.get(right).contains(left);
		} else {
			return false;
		}
	}
	
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("size is: " + _synonyms.size() + "\n");
		if (_synonyms.containsKey("kill")) {
			b.append("'kill' is associated to:\n");
			for (String s : _synonyms.get("kill")) {
				b.append(s + " ");
			}
		} else if (_synonyms.containsKey("merger")) {
			b.append("\n'merger' is associated to:\n");
			for (String s : _synonyms.get("merger")) {
				b.append(s + " ");
			}
		} else if (_synonyms.containsKey("deal")) {
			b.append("\n'deal' is associated to:\n");
			for (String s : _synonyms.get("deal")) {
				b.append(s + " ");
			}
		}
		return b.toString();
	}

}
