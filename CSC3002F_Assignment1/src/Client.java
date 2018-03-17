

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
/***
 * An implementation of a Client for a Client-Server Chat Application.
 * @author Pieter Janse van Rensburg(jnspie007@myuct.ac.za)
 * @version 05/04/2017
 * @since 29/03/2017
 *
 */
public class Client {

	// static variables
	private final static int INCOMING_CONNECTION_PORT = 1337;
	private final static String SERVER_NAME = "Server";
	// instance variables
	private String username;
	private String password;
	private ReentrantReadWriteLock onlineClientNamesLock;
	private ArrayList<String> onlineClientNames;
	private ServerInteractionHandler serverConnectionHandler;
	private Scanner input;
	private volatile boolean isConfirming;
	private volatile boolean enteringInput;

	/***
	 * The constructor of the Client Class. Initializes a new Client and establishes a Connection to the Server
	 * @see ReentrantReadWriteLock
	 * @see ArrayList
	 * @see Client#setupConnectToServer(String)
	 * @see Client#inputUserCredentials()
	 */
	public Client() {
		this.onlineClientNamesLock = new ReentrantReadWriteLock();
		this.onlineClientNames = new ArrayList<String>();
		this.isConfirming = false;
		this.enteringInput = true;
		System.out.println("Please enter the IP/DNS address of the Server");
		input = new Scanner(System.in);
		String serverIP = input.nextLine();
		this.setupConnectToServer(serverIP);
		inputUserCredentials();

		}

	/***
	 * A method use to get the Client's user name which is unique to themselves.
	 * @return The String of the Client's user name
	 */
	public String getUsername() {
		return this.username;
		}

	/***
	 * A method used to get the Thread Managing the Client's interactions with the Server.
	 * @return A ServerInteractionHandler used to manage the Client's interactions with the Server.
	 * @see ServerInteractionHandler
	 */
	public ServerInteractionHandler getServerInteractionHandler() {
		return this.serverConnectionHandler;
		}

	/***
	 * A method used to get the number of names in the Online Client Names ArrayList.
	 * @return An integer indicating the number of names in the Online Client Names ArrayList. Returns -1 if an error occurs.
	 * @see ReentrantReadWriteLock
	 * @see ArrayList
	 */
	public int getOnlineClientNamesSize() {
		int size = -1;
		try{
			// lock the onlineClientNames Array since we are reading from it.
			this.onlineClientNamesLock.readLock().lock();
			size = this.onlineClientNames.size();
		}
		finally {
			// release the lock
			this.onlineClientNamesLock.readLock().unlock();
		}
		return size;
		}

	/***
	 * A method used print out the names of all Online Clients.
	 * @return A String of the Names of all Online Clients seperated by newline characters.
	 * @see ReentrantReadWriteLock
	 * @see ArrayList
	 */
	public String getOnlineClientNamesToString() {
		String temp = "";
		try {
			// lock the onlineClientNames Array since we are reading from it.
			this.onlineClientNamesLock.readLock().lock();
			for(String s: this.onlineClientNames)
				temp += s + "\n";
		}
		finally {
			// release the lock
			this.onlineClientNamesLock.readLock().unlock();
		}
		return temp;
		}

	/***
	 * A method used to update the List of Online Clients' Names.
	 * @param onlineClientNames An ArrayList of the current Online Clients' Names.
	 * @see ReentrantReadWriteLock
	 * @see ArrayList
	 */
	public void setOnlineClientNames(ArrayList<String> onlineClientNames) {
		try {
			// lock online client names since we are writing to the ArrayList
			this.onlineClientNamesLock.writeLock().lock();
			this.onlineClientNames = onlineClientNames;
		}
		finally {
			// release the lock
			this.onlineClientNamesLock.writeLock().unlock();
			}
		}

	/***
	 * A method used to get the Scanner reading System.in
	 * @return A Scanner Object used to read from System.in
	 * @see Scanner
	 * @see System#in
	 */
	public Scanner getInput() {
		return this.input;
		}

