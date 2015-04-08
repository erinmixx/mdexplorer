package mdexplorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;

public class RegexTrigger implements Trigger {

	private Pattern regex;
	private List<String> namedCaptures = new ArrayList<String>();
	
	public RegexTrigger() {
		// Prototype constructor.
	}
	
	public RegexTrigger(String regexStr) {
		regex = Pattern.compile(regexStr, Pattern.DOTALL);
	}
	
	@Override
	public RegexTrigger load(Node node) {
		Node keyNode = node.getAttributes().getNamedItem("keys");
		String keyStr = (keyNode != null ? keyNode.getNodeValue() : null);
		String regexStr = node.getTextContent().trim();
		RegexTrigger newTrigger = new RegexTrigger(regexStr);
		if (keyStr != null) {
			newTrigger.namedCaptures = Arrays.asList(keyStr.trim().split(","));
		}
		return newTrigger;
	}

	@Override
	public boolean triggeredBy(String input, Map<String, Object> vars) {
		Matcher matcher = regex.matcher(input);
		boolean success = matcher.find();
		if (success) {
			// Add any parsed values to the vars map.
			for(String nextKey: namedCaptures) {
				try {
					String nextValue = matcher.group(nextKey);
					vars.put(nextKey, nextValue);
				} catch (IllegalArgumentException e) {
					// Don't care.  Just don't put in an entry for that key.
				}
			}
		}
		return success;
	}

}
