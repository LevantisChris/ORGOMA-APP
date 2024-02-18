package com.example.orgoma;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    /* Google */
    private SignInClient oneTapClient;
    private BeginSignInRequest signUpRequest;
    private static final int REQ_ONE_TAP = 2;  // Can be any integer unique to the Activity.
    private boolean showOneTapUI = true;

    /* Firebase Auth */
    private FirebaseAuth firebaseAuth;

    /* Firestore */
    private FirebaseFirestore firebaseFirestore;

    /* TextView */
    private TextView emailText;
    private TextView passwdText;
    private TextView nameText;
    private TextView surnameText;

    /* Relative Layouts */
    private RelativeLayout relativeLayout1; // Submit Button

    /* Buttons */
    private Button Googlebtn;

    /* Progress Dialog */
    private ProgressDialog progressDialog;

    /* AutoCompleteTextView for the Spinner */
    AutoCompleteTextView autoCompleteTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        findViewsById();

        /* Initialize firebase auth */
        firebaseAuth = FirebaseAuth.getInstance();

        /* Initialize Firestore */
        firebaseFirestore = FirebaseFirestore.getInstance();

        /* Progress Dialog init */
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Creating your account");
        progressDialog.setCanceledOnTouchOutside(false);

        /* Set up Spinner */
        setSpinner(); /* Set the spinner that display the types */

        /* Google */
        oneTapClient = Identity.getSignInClient(this);
        signUpRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        // Your server's client ID, not your Android client ID.
                        .setServerClientId(getString(R.string.web_client_id))
                        // Show all accounts on the device.
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();

        ActivityResultLauncher<IntentSenderRequest> activityResultLauncher =
                registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        try {
                            SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(result.getData());
                            String idToken = credential.getGoogleIdToken();
                            /* Now trigger the TextFields so we can add all the required data lately in the database */
                            nameText.setText(credential.getGivenName());
                            surnameText.setText(credential.getFamilyName());
                            if (idToken != null) {
                                Log.d("TAG", "Got ID token from Google (Sign up)");
                                firebaseSignUpWithGoogle(idToken);
                            }
                        } catch (ApiException e) {
                            e.printStackTrace();
                            progressDialog.dismiss();
                        }
                    }
                });
        Googlebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* This for when the user clicks on the Sing up with Google.
                 *  With some way we need to know the type he choose */
                final CharSequence[] options = {"Owner", "Spray worker"};

                AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                builder.setTitle("Select a type");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { // This is for when the use clicks in a type
                        String selectedRole = options[which].toString();
                        autoCompleteTextView.setText(selectedRole); // trigger the type
                        oneTapClient.beginSignIn(signUpRequest)
                                .addOnSuccessListener(SignUpActivity.this, new OnSuccessListener<BeginSignInResult>() {
                                    @Override
                                    public void onSuccess(BeginSignInResult result) {
                                        IntentSenderRequest intentSenderRequest =
                                                new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender()).build();
                                        activityResultLauncher.launch(intentSenderRequest);
                                    }
                                })
                                .addOnFailureListener(SignUpActivity.this, new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // No Google Accounts found. Just continue presenting the signed-out UI.
                                        Log.d("TAG", e.getLocalizedMessage());
                                    }
                                });
                        Toast.makeText(SignUpActivity.this, "Selected type: " + selectedRole, Toast.LENGTH_SHORT).show();
                    }
                });

                builder.show();
            }
        });

        relativeLayout1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("WARNING: SUBMIT FOR SING UP IS PRESSED");
                if(checkCredentials() == 1) {
                    Toast.makeText(SignUpActivity.this, "The password must be least six characters long", Toast.LENGTH_SHORT).show();
                } else if(checkCredentials() == 0){
                    Toast.makeText(SignUpActivity.this, "The fields cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    /* Add the use to the firebase Authentication */
                    firebaseSignUp();
                }
            }
        });
    }

    private void firebaseSignUpWithGoogle(String idToken) {
        progressDialog.show();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        progressDialog.dismiss();
                        /* Check if the user is new or existing already */
                        if(authResult.getAdditionalUserInfo().isNewUser()) {
                            Toast.makeText(SignUpActivity.this, "User from google don't exist", Toast.LENGTH_SHORT).show();
                            /* We have to add him also in database */
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            String USER_EMAIL = firebaseUser.getEmail();
                            String USER_UID = firebaseUser.getUid();
                            addNewUserInDB(USER_EMAIL, USER_UID);
                        } else { // existing
                            Toast.makeText(SignUpActivity.this, "User from google already exists", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(SignUpActivity.this, "Sign up failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void firebaseSignUp() {
        progressDialog.show();
        firebaseAuth.createUserWithEmailAndPassword(String.valueOf(emailText.getText()), String.valueOf(passwdText.getText()))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(AuthResult authResult) {
                    progressDialog.dismiss();
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    String USER_EMAIL = firebaseUser.getEmail();
                    String USER_UID = firebaseUser.getUid();
                    addNewUserInDB(USER_EMAIL, USER_UID);
                }
            })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(SignUpActivity.this, "Sign up failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void addNewUserInDB(String USER_EMAIL, String USER_UID) {
        Map<String, Object> userInfo = buildUserInfoMap(USER_EMAIL, USER_UID);
        firebaseFirestore.collection("Users").add(userInfo).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                progressDialog.dismiss();
                System.out.println("WARNING: The insertion has been done successfully");
                Toast.makeText(SignUpActivity.this, "Sign up is successfully done\n" + USER_EMAIL + " with UID " + USER_UID, Toast.LENGTH_SHORT).show();
                // Open Main activity
                Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("no_google_user", nameText.getText().toString());
                startActivity(intent);
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
                progressDialog.dismiss();
                System.out.println("WARNING: The insertion has fail");
            }
        });
    }

    private Map<String, Object> buildUserInfoMap(String USER_EMAIL, String USER_UID) {
        String USER_NAME = nameText.getText().toString();
        String USER_SURNAME = surnameText.getText().toString();
        String USER_TYPE = autoCompleteTextView.getText().toString();
        /* Build the Map */
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("UID", USER_UID);
        userInfo.put("email", USER_EMAIL);
        userInfo.put("name", USER_NAME);
        userInfo.put("surname", USER_SURNAME);
        userInfo.put("type", USER_TYPE);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        userInfo.put("logInDate", currentDate);

        return userInfo;
    }

    private void setSpinner() {
        String[] type = new String[] {"Owner", "Spray worker"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                SignUpActivity.this,
                R.layout.activity_drop_down_item,
                type
        );
        autoCompleteTextView = SignUpActivity.this.findViewById(R.id.filled_exposed);
        autoCompleteTextView.setAdapter(adapter);

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                System.out.println("WARNING: In the Spinner the user clicked: " + autoCompleteTextView.getText().toString());
            }
        });
    }

    private int checkCredentials() {
        if(emailText.getText().equals("")
                || nameText.getText().equals("")
                || surnameText.getText().equals("")
                || autoCompleteTextView.getText().toString().equals("")) {
            return 0;
        }
        if(passwdText.getText().length() < 6) {
            return 1;
        }
        return 10;
    }

    private void findViewsById() {
        relativeLayout1 = findViewById(R.id.relativeLayout1);
        Googlebtn = findViewById(R.id.Googlebtn);
        emailText = findViewById(R.id.emailText);
        passwdText = findViewById(R.id.passwdText);
        nameText = findViewById(R.id.nameText);
        surnameText = findViewById(R.id.surnameText);
    }

}