	/***
	 * A method used to check if a Client with the given user name is online.
	 * @param onlineClientName The user name of the given Client.
	 * @return A boolean indicating whether the Client with the given user name is online.
	 * @see ReentrantReadWriteLock
	 * @see ArrayList
	 */
	public boolean containsOnlineClientName(String onlineClientName) {
		boolean contains = false;
		try {
			// lock online client's names since we are reading from it.
			this.onlineClientNamesLock.readLock().lock();
			for(String s: this.onlineClientNames)
				if(s.equals(onlineClientName)){
					contains = true;
					break;
					}
		}
		finally {
			// release the lock
			this.onlineClientNamesLock.readLock().unlock();
		}
		return contains;
		}

	/***
	 * A method used by the Client to establish a Connections to the Server via a Socket
	 * It catches an IOException if an error occurs.
	 * @param serverIP The Internet Protocol Address / Domain Name Service Address of the Server to Connect to.
	 * @see Socket
	 * @see ServerInteractionHandler
	 * @see IOException
	 */
	private void setupConnectToServer(String serverIP) {
		try {
			this.serverConnectionHandler = new ServerInteractionHandler(new Socket(serverIP, Client.INCOMING_CONNECTION_PORT));
			System.out.println("*********************************************************************\n"
					+ "System Notice - Client has Succesfully connected to the Server"
					+ "\n*********************************************************************");
			}
		catch (IOException e) {
			System.out.println(e);
			}

		}



	/***
	 * A method used to ask the Client to enter their login credentials and to check if they are correct.
	 * @see Client#checkCredentials()
	 */
	private void inputUserCredentials() {
		this.username = "";
		this.password = "";
		// ask for credentials at least 1 and then keep checking if they are correct.
		do {
			System.out.println("Please enter a Username:");
			this.username = input.nextLine();
			System.out.println("Please enter a Password:");
			this.password = input.nextLine();
			} while(!checkCredentials());

		}

	/***
	 * A method used to sent a Client's Login Details to the Server to see if they are correct.
	 * @return A boolean indicating whether the Client's Login Details were accepted.
	 */
	private boolean checkCredentials() {
		// formulates message to send to server
		Message output = new Message(MessageID.REGISTRATION_REQUEST, this.username, Client.SERVER_NAME, this.password);
		serverConnectionHandler.sendMessageToServer(output);
		// waits for input from server
		Message input = serverConnectionHandler.getMessageFromServer();
		// if details were correct return true.
		if(((boolean)input.getData()))
			return true;
		// else return false
		System.out.println("*********************************************************************\n"
				+ "System Notice - Login Failed: The Client Details entered were incorrect."
				+ "\n*********************************************************************");
		return false;
		}

	/***
	 * A method used to run the Client's ServerInteractionHandler in a new Thread.
	 * @see Thread
	 * @see ServerInteractionHandler
	 */
	public void startServerInteractionHandler() {
		new Thread(this.serverConnectionHandler).start();
		}

	/***
	 * A method used to get the value of isConfirming.
	 * @return a boolean with the value of isConfirming.
	 */
	public boolean getIsConfirming() {
		return this.isConfirming;
		}

	/***
	 * A method used to set the value of isConfirming.
	 * @param isConfirming A boolean with the new value of isConfirming.
	 */
	public void setIsConfirming(boolean isConfirming) {
		while(enteringInput);

		this.isConfirming = isConfirming;
		}

	/***
	 * A method used to get the value of enteringInput.
	 * @return A boolean with the value of enteringInput.
	 */
	public boolean getEnteringInput() {
		return this.enteringInput;
		}

	/***
	 * A method used to set the value of enteringInput.
	 * @param enteringInput A boolean with the new value of enteringInput.
	 */
	public void setEnteringInput(boolean enteringInput) {
		while(isConfirming);

		this.enteringInput = enteringInput;
		}

