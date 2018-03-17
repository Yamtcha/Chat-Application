

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
// java imports
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/***
 * An implementation of a Chat Server.
 * @author Pieter Janse van Rensburg (jnspie007@myuct.ac.za)
 * @version 29/03/2017
 * @since 29/03/2017
 */
public class Server implements Runnable {
	
	private final static int INCOMING_CONNECTION_PORT = 1337;
	private final static String SERVER_NAME = "Server";
	private final static String USER_LOGIN_DETAILS = "server_data/user_details.txt";
	
	//instance variables
	private ServerSocket serverSocket;
	private ConcurrentHashMap<String, String> knownClientDetails;
	private ArrayList<ClientInteractionHandler> currentConnections;
	// Since the ArrayList is not inherently thread-safe requires a ReadWriteLock.
	private ReentrantReadWriteLock currentConnectionsLock;
	/***
	 * Constructor of the Server Class.
	 * @see ConcurrentHashMap
	 * @see ArrayList
	 * @see Server#initialiseServer()
	 * @see Server#listenForConnections()
	 */
	public Server() {
		this.knownClientDetails = new ConcurrentHashMap<String, String>();
		// Checks if the Hash Map is populated successfully from database.
		if(this.loadKnownClientDetailsFromDatabase())
			System.out.println("******************************************\n"
					+ "System Notice - Concurrent Hash Map Populated Successfully with the Following Keys and Values:\n"
					+ this.knownClientDetails.toString() + "\n******************************************");
		// initializes ArrayList of Current Connections
		this.currentConnections = new ArrayList<ClientInteractionHandler>();
		// create a ReentratReadWriteLock for the ArrayList to make it thread-safe.
		this.currentConnectionsLock = new ReentrantReadWriteLock();
		this.initialiseServer();
		}
	
	/***
	 * A method used to start a ServerSocket to start listening for incoming client connections on a predetermined port.
	 * Catches IOException if the ServerSocket cannot be instantiated.
	 * @see ServerSocket
	 * @see IOException
	 */
	private void initialiseServer() {
		// start server socket to listen for incoming client connections
		try {
			this.serverSocket = new ServerSocket(Server.INCOMING_CONNECTION_PORT);
			} catch (IOException e) {
				System.out.println(e);
				}
		// tell the administrator that the server has started and is waiting for incoming connections
		System.out.println("******************************************\n"
				+ "System Notice - Server Started and waiting for Connections on Port: " + Server.INCOMING_CONNECTION_PORT +
				".\n******************************************");
		}
	
	/***
	 * A method used to accept incoming client connections and to start ClientInteractionHandler threads to deal with Client requests in parallel.
	 * Catches IOException if an error occurs.
	 * @see Socket
	 * @see ClientInteractionHandler
	 * @see IOException
	 */
	private void listenForConnections() {
		Socket clientSocket = null;
		// while the server is on, listen for incoming connections
		while(true) {
			try {
				// accept the incoming connections on the specific port
				clientSocket = this.serverSocket.accept();
				} catch (IOException e) {
					System.out.println(e);
					}
			// create a new thread to handle the client's connection in parallel.
			ClientInteractionHandler currentClient = new ClientInteractionHandler(clientSocket);
			try {
				// locks the ArrayList in case other threads are reading from it.
				this.currentConnectionsLock.writeLock().lock();
				// add the connection to the array list.
				this.currentConnections.add(currentClient);
				System.out.println("------------------------------------------\n"
						+ "System Action - Current Connection to a Client has been "
						+ "added to ArrayList of Currently Open Connections."
						+ "\n------------------------------------------");
				} finally {
					// unlock the lock once the writing has occurred or in the case of an Exception.
					this.currentConnectionsLock.writeLock().unlock();
					}
			// start new Thread to handle connection in parallel.
			System.out.println("------------------------------------------\n"
					+ "System Action - New Thread started for current Client."
					+ "\n------------------------------------------");
			new Thread(currentClient).start();
			}
		}
	
