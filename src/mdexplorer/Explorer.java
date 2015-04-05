package mdexplorer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
	}

	public static void main(String[] args) throws IOException {
		 InputStream strm = Explorer.class.getClassLoader().getResourceAsStream("settings.props");
		 Properties props = new Properties();
		 props.load(strm);
		 Explorer explorer = new Explorer(props);
		 explorer.go();
	}

}
