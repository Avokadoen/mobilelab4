package com.example.avokado.lab4;

// sources: https://www.androidhive.info/2016/01/android-working-with-recycler-view/

public class Message {
	public String message, username, timestamp;

	Message(String message, String username, String timestamp){
		this.message = message;
		this.username = username;
		this.timestamp = timestamp;
	}
}
