package mdexplorer;

import java.util.Arrays;
import java.util.List;

public class SendCommands implements Action {

	private List<String> commands;
	
	public void execute(Robot r, String user) {
		for(String nextCommand: commands) {
			r.getMud().send(user, nextCommand);
		}
	}
	
	public SendCommands(String[] commandArray) {
		commands = Arrays.asList(commandArray);
	}
}
