package mdexplorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A finite state machine that matches text seen from the MUD and triggers commands sent to the MUD
 */
public class Scenario {

	private final static String START_STATE_NAME = "START";
	private final static String END_STATE_NAME = "END";
	private final static String ERROR_STATE_NAME = "ERROR";
	
	public static class Transition {
		public Trigger trigger;
		public List<Action> actions = new ArrayList<Action>();
		public String toState;
		public static Transition load(Node node) {
			return Scenario.loadTransition(node);
		}
	}

	public static class State {
		public String name;
		public List<Transition> transitions = new ArrayList<Transition>();
		public State(String inName) {
			name = inName;
		}
		public static State load(Node node) {
			return Scenario.loadState(node);
		}
	}
	
	/**
	 * The prototype or exemplar pattern.  An object knows how to read a configuration and create a new
	 * instance per that specification.  Usually the class also has a no-argument constructor that 
	 * creates an object solely for the purpose of creating the configured object, but no way to 
	 * express that in an interface.
	 */
	public static interface Prototype {
		public Prototype load(Node node);
	}
	
	private String name;
	private Map<String, State> states = new HashMap<String, State>();
	private State currentState;
	
	// Process data from the MUD and see if the finite state machine updates 
	public void processInput(Robot robot, String user, String input) {
		Map<String, Object> vars = new HashMap<String, Object>();
		for (Transition t: currentState.transitions) {
			if ((t.trigger == null) || t.trigger.triggeredBy(input, vars)) {
				System.out.println("XXXXXXXXXXXXXXXXXXX Switching to state " + t.toState);
				for(Action a: t.actions) {
					a.execute(robot, user, vars);
				}
				// TBD: Handle out-of-date state transition commands
				currentState = states.get(t.toState);
				break;
			}
		}
	}
	
	public String getName() {
		return name;
	}
	
	public String getState() {
		return currentState.name;
	}
	
	public boolean isDone() {
		return currentState == null;
	}
	
	public static Scenario load(Node node) {
		Scenario loaded = new Scenario();
		String scenarioName = node.getAttributes().getNamedItem("name").getNodeValue();
		loaded.name = scenarioName;
		NodeList stateNodes = node.getChildNodes();
		for (int i=0; i<stateNodes.getLength(); ++i) {
			Node nextNode = stateNodes.item(i);
			if ((nextNode.getNodeType() == Node.ELEMENT_NODE) && (nextNode.getNodeName() == "State")) {
				State nextState = State.load(nextNode);
				if (nextState != null) {
					loaded.states.put(nextState.name, nextState);
				}
			}
		}
		// Find start state.  Add end and fail states.	
		loaded.currentState = loaded.states.get(START_STATE_NAME);
		if (loaded.currentState == null) {
			throw new RuntimeException("Could not parse scenario.  No start state defined.");
		}
		if (loaded.states.get(END_STATE_NAME) == null) {
			loaded.states.put(END_STATE_NAME, new State(END_STATE_NAME));
		}
		if (loaded.states.get(ERROR_STATE_NAME) == null) {
			loaded.states.put(ERROR_STATE_NAME, new State(ERROR_STATE_NAME));
		}

		return loaded;
	}

	private static State loadState(Node node) {
		String stateName = node.getAttributes().getNamedItem("name").getNodeValue();
		State loaded = new State(stateName);
		NodeList transxNodes = node.getChildNodes();
		for (int i=0; i<transxNodes.getLength(); ++i) {
			Node nextNode = transxNodes.item(i);
			if (nextNode.getNodeType() == Node.ELEMENT_NODE) {
				if (nextNode.getNodeName() == "Transition") {
					Transition nextTransx = Transition.load(nextNode);
					if (nextTransx != null) {
						loaded.transitions.add(nextTransx);
					}
				} else {
					throw new RuntimeException("Unknown tag <" + nextNode.getNodeName() + "> found in <State> tag.");
				}
			}
		}
		return loaded;
	}
	
	/**
	 * Read an XML configuration for a transition and create the Transition described.
	 */
	private static Transition loadTransition(Node node) {
		Transition loaded = new Transition();
		loaded.toState = node.getAttributes().getNamedItem("to").getNodeValue();
		NodeList subnodes = node.getChildNodes();
		for (int i=0; i<subnodes.getLength(); ++i) {
			Node nextNode = subnodes.item(i);
			if (nextNode.getNodeType() == Node.ELEMENT_NODE) {
				if (nextNode.getNodeName() == "Trigger") {
					if (loaded.trigger != null) {
						throw new RuntimeException("Error.  Found multiple <Trigger> tags in <Transition> tag.");
					}
					loaded.trigger = (Trigger)loadObject(nextNode);
				} else if  (nextNode.getNodeName() == "Action") {
					Action nextAction = (Action)loadObject(nextNode);
					loaded.actions.add(nextAction);
				} else {
					throw new RuntimeException("Unknown tag <" + nextNode.getNodeName() + "> found in <State> tag.");
				}
			}
		}
		return loaded;
	}
	
	/**
	 * In the Scenarion XML configuration, both the <Action> and <Trigger> tags must support polymorphic subclasses,
	 * so a "type" attribute specifies the class of the object to load.  This uses reflection to load the class. 
	 * @param node the XML configuration of the object.  Expects a "type" attribute to specify the fully qualified name 
	 * of the Java class to load (though if in the mdexplorer package, then it can just be the class name).  Everything
	 * in the content of the node is handed to the derived class to parse.
	 */
	private static Prototype loadObject(Node node) {
		Prototype loaded = null;
		String className = node.getAttributes().getNamedItem("type").getNodeValue();
		if (!className.contains(".")) {
			className = Scenario.class.getPackage().getName() + "." + className;
		}
		
		try {
			Class<?> desiredClass = Class.forName(className);
			Prototype prototype = (Prototype)desiredClass.newInstance();
			loaded = prototype.load(node);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return loaded;
	}
	
}
