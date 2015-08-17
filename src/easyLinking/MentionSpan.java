package easyLinking;

import java.util.Arrays;
import java.util.HashSet;

import com.google.common.collect.Sets;

public class MentionSpan {
	
	private String _content, _lemma;
	private final String _attr, _m_id;
	private String _t_id;
	
	public MentionSpan(String m_id, String attribute) {
		// _content = content;
		//_id = id;  // "doc/t_id doc/t_id ..."    final
		_m_id = m_id;  // doc/m_id               final
		_attr = attribute; // P/A/""             final
	}
	
	public MentionSpan(String m_id, String t_id, String attribute) {
		_m_id = m_id;  // doc/m_id               final
		_attr = attribute;// P/A/""              final
		_t_id = t_id;
	}
	
	public MentionSpan(String m_id, String t_id, String content, String attribute) {
		_m_id = m_id;  // doc/m_id               final
		_attr = attribute;// P/A/""              final
		_t_id = t_id;
		_content = content;
	}
	
	@Override
	// if two mentions overlap, return true.
	public boolean equals(Object obj) {
		if (!(obj instanceof MentionSpan)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		MentionSpan ms = (MentionSpan) obj;
		HashSet<String> thisSet = new HashSet<String>(Arrays.asList(_lemma.split(" ")));
		HashSet<String> objSet = new HashSet<String>(Arrays.asList(ms.getLemma().split(" ")));
		return Sets.intersection(thisSet, objSet).size() >= 1;
	}
	
	public String getContent() {
		return _content;
	}
	
	public void setContent(String content) {
		_content = content;
	}
	
	public void setTId(String id) {
		_t_id = id;
	}
	
	public String getTId() {
		return _t_id;
	}
	
	public String getMId() {
		return _m_id;
	}
	
	public String getAttribute() {
		return _attr;
	}
	
	public void setLemma(String lemma) {
		_lemma = lemma;
	}
	
	public String getLemma() {
		return _lemma;
	}
	
	public boolean containsID(String that) {
		for (String id : _t_id.split(" ")) {
			if (id.equals(that)) {
				return true;
			}
		}
		return false;
	}
}
