package mdexplorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A finite state machine that matches text seen from the MUD and triggers commands sent to the MUD
 */
public class Scenario {

	private final static String END_STATE_NAME = "END";
	
	public static class Transition {
		public Trigger trigger;
		public List<Action> actions = new ArrayList<Action>();
		public String newState;
	}

	public static class State {
		public String name;
		public List<Transition> transitions = new ArrayList<Transition>();
	}
	
	
	private Map<String, State> states = new HashMap<String, State>();
	private State currentState;
	
	// Process data from the MUD and see if the finite state machine updates 
	public void processInput(Robot robot, String user, String input) {
		for (Transition t: currentState.transitions) {
			if ((t.trigger == null) || t.trigger.triggeredBy(input)) {
				for(Action a: t.actions) {
					a.execute(robot, user);
				}
				// TBD: Handle out-of-date state transition commands
				currentState = states.get(t.newState);
				break;
			}
		}
	}
	
	public boolean isDone() {
		return currentState == null;
	}
	
	public static Scenario createTestScenario() {
		Scenario scenario = new Scenario();
		State initialState = new State();
		String[] commands = {"go east",
							 "go east",
							 "go north",
							 "dial town",
							 "enter booth",
							 "go east",
		                     "go south",
		                     "look in bin",
		                     "get slips from bin",
		                     "go north",
		                     "go north",
		                     "sell slips to otik"};
		Action initialAction = new SendCommands(commands);
		Action[] initialActions = {initialAction};
		Transition autoTransition = new Transition();
		autoTransition.trigger = null;
		autoTransition.actions =Arrays.asList(initialActions);
		autoTransition.newState = END_STATE_NAME;
		initialState.name = "START";
		initialState.transitions.add(autoTransition);
		scenario.currentState = initialState;
		scenario.states.put(initialState.name, initialState);
		return scenario;
	}
	

}
