package mdexplorer;

public interface Trigger extends Scenario.Prototype {
	
	public boolean triggeredBy(String input);
}
