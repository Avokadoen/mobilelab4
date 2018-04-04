package com.example.avokado.lab4;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.MyViewHolder> {

	private List<Friends> FriendsList;

	public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
		private TextView username;

		private MyViewHolder(View view)
		{
			super(view);
			view.setOnClickListener(this);
			username = (TextView) view.findViewById(R.id.username);
		}

		@Override
		public void onClick(View v)
		{
			Log.d("debug", "onClick: " + username.getText().toString());
			Intent i = new Intent("friend log");
			i.putExtra("username", username.getText().toString());
			LocalBroadcastManager.getInstance(v.getContext()).sendBroadcast(i);
		}
	}


	public FriendsAdapter(List<Friends> FriendsList) {
		this.FriendsList = FriendsList;
	}

	@Override
	public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View itemView = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.message_list_element, parent, false);

		return new MyViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(MyViewHolder holder, int position) {
		Friends friends = FriendsList.get(position);
		holder.username.setText(friends.getUsername());
	}

	@Override
	public int getItemCount() {
		return FriendsList.size();
	}

}