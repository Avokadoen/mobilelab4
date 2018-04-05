package com.example.avokado.lab4;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/* SOURCES:
Firestore basics: 			https://cloud.google.com/firestore/docs/quickstart
realtime data: 				https://firebase.google.com/docs/firestore/query-data/listen
broadcasting from adapter: 	https://stackoverflow.com/a/35061883
*/

// TODO: hints when filling username
// TODO: Fix timestamp format in chat
public class MainActivity extends AppCompatActivity {

	private static final int RC_SIGN_IN = 100;
	private static final int RC_USR_REG = 101;

	FirebaseFirestore db;
	String username;

	protected String rtrUsername;
	protected boolean validName;
	protected boolean unusedName;

	//chat data
	public List<Message> messageList;
	private MessageAdapter messageAdapter;
	private RecyclerView RecyclerViewChat;

	//friends data
	public List<Friends> friendsList;
	private FriendsAdapter friendsAdapter;

	//used to showcase selected friend chat
	public List<Message> thisFriendLogList;
	public MessageAdapter thisFriendLog;

	// anon login
	public FirebaseAuth mAuth;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// prepare chat view
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// prepare chat
		messageList = new ArrayList<>();
		messageAdapter = new MessageAdapter(messageList);
		RecyclerViewChat = (RecyclerView) findViewById(R.id.chat_view_rc);
		RecyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
		RecyclerViewChat.setAdapter(messageAdapter);

		// prepare friends
		friendsList = new ArrayList<>();
		friendsAdapter = new FriendsAdapter(friendsList);

		// start notification service if not active
		if(!checkNotificationService()){
			Intent ServiceIntent = new Intent(this, NotificationService.class);
			this.startService(ServiceIntent);
		}


		// post message when user press button
		findViewById(R.id.post_mess_bt).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				TextView messageTextView = findViewById(R.id.new_mess_cont_tv);

				// TODO: should be a rule in firestore

