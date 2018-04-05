package com.example.avokado.lab4;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;

public class NotificationService extends Service {

	NotificationCompat.Builder builder;
	NotificationManager manager;

	FirebaseFirestore db;

	private int NOTIF_ID = 1;
	String NOTIF_KEY = "com.example.avokado.lab4";

	public NotificationService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");

	}

	@Override
	public void onCreate(){
		// prepare notification builder and manager
		this.builder =
				new NotificationCompat.Builder(this, NOTIF_KEY);
		this.manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// retrieve connection with firebase
		db = FirebaseFirestore.getInstance();

		// if SDK running is higher than 26
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Log.d("debug", "addNotification: true");

			// Create the NotificationChannel, but only on API 26+ because
			// the NotificationChannel class is new and not in the support library
			NotificationChannel channel = new NotificationChannel(NOTIF_KEY, "notifchannel", IMPORTANCE_DEFAULT);
			channel.setDescription("notify user about new messages");

			// Register the channel with the system
			manager.createNotificationChannel(channel);
		}

		// create listener for spotting change in db
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

							if(changes > 0){
								addNotification();
							}
						}
						else {
							Log.d("debug", "Current data: null");
						}

					}
				});


	}

	/*
		creates a notification for system to show user
	*/
	private void addNotification()
	{
		Log.d("debug", "addNotification: new notification");

		builder.setSmallIcon(R.drawable.ic_launcher_background);
		builder.setContentTitle("Unread Message");
		builder.setColor(101);
		builder.setContentText("You have an unread message.");

		Intent intent = new Intent(this, MainActivity.class);
		builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
		PendingIntent contentIntent = PendingIntent.getActivity(this, NOTIF_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(contentIntent);

		manager.notify(NOTIF_ID, builder.build());
	}
}