	/***
	 * A method used to Shut Down the Server.
	 * @see System#exit(int)
	 */
	private void shutdownServer() {
		if(!this.currentConnections.isEmpty()) {
			for(ClientInteractionHandler client: currentConnections) {
				Message shutdownM = new Message(MessageID.CLOSE_CONNECTION, Server.SERVER_NAME, 
												client.getClientUsername(), "Shut Down");
				client.sendMessageToClient(shutdownM);
				
			}
		}
		// inform the administrator that the Server has shut down.
		System.out.println("******************************************\n"
				+ "System Notice - Server has Shutdown & is no longer listening for connections."
				+ "\n******************************************");
		// Exit the JVM, thereby shutting down the Server.
		System.exit(0);
	}
	
	
	/***
	 * The main method of the program. Run if the current Computer is to be set up as the Server.
	 * @param args Command-line arguments for the Server.
	 * @see Scanner
	 * @see Server
	 * @see Thread
	 */
	public static void main(String args[]) {
		// instantiate a server object
		System.out.println("Server Log:\n" +
		"###############################################################");
		Server server = new Server();
		// start server in a new thread, so main thread can listen for administrator input
		Thread thread = new Thread(server);
		thread.start();
		// ask administrator to enter a server command
		System.out.println("Please Enter a Server Command(Exit):");
		Scanner input = new Scanner(System.in);
		String command = input.nextLine();
		while(!command.equals("Exit"))
			command = input.nextLine();
		
		// closes input and shuts down the server.
		input.close();
		server.shutdownServer();
		
		
		}
	
	/***
	 * A method used to load all known Client's Details from the Database into the ConcurrentHashMap.
	 * @return A boolean indicating whether the loading was successful.
	 * @see ConcurrentHashMap
	 */
	public synchronized boolean loadKnownClientDetailsFromDatabase() {
		// If you are doing the database, please implement this method (Look at ServerDatabase Class & libs folder)
		loadKnownClientDetailsFromTextFile();
		System.out.println("------------------------------------------\n"
				+ "System Action - Loaded All Known Client Details from Database."
				+ "\n------------------------------------------");
		return true;
		}
	
	/***
	 * A method used to load all know Client's Details from the text file into the ConcurrentHashMap.
	 * Catches a FileNotFoundException if unsuccessful.
	 * @return A boolean indicating whether the loading was successful.
	 * @see ConcurrentHashMap
	 * @see Scanner
	 * @see FileReader
	 * @see FileNotFoundException
	 */
	public synchronized boolean loadKnownClientDetailsFromTextFile() {
		try {
			@SuppressWarnings("resource")
			Scanner infile = new Scanner(new FileReader(USER_LOGIN_DETAILS)).useDelimiter("\n");
			while(infile.hasNext()) {
				String line = infile.next();
				String username = line.substring(0, line.indexOf('#'));
				String password = line.substring(line.indexOf('#') + 1);
				this.knownClientDetails.put(username, password);
				}
			} catch (FileNotFoundException e) {
				System.out.println(e);
				return false;
				}
		return true;
		
		}
	
	/***
	 * A method used to save a new Client's login details to the database.
	 * @param username The user name of the Client to be Added to the database.
	 * @param password The password of the Client to be Added to the database.
	 * @return A boolean indicating whether the new Client was added to the Database successfully.
	 */
	public synchronized boolean saveUserDetailsToDatabase(String username, String password) {
		// If you are doing the database, please implement this method (Look at ServerDatabase Class & libs folder)
		saveUserDetailsToTextFile(username, password);
		System.out.println("------------------------------------------\n"
				+ "System Action - " + username + "'s Details Successfully added to Database."
						+ "\n------------------------------------------");
		return true;
		}
	
	/***
	 * A method used to save a new Client's login details to the text file.
	 * Catches an IOException if unsuccessful.
	 * @param username The user name of the Client to be Added to the text file.
	 * @param password The password of the Client to be Added to the text file.
	 * @return A boolean indicating whether the new Client was added to the text file successfully.
	 * @see FileWriter
	 * @see IOException
	 */
	public synchronized boolean saveUserDetailsToTextFile(String username, String password) {
		
		try {
			@SuppressWarnings("resource")
			FileWriter outfile = new FileWriter(USER_LOGIN_DETAILS, true);
			outfile.write("\n" + username + "#" + password);
			outfile.flush();
			} catch (IOException e) {
				System.out.println(e);
				return false;
				}
			return true;
		}
	
	/***
	 * A method used to check if the given Client's login details are correct.
	 * @return A boolean value which is true if the Client's login details are correct.
	 * @param username The name of the Client to check the credentials for.
	 * @param password The password of the Client to check the credentials for.
 	 * @see ConcurrentHashMap
	 */
	public boolean checkUserCredentials(String username, String password) {
		// If the User isn't in the database, then they must be added to it and the HashMap
		if(!this.knownClientDetails.containsKey(username)) {
			this.saveUserDetailsToDatabase(username, password);
			this.knownClientDetails.put(username, password);
			return true;
			}
		// Else check if the User Login Details Given is Correct
		if(this.knownClientDetails.get(username).equals(password))
				return true;
		// If they are incorrect return false
		return false;
		}
	
