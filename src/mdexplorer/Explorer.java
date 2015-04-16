package mdexplorer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Explorer {
	
	private static class Job {
		public String user;
		public String password;
		public String scenario;
		public boolean repeat;
		public int delay; // in minutes
	}
	
	private Properties props;
	
	/** A map of known user names to their passwords.  */
	private Map<String, String> users;
	
	/** A list of scenario definitions indexed by their name */
	private Map<String, Node> scenarios;
	
	private List<Job> jobs;
	
	public Explorer(Properties props) {
		this.props = props;
		this.users = parseUsers(props);
		this.scenarios = loadScenarios(props.getProperty("mdexplorer.scenario_files"));
	}
	
	public void go() throws IOException {
		String server = props.getProperty("mdexplorer.server");
		String portStr = props.getProperty("mdexplorer.port");
		int port = Integer.parseInt(portStr);
		
		final MUD mud = new MUD(server, port);

		// TBD: Load the jobs from somewhere like command-line or setup file
		jobs = new ArrayList<Job>();
		Job job = new Job();
		job.user = users.keySet().iterator().next();
		job.password = users.get(job.user);
		job.scenario = "get money";
		job.repeat = true;
		job.delay = -2;
		jobs.add(job);
		
		
		new UserClient(mud, true);

		while (!Thread.interrupted()) {
			long nextReboot = getNextReboot(mud);
			System.out.println("Next reboot in " + new Date(nextReboot));
			
			// TBD Assuming one job and that it is to execute at the end.
			final Job toRun = jobs.iterator().next();
			final Robot robot = new Robot(mud);
			final Scenario scenario = Scenario.load(scenarios.get(toRun.scenario));
			long waitTime = (toRun.delay > 0 ? toRun.delay : nextReboot - System.currentTimeMillis() + (toRun.delay*60000));
			System.out.println("XXXXXXXXXXXXXXXXXXX Waiting " + (waitTime/60000) + " minutes until right before MUD reboots");
			try {
				Thread.sleep(waitTime);
				mud.connect(toRun.user, toRun.password);
				robot.executeScenario(scenario);
				mud.waitFor();
			} catch (InterruptedException e) {
				System.err.println("Execution interrupted.");
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
			
			// Then we wait for the MUD to shut down, wait 10 minutes for it to start up, and try again.

			waitTime = nextReboot + 600000 - System.currentTimeMillis();
			System.out.println("XXXXXXXXXXXXXXXXXXX Waiting " + (waitTime/60000.0) + " minutes until MUD is rebooted");
			try {
				Thread.sleep(waitTime);
			} catch (InterruptedException e) {
				System.err.println("Execution interrupted.");
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
	}
	
	public void test() throws IOException {
		String server = props.getProperty("mdexplorer.server");
		String portStr = props.getProperty("mdexplorer.port");
		int port = Integer.parseInt(portStr);

		final MUD mud = new MUD(server, port);
		UserClient client = new UserClient(mud, true);
		client.connect();
		
		System.out.println("Ready...");
		System.in.read();
		String user = users.keySet().iterator().next();
		String password = users.get(user);
		mud.connect(user, password);
		final Robot robot = new Robot(mud);
		final Scenario scenario = Scenario.load(scenarios.get("get money"));
		robot.executeScenario(scenario);
		try {
			mud.waitFor();
			System.out.println("Scenario done");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
	}
	
	/**
	 * Read in all the known users from the configuration.
	 * @return a map of known user names to their passwords
	 */
	private Map<String, String> parseUsers(Properties userProps) {
		// TBD: read multiple users in.  Right now just reading one in.
		Map<String, String> parsed = new HashMap<String,String>();
		String user = userProps.getProperty("mdexplorer.username");
		String password = userProps.getProperty("mdexplorer.password");	
		parsed.put(user,  password);
		return parsed;
	}
	
	private Map<String, Node> loadScenarios(String scenarioList) {
		Map<String, Node> loaded = new HashMap<String,Node>();
		String[] scenarioFiles = scenarioList.trim().split(",");
		for(String nextFile: scenarioFiles) {

			Scenario scenario = null;
			try {
			    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			    DocumentBuilder builder = factory.newDocumentBuilder();
				InputStream strm = Explorer.class.getClassLoader().getResourceAsStream(nextFile);
			    Document document = builder.parse(strm);
			    Node rootNode = document.getDocumentElement();
			    // TBD: Handle multiple scenarios in one file
			    // Load in the scenario to assert it is a valid scenario description
				scenario = Scenario.load(rootNode);
				loaded.put(scenario.getName(),  rootNode);
			} catch (SAXException e) {
				throw new RuntimeException("Couldn't read XML file", e);
			} catch (ParserConfigurationException e) {
				throw new RuntimeException("Couldn't read XML file", e);
			} catch (IOException e) {
				throw new RuntimeException("Couldn't read XML file", e);
			}

		}
		
		return loaded;
	}
	
	/**
	 * Connect to the MUD with the sole purpose of reading the next reboot time.
	 * @param mud
	 * @param user
	 * @param password
	 * @return
	 */
	public long getNextReboot(MUD mud) {
		long nextReboot = -1;
		// Get the next reboot time
		try {
			mud.connect("", "");
			mud.waitFor();
			nextReboot = mud.getRebootTime();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return nextReboot;
	}

	public static void main(String[] args) throws IOException {
		 InputStream strm = Explorer.class.getClassLoader().getResourceAsStream("settings.props");
		 Properties props = new Properties();
		 props.load(strm);
		 Explorer explorer = new Explorer(props);
		 explorer.test();
	}

}
