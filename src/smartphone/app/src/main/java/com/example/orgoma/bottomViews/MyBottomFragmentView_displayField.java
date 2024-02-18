package com.example.orgoma.bottomViews;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.orgoma.MainActivity;
import com.example.orgoma.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class MyBottomFragmentView_displayField extends BottomSheetDialogFragment {

    private TextView nameText;
    private TextView emailText;
    private TextView organicFilledText;
    private TextView oliveVarietyFilledText;
    private TextView cityText;
    private TextView sprayedText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_fragment_bottom_window_display, container, false);

        findViewById(view);

        /* Get the email, name of the owner, type and olive variety, automatically from the previous view */
        Bundle args = getArguments();
        if (args != null) {
            String currentUser = args.getString("currentUser", "NaN");
            String ownersName = args.getString("ownersName", "NaN");
            String ownersEmail = args.getString("ownersEmail", "NaN");
            String organicFilled = args.getString("organicFilled", "NaN");
            String oliveVarietyFilled = args.getString("oliveVarietyFilled", "NaN");
            String city_located = args.getString("city_located", "NaN");
            String spayed = args.getString("spayed", "NaN");
            //
            nameText.setText(ownersName);
            emailText.setText(ownersEmail);
            organicFilledText.setText(organicFilled);
            oliveVarietyFilledText.setText(oliveVarietyFilled);
            cityText.setText(city_located);
            sprayedText.setText(spayed);
        }
        return view;
    }

    private void findViewById(View view) {
        nameText = view.findViewById(R.id.nameText);
        emailText = view.findViewById(R.id.emailText);
        organicFilledText = view.findViewById(R.id.organicFilledText);
        oliveVarietyFilledText = view.findViewById(R.id.oliveVarietyFilledText);
        cityText = view.findViewById(R.id.cityText);
        sprayedText = view.findViewById(R.id.sprayedText);
    }
}