	/***
	 * A method used to check if the given Client is connected to the server.
	 * @param username The user name of the given Client.
	 * @return A boolean which is true is the given Client is connected to the Server.
	 * @see ClientInteractionHandler
	 */
	public boolean checkOnline(String username) {
		boolean isOnline = false;
		try {
			// locks the ArrayList with a ReadLock
			this.currentConnectionsLock.readLock().lock();
			for(ClientInteractionHandler clientConnection: this.currentConnections)
				if(clientConnection.getClientUsername().equals(username))
					isOnline =  true;
			} finally {
				// Releases the lock since reading has occurred.
				this.currentConnectionsLock.readLock().unlock();
				}
		return isOnline;
			
		}
	
	/***
	 * A method used to get the ClientInteractionHandler responsible for the Socket to the Client with the given user name.
	 * @param username A name which uniquely identifies a Client.
	 * @return The ClientInteractionHandler responsible for the Socket to the Client with the given user name.
	 * @see ClientInteractionHandler
	 */
	public ClientInteractionHandler getOnlineClient(String username) {
		ClientInteractionHandler soughtConnection = null;
		try {
			// locks the ArrayList with a ReadLock
			this.currentConnectionsLock.readLock().lock();
			for(ClientInteractionHandler clientConnection: this.currentConnections)
				if(clientConnection.getClientUsername().equals(username))
					soughtConnection = clientConnection;
			} finally {
				// Releases the lock since reading has occurred.
				this.currentConnectionsLock.readLock().unlock();
				}
		return soughtConnection;
		}
	
	/***
	 * The method which is called when Server is parsed into a Thread and start is called.
	 * @see Thread
	 * @see Runnable
	 */
	@Override
	public void run() {
		// makes the server listen for new connections on a seperate thread so it can still accept admin commands.
		this.listenForConnections();
		}
	
	
	
	
	
	
//***********************************************************************************
	
	
private class ClientInteractionHandler implements Runnable {
	
	private final static String IMAGE_CONFIRMATION_REQUEST_TEXT = " would like to send you an Image. Would you like to Download it? (Yes/No)";
	
	//instance variables
	private Socket connectionToClient;
	private String clientUsername;
	private ObjectInputStream oInputStream;
	private ObjectOutputStream oOutputStream;
	private ReentrantReadWriteLock outstandingMessagesLock;
	private ArrayList<Message> outstandingMessages;
	
	/***
	 * Constructor for the ClientInteractionHandler Class
	 * @param connectionToClient A Socket on which the Client is connected to the Server
	 * @see Socket
	 */
	public ClientInteractionHandler(Socket connectionToClient) {
		this.connectionToClient = connectionToClient;
		this.clientUsername = "";
		this.outstandingMessagesLock = new ReentrantReadWriteLock();
		this.outstandingMessages = new ArrayList<Message>();
		try {
			this.oOutputStream = new ObjectOutputStream(new BufferedOutputStream(this.connectionToClient.getOutputStream()));
			this.oOutputStream.flush();
			this.oInputStream = new ObjectInputStream(new BufferedInputStream(this.connectionToClient.getInputStream()));
			} 
		catch (IOException e) {
			System.out.println(e);
			}
	}
	
	/***
	 * A method used to get the user name of the Client who the ClientInteractionHandler is managing.
	 * @return The user name of the Client who the ClientInteractionHandler is managing.
	 */
	public String getClientUsername() {
		return this.clientUsername;
		}
	
	/***
	 * A method to set the user name of the Client who the ClientInteractionHandler is managing.
	 * @param username The user name of the Client who the ClientInteractionHandler is managing.
	 */
	public void setClientUsername(String username) {
		this.clientUsername = username;
		}
	
	/***
	 * A method used to retrieve a message from the Client through a Socket.
	 * If it doesn't work then it catches an IOException and ClassNotFoundException.
	 * @return The Message Object sent by the Client through a Socket.
	 * @see ObjectInputStream
	 * @see Message
	 * @see IOException
	 * @see ClassNotFoundException
	 */
	public Message getMessageFromClient() {
		Message message = null;
		try {
			MessageID messageID = ((MessageID) this.oInputStream.readUnshared());
			String sourceName = this.oInputStream.readUTF();
			String destinationName = this.oInputStream.readUTF();
			Object data = this.oInputStream.readUnshared();
			message = new Message(messageID, sourceName, destinationName, data);
			
			} catch (IOException | ClassNotFoundException e) {
				System.out.println(e);
				}
			return message;
		}
	
