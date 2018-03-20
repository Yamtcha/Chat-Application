/***
 * An implementation of a Message to be sent between a Server and its Clients.
 */
public class Message {
	//instance variables
	private MessageID messageID;
	private String sourceName;
	private String destinationName;
	private Object data;

	/***
	 * The Constructor of the Message Class.
	 */
	public Message(MessageID messageID, String sourceName, String destinationName, Object data) {
		this.messageID = messageID;
		this.sourceName = sourceName;
		this.destinationName = destinationName;
		this.data = data;
	}

	/***
	 * A method to retrieve the ID code of a Message.
	 */
	public MessageID getMessageID() {
		return this.messageID;
	}

	/***
	 * A method to retrieve the Name of the Sender/Source of a Message.
	 */
	public String getSourceName() {
		return this.sourceName;
	}

	/***
	 * A method to set the Name of the Sender/Source of a Message.
	 */
	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	/***
	 * A method to retrieve the Name of the Receiver/Destination of a Message.
	 */
	public String getDestinationName() {
		return this.destinationName;
	}

	/***
	 * A method to set the Name of the Receiver/Destination of a Message.
	 */
	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	/***
	 * A method to retrieve the Data of a Message.
	 */
	public Object getData() {
		return this.data;
	}

	/***
	 * A method to set the Data of a Message.
	 */
	public void setData(Object data) {
		this.data = data;
	}
}
