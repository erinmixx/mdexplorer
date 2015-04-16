package mdexplorer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import mdexplorer.MUD.Listener;

public class UserClient implements MUD.Listener {
	
	/**
	 * This acts as a user client, but runs as a completely separate program that talks with the MUD Explorer via sockets.
	 */
	public static class RemoteClient {

		public static final int PORT = 7453;
		
		public Thread keyboardListenThread;
		
		public RemoteClient() {
		}
		
		public void connect() {
			while (!Thread.currentThread().isInterrupted()) {
				Socket socket = null;
				try {
					socket = new Socket("localhost", PORT);
					System.out.println("Connected...");
					// Need a separate thread for piping keyboard input to the MUD.
					listenOnKeyboard(socket.getOutputStream());
					byte[] buffer = new byte[4096];
					int numBytes = 0;
					while (!Thread.currentThread().isInterrupted()) {
						InputStream istrm = socket.getInputStream();
						numBytes = istrm.read(buffer);
						if (numBytes < 0) {
							throw new IOException("Error reading from standard in.  Unexpected end of input.");
						}
						System.out.write(buffer, 0, numBytes);
						System.out.flush();
					}
				} catch (IOException e) {
					// Shutdown listen thread before starting over
					if (socket != null) {
						try {
							socket.close();
						} catch (IOException e1) {
							// We're doing a panic shutdown.  Ignore errors.
						}
					}
					if (keyboardListenThread != null) {
						keyboardListenThread.interrupt();
						keyboardListenThread = null;
					}
				}
			}
		}
		
		private void listenOnKeyboard(final OutputStream ostrm) {
			if (keyboardListenThread != null) {
				keyboardListenThread.interrupt();
				keyboardListenThread = null;
			}
			keyboardListenThread = new Thread("Keyboard Listener Thread") {
				public void run() {
					doListenOnKeyboard(ostrm);
				}
			};
			keyboardListenThread.start();
		}
		
		private void doListenOnKeyboard(OutputStream ostrm) {
			byte[] buffer = new byte[4096];
			int numBytes = 0;
			while (!Thread.currentThread().isInterrupted()) {
				try {
					numBytes = System.in.read(buffer);
				}
				catch (IOException e) {
					throw new RuntimeException("Error reading from standard in");
				}
				try {
					if (numBytes < 0) {
						throw new RuntimeException("Error reading from standard in");
					}
					else {
						ostrm.write(buffer, 0, numBytes);
					}
				}
				catch (IOException e) {
					// The connection to the MUD Explorer was lost.
					System.err.println("ERROR: Lost connectin to MUD Explorer.");
					// TBD: How to reconnect
				}
			}

		}
	}


	private MUD mud;
	private boolean remote;
	private ServerSocket ssocket = null;
	private Socket socket = null;
	private OutputStream ostrm = null;
	private InputStream istrm = null;
	
	// UserClient has its own thread that listens on System.in.
	private Thread inputThread = null;
	
	public UserClient(MUD mud, boolean remote) {
		this.mud = mud;
		this.remote = remote;
	}
	
	public void connect() {
		if (this.remote) {
			checkSocket(false);
		} else {
			ostrm = System.out;
			istrm = System.in;
		}
		mud.listen(this);
	}
	
	private boolean checkSocket() {
		return checkSocket(true);
	}
	
	private boolean checkSocket(boolean isReconnect) {
		if (remote && (socket == null)) {
			try {
				ssocket = new ServerSocket(RemoteClient.PORT);
				ssocket.setSoTimeout(5000);
				socket = ssocket.accept();
				ostrm = socket.getOutputStream();
				istrm = socket.getInputStream();
				if (isReconnect) {
					ostrm.write("RECONNECT\n".getBytes());
					ostrm.flush();
				}
			} catch (IOException e) {
				// We ignore all errors
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e1) {
						// Ignore
					}
				}
				socket = null;
			}	
		}
		return (!remote || (socket != null));
	}
	
	@Override
	public void connected(String user) {
		// TBD: Only supports one user right now
		startInput(user);
	}

	@Override
	public void newText(String user, String lineOfText) {
		if (checkSocket()) {
			try {
				ostrm.write("----------------------------------------------------\n".getBytes());
				ostrm.write(lineOfText.getBytes());
				ostrm.write("\n".getBytes());
				ostrm.flush();
			} catch (IOException e) {
				// Reset the socket so we try to connect again.
				socket = null;
			}
		}
	}

	@Override
	public void disconnected(String user, boolean expected) {
		// TBD: Only supports one user right now.
		inputThread.interrupt();
	}
	
	private synchronized void startInput(final String username) {
		Runnable broadcastOutput = new Runnable() {
			public void run() {
				byte[] buffer = new byte[4096];
				int numBytes = 0;
				while (!Thread.currentThread().isInterrupted()) {
					if (checkSocket()) {
						try {
							numBytes = istrm.read(buffer);
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
			}
		};
		inputThread = new Thread(broadcastOutput, "Client Input Listen Thread");
		inputThread.start();
	}
	
	public static void main(String args[]) {
		RemoteClient client = new RemoteClient();
		client.connect();
	}

}
