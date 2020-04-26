package net.erikdevelopernot.jsonP;

public class JsonP_ParseException extends Exception {
	
	private String msg;

	public JsonP_ParseException() {
		msg = "An unknow JsonP_Parse exception has happened";
	}


	public JsonP_ParseException(String message) {
		msg = message;
	}


	/* (non-Javadoc)
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		return msg;
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Throwable#toString()
	 */
	@Override
	public String toString() {
		return msg;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1276983178561874306L;

}
