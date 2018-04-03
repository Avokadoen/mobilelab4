package com.example.avokado.lab4;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* SOURCES:
Firestore basics: https://cloud.google.com/firestore/docs/quickstart
realtime data: https://firebase.google.com/docs/firestore/query-data/listen
*/

public class MainActivity extends AppCompatActivity {

	private static final int RC_SIGN_IN = 100;
	private static final int RC_USR_REG = 101;

	FirebaseFirestore db;
	String username;
	boolean validName;

	//chat data
	public List<Message> messageList;
	private RecyclerView chatView;
	private MessageAdapter messageAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// prepare chat view
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		messageList = new ArrayList<>();
		messageAdapter = new MessageAdapter(messageList);
		chatView = (RecyclerView) findViewById(R.id.chat_view_rc);
		chatView.setLayoutManager(new LinearLayoutManager(this));
		chatView.setAdapter(messageAdapter);

		findViewById(R.id.post_mess_bt).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				TextView messageTextView = (TextView) findViewById(R.id.new_mess_cont_tv);

				CollectionReference messages = db.collection("messages");

				Map<String, Object> newMessage = new HashMap<>();
				newMessage.put("message", messageTextView.getText().toString());
				newMessage.put("timestamp", new java.util.Date());
				newMessage.put("username", username);
				messages.document().set(newMessage);

