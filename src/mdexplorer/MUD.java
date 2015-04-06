package mdexplorer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MUD {

	/**
	 * The delegate interface used to feed MUD output to any Java class.
	 */
	public interface Listener {
		
		/**
		 * Notify that a user is now connected.
		 * @param user the user connected
		 */
		void connected(String user);
		
		/**
		 * Called whenever a new line of text is received from the MUD.
		 * @param user the user seeing this text (there could be multiple logged in)
		 * @param lineOfText the next line of text.  Will be an entire line
		 * terminated by a newline
		 */
		void newText(String user, String lineOfText);
		
		/**
		 * Called when the connection to the MUD is closed.
		 * @param user the user who got disconnected (there could be multiple logged in)
		 * @param expected whether the disconnect was expected or unexpected
		 */
		void disconnected(String user, boolean expected);
	}
	
	private String server;
	private int port;
	private Socket socket;
	private List<Listener> listeners = new ArrayList<Listener>();
	private PrintStream mudInput;
	private Thread listenThread;
	
	/**
	 * Create an interface to a MUD on the internet
	 * @param server the server name of the mud
	 * @param port the port of the mud
	 */
	public MUD(String server, int port) {
		this.server = server;
		this.port = port;
	}
	
	/**
	 * Register a listener to receive text from the MUD.
	 * If called more than once with the same listener, duplicate registrations are ignored.
	 */
	public void listen(Listener newListener) {
		if (!listeners.contains(newListener)) {
			listeners.add(newListener);
		}
	}
	
	/** 
	 * Connect a user to the MUD
	 * @param username name of user 
	 * @param password password of user
	 */
	public void connect(String username, String password) throws IOException {
		// TBD: Only supports one user right now
		if (socket != null) {
			throw new RuntimeException("Only supports one user");
		}
		socket = new Socket(server, port);
		// TBD: Be a little more reactive to what the MUD is reporting
		startListening(username, socket.getInputStream());
		mudInput = new PrintStream(socket.getOutputStream());
		mudInput.println(username);
		mudInput.println(password);
		for(Listener nextListener: listeners) {
			nextListener.connected(username);
		}
	}

	/**
	 * Have the user currently logged in execute a command in the MUD
	 * @param command the command to execute
	 */
	public void send(String user, String command) {
		mudInput.println(command);
	}
	
	/**
	 * Log the user out of the MUD and close the connection
	 * @param user the user to log out
	 */
	public void disconnect(String user) {
		// TBD
	}
	
	private void startListening(final String username, final InputStream mudOutput) {
		Runnable broadcastOutput = new Runnable() {
			public void run() {
				byte[] buffer = new byte[4096];
				int numBytes = 0;
				while (!Thread.interrupted()) {
					
					try {
						numBytes = mudOutput.read(buffer);
					}
					catch (IOException e) {
						// TBD: Register disconnect
					}
					if (numBytes < 0) {
						// TBD: Register disconnect
					}
					else {
						String nextOutput = new String(buffer, 0, numBytes);
						for(Listener nextListener: listeners) {
							try {
								nextListener.newText(username, nextOutput);
							}
							catch (Exception e) {
								// Log it and keep going
								e.printStackTrace();
							}
						}
					}
				}
			}
		};
		listenThread = new Thread(broadcastOutput, "MUD Output Listen Thread");
		listenThread.start();
	}
}