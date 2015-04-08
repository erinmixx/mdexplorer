package mdexplorer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
	
	private Properties props;
	
	public Explorer(Properties props) {
		this.props = props;
	}
	
	public void go() throws IOException {
		String server = props.getProperty("mdexplorer.server");
		String portStr = props.getProperty("mdexplorer.port");
		int port = Integer.parseInt(portStr);
		
		MUD mud = new MUD(server, port);
		new UserClient(mud);
		
		String user = props.getProperty("mdexplorer.username");
		String password = props.getProperty("mdexplorer.password");
		
		mud.connect(user, password);
		
		Robot robot = new Robot(mud);
		
		Scenario scenario = null;
		try {
		    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream strm = Explorer.class.getClassLoader().getResourceAsStream("playRecordScenario.xml");
		    Document document = builder.parse(strm);
		    Node rootNode = document.getDocumentElement();
			scenario = Scenario.load(rootNode);
		} catch (SAXException e) {
			throw new RuntimeException("Couldn't read XML file", e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Couldn't read XML file", e);
		}

		 robot.executeScenario(scenario);
	}

	public static void main(String[] args) throws IOException {
		 InputStream strm = Explorer.class.getClassLoader().getResourceAsStream("settings.props");
		 Properties props = new Properties();
		 props.load(strm);
		 Explorer explorer = new Explorer(props);
		 explorer.go();
	}

}
