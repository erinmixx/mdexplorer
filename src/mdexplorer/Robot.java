package mdexplorer;

/**
 * Executes a scenario
 */
public class Robot implements MUD.Listener {
	
	private MUD mud;
	
	private Scenario currentScenario;
	
	public Robot(MUD mud) {
		this.mud = mud;
	}
	
	public MUD getMud() {
		return mud;
	}
	
	public void executeScenario(Scenario s) {
		currentScenario = s;
		System.out.println("XXXXXXXXXXXXXXXXXXX Executing scenario " + s.getName());
		System.out.println("XXXXXXXXXXXXXXXXXXX Starting in state " + s.getState());
		mud.listen(this);
	}
	
	@Override
	public void connected(String user) {
		// Don't care
	}
	
	@Override
	public void newText(String user, String lineOfText) {
		if (currentScenario != null) {
			currentScenario.processInput(this, user, lineOfText);			
			if (currentScenario.isDone()) {
				currentScenario = null;
			}
		}
	}
	
	@Override
	public void disconnected(String user, boolean expected) {
		// Don't care
		
	}

}