				// makes sure string is not empty/only whitespace
				String test = messageTextView.getText().toString().replaceAll("\\s+","");
				if(!test.equals("")) {

					CollectionReference messages = db.collection("messages");

					Map<String, Object> newMessage = new HashMap<>();
					newMessage.put("message", messageTextView.getText().toString());
					newMessage.put("timestamp", new java.util.Date());
					newMessage.put("username", username);
					messages.document().set(newMessage);

					messageTextView.setText("");
				}
			}
		});

		// change recyclerview content based on tab
		findViewById(R.id.chat_tab_bt).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				RecyclerViewChat.setAdapter(messageAdapter);
				RecyclerViewChat.scrollToPosition(messageList.size() - 1);
			}
		});

		findViewById(R.id.friend_tab_bt).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				RecyclerViewChat.setAdapter(friendsAdapter);
			}
		});

		// Register to receive messages.
		// We are registering an observer (mMessageReceiver) to receive Intents
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
				new IntentFilter("friend log"));


		validName = false;
		username = "";
		db = FirebaseFirestore.getInstance();

		// retrieve ID of user if existing
		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		String ID = sharedPref.getString("ID", "");

		String idTest = ID.replaceAll("\\s+","");

		// if ID is not stored, create new ID
		if (idTest.equals("")) {

			mAuth = FirebaseAuth.getInstance();

			FirebaseUser currentUser = mAuth.getCurrentUser();

			// sign user first in as anonymous
			mAuth.signInAnonymously()
					.addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
						@Override
						public void onComplete(@NonNull Task<AuthResult> task) {
							if (task.isSuccessful()) {
								// Sign in success, update UI with the signed-in user's information
								Log.d("debug", "signInAnonymously:success");
								FirebaseUser user = mAuth.getCurrentUser();
								Toast.makeText(MainActivity.this, "Authentication successful. \n Press back key to close further login",
										Toast.LENGTH_LONG).show();

								username = generateRandomUsrnm();


							} else {
								// If sign in fails, display a message to the user.
								Log.w("debug", "signInAnonymously:failure", task.getException());
								Toast.makeText(MainActivity.this, "Authentication failed.",
										Toast.LENGTH_SHORT).show();
							}

							// ...
						}
					});

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
		else{

			// validate ID with hash key
			Log.d("debug", "using existing user with id: " + ID);
			DocumentReference docRef = db.collection("users").document(ID);
			docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
				@Override
				public void onComplete(@NonNull Task<DocumentSnapshot> task) {
					if (task.isSuccessful()) {
						DocumentSnapshot document = task.getResult();
						if (document != null && document.exists()) {
							Log.d("debug", "DocumentSnapshot data: " + document.getData());
							username = document.get("username").toString();
						} else {
							Log.d("debug", "ID does not exist. deleting ID from file");
							SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
							SharedPreferences.Editor editor = sharedPref.edit();
							editor.remove("ID");
							editor.apply();
						}
					} else {
						Log.d("debug", "get failed with ", task.getException());
					}
				}
			});
		}
		// subscribe to messages in db and update if there is a change in firestore
		db.collection("messages").addSnapshotListener(
				new EventListener<QuerySnapshot>() {
					@Override
					public void onEvent(@Nullable QuerySnapshot snapshot,
										@Nullable FirebaseFirestoreException e) {
						if (e != null) {
							Log.w("debug", "Listen failed.", e);
							return;
						}

						if (snapshot != null && !snapshot.isEmpty()) {
							Log.d("debug", "Current data: " + snapshot.getDocumentChanges().get(0).getDocument().getData());

							int changes = snapshot.getDocumentChanges().size();

							// update all new messages to adapter
							for(int i = 0; i < changes; i++) {
								Map m = snapshot.getDocumentChanges().get(i).getDocument().getData();
								String message = m.get("message").toString();
								String username = m.get("username").toString();
								String timestamp = m.get("timestamp").toString();
								Log.d("debug", message + username + timestamp);
								Message newMessage = new Message(message, username, timestamp);
								addToMessagesView(newMessage, messageList);
							}
							messageAdapter.notifyDataSetChanged();

							RecyclerViewChat.scrollToPosition(messageList.size() - 1);

						} else {
							Log.d("debug", "Current data: null");
						}
					}
				}
		);

		// subscribe to users in db and update if there is a change in firestore
		db.collection("users").addSnapshotListener(
				new EventListener<QuerySnapshot>() {
					@Override
					public void onEvent(@Nullable QuerySnapshot snapshot,
										@Nullable FirebaseFirestoreException e) {
						if (e != null) {
							Log.w("debug", "Listen failed.", e);
							return;
						}

						if (snapshot != null && !snapshot.isEmpty()) {
							Log.d("debug", "Current data: " + snapshot.getDocumentChanges().get(0).getDocument().getData());

							int changes = snapshot.getDocumentChanges().size();

							// update all new friends to adapter
							for(int i = 0; i < changes; i++) {
								Map m = snapshot.getDocumentChanges().get(i).getDocument().getData();
								String username = m.get("username").toString();
								Log.d("debug", username);
								Friends newFriends = new Friends(username);
								friendsList.add(newFriends);
							}
							friendsAdapter.notifyDataSetChanged();

						} else {
							Log.d("debug", "Current data: null");
						}
					}
				}
		);
	}

	// check if user request friends log
	public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			// Get extra data included in the Intent
			String userToLog = intent.getStringExtra("username");
			Log.d("debug", "onReceive: " + userToLog);

			thisFriendLogList = new ArrayList<>();
			thisFriendLog = new MessageAdapter(thisFriendLogList);

			RecyclerViewChat.setAdapter(thisFriendLog);
			RecyclerViewChat.scrollToPosition(messageList.size() - 1);

			// retrieve all messages from requested friend
			db.collection("messages").whereEqualTo("username", userToLog).addSnapshotListener(
					new EventListener<QuerySnapshot>() {
						@Override
						public void onEvent(@Nullable QuerySnapshot snapshot,
											@Nullable FirebaseFirestoreException e) {
							if (e != null) {
								Log.w("debug", "Listen failed.", e);
								return;
							}

							if (snapshot != null && !snapshot.isEmpty()) {
								Log.d("debug", "Current data: " + snapshot.getDocumentChanges().get(0).getDocument().getData());

								int changes = snapshot.getDocumentChanges().size();

								for(int i = 0; i < changes; i++) {
									Map m = snapshot.getDocumentChanges().get(i).getDocument().getData();
									String message = m.get("message").toString();
									String username = m.get("username").toString();
									String timestamp = m.get("timestamp").toString();
									Log.d("debug", message + username + timestamp);
									Message newMessage = new Message(message, username, timestamp);
									addToMessagesView(newMessage, thisFriendLogList);
								}
								messageAdapter.notifyDataSetChanged();

								RecyclerViewChat.scrollToPosition(messageList.size() - 1);

							} else {
								Log.d("debug", "Current data: null");
							}
						}
					}
			);
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// if request is a sign in
		if (requestCode == RC_SIGN_IN) {
			if (resultCode == RESULT_OK) {

				// Successfully signed in
				FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

				// start registration activity and supply user with random username
				Intent i = new Intent(MainActivity.this, RegisterUser.class);
				i.putExtra("rndusername", generateRandomUsrnm());
				startActivityForResult(i, RC_USR_REG);

			} else {
				// Sign in failed, check response for error code
				// ...
			}
		}
		// if request is creating user in db
		else if (requestCode == RC_USR_REG) {
			if (resultCode == RESULT_OK) {
				validName = true;
				username = data.getStringExtra("requsername");
				data.putExtra("requsername", "");
				db.collection("users")
						.whereEqualTo("username", username)
						.get()
						.addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
							@Override
							public void onComplete(@NonNull Task<QuerySnapshot> task) {
								if (task.isSuccessful()) {
									// make sure atleast one element exists
									for (DocumentSnapshot document : task.getResult()) {
										Log.d("debug", document.getId() + " => " + document.getData());
										// invalid name if it already exist
										if (document.get("username").toString().equals(username)) {
											Log.w("debug", "validName set to false");
											validName = false;
										}
										break;
									}
								} else {
									Log.d("debug", "unused username", task.getException());
								}
								if (validName == false) {
									Log.w("debug", "equal usernames");
									startActivityForResult(new Intent(MainActivity.this, RegisterUser.class), RC_USR_REG);
								}
								else { // save user in db

									Map<String, Object> user = new HashMap<>();
									user.put("username", username);

									db.collection("users").add(user).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
										@Override
										public void onSuccess(DocumentReference documentReference) {
											Log.d("debug", "DocumentSnapshot added with ID: " + documentReference.getId());
											// if username was successfully added to db, save ID for username
											SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
											SharedPreferences.Editor editor = sharedPref.edit();
											editor.putString("ID", documentReference.getId());
											editor.apply();
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
						});


			}
		}
	}
	/*
		adds messages to adapter in correct order based on timestamp
		where recent timestamps comes at the bottom
	*/
	public void addToMessagesView(Message message, List<Message> m) {
		Log.d("debug", "1");
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d hh:mm:ss z yyyy");
			Date newParsedDate = dateFormat.parse(message.getTimestamp());
			Timestamp newMessageTimeStamp = new java.sql.Timestamp(newParsedDate.getTime());;
			boolean added = false;
				if (m.size() > 1) {
					for (int i = 0; i < m.size()-1; i++) {
						Date parsedDate = dateFormat.parse(m.get(i).getTimestamp());
						Timestamp memberTimestamp = new java.sql.Timestamp(parsedDate.getTime());

						Date nextParsedDate = dateFormat.parse(m.get(i+1).getTimestamp());
						Timestamp nextMemberTimestamp = new java.sql.Timestamp(nextParsedDate.getTime());

						if(newMessageTimeStamp.before(memberTimestamp)){
							Log.d("debug", "message added first: " + i + message + "\n");
							m.add(0, message);
							added = true;
							break;
						}
						else if (newMessageTimeStamp.after(memberTimestamp) &&
								newMessageTimeStamp.before(nextMemberTimestamp)) {

							Log.d("debug", "message added inbetween: " + i + message + "\n");
							m.add(i+1, message);
							added = true;
							break;
						}

					}
					if (!added) {
						Log.d("debug", "message added after loop: " + message + "\n");
						m.add(message);
					}
				} else if (m.size() == 0) {
					Log.d("debug", "2");
					Log.d("debug", "first message added: " + message + "\n");
					m.add(message);
				} else {
					Date parsedDate = dateFormat.parse(m.get(0).getTimestamp());
					Timestamp memberTimestamp = new java.sql.Timestamp(parsedDate.getTime());
					if (newMessageTimeStamp.before(memberTimestamp)) {
						Log.d("debug", "message added at start: " + message + "\n");
						m.add(0, message);
					} else {
						Log.d("debug", "message added at end: " + message + "\n");
						m.add(message);
					}
				}
		}
		catch (Exception e){
			Log.d("debug", "failed to display message: " + message + e + "\n");
		}

	}

	/*
		generates a random username for new users where output will be
		(adjective)(noun)(5 numbers)
	*/
	public String generateRandomUsrnm(){
		Random rand = new Random();
		int min = 10000;
		int max = 90000;

		// hardcoded list of adjectives
		String adjectiveList =
				"stabby\n" +
				"attractive\n" +
				"bald\n" +
				"beautiful\n" +
				"chubby\n" +
				"clean\n" +
				"dazzling\n" +
				"drab\n" +
				"elegant\n" +
				"fancy\n" +
				"fit\n" +
				"flabby\n" +
				"glamorous\n" +
				"gorgeous\n" +
				"handsome\n" +
				"long\n" +
				"magnificent\n" +
				"muscular\n" +
				"plain\n" +
				"plump\n" +
				"quaint\n" +
				"scruffy\n" +
				"shapely\n" +
				"short\n" +
				"skinny\n" +
				"stocky\n" +
				"ugly\n" +
				"unkempt\n" +
				"unsightly\n" +
				"ashy\n" +
				"black\n" +
				"blue\n" +
				"gray\n" +
				"green\n" +
				"icy\n" +
				"lemon\n" +
				"mango\n" +
				"orange\n" +
				"purple\n" +
				"red\n" +
				"salmon\n" +
				"white\n" +
				"yellow\n" +
				"alive\n" +
				"better\n" +
				"careful\n" +
				"clever\n" +
				"dead\n" +
				"easy\n" +
				"famous\n" +
				"gifted\n" +
				"hallowed\n" +
				"helpful\n" +
				"important\n" +
				"inexpensive\n" +
				"mealy\n" +
				"mushy\n" +
				"odd\n" +
				"poor\n" +
				"powerful\n" +
				"rich\n" +
				"shy\n" +
				"tender\n" +
				"unimportant\n" +
				"uninterested\n" +
				"vast\n" +
				"wrong";

		// hardcoded list of nouns
		String nounList =
				"Roberto\n" +
				"bot\n" +
				"apple\n" +
				"arm\n" +
				"banana\n" +
				"bike\n" +
				"bird\n" +
				"book\n" +
				"chin\n" +
				"clam\n" +
				"class\n" +
				"clover\n" +
				"club\n" +
				"corn\n" +
				"crayon\n" +
				"crow\n" +
				"crown\n" +
				"crowd\n" +
				"crib\n" +
				"desk\n" +
				"dime\n" +
				"dirt\n" +
				"dress\n" +
				"fang \n" +
				"field\n" +
				"flag\n" +
				"flower\n" +
				"fog\n" +
				"game\n" +
				"heat\n" +
				"hill\n" +
				"home\n" +
				"horn\n" +
				"hose\n" +
				"joke\n" +
				"juice\n" +
				"kite\n" +
				"lake\n" +
				"maid\n" +
				"mask\n" +
				"mice\n" +
				"milk\n" +
				"mint\n" +
				"meal\n" +
				"meat\n" +
				"moon\n" +
				"mother\n" +
				"morning\n" +
				"name\n" +
				"nest\n" +
				"nose\n" +
				"pear\n" +
				"pen\n" +
				"pencil\n" +
				"plant\n" +
				"rain\n" +
				"river\n" +
				"road\n" +
				"rock\n" +
				"room\n" +
				"rose\n" +
				"seed\n" +
				"shape\n" +
				"shoe\n" +
				"shop\n" +
				"show\n" +
				"sink\n" +
				"snail\n" +
				"snake\n" +
				"snow\n" +
				"soda\n" +
				"sofa\n" +
				"star\n" +
				"step\n" +
				"stew\n" +
				"stove\n" +
				"straw\n" +
				"string\n" +
				"summer\n" +
				"swing\n" +
				"table\n" +
				"tank\n" +
				"team\n" +
				"tent\n" +
				"test\n" +
				"toes\n" +
				"tree\n" +
				"vest\n" +
				"water\n" +
				"wing\n" +
				"winter\n" +
				"woman\n" +
				"women";

		// split said lists on newline
		String adjectiveArray[] = adjectiveList.split("\\r?\\n");
		String nounArray[] = nounList.split("\\r?\\n");

		unusedName = false;
		rtrUsername = "";

		// generate random usernames until we find one that is not taken
		while(!unusedName) {

			String adjective = adjectiveArray[rand.nextInt(adjectiveArray.length)];
			String noun = nounArray[rand.nextInt(nounArray.length)];
			int numbers = rand.nextInt(max) + min;
			rtrUsername =  adjective + noun + numbers;
			unusedName = true;

			db.collection("users")
					.whereEqualTo("username", rtrUsername)
					.get()
					.addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
						@Override
						public void onComplete(@NonNull Task<QuerySnapshot> task) {
							if (task.isSuccessful()) {
								// make sure atleast one element exists
								for (DocumentSnapshot document : task.getResult()) {
									Log.d("debug", document.getId() + " => " + document.getData());
									if (document.get("username").toString().equals(rtrUsername)) {
										Log.w("debug", "validName set to false");
										unusedName = false;
									}
									break;
								}
							} else {
								Log.d("debug", "unused username", task.getException());
							}
						}
					});
		}
		return rtrUsername;
	}

	// checks if our notification service is running on system
	private boolean checkNotificationService(){
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
			if(NotificationService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

}