package com.example.ble;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.FileOutputStream;
import java.io.IOException;

public class GetNameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_name);
        Button button = findViewById(R.id.save);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editText = findViewById(R.id.editext);
                String username = editText.getText().toString();

                saveusername(username);
                startActivity(new Intent(GetNameActivity.this, MainActivity.class));
                finish();
            }
        });
    }
    private void saveusername(String name){
        String filname = "username.txt";
        try{
            FileOutputStream fos = openFileOutput(filname, Context.MODE_PRIVATE);
            fos.write(name.getBytes());
            fos.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}