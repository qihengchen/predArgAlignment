package easyLinking;

import java.util.Arrays;
import java.util.HashSet;

import com.google.common.collect.Sets;

public class MentionSpan extends Span{
	
	private String _content;
	private final String _id, _attr, _m_id;
	
	public MentionSpan(String content, String id, String m_id, String attribute) {
		super(content);
		// _content = content;
		_id = id;  // "doc/t_id doc/t_id ..."    final
		_m_id = m_id;  // doc/m_id               final
		_attr = attribute; // P/A/""             final
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
		HashSet<String> thisSet = new HashSet<String>(Arrays.asList(_content.split(" ")));
		HashSet<String> objSet = new HashSet<String>(Arrays.asList(ms.getContent().split(" ")));
		return Sets.intersection(thisSet, objSet).size() >= 1;
	}
	
	public String getContent() {
		return _content;
	}
	
	public void setContent(String content) {
		_content = content;
	}
	
	public String getId() {
		return _id;
	}
	
	public String getMId() {
		return _m_id;
	}
	
	public String getAttribute() {
		return _attr;
	}
}
