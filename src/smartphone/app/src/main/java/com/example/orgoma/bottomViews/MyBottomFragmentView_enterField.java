package com.example.orgoma.bottomViews;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.orgoma.MainActivity;
import com.example.orgoma.R;
import com.example.orgoma.appSounds.SoundPlayer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Map;

public class MyBottomFragmentView_enterField extends BottomSheetDialogFragment {

    private String ownersName;
    private String ownersEmail;
    private TextView nameText;
    private TextView emailText;
    private TextView submitText;

    /* Spinners */
    private String selectedOptionOrganicSpinner = "";
    private String selectedOliveVarietyOrganicSpinner = "";
    private String selectedSpayedOrganicSpinner = "";
    private AutoCompleteTextView autoCompleteTextView_spayed;

    /* Progress Dialog */
    private ProgressDialog progressDialog;

    /* Firebase firestore */
    private FirebaseFirestore firebaseFirestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_fragment_bottom_window, container, false);

        findViewById(view);

        /* Initialize Firestore */
        firebaseFirestore = FirebaseFirestore.getInstance();

        /* Progress Dialog init */
        progressDialog = new ProgressDialog(view.getContext());
        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        /* Get the email and name of the owner, automatically */
        Bundle args = getArguments();
        if (args != null) {
            ownersName = args.getString("ownersName", "NaN");
            ownersEmail = args.getString("ownersEmail", "NaN");
            //
            nameText.setText(ownersName);
            emailText.setText(ownersEmail);
        }

        /* Filled the spinners with elements */
        fillSpinnerForOrganic(view);
        fillSpinnerForOliveVariety(view);
        fillSpinnerForSprayed();

        submitText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("WARNING: Submit field pressed");
                if(!selectedOliveVarietyOrganicSpinner.equals("") &&
                         !selectedOptionOrganicSpinner.equals("") &&
                         !selectedSpayedOrganicSpinner.equals("") &&
                                   !nameText.getText().equals("") &&
                                 !emailText.getText().equals("")) {
                    dismiss();
                    /* Add the field in database */
                    storeFieldInDatabase(view, nameText.getText().toString(),
                                         emailText.getText().toString(),
                                         selectedOliveVarietyOrganicSpinner,
                                         selectedOptionOrganicSpinner,
                                         selectedSpayedOrganicSpinner);
                } else {
                    Toast.makeText(view.getContext(), "Please add all info", Toast.LENGTH_LONG).show();
                }
            }
        });

        return view;
    }

    private void fillSpinnerForOliveVariety(View view) {
        String[] type = new String[] {"Castelvetrano", "Cerignola", "Kalamata", "Arbequina", "Niçoise", "Picholine", "Gaeta", "Lucques", "Gordal"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.activity_drop_down_item,
                type
        );
        AutoCompleteTextView autoCompleteTextView = view.findViewById(R.id.oliveVarietyFilled_exposed);
        autoCompleteTextView.setAdapter(adapter);

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                System.out.println("WARNING: In the Spinner the user clicked: " + autoCompleteTextView.getText().toString());
                String selectedOption = (String) adapterView.getItemAtPosition(i);
                selectedOliveVarietyOrganicSpinner = selectedOption;
            }
        });
    }

    private void fillSpinnerForOrganic(View view) {
        String[] type = new String[] {"Yes", "No"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.activity_drop_down_item,
                type
        );
        AutoCompleteTextView autoCompleteTextView = view.findViewById(R.id.organicFilled_exposed);
        autoCompleteTextView.setAdapter(adapter);

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                System.out.println("WARNING: In the Spinner the user clicked: " + autoCompleteTextView.getText().toString());
                String selectedOption = (String) adapterView.getItemAtPosition(i);
                if(selectedOption.equals("Yes")) {
                    autoCompleteTextView_spayed.setEnabled(false);
                    autoCompleteTextView_spayed.setText("No");
                    selectedSpayedOrganicSpinner = "No";
                } else {
                    autoCompleteTextView_spayed.setEnabled(true);
                    fillSpinnerForSprayed();
                }
                selectedOptionOrganicSpinner = selectedOption;
            }
        });
    }

    private void fillSpinnerForSprayed() {
        String[] type = new String[] {"Yes", "No"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.activity_drop_down_item,
                type
        );
        autoCompleteTextView_spayed.setAdapter(adapter);

        autoCompleteTextView_spayed.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                System.out.println("WARNING: In the Spinner the user clicked: " + autoCompleteTextView_spayed.getText().toString());
                String selectedOption = (String) adapterView.getItemAtPosition(i);
                selectedSpayedOrganicSpinner = selectedOption;
            }
        });
    }

    public void storeFieldInDatabase(View view, String owner_name, String owner_email, String olive_variety, String organic_farming, String spayed) {
        progressDialog.show();
        Map<String, Object> userInfo = MainActivity.buildFieldInfoMap(owner_name, owner_email, olive_variety, organic_farming, spayed);
        firebaseFirestore.collection("Fields").add(userInfo).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                progressDialog.dismiss();
                System.out.println("WARNING: The insertion has been done successfully");
                /* If everything go well mark the field in the Map */
                MainActivity.drawPolygonBasedOnMarkers(selectedOptionOrganicSpinner, organic_farming, spayed);
                Toast.makeText(view.getContext(), "Insertion of field done", Toast.LENGTH_LONG).show();
                SoundPlayer soundPlayer = new SoundPlayer(view.getContext(), R.raw.success_sound);
                if(!soundPlayer.isPlaying())
                    soundPlayer.playSound();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
                Toast.makeText(view.getContext(), "The insertion of field has fail", Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
                System.out.println("WARNING: The insertion of field has fail");
            }
        });
    }

    private void findViewById(View view) {
        nameText = view.findViewById(R.id.nameText);
        emailText = view.findViewById(R.id.emailText);
        submitText = view.findViewById(R.id.submitText);
        autoCompleteTextView_spayed = view.findViewById(R.id.spayed_exposed);
    }
}
