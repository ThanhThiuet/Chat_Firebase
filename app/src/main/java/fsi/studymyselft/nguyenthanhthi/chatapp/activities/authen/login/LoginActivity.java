package fsi.studymyselft.nguyenthanhthi.chatapp.activities.authen.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Random;

import fsi.studymyselft.nguyenthanhthi.chatapp.R;
import fsi.studymyselft.nguyenthanhthi.chatapp.activities.authen.register.RegisterActivity;
import fsi.studymyselft.nguyenthanhthi.chatapp.activities.listUser.ListUserActivity;
import fsi.studymyselft.nguyenthanhthi.chatapp.data.model.User;

public class LoginActivity extends AppCompatActivity implements LoginView, View.OnClickListener {

    private final String TAG = "LoginActivity";

    private EditText edtEmail, edtPassword;
    private Button buttonLogin, buttonAutoLogin;
    private TextView goToRegister;
    private ProgressDialog progressDialog;

    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;
    private DatabaseReference rootReference, usersReference;

    private ArrayList<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userList = new ArrayList<>();

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        rootReference = database.getReference();
        usersReference = rootReference.child("Users");

        pushDataUsersToList();

        bindViews();

        //check to auto-login
        checkUserHaveSignedIn();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        auth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        if (authStateListener != null) {
//            auth.removeAuthStateListener(authStateListener);
//        }
    }

    @Override
    public void showAuthError() {
        Toast.makeText(getContext(), "Invalid username and password combination.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void bindViews() {
        edtEmail = (EditText) findViewById(R.id.edt_email);
        edtPassword = (EditText) findViewById(R.id.edt_password);
        buttonLogin = (Button) findViewById(R.id.btn_login);
        buttonAutoLogin = (Button) findViewById(R.id.btn_auto_login);
        goToRegister = (TextView) findViewById(R.id.goToRegister);

        buttonLogin.setOnClickListener(this);
        buttonAutoLogin.setOnClickListener(this);
        goToRegister.setOnClickListener(this);
    }

    @Override
    public Context getContext() {
        return LoginActivity.this;
    }

    @Override
    public void showProgress() {
        progressDialog = ProgressDialog.show(getContext(), "Signing in", "Please wait...");
    }

    @Override
    public void hideProgress() {
        progressDialog.dismiss();
    }

    @Override
    public void setUsernameError() {
        String email = edtEmail.getText().toString().trim();

        if (email.equals("") || TextUtils.isEmpty(email)) { //the string is null or 0-length
            edtEmail.setError("Email can't be blank!");
        } else if (!email.contains("@")) {
            edtEmail.setError("Invalid email!");
        }
    }

    @Override
    public void setPasswordError() {
        String password = edtPassword.getText().toString().trim();

        if (password.equals("") || TextUtils.isEmpty(password)) {
            edtPassword.setError("Password can't be blank!");
        } else if (password.length() < 6) {
            edtPassword.setError("Password must have min 6 characters");
        }
    }

    @Override
    public void navigateToSignUp() {
        Intent intent = new Intent(getContext(), RegisterActivity.class);
        startActivity(intent);
    }

    @Override
    public void navigateToHome() {

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_login) {
            login();
        } else if (v.getId() == R.id.goToRegister) {
            //go to Register Activity
            navigateToSignUp();
        } else if (v.getId() == R.id.btn_auto_login) {
            autoLogin();
        }
    }

    private void pushDataUsersToList() {
        usersReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    userList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        User user = snapshot.getValue(User.class);
                        userList.add(user);
                    }
                    Log.d(TAG, "total user in list user (1) = " + userList.size());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * automatic login - set default email and password
     */
    private void autoLogin() {
        showProgress();

        //input email and password of user
        String inputEmail = edtEmail.getText().toString().trim();
        String inputPass = edtPassword.getText().toString().trim();

        if (!hasError(inputEmail, inputPass)) {
            login();
            return;
        }
        if (inputEmail.isEmpty() && inputPass.isEmpty()) {
            //check database
            if (usersReference == null) {
                Toast.makeText(getContext(), "Users database has not been created!", Toast.LENGTH_SHORT).show();
                return;
            }

            int total = 0; //total user records in database Users

            while (total <= 0) {
                //push data users to userList
                total = userList.size();
                Log.d(TAG, "total user records = " + total);
            }

            //random users in database to login
            Random rd = new Random();
            int index;
            do {
                index = rd.nextInt(total);
            } while (index < 0 || index >= total);
            User user = userList.get(index);
            inputEmail = user.getEmail();
            inputPass = "123456";

            loginWithEmailPassword(inputEmail, inputPass);
        }
    }

    /**
     * do action login when click button Login
     */
    private void login() {
        //input email and password of user
        String inputEmail = edtEmail.getText().toString().trim();
        String inputPass = edtPassword.getText().toString().trim();

        if (!hasError(inputEmail, inputPass)) {
            showProgress();
            loginWithEmailPassword(inputEmail, inputPass);
        }
    }



    private Boolean hasError(String email, String pass) {
        Boolean hasError = false;

        //check email
        if (email.equals("") || TextUtils.isEmpty(email) || !email.contains("@")) {
            setUsernameError();
            hasError = true;
        }

        //check password
        if (pass.equals("") || TextUtils.isEmpty(pass)) {
            setPasswordError();
            hasError = true;
        }

        return hasError;
    }

    private void loginWithEmailPassword(final String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmailPassword:success:");
                            Toast.makeText(getContext(), "Login successfully!", Toast.LENGTH_SHORT).show();

                            //show greeting
                            Toast.makeText(getContext(), "Welcome " + email.toString(), Toast.LENGTH_SHORT).show();

                            //go to List Users Activity
                            startActivity(new Intent(getContext(), ListUserActivity.class));

                            hideProgress();
                        }
                        else {
                            Log.w(TAG, "signInWithEmailPassword:failure", task.getException());
                            Toast.makeText(getContext(), R.string.login_failed, Toast.LENGTH_SHORT).show();

                            showAuthError();
                            hideProgress();
                        }
                    }
                });
    }

    /**
     * check user have login but don't logout
     * if true then user must'n login
     */
    private void checkUserHaveSignedIn() {
//        authStateListener = new FirebaseAuth.AuthStateListener() {
//            @Override
//            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
//                FirebaseUser userSignedIn = firebaseAuth.getCurrentUser();
//                if (userSignedIn != null) {
//                    //user have login but don't logout
//                    Log.d(TAG, "onAuthStateChanged:signed_in:" + userSignedIn.getUid());
//                    startActivity(new Intent(LoginActivity.this, ListUserActivity.class));
//                    finish();
//                }
//                else {
//                    Log.d(TAG, "onAuthStateChanged:sign_out");
//                }
//            }
//        };
    }
}