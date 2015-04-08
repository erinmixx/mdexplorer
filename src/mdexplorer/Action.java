package mdexplorer;

import java.util.Map;

public interface Action extends Scenario.Prototype {

	/**
	 * Perform this action
	 * @param r the robot orchestrating the operation
	 * @param user the name of the user logged into the MUD
	 * @param vars any variables parsed from the triggering string
	 */
	public void execute(Robot r, String user, Map<String, Object> vars);
}
