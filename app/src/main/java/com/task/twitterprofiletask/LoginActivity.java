package com.task.twitterprofiletask;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar loadingProgressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null)
            openMainActivity();

        emailEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loadingProgressBar = findViewById(R.id.loading);
        loginButton = findViewById(R.id.login);
        addTextWatchers();

        loginButton.setOnClickListener(v -> {
            loadingProgressBar.setVisibility(View.VISIBLE);
            String email = emailEditText.getText().toString().trim();
            String pass = passwordEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(pass)) {
                trySignIn(email, pass);
            } else {
                Toast.makeText(LoginActivity.this, "Please fill all filds", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void signUp(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        openMainActivity();
                    } else {
                        Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    loadingProgressBar.setVisibility(View.GONE);
                });
    }

    private void trySignIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        openMainActivity();
                    } else {
                        signUp(email, password);
                    }
                });
    }

    private void addTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkEnableButton();
            }
        };
        emailEditText.addTextChangedListener(textWatcher);
        passwordEditText.addTextChangedListener(textWatcher);
    }

    private void checkEnableButton() {
        loginButton.setEnabled(!TextUtils.isEmpty(emailEditText.getText().toString().trim()) &&
                !TextUtils.isEmpty(passwordEditText.getText().toString().trim()));
    }

    private void openMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}