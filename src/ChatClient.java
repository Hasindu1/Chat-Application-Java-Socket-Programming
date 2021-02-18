
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A simple Swing-based client for the chat server. Graphically it is a frame
 * with a text field for entering messages and a textarea to see the whole
 * dialog.
 *
 * The client follows the Chat Protocol which is as follows. When the server
 * sends "SUBMITNAME" the client replies with the desired screen name. The
 * server will keep sending "SUBMITNAME" requests as long as the client submits
 * screen names that are already in use. When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start sending the server
 * arbitrary strings to be broadcast to all chatters connected to the server.
 * When the server sends a line beginning with "MESSAGE " then all characters
 * following this string should be displayed in its message area.
 */
//IT18153828 H.Y.D.Dahanayake
public class ChatClient {

	BufferedReader in;
	PrintWriter out;

	JFrame frame = new JFrame("Chatter");
	JTextField textField = new JTextField(40);
	JTextArea messageArea = new JTextArea(8, 40);
	JButton button;
	// TODO: Add a list box

	JList jList;
	DefaultListModel<String> dm = new DefaultListModel<String>();

	List<String> multicastUsers;

	/**
	 * Constructs the client by laying out the GUI and registering a listener with
	 * the textfield so that pressing Return in the listener sends the textfield
	 * contents to the server. Note however that the textfield is initially NOT
	 * editable, and only becomes editable AFTER the client receives the
	 * NAMEACCEPTED message from the server.
	 */
	public ChatClient() {

		// Layout GUI
		textField.setEditable(false);
		messageArea.setEditable(false);
		jList = new JList();
		jList.setModel(dm);

		// To confirm the selection of multicast users
		button = new JButton("Get Selected multicast users");

		// To store the list of multicast users
		multicastUsers = new ArrayList<String>();

		getSelected();
		frame.getContentPane().add(textField, "North");
		frame.getContentPane().add(new JScrollPane(messageArea), "Center");
		frame.getContentPane().add(jList, BorderLayout.EAST);
		button.setSize(100, 100);
		frame.getContentPane().add(button, BorderLayout.PAGE_END);
		frame.pack();

		// handling point to point
		// messaging,
		// where one client can send a message to a specific client. You can add some
		// header to
		// the message to identify the recipient. You can get the receipient name from
		// the listbox.
		textField.addActionListener(new ActionListener() {
			/**
			 * Responds to pressing the enter key in the textfield by sending the contents
			 * of the text field to the server. Then clear the text area in preparation for
			 * the next message.
			 */
			public void actionPerformed(ActionEvent e) {
				if (multicastUsers.size() == 0) {
					out.println(textField.getText());
					textField.setText("");
					// Selecting the multicast messages
				} else {
					out.println("Multi" + textField.getText());
					textField.setText("");

				}
			}
		});

	}

	/**
	 * Prompt for and return the address of the server.
	 */
	private String getServerAddress() {
		return JOptionPane.showInputDialog(frame, "Enter IP Address of the Server:", "Welcome to the Chatter",
				JOptionPane.QUESTION_MESSAGE);
	}

	/**
	 * Prompt for and return the desired screen name.
	 */
	private String getName() {
		return JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
				JOptionPane.PLAIN_MESSAGE);
	}

	private void getSelected() {
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Object[] values = jList.getSelectedValues();
				for (int i = 0; i < values.length; i++) {
					System.out.println("indices" + values[i]);
					multicastUsers.add(values[i].toString());
				}
			}
		});
	}

	/**
	 * Connects to the server then enters the processing loop.
	 */
	private void run() throws IOException {

		// Make connection and initialize streams
		String serverAddress = getServerAddress();
		Socket socket = new Socket(serverAddress, 9001);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);

		// Process all messages from server, according to the protocol.

	
		while (true) {

			String line = in.readLine();

			if (line.startsWith("SUBMITNAME")) {

				out.println(getName());

			} else if (line.startsWith("NAMEACCEPTED")) {

				textField.setEditable(true);

			}
			// printing all the logged in clients
			else if (line.startsWith("USERS")) {

				// adding a client for the jlist for the first time
				if (jList.getModel().getSize() == 0) {

					dm.addElement(line.substring(5));

				} else {

					boolean invalid = false;
					for (int i = 0; i < jList.getModel().getSize(); i++) {
						System.out.println("Size " + jList.getSize());
						System.out.println("Element in jList" + jList.getModel().getElementAt(i));
						if ((jList.getModel().getElementAt(i).equals(line.substring(5)))) {
							System.out.println("Adding eleemt " + line.substring(5));

							invalid = true;

						}
					}
					if (!invalid) {
						dm.addElement(line.substring(5));

					}
				}
				// Displaying the broadcast message
			} else if (line.startsWith("MESSAGE")) {

				messageArea.append(line.substring(8) + "\n");
				// displaying the point to point message
			} else if (line.startsWith("PointToPoint")) {

				messageArea.append(line.substring(12) + "\n");
				// sending the list of multicast users for the server
			} else if (line.startsWith("Multicast") && multicastUsers.size() > 0) {

				for (String tempUser : multicastUsers) {
					out.println(tempUser);
				}
				// To denote end of the multicast users list
				out.println("END");
				// displaying the multi cast messages
			} else if (line.startsWith("MultiCastMSG")) {

				messageArea.append(line.substring(12) + "\n");
				multicastUsers.clear();
				// removing the logged off user from the jLsit
			} else if (line.startsWith("RemoveUsers")) {
				for (int i = 0; i < jList.getModel().getSize(); i++) {
					if ((jList.getModel().getElementAt(i).equals(line.substring(11)))) {
						dm.removeElement(line.substring(11));
					}
				}
			}

		}

	}

	/**
	 * Runs the client as an application with a closeable frame.
	 */
	public static void main(String[] args) throws Exception {
		ChatClient client = new ChatClient();
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setVisible(true);
		client.run();
	}
}