	/***
	 * The main method of the Client Class.
	 * @param args A String Array containing command-line arguments.
	 */
	public static void main(String args[]) {

		// starts a new Client
		Client thisClient = new Client();
		// starts ServerInteractionHandler in new Thread
		thisClient.startServerInteractionHandler();
		System.out.println("*********************************************************************\n" +
		"System Notice - " + thisClient.getUsername() + ", you have logged in successfully."
				+ "\n*********************************************************************");

		Scanner input = thisClient.getInput();
		String choice = "";
		while(!choice.equals("Exit")) {
			thisClient.setEnteringInput(true);
			System.out.println("Please Enter a number or Exit corresponding to One of the Following Options\n" +
					   "1. Send Text Message to Another Client\n" +
					   "2. Send Image Message to Another Client\n" +
					   "3. Send Text Message to All Online Clients\n" +
					   "4. Send Image Message to All Online Clients\n" +
						 "5. Send Audio file to Another Client........."+
					   "Exit. Logout");
			choice = input.nextLine();
			switch(choice) {
				// sending a text message to another client
				case "1": {
					thisClient.setEnteringInput(false);
					thisClient.getServerInteractionHandler().updateOnlineClients();
					while(!thisClient.getServerInteractionHandler().getUpdated());

					// update the online client list
					thisClient.getServerInteractionHandler().setUpdated(false);
					thisClient.setEnteringInput(true);
					System.out.println("Currently Online Clients(" + thisClient.getOnlineClientNamesSize() + ") :\n"
							+ "-----------------------------------------\n" +
							thisClient.getOnlineClientNamesToString() +
		 "-----------------------------------------\nPlease Enter a Client's name to Send the Message to.");
					// get client user name to send message to
					String receivingClient = input.nextLine();
					thisClient.setEnteringInput(false);
					// check if they are online
					if(thisClient.containsOnlineClientName(receivingClient)) {
						thisClient.setEnteringInput(true);
						System.out.println("Please enter the Text Message to Send");
						// get the message to send
						String message = input.nextLine();
						thisClient.setEnteringInput(false);
						// send the message to the server
						Message output = new Message(MessageID.TEXT_TRANSFER_REQUEST, thisClient.getUsername(),
								receivingClient, message);
						thisClient.getServerInteractionHandler().sendMessageToServer(output);
						}
					else {
						System.out.println("*********************************************************************\n"
								+ "System Notice - The Client whose name has been entered is not online. Going Back to Main Menu."
								+ "\n*********************************************************************");
						}
					break;
					}
				// sending an image message to another client
				case "2": {
					thisClient.setEnteringInput(false);
					thisClient.getServerInteractionHandler().updateOnlineClients();
					while(!thisClient.getServerInteractionHandler().getUpdated());

					// update the online client list
					thisClient.getServerInteractionHandler().setUpdated(false);
					thisClient.setEnteringInput(true);
					System.out.println("Currently Online Clients(" + thisClient.getOnlineClientNamesSize() + ") :\n"
							+ "-----------------------------------------\n" +
							thisClient.getOnlineClientNamesToString() +
		 "-------------------------------------------\nPlease Enter a Client's name to Send the Image to.");
					// get client user name to send message to
					String receivingClient = input.nextLine();
					thisClient.setEnteringInput(false);
					// check if they are online
					if(thisClient.containsOnlineClientName(receivingClient)) {
						boolean loaded = false;
						ImageIcon image = null;
						String displayM = "Please enter the Location of the Image File to Send";
						// load image into ImageIcon
						while(!loaded) {
							try {
								thisClient.setEnteringInput(true);
								System.out.println(displayM);
								String imageURL = input.nextLine();
								thisClient.setEnteringInput(false);
								image = new ImageIcon(ImageIO.read(new File(imageURL)));
								loaded = true;
								}
							catch (IOException e) {
								System.out.println("The Specified Image could not be loaded." + e);
								displayM = "Please re-enter the Location of the Image File to Send";
						}
							}
						// send message to server
						Message output = new Message(MessageID.IMAGE_TRANSFER_REQUEST, thisClient.getUsername(),
								receivingClient, (Object)image);
						thisClient.getServerInteractionHandler().sendMessageToServer(output);
						}
					else {
						System.out.println("*********************************************************************\n"
								+ "System Notice - The Client whose name has been entered is not online. Going Back to Main Menu."
								+ "\n*********************************************************************");
						}
					break;
					}
				// send text message to all clients
				case "3": {
					thisClient.setEnteringInput(false);
					thisClient.getServerInteractionHandler().updateOnlineClients();
					while(!thisClient.getServerInteractionHandler().getUpdated());
					// update list of online clients
					thisClient.getServerInteractionHandler().setUpdated(false);
					thisClient.setEnteringInput(true);
					System.out.println("Please enter the Text Message to Send to Everyone");
					// get text message to send
					String message = input.nextLine();
					thisClient.setEnteringInput(false);
					// send message to server
					Message output = new Message(MessageID.TEXT_SEND_TO_ALL_REQUEST, thisClient.getUsername(),
								"All", message);
					thisClient.getServerInteractionHandler().sendMessageToServer(output);

					break;
					}
				// send image message to all client
				case "4" : {
					thisClient.setEnteringInput(false);
					thisClient.getServerInteractionHandler().updateOnlineClients();
					while(!thisClient.getServerInteractionHandler().getUpdated());
					// update list of online clients
					thisClient.getServerInteractionHandler().setUpdated(false);

					boolean loaded = false;
					ImageIcon image = null;
					String displayM = "Please enter the Location of the Image File to Send to Everyone";
					// load image into ImageIcon
					while(!loaded) {
						try {
							thisClient.setEnteringInput(true);
							System.out.println(displayM);
							String imageURL = input.nextLine();
							thisClient.setEnteringInput(false);
							image = new ImageIcon(ImageIO.read(new File(imageURL)));
							loaded = true;
							}
						catch (IOException e) {
							System.out.println("The Specified Image could not be loaded." + e);
							displayM = "Please re-enter the Location of the Image File to Send";
							}
						}
						// send message to server
						Message output = new Message(MessageID.IMAGE_SEND_TO_ALL_REQUEST, thisClient.getUsername(),
								"All", (Object)image);
						thisClient.getServerInteractionHandler().sendMessageToServer(output);

					break;
					}
				// exit
				case "Exit" : {
					thisClient.setEnteringInput(false);
					try {
						// tell the server that the connection is closing
						thisClient.getServerInteractionHandler().sendMessageToServer(new Message(MessageID.CLOSE_CONNECTION,
								thisClient.getUsername(), Client.SERVER_NAME, ""));
					}
					catch (Exception e) {
						System.out.println(e);
						}
					return;
					}
				default : {
					System.out.println("Sorry the input was not understood. Please enter your choice again. (1,2,3,Exit)");
					break;
					}
				}



			}
		}



//*****************************************************************************************************************

private class ServerInteractionHandler implements Runnable {
	// instance variables
	private Socket connectionToServer;
	private ObjectInputStream oInputStream;
	private ObjectOutputStream oOutputStream;
	private volatile boolean updated;

