package com.example.avokado.lab4;

// sources: https://www.androidhive.info/2016/01/android-working-with-recycler-view/

class Message {
	private String message;
	private String username;
	private String timestamp;

	// empty
	public Message(){}

	public Message(String message, String username, String timestamp){
		this.message = message;
		this.username = username;
		this.timestamp = timestamp;
	}

	public String getMessage(){
		return this.message;
	}

	public String getUsername(){
		return this.username;
	}

	public String getTimestamp(){
		return this.timestamp;
	}

}
