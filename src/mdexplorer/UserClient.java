package mdexplorer;

import java.io.IOException;

import mdexplorer.MUD.Listener;

public class UserClient implements MUD.Listener {

	private MUD mud;
	
	// UserClient has its own thread that listens on System.in.
	private Thread inputThread = null;
	
	public UserClient(MUD mud) {
		this.mud = mud;
		mud.listen(this);
	}
	
	@Override
	public void connected(String user) {
		// TBD: Only supports one user right now
		startInput(user);
	}

	@Override
	public void newText(String user, String lineOfText) {
		System.out.println(lineOfText);
	}

	@Override
	public void disconnected(String user, boolean expected) {
		System.exit(0);
	}
	
	private synchronized void startInput(String username) {
		Runnable broadcastOutput = new Runnable() {
			public void run() {
				byte[] buffer = new byte[4096];
				int numBytes = 0;
				while (!Thread.interrupted()) {
					try {
						numBytes = System.in.read(buffer);
					}
					catch (IOException e) {
						throw new RuntimeException("Error reading from standard in");
					}
					if (numBytes < 0) {
						throw new RuntimeException("Error reading from standard in");
					}
					else {
						String nextOutput = new String(buffer, 0, numBytes);
						mud.send(username, nextOutput);
					}
				}
			}
		};
		inputThread = new Thread(broadcastOutput, "Client Input Listen Thread");
		inputThread.start();
	}

}