	/***
	 * A method used to send a message to the Client through a Socket.
	 * If it doesn't work then it catches an IOException.
	 * @param message The Message Object to be sent to the Client through a Socket.
	 * @see ObjectOutputStream
	 * @see IOException
	 */
	public void sendMessageToClient(Message message) {
		try {
			
			this.oOutputStream.writeUnshared(message.getMessageID());
			this.oOutputStream.writeUTF(message.getSourceName());
			this.oOutputStream.writeUTF(message.getDestinationName());
			this.oOutputStream.writeUnshared(message.getData());
			this.oOutputStream.flush();
			} catch (IOException e) {
				System.out.println(e);
				}
		}
	
	public void addMessageToOutstandingMessages(Message message) {
		try {
		this.outstandingMessagesLock.writeLock().lock();
		this.outstandingMessages.add(message);
		}
		finally {
		this.outstandingMessagesLock.writeLock().unlock();	
		}
		}
	
	private void deleteMessageFromOutstandingMessages(String sourceName, String destinationName) {
		try {
			this.outstandingMessagesLock.writeLock().lock();
			for(Message m: this.outstandingMessages)
				if(m.getSourceName().equals(sourceName) && m.getDestinationName().equals(destinationName)) {
					this.outstandingMessages.remove(m);
					break;
					}
			}
		finally {
			this.outstandingMessagesLock.writeLock().unlock();
			}
		}
	
	public Message getMessageFromOutstandingMessages(String sourceName, String destinationName) {
		Message returnM = null;
		try {
			this.outstandingMessagesLock.writeLock().lock();	
			for(Message m : this.outstandingMessages)
				if(m.getSourceName().equals(sourceName) && m.getDestinationName().equals(destinationName)) {
					returnM = m;
					this.outstandingMessages.remove(m);
					break;
					}
			}
		finally {
			this.outstandingMessagesLock.writeLock().unlock();
		}
		return returnM;
		
		}
	
	/***
	 * A method used to transfer a Message from one Client's connection to another Client's connection.
	 * @param message The Message to be transferred to the other connection.
	 * @param clientConnection The connection of the Client to which the message must be delivered.
	 */
	private void transferMessageToConnection(Message message, ClientInteractionHandler clientConnection) {
		clientConnection.sendMessageToClient(message);
		}
	
	private void storeMessageinConnectionOutStandingMessages(Message message, ClientInteractionHandler clientConnection) {
		clientConnection.addMessageToOutstandingMessages(message);
		}
	
	
	/***
	 * A method used to send all online Client's user names to the Client.
	 * @see ArrayList
	 * @see ReentrantReadWriteLock
	 */
	private ArrayList<String> getAllOnlineClientDetails(String currentUsername) {
		ArrayList<String> onlineClientUsernames = new ArrayList<String>();
		
		try {
		currentConnectionsLock.readLock().lock();
		for(ClientInteractionHandler c: currentConnections)
			if(!c.getClientUsername().equals(currentUsername))
				onlineClientUsernames.add(c.getClientUsername());
			
		}
		finally {
			currentConnectionsLock.readLock().unlock();
		}
		return onlineClientUsernames;
		}
	
