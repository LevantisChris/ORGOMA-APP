package com.example.orgoma;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    /* TextFields */
    private TextView emailText;
    /* FirebaseAuth */
    private FirebaseAuth firebaseAuth;
    /* Button */
    private Button logOutbutton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        findViewById();

        /* firebaseAuth init */
        firebaseAuth = FirebaseAuth.getInstance();

        Intent intent = getIntent();
        emailText.setText(intent.getStringExtra("email"));

        logOutbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseAuth.signOut();
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void findViewById() {
        emailText = findViewById(R.id.emailText);
        logOutbutton = findViewById(R.id.logOutbutton);
    }
}
