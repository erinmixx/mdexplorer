package mdexplorer;

import java.util.Map;

public interface Trigger extends Scenario.Prototype {
	
	/**
	 * Will trigger if a certain input is expected
	 * @param input input string that causes the trigger
	 * @param props values may be pulled from the input string and put in the vars map for use by triggered actions (e.g. pulling out the name of an actor
	 * so that the action can target that actor)
	 * @return true if the input triggers this trigger
	 */
	public boolean triggeredBy(String input, Map<String, Object> vars);
}