	@Override
	public void run() {
		// When the connection first starts the User's Login Details Must be Checked
		Message input = this.getMessageFromClient();
		Message output;
		// If they are incorrect we keep looping until the correct details are supplied
		boolean isCorrect = checkUserCredentials(input.getSourceName(), input.getData().toString());
		while(!isCorrect) {
			// send output to client to tell them the details they entered are incorrect
			System.out.println("******************************************\n"
					+ "System Notice - Warning: " + input.getSourceName() + " Entered incorrect Client Credentials."
							+ "\n******************************************");
			output = new Message(MessageID.REGISTRATION_RESPONSE, Server.SERVER_NAME, input.getSourceName(), isCorrect);
			this.sendMessageToClient(output);
			// get new input from client
			input = this.getMessageFromClient();
			isCorrect = checkUserCredentials(input.getSourceName(), input.getData().toString());
			}
		// Set the user name of the client this ClientInteractionHandler is responsible for.
		this.setClientUsername(input.getSourceName());
		// tell the client that their user details were correct.
		System.out.println("******************************************\n"
				+ "System Notice - " + input.getSourceName() + " Logged In with correct Client Credentials."
						+ "\n******************************************");
		output = new Message(MessageID.REGISTRATION_RESPONSE, Server.SERVER_NAME, input.getSourceName(), isCorrect);
		this.sendMessageToClient(output);

		while(!this.connectionToClient.isClosed()) {
			
			input = this.getMessageFromClient();
			
			
			// based on the Message ID different actions have to be performed.
			switch(input.getMessageID()) {
				
				case ONLINE_CLIENTS_REQUEST: {
					output = new Message(MessageID.ONLINE_CLIENTS_RESPONSE, Server.SERVER_NAME, input.getSourceName(),
							getAllOnlineClientDetails(this.getClientUsername()));
					this.sendMessageToClient(output);
					System.out.println("------------------------------------------\n"
							+ "System Action - Sent Online Client Usernames to " + this.getClientUsername() 
							+ "\n------------------------------------------");
					break;
					}
				// When a Text Message is sent to the Server
				case TEXT_TRANSFER_REQUEST: {
					if(checkOnline(input.getDestinationName())) {
						// formulates output message
						output = new Message(MessageID.TEXT_TRANSFER_RECEIPT, input.getSourceName(), 
											input.getDestinationName(), input.getData());
						// Gives the message to the correct Socket to send to the Destination Client
						this.transferMessageToConnection(output, getOnlineClient(input.getDestinationName()));
						}
					break;
					}
				// When an Image Message is sent to the Server
				case IMAGE_TRANSFER_REQUEST: {
					if(checkOnline(input.getDestinationName())) {
						//make message to ask client if they would like to receive the Image.
						Message imageMessage = new Message(MessageID.IMAGE_TRANSFER_RECEIPT, input.getSourceName(),
								input.getDestinationName(), input.getData());
						this.storeMessageinConnectionOutStandingMessages(imageMessage, getOnlineClient(input.getDestinationName()));
						
						output = new Message(MessageID.IMAGE_TRANSFER_CONFIRMATION_REQUEST, input.getSourceName(), 
											input.getDestinationName(), (input.getSourceName() + 
													ClientInteractionHandler.IMAGE_CONFIRMATION_REQUEST_TEXT));
						this.transferMessageToConnection(output, getOnlineClient(input.getDestinationName()));
						}
					break;
					}
				
				case IMAGE_TRANSFER_CONFIRMATION_RESPONSE: {
					if((boolean)input.getData()) {
						output = this.getMessageFromOutstandingMessages(input.getDestinationName(), input.getSourceName());
						this.sendMessageToClient(output);
						}
					else {
						this.deleteMessageFromOutstandingMessages(input.getDestinationName(), input.getSourceName());
					}
					break;
					}
				
				case TEXT_SEND_TO_ALL_REQUEST: {
					for(ClientInteractionHandler client: currentConnections) {
						if(!client.getClientUsername().equals(input.getSourceName())) {
							output = new Message(MessageID.TEXT_SEND_TO_ALL_RECEIPT, input.getSourceName(), 
												client.getClientUsername(), input.getData());
							this.transferMessageToConnection(output, client);
							}
						}
					break;
					}
				case IMAGE_SEND_TO_ALL_REQUEST: {
					for(ClientInteractionHandler client: currentConnections) {
						if(!client.getClientUsername().equals(input.getSourceName())) {
							Message imageMessage = new Message(MessageID.IMAGE_TRANSFER_RECEIPT, input.getSourceName(),
									client.getClientUsername(), input.getData());
							this.storeMessageinConnectionOutStandingMessages(imageMessage, client);
						
							output = new Message(MessageID.IMAGE_TRANSFER_CONFIRMATION_REQUEST, input.getSourceName(), 
													client.getClientUsername(), (input.getSourceName() + 
													ClientInteractionHandler.IMAGE_CONFIRMATION_REQUEST_TEXT));
							this.transferMessageToConnection(output, client);
							}
						}
					break;
					}
				case CLOSE_CONNECTION : {
				
					output = new Message(MessageID.CLOSE_CONNECTION, 
							Server.SERVER_NAME, this.clientUsername, "");
					this.sendMessageToClient(output);
					System.out.println("******************************************\n"
										+ "System Notice - " + this.clientUsername + " closed the connection"
									+ "\n******************************************");
					return;
					}
				// Other Message Code i.e. the Message is not Meant for the Server
				default : {
					System.out.println("******************************************\n"
							+ "System Notice - Warning: Unknown Message Code Received"
							+ "\n******************************************");
					break;
					}
				}
			}
			
		}
	
		
		
		
		
		
}

	
	
	
	
}
