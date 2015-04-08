package mdexplorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

public class SendCmdsAction implements Action {

	private List<String> commands;
	
	public SendCmdsAction(String[] commandArray) {
		commands = Arrays.asList(commandArray);
	}
	
	/**
	 * Creates an empty action.  Mostly used as a prototype for spawning
	 * new SendCommands objects.
	 */
	public SendCmdsAction() {
		commands = new ArrayList<String>();
	}
	
	@Override
	public void execute(Robot r, String user, Map<String, Object> vars) {
		for(String nextCommand: commands) {
			r.getMud().send(user, nextCommand);
		}
	}
	
	@Override
	public SendCmdsAction load(Node node) {
		String commandString = node.getTextContent();
		String[] commandArray = commandString.trim().split("\n");
		for(int i=0; i<commandArray.length; ++i) {
			commandArray[i] = commandArray[i].trim();
		}
		SendCmdsAction newAction = new SendCmdsAction(commandArray);
		return newAction;
	}
}
