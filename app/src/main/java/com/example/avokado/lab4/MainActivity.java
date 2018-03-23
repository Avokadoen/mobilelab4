package com.example.avokado.lab4;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* SOURCES:
Firestore basics: https://cloud.google.com/firestore/docs/quickstart
*/

public class MainActivity extends AppCompatActivity {

	private static final int RC_SIGN_IN = 100;
	private static final int RC_USR_REG = 101;

	FirebaseFirestore db;
	String username;
	boolean validName;

	//chat data
	private List<Message>messageList;
	private RecyclerView chatView;
	private MessageAdapter messageAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// prepare chat view
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		messageList = new ArrayList<>();
		messageAdapter = new MessageAdapter(messageList);
		chatView = (RecyclerView) findViewById(R.id.chat_view);
		chatView.setLayoutManager(new LinearLayoutManager(this));
		chatView.setAdapter(messageAdapter);

		Message m = new Message("Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.", "u", "t");
		messageList.add(m);
		messageList.add(m);
		messageList.add(m);
		messageList.add(m);
		messageList.add(m);
		messageList.add(m);
		messageList.add(m);

		// validate user
		validName = false;
		username = "";
		db = FirebaseFirestore.getInstance();

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
		}
		else if(requestCode == RC_USR_REG){
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
										if (document.getData().values().toArray()[0].toString().equals(username)){
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

				if(validName == false){
					Log.w("debug", "equal usernames");
					startActivityForResult(new Intent(MainActivity.this, RegisterUser.class), RC_USR_REG);
				}
				else{
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
}
