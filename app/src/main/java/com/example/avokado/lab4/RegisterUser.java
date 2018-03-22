package com.example.avokado.lab4;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class RegisterUser extends AppCompatActivity implements View.OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register_user);

		Button regBtn = (Button) findViewById(R.id.bt_activate_reg_use);

		regBtn.setOnClickListener(this);
	}

	@Override
	public void onClick(View view){

		TextView usrname = findViewById(R.id.pt_username_field);

		int strlenght = usrname.getText().toString().length();

		if(strlenght > 3 && strlenght < 16 ){
			Intent returnIntent = new Intent();
			returnIntent.putExtra("requsername", usrname.getText().toString());
			setResult(RegisterUser.RESULT_OK, returnIntent);
			finish();
		}
		else{
			if(strlenght <= 3) {
				usrname.setText("");
				usrname.setHint("string is too short");
			}
			else if(strlenght >= 16) {
				usrname.setText("");
				usrname.setHint("string is too long");
			}
		}
	}
}