	/***
	 * The constructor of the ServerInteractionHandler class.
	 * @param connectionToServer The Socket connection the Client to the Server
	 * @see Socket
	 */
	public ServerInteractionHandler(Socket connectionToServer) {
		this.connectionToServer = connectionToServer;
		this.updated = false;
		// initialize input and output streams.
		try {
			this.oOutputStream = new ObjectOutputStream(new BufferedOutputStream(this.connectionToServer.getOutputStream()));
			this.oOutputStream.flush();
			this.oInputStream = new ObjectInputStream(new BufferedInputStream(this.connectionToServer.getInputStream()));
		} catch (IOException e) {
			System.out.println(e);
			}
		}

	/***
	 * A method to set the value of updated.
	 * @param updated The new boolean value of updated.
	 */
	public void setUpdated(boolean updated) {
		this.updated = updated;
		}

	/***
	 * A method to get the value of updated.
	 * @return The boolean value of updated.
	 */
	public boolean getUpdated() {
		return this.updated;
		}

	/***
	 * A method used to send a message to the Server.
	 * Catches an IOException if an error occurs.
	 * @param message The Message to send to the Server.
	 * @see ObjectOutputStream
	 * @see IOException
	 */
	public void sendMessageToServer(Message message) {
		try {
			// write to stream
			this.oOutputStream.writeUnshared(message.getMessageID());
			this.oOutputStream.writeUTF(message.getSourceName());
			this.oOutputStream.writeUTF(message.getDestinationName());
			this.oOutputStream.writeUnshared(message.getData());
			// flush data across socket for input stream
			this.oOutputStream.flush();
			}
		catch (IOException e) {
			System.out.println(e);
			}
		}

