package com.example.avokado.lab4;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/* SOURCES:
 recycler feed: https://www.androidhive.info/2016/01/android-working-with-recycler-view/

 */

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {

	private List<Message> messagesList;

	public class MyViewHolder extends RecyclerView.ViewHolder {
		public TextView message, timestamp, username;

		public MyViewHolder(View view) {
			super(view);
			message = (TextView) view.findViewById(R.id.message);
			timestamp = (TextView) view.findViewById(R.id.timestamp);
			username = (TextView) view.findViewById(R.id.username);
		}
	}


	public MessageAdapter(List<Message> messagesList) {
		this.messagesList = messagesList;
	}

	@Override
	public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View itemView = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.message_list_element, parent, false);

		return new MyViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(MyViewHolder holder, int position) {
		Message message = messagesList.get(position);
		holder.message.setText(message.getMessage());
		holder.timestamp.setText(message.getTimestamp());
		holder.username.setText(message.getUsername());
	}

	@Override
	public int getItemCount() {
		return messagesList.size();
	}
}