				messageTextView.setText("");
			}
		});


		//recyclerLayoutTest();
		retrieveInitialData("messages");

		// validate user
		validName = false;
		username = "";
		db = FirebaseFirestore.getInstance();
		if (true) {
			// Choose authentication providers
			List<AuthUI.IdpConfig> providers = Arrays.asList(
					new AuthUI.IdpConfig.EmailBuilder().build(),
					new AuthUI.IdpConfig.GoogleBuilder().build()
			);
			// Create and launch sign-in intent
			startActivityForResult(AuthUI.getInstance()
							.createSignInIntentBuilder()
							.setAvailableProviders(providers)
							.build(),
					RC_SIGN_IN);
		}

		//retrieveData("messages", );
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == RC_SIGN_IN) {
			//IdpResponse response = IdpResponse.fromResultIntent(data);

			if (resultCode == RESULT_OK) {
				// Successfully signed in
				FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

				startActivityForResult(new Intent(MainActivity.this, RegisterUser.class), RC_USR_REG);

			} else {
				// Sign in failed, check response for error code
				// ...
			}
		} else if (requestCode == RC_USR_REG) {
			if (resultCode == RESULT_OK) {
				username = data.getStringExtra("requsername");
				db.collection("users")
						.get()
						.addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
							@Override
							public void onComplete(@NonNull Task<QuerySnapshot> task) {
								if (task.isSuccessful()) {
									validName = true;
									for (DocumentSnapshot document : task.getResult()) {
										Log.d("debug", document.getId() + " => " + document.getData());
										if (document.getData().values().toArray()[0].toString().equals(username)) {
											Log.w("debug", "validName set to false");
											validName = false;
											break;
										}
									}
								} else {
									Log.w("debug", "Error getting documents.", task.getException());
								}
							}
						});

				if (validName == false) {
					Log.w("debug", "equal usernames");
					startActivityForResult(new Intent(MainActivity.this, RegisterUser.class), RC_USR_REG);
				} else {
					Map<String, Object> user = new HashMap<>();
					user.put("username", username);

					db.collection("users").add(user).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
						@Override
						public void onSuccess(DocumentReference documentReference) {
							Log.d("debug", "DocumentSnapshot added with ID: " + documentReference.getId());
						}
					})
							.addOnFailureListener(new OnFailureListener() {
								@Override
								public void onFailure(@NonNull Exception e) {
									Log.w("debug", "Error adding document", e);
								}
							});
				}
			}
		}
	}

	// todo: insert on correct position
	// note: missing if on for loop for if new message is before first index
	public void addToMessagesView(Message message) {
		Log.d("debug", "1");
		try {
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d hh:mm:ss z yyyy");
		Date newParsedDate = dateFormat.parse(message.getTimestamp());
		Timestamp newMessageTimeStamp = new java.sql.Timestamp(newParsedDate.getTime());;
		boolean added = false;
			if (messageList.size() > 1) {
				for (int i = 0; i < messageList.size() - 1; i++) {
					Date parsedDate = dateFormat.parse(messageList.get(i).getTimestamp());
					Timestamp memberTimestamp = new java.sql.Timestamp(parsedDate.getTime());

					Date nextParsedDate = dateFormat.parse(messageList.get(i+1).getTimestamp());
					Timestamp nextMememberTimestamp = new java.sql.Timestamp(parsedDate.getTime());

					if (newMessageTimeStamp.before(memberTimestamp) &&
							newMessageTimeStamp.after(nextMememberTimestamp)) {

						Log.d("debug", "message added: " + i + message);
						messageList.add(i+1, message);
						added = true;
						break;
					}
				}
				if (!added) {
					Log.d("debug", "message added: " + message);
					messageList.add(message);
				}
			} else if (messageList.size() == 0) {
				Log.d("debug", "2");
				Log.d("debug", "message added: " + message);
				messageList.add(message);
			} else {
				Date parsedDate = dateFormat.parse(messageList.get(0).getTimestamp());
				Timestamp memberTimestamp = new java.sql.Timestamp(parsedDate.getTime());
				if (newMessageTimeStamp.before(memberTimestamp)) {
					Log.d("debug", "message added: " + message);
					messageList.add(0, message);
				} else {
					Log.d("debug", "message added: " + message);
					messageList.add(message);
				}
			}
		}
		catch (Exception e){
			Log.d("debug", "failed to display message: " + message + e);
		}

	}

	private void recyclerLayoutTest() {
		Message m = new Message("Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.", "u", "t");
		Message fm = new Message("first.", "first", "2004");
		Message lm = new Message("last.", "last", "2020");
		messageList.add(fm);
		messageList.add(m);
		messageList.add(m);
		messageList.add(m);
		messageList.add(m);
		messageList.add(lm);
	}

	// TODO:
	// NOTAT: ikke kall enda. skal skrives om for recycler
	public void retrieveRecentData(String collectName) {
		Timestamp timestamp = Timestamp.valueOf(messageList.get(0).getTimestamp());
		db.collection(collectName).whereGreaterThan("timestamp", timestamp)
				.get()
				.addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
					@Override
					public void onComplete(@NonNull Task<QuerySnapshot> task) {
						if (task.isSuccessful()) {
							for (DocumentSnapshot document : task.getResult()) {
								Log.d("debug", document.getId() + " => " + document.getData());
							}
						} else {
							Log.d("debug", "@retrieveRecentData Error getting documents: ", task.getException());
						}
					}
				});
	}

	// TODO: lag
	public void retrieveInitialData(String collectName) {

		db = FirebaseFirestore.getInstance();

		try {
			db.collection(collectName)
					.get()
					.addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

						@Override
						public void onComplete(@NonNull Task<QuerySnapshot> task) {
							if (task.isSuccessful()) {
								for (QueryDocumentSnapshot document : task.getResult()) {
									Log.d("debug", document.getId() + " => " + document.getData());
									Map m = document.getData();
									String message = m.get("message").toString();
									String username = m.get("username").toString();
									String timestamp = m.get("timestamp").toString();
									Log.d("debug", message + username + timestamp);
									Message newMessage = new Message(message, username, timestamp);
									addToMessagesView(newMessage);
									messageAdapter.notifyDataSetChanged();
								}
							} else {
								Log.d("debug", "Error getting documents: ", task.getException());
							}
						}

					});
		} catch (NullPointerException n) {
			Log.d("debug", "Error getting documents: ", n);
		}

	}

	public void postComment(){

	}

}