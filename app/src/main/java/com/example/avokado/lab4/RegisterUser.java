package com.example.avokado.lab4;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class RegisterUser extends AppCompatActivity implements View.OnClickListener {

	final static int maxstr = 35;
	final static int minstr = 3;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register_user);

		Button regBtn = findViewById(R.id.bt_activate_reg_use);

		String rndName = getIntent().getStringExtra("rndusername");
		EditText usernameField = findViewById(R.id.pt_username_field);
		usernameField.setText(rndName);

		regBtn.setOnClickListener(this);
	}

	@Override
	public void onClick(View view){

		TextView usrname = findViewById(R.id.pt_username_field);

		int strlenght = usrname.getText().toString().length();

		if(strlenght > minstr && strlenght < maxstr ){
			Intent returnIntent = new Intent();
			returnIntent.putExtra("requsername", usrname.getText().toString());
			setResult(RegisterUser.RESULT_OK, returnIntent);
			finish();
		}
		else{
			if(strlenght <= minstr) {
				usrname.setText("");
				usrname.setHint("string is too short");
			}
			else if(strlenght >= maxstr) {
				usrname.setText("");
				usrname.setHint("string is too long");
			}
		}
	}
}
