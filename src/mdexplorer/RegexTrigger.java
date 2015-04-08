package mdexplorer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;

public class RegexTrigger implements Trigger {

	private Pattern regex;
	
	public RegexTrigger() {
		// Prototype constructor.
	}
	
	public RegexTrigger(String regexStr) {
		regex = Pattern.compile(regexStr, Pattern.DOTALL);
	}
	
	@Override
	public RegexTrigger load(Node node) {
		String regexStr = node.getTextContent().trim();
		RegexTrigger newTrigger = new RegexTrigger(regexStr);
		return newTrigger;
	}

	@Override
	public boolean triggeredBy(String input) {
		Matcher matcher = regex.matcher(input);
		return matcher.find();
	}

}
