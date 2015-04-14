package mdexplorer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	/** The time when the MUD is next scheduled to reboot.  Negative number means unknown. */
	private long rebootTime = -1;
	
	/** Timestamp of last command.  Used to throttle commands. */
	private long timeOfLastCommand; 
	
	/** Whether we have asked the server to disconnect, like with a 'quit' command. */
	private boolean expectingDisconnect = false;
	
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
	 * Unregister a listener.  Will no longer receive text from the MUD.
	 * @param listener the listener to unregister
	 */
	public void stopListening(Listener listener) {
		listeners.remove(listener);
	}
	
	/** 
	 * Connect a user to the MUD
	 * @param username name of user 
	 * @param password password of user
	 */
	public void connect(String username, String password) throws IOException {
		expectingDisconnect = username.trim().isEmpty();
		// Reset reboot time if its from an ended MUD session
		if (rebootTime < System.currentTimeMillis()) {
			rebootTime = -1;
		}
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
		// TBD: Recognize a 'quit' command so we can expect a disconnect
		long now = System.currentTimeMillis();
		long sinceLast = now-timeOfLastCommand;
		long THROTTLE = 200;
		if (sinceLast < THROTTLE) {
			// Throttle commands so we don't
			// exceed the 10 commands per second limit
			try {
				Thread.sleep(THROTTLE-sinceLast);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		
		if (command.trim().equals("quit")) {
			expectingDisconnect = true;
		}
		mudInput.println(command);
		timeOfLastCommand = System.currentTimeMillis();;
	}
	
	/**
	 * Block this thread until the MUD disconnects
	 * @throws InterruptedException 
	 */
	public synchronized void waitFor() throws InterruptedException {
		while (listenThread != null) {
			this.wait();
		}
	}
	
	/**
	 * Log the user out of the MUD and close the connection
	 * @param user the user to log out
	 */
	public void disconnect(String user) {
		doDisconnect(user, true);
	}
	
	public long getRebootTime() {
		return (rebootTime < System.currentTimeMillis() ? -1 : rebootTime);
	}
	
	private void startListening(final String username, final InputStream mudOutput) {
		final MUD thisMud = this;
		Runnable broadcastOutput = new Runnable() {
			public void run() {
				byte[] buffer = new byte[4096];
				int numBytes = 0;
				while (!Thread.interrupted()) {
					
					try {
						numBytes = mudOutput.read(buffer);
					}
					catch (IOException e) {
						System.err.println("Exception encountered reading " + username + "'s MUD session.");
						e.printStackTrace();
						thisMud.listenThread.interrupt();
					}
					if (numBytes < 0) {
						if (!expectingDisconnect) {
							System.err.println("Server terminated " + username + "'s MUD session.");
						}
						thisMud.listenThread.interrupt();
					}
					else {
						String nextOutput = new String(buffer, 0, numBytes);
						if (rebootTime < 0) {
							parseRebootTime(nextOutput);
						}
						for(Listener nextListener: listeners) {
							try {
								nextListener.newText(username, nextOutput);
							}
							catch (ArithmeticException e) {
								// Log it and keep going
								e.printStackTrace();
							}
						}
					}
				}
				// Thread has been interrupted.  Disconnect.
				doDisconnect(username, expectingDisconnect);
			}
		};
		listenThread = new Thread(broadcastOutput, "MUD Output Listen Thread");
		listenThread.start();
	}
	
	private void doDisconnect(String user, boolean expected) {
		listenThread = null;
		try {
			socket.close();
		} catch (IOException e) {
			// Don't care.  We're closing it out.
		} catch (RuntimeException e) {
			// Don't care.  We're closing it out.
		}
		socket = null;
		for(Listener nextListener: listeners) {
			try {
				nextListener.disconnected(user, expected);
			}
			catch (Exception e) {
				// Log it and keep going
				e.printStackTrace();
			}
		}
		synchronized(this) {
			this.notify();
		}
	}
	
	private void parseRebootTime(String mudOutput) {
		String regex = "Next reboot: [^\\n]*(\\d\\d?)h (\\d\\d?)m (\\d\\d?)s";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(mudOutput);
		if (matcher.find()) {
			long time = System.currentTimeMillis();
			String hourStr = matcher.group(1);
			time += Long.parseLong(hourStr) * 60 * 60 * 1000;
			String minuteStr = matcher.group(2);
			time += Long.parseLong(minuteStr) * 60 * 1000;
			String secondStr = matcher.group(3);
			time += Long.parseLong(secondStr) * 1000;
			
			rebootTime = time;
		}
	}
}