	/***
	 * A method used to receive a message from the Server.
	 * Catches IOException and ClassNotFoundException if an error occurs.
	 * @return The Message retrieved from the Server.
	 * @see ObjectInputStream
	 * @see IOException
	 * @see ClassNotFoundException
	 */
	public Message getMessageFromServer() {
		Message message = null;
		try {
			// read the message variable by variable since Message isn't serializable.
			MessageID messageID = ((MessageID)this.oInputStream.readUnshared());
			String sourceName = this.oInputStream.readUTF();
			String destinationName = this.oInputStream.readUTF();
			Object data = this.oInputStream.readUnshared();
			message = new Message(messageID, sourceName, destinationName, data);

			}
		catch (IOException | ClassNotFoundException e) {
			System.out.println(e);
			}
		return message;
		}

	/***
	 * A method used to send a request to the Server to retrieve All Online Client's user names.
	 * @see Message
	 * @see ServerInteractionHandler#sendMessageToServer(Message)
	 */
	public void updateOnlineClients() {
		Message output = new Message(MessageID.ONLINE_CLIENTS_REQUEST, getUsername(), Client.SERVER_NAME,
				"update");
		this.sendMessageToServer(output);
		}

	/***
	 * A method used to close the Socket Connection to the Server.
	 * Catches an IOException if an error occurs.
	 */
	public void closeConnectionToServer() {
		try {
			this.connectionToServer.close();
			}
		catch (IOException e) {
			System.out.println(e);
			}
		}

	@Override
	public void run() {
		Message input;
		// while the connection is open keep checking for input from the server
		while(!this.connectionToServer.isClosed()) {
			input = this.getMessageFromServer();
			switch(input.getMessageID()) {
				// received a text message for this client
				case TEXT_TRANSFER_RECEIPT: {
					// print out the text message
					System.out.println("---------------------------------------------\nText Message from " +
							input.getSourceName() + "(To You): " + input.getData().toString() +
							"\n---------------------------------------------");
					break;
					}
				// received an image message confirmation for this client
				case IMAGE_TRANSFER_CONFIRMATION_REQUEST: {
					String display = input.getData().toString();
					Scanner in = getInput();
					String choice = "";
					boolean retrieveImage = false;
					// asks the user if they want to download the image
					while(!(choice.equals("Yes") || choice.equals("No"))){
						// inform the user we are waiting for System.in to be free
						System.out.println("*********************************************************************\n"
										+ "System Notice : Waiting for Previous Input to Finish on System.in"
										+ "\n*********************************************************************");
						setIsConfirming(true);
						System.out.println(display);
						choice = in.nextLine();
						switch(choice) {
							// they want to view the image
							case "Yes": {
								setIsConfirming(false);
								retrieveImage = true;
								break;
								}
							// they dont want to view the image
							case "No": {
								setIsConfirming(false);
								retrieveImage = false;
								break;
								}
							// ask them to enter their option again
							default : {
								display = "Invalid Option, Please enter Yes or No";
								break;
								}
						}
					}

					// sends the confirmation to the server
					Message outMessage = new Message(MessageID.IMAGE_TRANSFER_CONFIRMATION_RESPONSE, getUsername(),
													input.getSourceName(), retrieveImage);
					this.sendMessageToServer(outMessage);

					break;
					}
				// receive an image message
				case IMAGE_TRANSFER_RECEIPT : {
					System.out.println("*********************************************************************\n" +
										"System Notice : " + input.getSourceName() +
										" sent you an Image Opening in JFrame." +
										"\n*********************************************************************");
					// start a new JFrame in a new Thread to display the Image
					new Thread(new ClientImageDisplayer((ImageIcon)input.getData())).start();
					break;
					}
				// receive a text message send to everyone
				case TEXT_SEND_TO_ALL_RECEIPT: {
					System.out.println("---------------------------------------------\nText Message from " +
							input.getSourceName() +" (To Everyone): " + input.getData().toString() +
							"\n---------------------------------------------");
					break;
					}
				// received a response to updating online clients' user names
				case ONLINE_CLIENTS_RESPONSE: {
					// update the array list
					setOnlineClientNames((ArrayList<String>)input.getData());
					// indicate it has been updated
					this.setUpdated(true);
					break;
					}
				// receive a message from the Server instructing the client to close it's socket
				case CLOSE_CONNECTION: {
					this.closeConnectionToServer();
					System.exit(0);
					return;
					}
				// unknown message identifier
				default: {
					System.out.println("*********************************************************************\n"
							+ "System Notice : Warning unknown Message Code"
							+ "\n*********************************************************************");
					break;
					}

				}
			}


		}




}



}
