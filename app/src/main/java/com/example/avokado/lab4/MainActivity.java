package com.example.avokado.lab4;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

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
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
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
import java.util.Random;
import java.util.regex.Pattern;

/* SOURCES:
Firestore basics: https://cloud.google.com/firestore/docs/quickstart
realtime data: https://firebase.google.com/docs/firestore/query-data/listen
*/

// TODO: friendlist
// TODO: hints when filling username

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {


		// prepare chat view
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		messageList = new ArrayList<>();
		messageAdapter = new MessageAdapter(messageList);
		RecyclerViewChat = (RecyclerView) findViewById(R.id.chat_view_rc);
		RecyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
		RecyclerViewChat.setAdapter(messageAdapter);


		findViewById(R.id.post_mess_bt).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				TextView messageTextView = findViewById(R.id.new_mess_cont_tv);

				// TODO: should be a rule in firestore
				// make sure string is not empty/only whitespace
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


		//recyclerLayoutTest();
		//retrieveInitialData("messages");

		// validate user
		validName = false;
		username = "";
		db = FirebaseFirestore.getInstance();

		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		String ID = sharedPref.getString("ID", "");


		// ID.equals("")
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
		else{
			//TODO

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
							editor.putString("ID", "");
							editor.apply();
						}
					} else {
						Log.d("debug", "get failed with ", task.getException());
					}
				}
			});
		}
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

							for(int i = 0; i < changes; i++) {
								Map m = snapshot.getDocumentChanges().get(i).getDocument().getData();
								String message = m.get("message").toString();
								String username = m.get("username").toString();
								String timestamp = m.get("timestamp").toString();
								Log.d("debug", message + username + timestamp);
								Message newMessage = new Message(message, username, timestamp);
								addToMessagesView(newMessage);
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RC_SIGN_IN) {
			//IdpResponse response = IdpResponse.fromResultIntent(data);

			if (resultCode == RESULT_OK) {
				// Successfully signed in
				FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

				Intent i = new Intent(MainActivity.this, RegisterUser.class);
				i.putExtra("rndusername", generateRandomUsrnm());
				startActivityForResult(i, RC_USR_REG);

			} else {
				// Sign in failed, check response for error code
				// ...
			}
		} else if (requestCode == RC_USR_REG) {

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
								else {

									Map<String, Object> user = new HashMap<>();
									user.put("username", username);

									db.collection("users").add(user).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
										@Override
										public void onSuccess(DocumentReference documentReference) {
											Log.d("debug", "DocumentSnapshot added with ID: " + documentReference.getId());

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

	public void addToMessagesView(Message message) {
		Log.d("debug", "1");
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d hh:mm:ss z yyyy");
			Date newParsedDate = dateFormat.parse(message.getTimestamp());
			Timestamp newMessageTimeStamp = new java.sql.Timestamp(newParsedDate.getTime());;
			boolean added = false;
				if (messageList.size() > 1) {
					for (int i = 0; i < messageList.size()-1; i++) {
						Date parsedDate = dateFormat.parse(messageList.get(i).getTimestamp());
						Timestamp memberTimestamp = new java.sql.Timestamp(parsedDate.getTime());

						Date nextParsedDate = dateFormat.parse(messageList.get(i+1).getTimestamp());
						Timestamp nextMemberTimestamp = new java.sql.Timestamp(nextParsedDate.getTime());

						if(newMessageTimeStamp.before(memberTimestamp)){
							Log.d("debug", "message added first: " + i + message + "\n");
							messageList.add(0, message);
							added = true;
							break;
						}
						else if (newMessageTimeStamp.after(memberTimestamp) &&
								newMessageTimeStamp.before(nextMemberTimestamp)) {

							Log.d("debug", "message added inbetween: " + i + message + "\n");
							messageList.add(i+1, message);
							added = true;
							break;
						}

					}
					if (!added) {
						Log.d("debug", "message added after loop: " + message + "\n");
						messageList.add(message);
					}
				} else if (messageList.size() == 0) {
					Log.d("debug", "2");
					Log.d("debug", "first message added: " + message + "\n");
					messageList.add(message);
				} else {
					Date parsedDate = dateFormat.parse(messageList.get(0).getTimestamp());
					Timestamp memberTimestamp = new java.sql.Timestamp(parsedDate.getTime());
					if (newMessageTimeStamp.before(memberTimestamp)) {
						Log.d("debug", "message added at start: " + message + "\n");
						messageList.add(0, message);
					} else {
						Log.d("debug", "message added at end: " + message + "\n");
						messageList.add(message);
					}
				}
		}
		catch (Exception e){
			Log.d("debug", "failed to display message: " + message + e + "\n");
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

	// TODO: remove
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

	public String generateRandomUsrnm(){
		Random rand = new Random();
		int min = 10000;
		int max = 90000;

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

		String adjectiveArray[] = adjectiveList.split("\\r?\\n");
		String nounArray[] = nounList.split("\\r?\\n");



		unusedName = false;
		rtrUsername = "";

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
}