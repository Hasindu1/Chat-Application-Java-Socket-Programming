
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * A multithreaded chat room server. When a client connects the server requests
 * a screen name by sending the client the text "SUBMITNAME", and keeps
 * requesting a name until a unique one is received. After a client submits a
 * unique name, the server acknowledges with "NAMEACCEPTED". Then all messages
 * from that client will be broadcast to all other clients that have submitted a
 * unique screen name. The broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple chat server,
 * there are a few features that have been left out. Two are very useful and
 * belong in production code:
 *
 * 1. The protocol should be enhanced so that the client can send clean
 * disconnect messages to the server.
 *
 * 2. The server should do some logging.
 */
//IT18153828 H.Y.D.Dahanayake
public class ChatServer {

	/**
	 * The port that the server listens on.
	 */
	private static final int PORT = 9001;

	/**
	 * The set of all names of clients in the chat room. Maintained so that we can
	 * check that new clients are not registering name already in use.
	 */

	private static HashSet<String> names = new HashSet<String>();

	/**
	 * The set of all the print writers for all the clients. This set is kept so we
	 * can easily broadcast messages.
	 */
	private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

	/**
	 * The appplication main method, which just listens on a port and spawns handler
	 * threads.
	 */
	// To store the pointtoPoint user names with their sockets
	private static HashMap<String, Socket> pointTopoint = new HashMap<String, Socket>();

	// To store the names of multicast users

	private static HashSet<String> multiCast = new HashSet<String>();

	public static void main(String[] args) throws Exception {
		System.out.println("The chat server is running.");
		ServerSocket listener = new ServerSocket(PORT);
		try {
			while (true) {
				System.out.println("Thread is creating");

				Socket socket = listener.accept();
				System.out.println("socket is " + socket);
				System.out.println("got the socket");
				// Putting the relevant client socket
				Thread handlerThread = new Thread(new Handler(socket));
				handlerThread.start();
			}
		} finally {
			listener.close();
		}
	}

	/**
	 * A handler thread class. Handlers are spawned from the listening loop and are
	 * responsible for a dealing with a single client and broadcasting its messages.
	 */
	private static class Handler implements Runnable {
		private String name;
		private Socket socket;
		private BufferedReader in;
		private PrintWriter out;

		/**
		 * Constructs a handler thread, squirreling away the socket. All the interesting
		 * work is done in the run method.
		 */
		public Handler(Socket socket) {
			this.socket = socket;
		}

		/**
		 * Services this thread's client by repeatedly requesting a screen name until a
		 * unique one has been submitted, then acknowledges the name and registers the
		 * output stream for the client in a global set, then repeatedly gets inputs and
		 * broadcasts them.
		 */
		public void run() {
			try {

				// Create character streams for the socket.
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);

				// Request a name from this client. Keep requesting until
				// a name is submitted that is not already used. Note that
				// checking for the existence of a name and adding the name
				// must be done while locking the set of names.

				while (true) {
					System.out.println("came!!!");
					out.println("SUBMITNAME");
					name = in.readLine();
					if (name == null) {
						System.out.println("returned");
						return;
					}

					'
					synchronized (ChatServer.class) {
						if (!names.contains(name)) {
							names.add(name);
							break;
						}
					}
				}

				// Now that a successful name has been chosen, add the
				// socket's print writer to the set of all writers so
				// this client can receive broadcast messages.
				out.println("NAMEACCEPTED");
				synchronized (ChatServer.class) {
					writers.add(out);
					pointTopoint.put(name, socket);
				}


				// Accept messages from this client and broadcast them.
				// Ignore other clients that cannot be broadcasted to.

				while (true) {
					// Sending the logged in user names to client side jList(To all clients)
					for (PrintWriter writer : writers) {
						for (String temp : names) {
							synchronized (ChatServer.class) {

								writer.println("USERS" + temp);

							}

						}

					}

					String input = in.readLine();

					if (input == null) {
						return;

					}
					// sending the broadcast message to all clients
					if (!((input.contains(">>")) || input.startsWith("Multi"))) {

						for (PrintWriter writer : writers) {
							writer.println("MESSAGE " + name + ": " + input);
						}
					}

					// sending a mesage to a specific client and not
					// all clients. You may have to use a HashMap to store the sockets along
					// with the chat client names

					// Sending the point to point message to the sender and receiver
					if (input.contains(">>")) {
						String totString = input;
						String inputPointToPoint = input.substring(0, input.indexOf('>'));

						Iterator<Map.Entry<String, Socket>> entrySet = pointTopoint.entrySet().iterator();

						while (entrySet.hasNext()) {
							Map.Entry<String, Socket> entry = entrySet.next();

							if (entry.getKey().equals(inputPointToPoint)) {

								Socket socket = entry.getValue();
								Socket socketSender = pointTopoint.get(name);

								PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
								// sending to the relevant receiver
								out.println("PointToPoint" + totString);
								// sending to the sender
								PrintWriter outSender = new PrintWriter(socketSender.getOutputStream(), true);
								outSender.println("PointToPoint" + totString);

							}
						}

					}
					// Multicast messaging
					if (input.startsWith("Multi")) {

						out.println("Multicast");

						while (true) {
							String input2 = in.readLine();
							// Adding the multicast users to the Hash set
							if (!(input2.equals("END"))) {
								synchronized (ChatServer.class) {
									multiCast.add(input2);
								}

							}
							// sending the multicast message to relevant parties
							if (input2.equals("END")) {
								try {
									Iterator<String> itr = multiCast.iterator();
									while (itr.hasNext()) {
										String user = itr.next();
										Socket socket = pointTopoint.get(user);
										if ((socket != null)) {

											PrintWriter outmultiCast = new PrintWriter(socket.getOutputStream(), true);

											outmultiCast.println("MultiCastMSG" + name + ":" + input.substring(5));

										}

									}
									PrintWriter sender = new PrintWriter(pointTopoint.get(name).getOutputStream(),
											true);
									sender.println("MultiCastMSG" + name + ":" + input.substring(5));
									multiCast.clear();
								} catch (Exception e) {
									e.printStackTrace();
								}
								break;
							}
						}
					}

				}

			}
			// Handling the SocketException here to handle a client closing the socket
			catch (SocketException e) {
				System.out.println(name + "is going down");

			} catch (IOException e) {
				System.out.println(e);
			} finally {
				// This client is going down! Remove its name and its print
				// writer from the sets, and close its socket.

				// Removing the user from other clients JLists
				for (PrintWriter writer : writers) {
					synchronized (ChatServer.class) {
						writer.println("RemoveUsers" + name);

					}

				}
				if (name != null) {
					names.remove(name);
				}

				if (out != null) {
					writers.remove(out);
				}

				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
