package mdexplorer;

public interface Action extends Scenario.Prototype {

	/**
	 * Perform this action
	 */
	public void execute(Robot r, String user);
}
