package com.example.orgoma;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
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
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    /* FrameLayout */
    private FrameLayout publicMapFrame;

    /* TextViews */
    private TextView signUpText;
    private TextView emailText;
    private TextView passwdText;

    /* Google */
    private SignInClient oneTapClient;
    private BeginSignInRequest signUpRequest;
    private static final int REQ_ONE_TAP = 2;  // Can be any integer unique to the Activity.
    private boolean showOneTapUI = true;

    /* Firebase */
    private FirebaseAuth firebaseAuth;

    /* Firestore */
    private FirebaseFirestore firebaseFirestore;

    /* Buttons */
    private Button Googlebtn;

    /* Relative layouts */
    private RelativeLayout relativeLayout1;

    /* Progress Dialog */
    private ProgressDialog progressDialog;

    /* Local Types */
    private String type = ""; /* This is the type the user is, we get it from the database based on the email */

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        findViewsById();

        /* Initialize Firestore */
        firebaseFirestore = FirebaseFirestore.getInstance();

        /* Initialize firebase auth */
        firebaseAuth = FirebaseAuth.getInstance();

        /* Progress Dialog init */
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Getting your account");
        progressDialog.setCanceledOnTouchOutside(false);

        /* Check if the user is already been logged in*/
        checkUserStatus();

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
                                    String user_email = credential.getId(); // we will use the email in the firebaseLogInWithGoogle to check if the user exists or not
                                    if (idToken != null) {
                                        // Got an ID token from Google. Use it to authenticate
                                        // with your backend.
                                        Log.d("TAG", "Got ID token from Google (Log in)");

                                        /* We need to know if the user already exists in the Firebase Auth.
                                         * If the user DON'T exist we need to STOP THE PROCESS.
                                         * The problem here is that firebaseAuth.signInWithCredential(...) creates automatically the user,
                                         * so we need to handle it separately in the code.
                                         *
                                         * At first we need to know if the user with the specific email that we get from Google Sign in exist.
                                         * If the email is stored in the Firebase Firestore that means that the user exists so we proceed in the
                                         * function  firebaseLogInWithGoogle(idToken).
                                         * This function uses firebaseAuth.signInWithCredential(...) but know we are sure that the user exists so there is no
                                         * chance that the user will be created.
                                         * We that way we handle the user LOG IN.
                                         */
                                        checkIfUserExists_safe_way(user_email, idToken);
                                    }
                                } catch (ApiException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        Googlebtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                oneTapClient.beginSignIn(signUpRequest)
                                        .addOnSuccessListener(LoginActivity.this, new OnSuccessListener<BeginSignInResult>() {
                                            @Override
                                            public void onSuccess(BeginSignInResult result) {
                                                IntentSenderRequest intentSenderRequest =
                                                        new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender()).build();
                                                activityResultLauncher.launch(intentSenderRequest);
                                            }
                                        })
                                        .addOnFailureListener(LoginActivity.this, new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // No Google Accounts found. Just continue presenting the signed-out UI.
                                                Log.d("TAG", e.getLocalizedMessage());
                                            }
                                        });

                            }
                        });
        relativeLayout1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("WARNING: Login in Login activity pressed");
                int status = checkCredentials();
                if(status == 0) {
                    Toast.makeText(LoginActivity.this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                } else if(status == 1) {
                    Toast.makeText(LoginActivity.this, "The password must be least six characters long", Toast.LENGTH_SHORT).show();
                } else {
                    firebaseLogIn();
                }

            }
        });

        signUpText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SignUpActivity.class);
                startActivity(intent);
            }
        });

        publicMapFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("WARNING: Public Map clicked");
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("visitor", "Yes");
                startActivity(intent);
            }
        });
    }

    private void checkUserStatus() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if(firebaseUser != null) {
            progressDialog.show();
            calcUserLogInDate(firebaseUser);
        }
    }

    private void calcUserLogInDate(FirebaseUser firebaseUser) {
        String UID = firebaseUser.getUid();
        firebaseFirestore.collection("Users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {

                        String email = document.getString("UID");
                        String logInDate = document.getString("logInDate");
                        String name = document.getString("name");

                        if(email.equals(UID)) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            String currentDate_str = dateFormat.format(new Date());
                            DocumentReference userDocument = firebaseFirestore.collection("Users").document(document.getId());
                            //
                            try {
                                Date currentDateDatabase = dateFormat.parse(logInDate);
                                Date currentDate = dateFormat.parse(currentDate_str);
                                // difference in milliseconds
                                long differenceInMillis = currentDate.getTime() - currentDateDatabase.getTime();
                                // difference in days
                                long differenceInDays = differenceInMillis / (24 * 60 * 60 * 1000);
                                // differenceInDays contains the difference in days
                                System.out.println("Difference in days: " + differenceInDays);
                                if(differenceInDays > 1) {
                                    /* Sign out the user */
                                    firebaseAuth.signOut();
                                    Intent intent = new Intent(LoginActivity.this, LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                    progressDialog.dismiss();
                                } else {
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.putExtra("no_google_user", name);
                                    startActivity(intent);
                                    finish();
                                    progressDialog.dismiss();
                                }
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            //
                        }
                    }
                    progressDialog.dismiss();
                } else {
                    Log.e("TAG", "Error getting documents: ", task.getException());
                    progressDialog.dismiss();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
                progressDialog.dismiss();
            }
        });
    }

    /* This function checks if the user exist in the Firebase Firestore */
    private void checkIfUserExists_safe_way(String user_email, String idToken) {
        progressDialog.show();
        firebaseFirestore.collection("Users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        if(document.getString("email").equals(user_email)) {
                            /* User exists */
                            Toast.makeText(LoginActivity.this, "User from Google exist", Toast.LENGTH_SHORT).show();
                            firebaseLogInWithGoogle(idToken, user_email);
                        }
                    }
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "The user from Google don't exist", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Error checking user existence: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("TAG", "Error getting documents: ", task.getException());
                    progressDialog.dismiss();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
                progressDialog.dismiss();
            }
        });
    }

    /* ON Sept 15 2023: THE FUNCTION firebaseAuth.fetchSignInMethodsForEmail(...) IS DEPRECATED, TO PREVENT EMAIL ENUMERATION ATTACKS
    *  This function help us see if the user with an specific email exists directly in the Firebase Authentication.
    *
    * USE: checkIfUserExists_safe_way(...)
    private void checkIfUserExists(String idToken, String user_email) {
        progressDialog.show();
        // We will try to fetch from the Firebase Auth the user, base on the email we get from Google Log in
        // If the fetch fail then we will be sure that the user DON'T exist
        firebaseAuth.fetchSignInMethodsForEmail(user_email)
                .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                    @Override
                    public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                        if (task.isSuccessful()) {
                            SignInMethodQueryResult result = task.getResult();
                            if (result != null && result.getSignInMethods() != null && result.getSignInMethods().isEmpty()) {
                                // User doesn't exist
                                Toast.makeText(LoginActivity.this, "User from Google doesn't exist", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            } else {
                                progressDialog.dismiss();
                                // The user exists do proceed in the Main View
                                // Open Main activity
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(LoginActivity.this, "Error checking user existence: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    */

    /* Log in (Sing in) with Google
    * no need to crate the use because the user already exists*/
    private void firebaseLogInWithGoogle(String idToken, String user_email) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        progressDialog.dismiss();
                        if(authResult.getAdditionalUserInfo().isNewUser()) {
                            Toast.makeText(LoginActivity.this, "User from google don't exist", Toast.LENGTH_SHORT).show();
                        } else { // existing
                            Toast.makeText(LoginActivity.this, "Successful login using Google", Toast.LENGTH_LONG).show();
                            findUserIdInDatabase_baseOnEmail(user_email, idToken);
                            /*
                            // Open Main activity
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("visitor", "No");
                            startActivity(intent);
                            finish();*/
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, "Sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void firebaseLogIn() {
        progressDialog.show();

        firebaseAuth.signInWithEmailAndPassword(String.valueOf(emailText.getText()), String.valueOf(passwdText.getText()))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        String USER_EMAIL = firebaseUser.getEmail();
                        String USER_UID = firebaseUser.getUid();
                        findUserIdInDatabase_baseOnEmail(USER_EMAIL, USER_UID);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, "Sign in (Log in) failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /* Find the user in database to get the ID of the Document and then Update the field logInDate to the current date */
    private void findUserIdInDatabase_baseOnEmail(String USER_EMAIL, String USER_UID) {
        firebaseFirestore.collection("Users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {

                        String email = document.getString("email");
                        String name = document.getString("name");

                        if(email.equals(USER_EMAIL)) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            String currentDate = dateFormat.format(new Date());
                            DocumentReference userDocument = firebaseFirestore.collection("Users").document(document.getId());
                            userDocument.update("logInDate", currentDate)
                                    .addOnSuccessListener(aVoid -> {
                                        // Login date updated successfully
                                        Toast.makeText(LoginActivity.this, "Sign in (Log in) is successfully done\n" + USER_EMAIL + " with UID " + USER_UID, Toast.LENGTH_SHORT).show();
                                        Toast.makeText(LoginActivity.this, "The logInDate successfully updated to " + currentDate, Toast.LENGTH_SHORT).show();
                                        // Open Main activity
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        intent.putExtra("no_google_user", name);
                                        intent.putExtra("email", USER_EMAIL);
                                        intent.putExtra("visitor", "No");
                                        startActivity(intent);
                                        finish();
                                        progressDialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> {
                                        e.printStackTrace();
                                        Toast.makeText(LoginActivity.this, "Failed to update login time: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        progressDialog.dismiss();
                                    });
                        }
                    }
                    progressDialog.dismiss();
                } else {
                    Log.e("TAG", "Error getting documents: ", task.getException());
                    progressDialog.dismiss();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
                progressDialog.dismiss();
            }
        });
    }

    /*
    * Password is empty OR email is empty return 0
    * Password is less than 6 return 1
    * if all is fine return 10 */
    private int checkCredentials() {
        if(passwdText.getText().equals("") || emailText.getText().equals("")) {
            return 0;
        }
        if(passwdText.getText().length() < 6) {
            return 1;
        }
        return 10;
    }

    private void findViewsById() {
        publicMapFrame = findViewById(R.id.publicMapFrame);
        signUpText = findViewById(R.id.signUpText);
        Googlebtn = findViewById(R.id.Googlebtn);
        relativeLayout1 = findViewById(R.id.relativeLayout1);
        emailText = findViewById(R.id.emailText);
        passwdText = findViewById(R.id.passwdText);
    }
}
