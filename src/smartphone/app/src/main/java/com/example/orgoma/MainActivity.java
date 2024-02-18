package com.example.orgoma;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentContainerView;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

import com.example.orgoma.appSounds.SoundPlayer;
import com.example.orgoma.helpers.Field;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;

import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.orgoma.bottomViews.MyBottomFragmentView_displayField;
import com.example.orgoma.bottomViews.MyBottomFragmentView_enterField;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.model.Document;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity  implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener, GoogleMap.OnMyLocationChangeListener {

    /* Animation */
    private AnimatorSet rotateAndColorChangeAnimator;
    /* ImageView */
    private ImageView imageUserView;
    /* TextViews */
    private TextView userUsernameText;
    private TextView markText;
    private TextView recordText;
    private TextView typeText;
    private TextView addMarkText;
    private TextView saveMarkText;
    private TextView ordersMarkText;
    private static TextView cancelMarkText;
    /* Record */
    private TextView startRecordText;
    private TextView stopRecordText;
    private TextView cancelRecordText;
    private TextView saveRecordText;
    /* FirebaseAuth */
    private FirebaseAuth firebaseAuth;

    /* Progress Dialog */
    private ProgressDialog progressDialog;

    /* HorizontalScrollViews */
    private HorizontalScrollView functionsHorizontalScrollView;

    /* Linear Layouts */
    private LinearLayout functionsLinearLayout;

    /* Fragments */
    private FragmentContainerView mapFragment;

    /* Firebase Firestore */
    private FirebaseFirestore firebaseFirestore;

    /* Local Types */
    private String no_google_user;

    /*-------------------------------------------------------------------------------------------------------*/

    /* Google Map */
    private static GoogleMap myGoogleMap;
    private Marker default_marker;
    private static ArrayList<Marker> markersArrayList; // This List represents the markers the user adds to mark a field

    /* For deletion of polygons (fields) in the Map */
    private static ArrayList<Field> loaded_fields; // Fields that already have been added in the Map (and also in the database)
    private static GeoPoint clicked_cord1;
    private static GeoPoint clicked_cord2;
    private static GeoPoint clicked_cord3;
    private static GeoPoint clicked_cord4;

    /* Location */
    private LocationRequest locationRequest;
    private boolean isRecording = false; // Flag to check if recording is in progress
    private List<LatLng> recordedPath = new ArrayList<>();
    private double user_lat_GPS;
    private double user_lng_GPS;
    private Polyline currentPolyline; /* the 'current' line that represent the 'current movement of the user when the app is recording' */
    private ArrayList<Polyline> whole_path_polylines = new ArrayList<>(); /* This represents the whole path of the user bad in Polylines, it takes
                                                                             all the lines (Polylines) the user have made because of teh movement he do.
                                                                             We will user it in the Cancel button because when the user click in Cancel a record
                                                                             the Polyline should removed from the Map*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById();

        Intent intent = getIntent();
        String visitor = intent.getStringExtra("visitor");
        no_google_user = intent.getStringExtra("no_google_user");
        if(visitor != null && visitor.equals("Yes")) {
            findViewById(R.id.myToolBar).setVisibility(View.GONE);
            functionsHorizontalScrollView.setVisibility(View.GONE);
        }

        loaded_fields = new ArrayList<>();

        /* Ask for notifications permission */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[] {Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        /* Initialize Firestore */
        firebaseFirestore = FirebaseFirestore.getInstance();

        /* Progress Dialog init */
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        progressDialog.show();
        firebaseAuth = FirebaseAuth.getInstance();
        if(visitor == null || visitor.equals("No")) {
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            userUsernameText.setText(firebaseUser.getEmail());
            getTypeOfUser(firebaseUser.getEmail()); // add the type in the TextFields (typeText)
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

        markersArrayList = new ArrayList<>();

        mapFragment.getMapAsync(this);

        mapFragment.getMapAsync(googleMap -> {
            googleMap.setOnCameraMoveListener(() -> onCameraMove(googleMap));
            googleMap.setOnMapClickListener(latLng -> onMapClick(latLng, googleMap));
            googleMap.setOnMapLongClickListener(latLng -> onMapLongClick(latLng, googleMap));
        });

        imageUserView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra("email", userUsernameText.getText().toString());
                startActivity(intent);
            }
        });

        /* --------------Mark-------------- */
        if(markText != null) {
            markText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    System.out.println("The user click --Mark field--");
                    removeAllViewsFrom_functionsLinearLayout();
                    /* Make the Horizontal View Green */
                    functionsHorizontalScrollView.setBackgroundColor(Color.parseColor("#00ff13"));
                    /* Set Visible the TextVies for the Mark function */
                    cancelMarkText.setVisibility(View.VISIBLE);
                    addMarkText.setVisibility(View.VISIBLE);
                    ordersMarkText.setVisibility(View.VISIBLE);

                    /* Show a Marker in the Map */
                    LatLng initialPosition = new LatLng(38.427083893443374, 24.05007200564581);
                    default_marker = myGoogleMap.addMarker(new MarkerOptions().position(initialPosition).title("Marker Title"));

                }
            });
        }

        if(cancelMarkText != null) {
            cancelMarkText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    removeMarkersFromMap();
                    System.out.println("The user click --Cancel--, to cancel the function of marking a field");
                    reCreateAllViewsFrom_functionsLinearLayout();
                    ordersMarkText.setText("/4 markers");
                    functionsHorizontalScrollView.setBackgroundColor(Color.parseColor("#CBB26A"));
                    default_marker.remove();
                }
            });
        }

        if(addMarkText != null) {
            addMarkText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    System.out.println("The user click --Add--, to add a field");
                    LatLng default_marker_position = default_marker.getPosition();

                    /* Check if the selected position is in another (already existing polygon) */
                    checkIfMarkerPolygonCanAdded(default_marker_position);
                }
            });
        }

        if(saveMarkText != null) {
            saveMarkText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    /* Open bottom fragment */
                    MyBottomFragmentView_enterField bottomSheetDialogFragment = new MyBottomFragmentView_enterField();
                    /* But extra values to the bottomSheetDialogFragment */
                    Bundle args = new Bundle();
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if((firebaseUser != null)) {
                        if(firebaseUser.getDisplayName() != null) {
                            if (!firebaseUser.getDisplayName().equals("")) {
                                args.putString("ownersName", firebaseUser.getDisplayName());
                            }
                        } else {
                            args.putString("ownersName", no_google_user);
                        }
                    }
                    args.putString("ownersEmail", userUsernameText.getText().toString());
                    bottomSheetDialogFragment.setArguments(args);
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            });
        }

        /* --------------Record-------------- */
        if(recordText != null) {
            recordText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    System.out.println("The user click --Record field--");
                    removeAllViewsFrom_functionsLinearLayout();
                    /* Make the Horizontal View Yellow, the recording has not start yet */
                    functionsHorizontalScrollView.setBackgroundColor(Color.parseColor("#fff700"));
                    /* Set Visible the TextVies for the Record function */
                    startRecordText .setVisibility(View.VISIBLE);
                    stopRecordText .setVisibility(View.VISIBLE);
                    cancelRecordText  .setVisibility(View.VISIBLE);
                }
            });
        }

        if(cancelRecordText != null) {
            cancelRecordText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    System.out.println("The user click --Cancel--, to cancel the function of recording a field");
                    reCreateAllViewsFrom_functionsLinearLayout();
                    functionsHorizontalScrollView.setBackgroundColor(Color.parseColor("#CBB26A"));
                    stopRecording();
                    recordedPath.clear();
                    if(currentPolyline != null)
                        currentPolyline.remove();
                    /* Remove all the Polylines from the Map */
                    removeAllPolylines();
                    disableLocationOnGoogleMap();
                }
            });
        }

        if(startRecordText != null) {
            startRecordText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    System.out.println("The user click --Start recording--, te recording will start");
                    progressDialog.show();
                    if (isGPSEnabled()) { // the Location is turned on
                        getCurrentLocation();
                    } else { // We need to turn on the location
                        Toast.makeText(MainActivity.this, "GPS is not enabled, the app needs to enable it", Toast.LENGTH_SHORT).show();
                        turnOnGPS();
                    }
                }
            });
        }

        if(stopRecordText != null) {
            stopRecordText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    System.out.println("The user click --Stop recording--, te recording will stopped");
                    functionsHorizontalScrollView.setBackgroundColor(Color.parseColor("#fff700"));
                    stopRecording();
                }
            });
        }

    }

    /* ------------------------------------------------------------The following are for the Location------------------------------------------------------------*/
    private void turnOnGPS () {
        progressDialog.show();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                .checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {

                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    Toast.makeText(MainActivity.this, "GPS is already turned on", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                } catch (ApiException e) {
                    switch (e.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                resolvableApiException.startResolutionForResult(MainActivity.this, 2);
                            } catch (IntentSender.SendIntentException ex) {
                                ex.printStackTrace();
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            //Device does not have location
                            Log.e("Location", "The device do not have GPS");
                            showAlertDialog(MainActivity.this,  "Warning", "The device do not have GPS, you cannot proceed in the app", 1);
                            progressDialog.dismiss();
                            break;
                    }
                }
            }
        });
    }

    /* Solves the problem with the first time ... */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 2) {
            if (resultCode == Activity.RESULT_OK) {
                getCurrentLocation();
            } else {
                Toast.makeText(MainActivity.this, "GPS did not open, some features require it", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        }
    }

    /* From GitHub --> https://github.com/Pritish-git/get-Current-Location/blob/main/MainActivity.java#L103 */
    private void getCurrentLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                if (isGPSEnabled()) {

                    LocationServices.getFusedLocationProviderClient(MainActivity.this)
                            .requestLocationUpdates(locationRequest, new LocationCallback() {
                                @Override
                                public void onLocationResult(@NonNull LocationResult locationResult) {
                                    super.onLocationResult(locationResult);

                                    LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                            .removeLocationUpdates(this);

                                    if (locationResult != null && locationResult.getLocations().size() > 0){

                                        enableLocationOnGoogleMap();

                                        int index = locationResult.getLocations().size() - 1;
                                        double lat = locationResult.getLocations().get(index).getLatitude();
                                        double lng = locationResult.getLocations().get(index).getLongitude();

                                        Toast.makeText(MainActivity.this, "Location from GPS: " + lat + ", " + lng, Toast.LENGTH_SHORT).show();
                                        Log.e("Location from GPS", "Location from GPS: " + lat + ", " + lng);
                                        user_lat_GPS = lat;
                                        user_lng_GPS = lng;
                                        Log.e("Location", "The variables user_lat_GPS == " + user_lat_GPS + "and the user_lng_GPS == " + user_lng_GPS);
                                        progressDialog.dismiss();
                                        if(isRecording == false) {
                                            progressDialog.dismiss();
                                            startRecording(lat, lng);
                                        }
                                    }
                                }
                            }, Looper.getMainLooper());

                } else {
                    turnOnGPS();
                }

            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    private boolean isGPSEnabled() {
        LocationManager locationManager = null;
        boolean isEnabled = false;

        if(locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isEnabled;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PackageManager.PERMISSION_GRANTED) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                System.out.println("Location permission granted");
            } else {
                // Permission denied
                System.out.println("Location permission denied");
                showAlertDialog(MainActivity.this, "Warning", "The app needs access to the location of the device to function properly\nPlease enable the location permission in Settings", 1);
            }
        }
    }
    /*------------------------------------------------------------End of location------------------------------------------------------------*/

    /* In this function we check if the polygon can be created,
     * WE DO THIS BECAUSE A A FIELD CAN NOT BE IN THE TOP OF ANOTHER FIELD */
    private void checkIfMarkerPolygonCanAdded(LatLng newPointlatLng) {
        progressDialog.show();
        /* Retrieve data from database */
        firebaseFirestore.collection("Fields").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    PolygonOptions polygonOptions = null;
                    boolean state = false; // false: the new do not override, true: the new override
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        System.out.println("Document Data: " + document.getData());

                        GeoPoint cord1 = document.getGeoPoint("cord_1");
                        LatLng latLng1= new LatLng(cord1.getLatitude(), cord1.getLongitude());

                        GeoPoint cord2 = document.getGeoPoint("cord_2");
                        LatLng latLng2= new LatLng(cord2.getLatitude(), cord2.getLongitude());

                        GeoPoint cord3 = document.getGeoPoint("cord_3");
                        LatLng latLng3= new LatLng(cord3.getLatitude(), cord3.getLongitude());

                        GeoPoint cord4 = document.getGeoPoint("cord_4");
                        LatLng latLng4= new LatLng(cord3.getLatitude(), cord3.getLongitude());

                        if (cord1 != null && cord2 != null && cord3 != null && cord4 != null) {
                            polygonOptions = new PolygonOptions().add(latLng1, latLng2, latLng3, latLng4)
                                    .strokeColor(Color.RED)
                                    .fillColor(Color.argb(128, 255, 0, 0));
                            if (PolyUtil.containsLocation(newPointlatLng, polygonOptions.getPoints(), true)) {
                                showAlertDialog(MainActivity.this, "Warning", "You can't mark inside any existing field", 0);
                                state = true;
                                break;
                            }
                        }
                    }
                    if(state == false) {
                        /* If the marker is valid continue in the add of the field */
                        addMarker(newPointlatLng);
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

    /* Check if the new Polygon includes any already loaded field (Polygon) */
    private boolean checkIfWholePolygonCanAdded(List<LatLng> new_polygon) {
        //List<LatLng> newPolygonLatLng = new_polygon.getPoints();
        for(int i = 0;i < loaded_fields.size();i++) {
            List<LatLng> existingPolygonLatLng = loaded_fields.get(i).getPolygon().getPoints();
            for (LatLng vertex : existingPolygonLatLng) {
                if (PolyUtil.containsLocation(vertex, new_polygon, true)) {
                    return true;
                }
            }
        }
        return false;
     }

    /* Add the makrer to the map */
    private void addMarker(LatLng default_marker_position) {
        Toast.makeText(MainActivity.this, "Lat: " + default_marker_position.latitude + "\nLon:" + default_marker_position.longitude, Toast.LENGTH_SHORT).show();
        /* Add a marker to that position */
        LatLng staticMarkerPosition = new LatLng(default_marker_position.latitude, default_marker_position.longitude);
        MarkerOptions markerOptions = new MarkerOptions().position(staticMarkerPosition).title("Field Marker");
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_30));
        Marker pinnedMarker = myGoogleMap.addMarker(markerOptions);
        ordersMarkText.setText(markersArrayList.size() + 1 + "/4 markers");
        markersArrayList.add(pinnedMarker);
        /* Check if the user has add 4 markers */
        if(markersArrayList.size() == 4) {
            /* Initialize the new polygon the user temporary created */
            ArrayList<LatLng> markerCoordinates = new ArrayList<>();
            for (Marker marker : markersArrayList) {
                LatLng position = marker.getPosition();
                System.out.println("TESTING: LAT --> " + position.latitude + " LONG --> " + position.longitude);
                markerCoordinates.add(position);
            }
            /* Check if the new polygon include inside of it any already defined polygon */
            if(checkIfWholePolygonCanAdded(markerCoordinates) == false) { // the new polygon don't include
                functionsHorizontalScrollView.setBackgroundColor(Color.parseColor("#ff0000")); // make red the functionsHorizontalScrollView
                addMarkText.setVisibility(View.GONE); // The use must not add more markers
                saveMarkText.setVisibility(View.VISIBLE);
                /* Open bottom fragment */
                MyBottomFragmentView_enterField bottomSheetDialogFragment = new MyBottomFragmentView_enterField();
                /* But extra values to the bottomSheetDialogFragment */
                Bundle args = new Bundle();
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if((firebaseUser != null)) {
                    if(firebaseUser.getDisplayName() != null) {
                        if (!firebaseUser.getDisplayName().equals("")) {
                            args.putString("ownersName", firebaseUser.getDisplayName());
                        }
                    } else {
                        args.putString("ownersName", no_google_user);
                    }
                }
                args.putString("ownersEmail", userUsernameText.getText().toString());
                bottomSheetDialogFragment.setArguments(args);
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
            } else { // the new polygon include
                cancelMarkText.performClick();
                showAlertDialog(MainActivity.this, "Warning", "You can't override any existing field", 0);
            }
        }
    }

    private void getTypeOfUser(String USER_EMAIL) {
        /* Retrieve data from database */
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseFirestore.collection("Users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        if(document.getString("email").equals(USER_EMAIL)) {
                            String type = document.getString("type");
                            typeText.setText(type);
                            setUpAvailableFunctions();
                            /* When all data have been taken, close the ProgressDialog */
                            progressDialog.dismiss();
                        }
                    }
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

    /* Based in the type of user we have to load specific TextViews in the HorizontalScrollView */
    private void setUpAvailableFunctions() {
        functionsHorizontalScrollView.setVisibility(View.VISIBLE);
        if(typeText.getText().equals("Owner")) {
            markText.setVisibility(View.VISIBLE);
            recordText.setVisibility(View.VISIBLE);
        } else {
            recordText.setVisibility(View.VISIBLE);
        }
    }

    private void removeMarkersFromMap() {
        /* Remove the markers the user have previously added */
        for (int i = 0;i < markersArrayList.size();i++) {
            markersArrayList.get(i).remove();
        }
        markersArrayList.removeAll(markersArrayList);
    }

    private void removeAllViewsFrom_functionsLinearLayout() {
        markText.setVisibility(View.GONE);
        recordText.setVisibility(View.GONE);
    }

    private void reCreateAllViewsFrom_functionsLinearLayout() {
        if(typeText.getText().equals("Owner")) {
            markText.setVisibility(View.VISIBLE);
            /* For Mark */
            cancelMarkText.setVisibility(View.GONE);
            addMarkText.setVisibility(View.GONE);
            ordersMarkText.setVisibility(View.GONE);
            saveMarkText.setVisibility(View.GONE);
        }
        if(typeText.getText().equals("Owner") || typeText.getText().equals("Spray worker")) {
            recordText.setVisibility(View.VISIBLE);
            saveMarkText.setVisibility(View.GONE);
            /* For Record */
            startRecordText.setVisibility(View.GONE);
            stopRecordText.setVisibility(View.GONE);
            cancelRecordText.setVisibility(View.GONE);
        }
    }

    /* IF TYPE IS 1 THEN THE Alert Dialog is about the location permission */
    private void showAlertDialog(Context context, String title, String message, int type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Set the title and message
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(type == 1) { // the user is not allowing the app to access the location, so the Main Activity will close an the user is gonna go in the Login View
                            /* Go the user back in the Log in/ Sing up View
                            firebaseAuth.signOut();
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();*/
                            /* Try again to enable the GPS */
                            turnOnGPS ();
                        }
                        // Positive button click action (e.g., dismiss the dialog)
                        dialog.dismiss();
                    }
                });

        // Create and show the dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void findViewById() {
        imageUserView = findViewById(R.id.imageUserView);
        userUsernameText = findViewById(R.id.userUsernameText);
        typeText = findViewById(R.id.typeText);
        functionsHorizontalScrollView = findViewById(R.id.functionsHorizontalScrollView);
        markText = findViewById(R.id.markText);
        recordText = findViewById(R.id.recordText);
        mapFragment = findViewById(R.id.mapFragment);
        addMarkText = findViewById(R.id.addMarkText);
        saveMarkText = findViewById(R.id.saveMarkText);
        ordersMarkText = findViewById(R.id.ordersMarkText);
        cancelMarkText = findViewById(R.id.cancelMarkText);
        functionsLinearLayout = findViewById(R.id.functionsLinearLayout);
        /* Record TextViews */
        startRecordText = findViewById(R.id.startRecordText);
        stopRecordText = findViewById(R.id.stopRecordText);
        cancelRecordText = findViewById(R.id.cancelRecordText);
        saveRecordText = findViewById(R.id.saveRecordText);
        rotateAndColorChangeAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.rotate_and_color_change);
        rotateAndColorChangeAnimator.setTarget(saveRecordText);
    }

    /*--------------------------------------------------------- Google Map ---------------------------------------------------------*/

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myGoogleMap = googleMap;

        /* Location */
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        /* Request location permission from the user */
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    PackageManager.PERMISSION_GRANTED);
        }
        /*-------------------------------------------------------------*/

        /* Load all the fields users already assign */
        loadFieldInTheMap();

        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        /* Navigate camera to Greece in the start */
        navCameraToHome();
    }


    private void enableLocationOnGoogleMap() {
        if(isGPSEnabled()) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                // Permission is already granted
                myGoogleMap.setMyLocationEnabled(true);
                myGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
                myGoogleMap.setOnMyLocationButtonClickListener(this);
                myGoogleMap.setOnMyLocationClickListener(this);
                myGoogleMap.setOnMyLocationChangeListener(this);
            }

        }
    }

    private void disableLocationOnGoogleMap() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            myGoogleMap.setMyLocationEnabled(false);
            myGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
            myGoogleMap.setOnMyLocationButtonClickListener(null);
            myGoogleMap.setOnMyLocationClickListener(null);
            myGoogleMap.setOnMyLocationChangeListener(null);
        }
    }


    private void onCameraMove(GoogleMap googleMap) {
        if(default_marker != null) {
            LatLng centerOfMap = myGoogleMap.getCameraPosition().target;
            default_marker.setPosition(centerOfMap);
        }
    }

    private void navCameraToHome() {
        LatLng latGreece = new LatLng(38.427083893443374, 24.05007200564581);
        myGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latGreece));
        myGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latGreece, 6.0f));
    }

    /* This method is for loading all the fields the users have previously add in the Google map */
    private void loadFieldInTheMap() {
        progressDialog.show();
        /* Retrieve data from database */
        firebaseFirestore.collection("Fields").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        System.out.println("Document Data: " + document.getData());
                        String organic_farming = document.getString("organic_farming");
                        String sprayed = document.getString("sprayed");
                        GeoPoint cord1 = document.getGeoPoint("cord_1");
                        GeoPoint cord2 = document.getGeoPoint("cord_2");
                        GeoPoint cord3 = document.getGeoPoint("cord_3");
                        GeoPoint cord4 = document.getGeoPoint("cord_4");
                        if (organic_farming != null && cord1 != null && cord2 != null && cord3 != null && cord4 != null) {
                            ArrayList<LatLng> markerCoordinates = new ArrayList<>();
                            markerCoordinates.add(new LatLng(cord1.getLatitude(), cord1.getLongitude()));
                            markerCoordinates.add(new LatLng(cord2.getLatitude(), cord2.getLongitude()));
                            markerCoordinates.add(new LatLng(cord3.getLatitude(), cord3.getLongitude()));
                            markerCoordinates.add(new LatLng(cord4.getLatitude(), cord4.getLongitude()));
                            /* Draw the field in the map */
                            drawLoadedFields(markerCoordinates, organic_farming, sprayed);
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

    /* Build a hashMap that contains created the JSON Files that later will be inseted in the database
    *
    * Called from MyBottomFragmentView_enterField before the insertion in the database */
    public static Map<String, Object> buildFieldInfoMap(String owner_name, String owner_email, String olive_variety, String organic_farming, String spayed) {
        /* Build the Map */
        Map<String, Object> fieldInfo = new HashMap<>();

        /* Dynamically convert markers from list (markersArrayList) to LatLng and then convert LatLng to GeoPoint's */
        String temp;
        for(int i = 0;i < markersArrayList.size();i++) {
            LatLng cordLatLng = markersArrayList.get(i).getPosition();
            GeoPoint cordGeoPoint = new GeoPoint(cordLatLng.latitude, cordLatLng.longitude);
            temp = "cord_" + (i + 1);
            fieldInfo.put(temp, cordGeoPoint);
        }
        /* Add the other info */
        fieldInfo.put("creator_email", owner_email);
        fieldInfo.put("creator_name", owner_name);
        fieldInfo.put("olive_variety", olive_variety);
        fieldInfo.put("organic_farming", organic_farming);
        fieldInfo.put("sprayed", spayed);

        return fieldInfo;
    }

    /* This function will make a nice polygon in the map based on the markers the user pick
    * It is static because we call it from MyBottomFragmentView_enterField
    *
    * Called from MyBottomFragmentView_enterField after the insertion in the database */
    public static void drawPolygonBasedOnMarkers(String selectedOptionOrganicSpinner, String organic_farming, String sprayed) {
        if(markersArrayList != null && markersArrayList.size() != 0) {
            ArrayList<LatLng> markerCoordinates = new ArrayList<>();
            for (Marker marker : markersArrayList) {
                LatLng position = marker.getPosition();
                markerCoordinates.add(position);
            }

            if(selectedOptionOrganicSpinner.equals("No")) { // For non-organic add red color
                int color;
                if(sprayed.equals("Yes"))
                    color = Color.YELLOW; // spayed color is yellow
                else
                    color = Color.RED;
                PolygonOptions polygonOptions = new PolygonOptions()
                        .addAll(markerCoordinates)
                        .strokeColor(color)
                        .fillColor(Color.argb(128, 255, 0, 0));
                Field my_field = new Field(myGoogleMap.addPolygon(polygonOptions), organic_farming, sprayed);
                loaded_fields.add(my_field);
            } else { // For organic add green color
                PolygonOptions polygonOptions = new PolygonOptions()
                        .addAll(markerCoordinates)
                        .strokeColor(Color.GREEN)
                        .fillColor(Color.argb(128, 0, 255, 0));
                Field my_field = new Field(myGoogleMap.addPolygon(polygonOptions), organic_farming, sprayed);
                loaded_fields.add(my_field);
            }
            cancelMarkText.performClick(); /* Trigger the click to recreate the View */
        } else {
            System.out.println("Cannot add the field, markersArrayList is empty or null");
        }
    }

    /* Draw the fields that users have already add in the Map */
    private void drawLoadedFields(ArrayList<LatLng> markerCoordinates, String organic_farming, String sprayed) {
        //LatLng cordLatLng = new LatLng(lati, longi);
        if(organic_farming.equals("No")) { // For non-organic add red color
            int color;
            if(sprayed.equals("Yes"))
                color = Color.YELLOW; // spayed color is yellow
            else
                color = Color.RED;
            PolygonOptions polygonOptions = new PolygonOptions()
                    .addAll(markerCoordinates)
                    .strokeColor(color)
                    .fillColor(Color.argb(128, 255, 0, 0));
            Field my_field = new Field(myGoogleMap.addPolygon(polygonOptions), organic_farming, sprayed);
            loaded_fields.add(my_field);
        } else { // For organic add green color
            PolygonOptions polygonOptions = new PolygonOptions()
                    .addAll(markerCoordinates)
                    .strokeColor(Color.GREEN)
                    .fillColor(Color.argb(128, 0, 255, 0));
            Field my_field = new Field(myGoogleMap.addPolygon(polygonOptions), organic_farming, sprayed);
            loaded_fields.add(my_field);
        }
    }

    private void onMapClick(LatLng latLng, GoogleMap googleMap) {
        // Get the latitude and longitude of the clicked point
        double clickedLatitude = latLng.latitude;
        double clickedLongitude = latLng.longitude;

        // Do something with the clicked coordinates
        Toast.makeText(this, "Clicked at: " + clickedLatitude + ", " + clickedLongitude, Toast.LENGTH_SHORT).show();
        System.out.println("Clicked at: " + clickedLatitude + ", " + clickedLongitude);

        findClickedField(new LatLng(clickedLatitude, clickedLongitude), 0);
    }

    private void findClickedField(LatLng latLngClicked, int state) {
        /* Retrieve data from database */
        firebaseFirestore.collection("Fields").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {

                        System.out.println("Document Data: " + document.getData());
                        String organic_farming = document.getString("organic_farming");
                        String oliveVarietyFilled = document.getString("olive_variety");
                        String ownerName = document.getString("creator_name");
                        String ownerEmail = document.getString("creator_email");
                        String spayed = document.getString("sprayed");

                        GeoPoint cord1 = document.getGeoPoint("cord_1");
                        GeoPoint cord2 = document.getGeoPoint("cord_2");
                        GeoPoint cord3 = document.getGeoPoint("cord_3");
                        GeoPoint cord4 = document.getGeoPoint("cord_4");

                        /* Extract location (city) from one coordinate we don't need to use all of them */
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        List<Address> addresses = null;
                        try {
                            addresses = geocoder.getFromLocation(cord1.getLatitude(), cord1.getLongitude(), 1);
                            Address address = addresses.get(0);

                            if (cord1 != null && cord2 != null && cord3 != null && cord4 != null) {
                                PolygonOptions polygonOptions = new PolygonOptions()
                                        .add(new LatLng(cord1.getLatitude(), cord1.getLongitude()),
                                                new LatLng(cord2.getLatitude(), cord2.getLongitude()),
                                                new LatLng(cord3.getLatitude(), cord3.getLongitude()),
                                                new LatLng(cord4.getLatitude(), cord4.getLongitude()))
                                        .strokeColor(Color.RED)
                                        .fillColor(Color.argb(128, 255, 0, 0));
                                if (PolyUtil.containsLocation(latLngClicked, polygonOptions.getPoints(), true)) {
                                    /* Store the cords of the clicked field */
                                    clicked_cord1 = cord1;
                                    clicked_cord2 = cord2;
                                    clicked_cord3 = cord3;
                                    clicked_cord4 = cord4;
                                    //
                                    if(state == 0) { // called from click
                                        /* Open bottom fragment */
                                        MyBottomFragmentView_displayField bottomSheetDialogFragment = new MyBottomFragmentView_displayField();
                                        /* But extra values to the bottomSheetDialogFragment */
                                        Bundle args = new Bundle();
                                        args.putString("currentUser", userUsernameText.getText().toString());
                                        args.putString("ownersName", ownerName);
                                        args.putString("ownersEmail", ownerEmail);
                                        args.putString("organicFilled", organic_farming);
                                        args.putString("oliveVarietyFilled", oliveVarietyFilled);
                                        args.putString("spayed", spayed);
                                        args.putString("city_located", address.getLocality()); // get city
                                        bottomSheetDialogFragment.setArguments(args);
                                        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                                    } else { // called from long click
                                        if(userUsernameText.getText().toString().equals(ownerEmail))  { // if the field belongs to the log in user then he can delete it else not
                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                            builder.setMessage("Do you want to delete the field that is located in, " + address.getLocality() + "\nThe map will automatically restart after it")
                                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            deleteFieldFromDatabase(document); // proceed in the deletion of the field
                                                        }
                                                    })
                                                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            // do nothing
                                                        }
                                                    });
                                                AlertDialog dialog = builder.create();
                                                dialog.show();
                                        } else {
                                            showAlertDialog(MainActivity.this, "Warning", "You can't delete this field because you are not the owner", 0);
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) { // for the geocoder.getFromLocation(...) function
                            e.printStackTrace();
                            /* Error 500 from the server (source: https://support.google.com/code/answer/70638?hl=en)*/
                            showAlertDialog(MainActivity.this, "Server error", "Error with the communication with the server\nPlease try again", 0);
                        }
                    }
                } else {
                    Log.e("TAG", "Error getting documents: ", task.getException());
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

    private void onMapLongClick(LatLng latLng, GoogleMap googleMap) {
        Toast.makeText(MainActivity.this, "LONG CLICKED Lat: " + latLng.latitude + "\nLon:" + latLng.longitude, Toast.LENGTH_SHORT).show();
        findClickedField(new LatLng(latLng.latitude, latLng.longitude), 1);
    }

    private void deleteFieldFromDatabase(QueryDocumentSnapshot clicked_document) {
        progressDialog.show();
        firebaseFirestore.collection("Fields").document(clicked_document.getId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this, "Delete has been done successfully" + clicked_document.getId(), Toast.LENGTH_SHORT).show();
                        Log.d("TAG", "DocumentSnapshot successfully deleted! ID: " + clicked_document.getId());
                        recreate(); // recreate the Map to load the new fields
                        progressDialog.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("TAG", "Error deleting document", e);
                        Toast.makeText(MainActivity.this, "Error in the deletion of document" + clicked_document.getId(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onMyLocationChange(@NonNull Location location) {
        if (isRecording) {
            System.out.println("onMyLocationChange called ...");
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            /* represent the location of the user every time he changes */
            user_lat_GPS = location.getLatitude();
            user_lng_GPS = location.getLongitude();
            Log.e("Location changed", "The variables user_lat_GPS == " + user_lat_GPS + "and the user_lng_GPS == " + user_lng_GPS);
            Toast.makeText(this, "The variables user_lat_GPS == " + user_lat_GPS + "and the user_lng_GPS == " + user_lng_GPS, Toast.LENGTH_SHORT).show();

            recordedPath.add(currentLocation);

            calculateDistance();

            /* Draw a line based on the user movement */
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(recordedPath)
                    .color(Color.BLUE)
                    .width(5);

            currentPolyline = myGoogleMap.addPolyline(polylineOptions);
            whole_path_polylines.add(currentPolyline);
        }
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {

    }

    // Function to start recording
    private void startRecording(double startLat, double startLng) {
        Log.e("Location", "The recording will start");
        /* Make the Horizontal View Green, the recording has start */
        functionsHorizontalScrollView.setBackgroundColor(Color.parseColor("#32ff00"));
        isRecording = true;
        recordedPath.clear();
        LatLng startPoint = new LatLng(startLat, startLng);
        recordedPath.add(startPoint);
    }

    // Function to stop recording
    private void stopRecording() {
        Log.e("Location", "The recording will stop");
        isRecording = false;
    }

    /* This function removes all the polylines from the Map */
    private void removeAllPolylines() {
        for(int i = 0;i < whole_path_polylines.size();i++) {
            whole_path_polylines.get(i).remove();
        }
        whole_path_polylines.removeAll(whole_path_polylines); // also empty the whole_path_polylines List
    }

    /* Algorithms to know if the user is close to 20m from an organic field */
    public void calculateDistance() {
        Location userLocation = new Location("User");
        userLocation.setLatitude(user_lat_GPS);
        userLocation.setLongitude(user_lng_GPS);

        int countCloseOrganicFields = 0;
        SoundPlayer soundPlayer = new SoundPlayer(this, R.raw.alert_sound);
        for (Field loadedField : loaded_fields) {
            Polygon polygon = loadedField.getPolygon();
            boolean userCloseToOrganicField = isUserCloseToOrganicField(userLocation, polygon);
            boolean userCloseToNonOrganicField = isUserCloseToNonOrganicField(userLocation, polygon);

            /* For organic fields */
            if (userCloseToOrganicField && loadedField.getOrganic_farming().equals("Yes") && !loadedField.getSprayed().equals("Yes")) {
                Toast.makeText(MainActivity.this, "Close to an organic field", Toast.LENGTH_SHORT).show();
                countCloseOrganicFields++;
            }
            /* For non-organic fields */
            if (userCloseToNonOrganicField && loadedField.getOrganic_farming().equals("No") && loadedField.getSprayed().equals("No")) {
                Toast.makeText(MainActivity.this, "Close to an non-organic field", Toast.LENGTH_SHORT).show();
                /* The user is close to a non-organic field, so we need to change the state "sprayed" from No to Yes.
                *  Firstly we will find the Document reference of the field from the Database,
                *  If we succeed we will continue to UPDATE the document (sprayed from No to Yes) in the database based on the document Reference we found
                *  If we succeed then we will update the UI of the user by changing the borders of the field to YELLOW.  */
                saveRecordText.setVisibility(View.VISIBLE);
                rotateAndColorChangeAnimator.start();
                findDocumentFieldBasedOnCoordinates(loadedField);
            }
        }

        if (countCloseOrganicFields > 0) {
            showAlertDialog(this, "Warning", "Close to organic field, the recording stopped. (" + (countCloseOrganicFields - 1) + ")", 0);
            createNotification("Warning", "You are close to an organic field");
            if (!soundPlayer.isPlaying()) {
                soundPlayer.playSound();
            }
            stopRecordText.performClick();
        }
    }

    private void changeSpayedFieldInDB(QueryDocumentSnapshot docRef, Field field_spayed) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("sprayed", "Yes");

        DocumentReference doc = docRef.getReference();
        doc.update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e("Update DB", "DocumentSnapshot successfully updated, DOC_ID: " + docRef.getId());
                        field_spayed.setSprayed("Yes");
                        changeSpayedFieldInMap(field_spayed);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Update DB", "Error in updating DocumentSnapshot, DOC_ID: " + docRef.getId());
                    }
                });
    }

    private void findDocumentFieldBasedOnCoordinates(Field field_spayed) {
        List<LatLng> polygonVertices = field_spayed.getPolygon().getPoints();
        firebaseFirestore.collection("Fields").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        GeoPoint cord_1 = document.getGeoPoint("cord_1");
                        GeoPoint cord_2 = document.getGeoPoint("cord_2");
                        GeoPoint cord_3 = document.getGeoPoint("cord_3");
                        GeoPoint cord_4 = document.getGeoPoint("cord_4");
                        if(((cord_1.getLatitude() == polygonVertices.get(0).latitude) && (cord_1.getLongitude() == polygonVertices.get(0).longitude))
                                && (cord_2.getLatitude() == polygonVertices.get(1).latitude) && (cord_2.getLongitude() == polygonVertices.get(1).longitude)
                                && (cord_3.getLatitude() == polygonVertices.get(2).latitude) && (cord_3.getLongitude() == polygonVertices.get(2).longitude)
                                && (cord_4.getLatitude() == polygonVertices.get(3).latitude) && (cord_4.getLongitude() == polygonVertices.get(3).longitude)) {
                            System.out.println("The field found with document ID: " + document.getId());
                            /* Now we have the document ref lets update the field "sprayed" also in the database */
                            changeSpayedFieldInDB(document, field_spayed);
                        } else {
                            System.out.println("NOT FOUNDD");
                        }
                    }
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

    private void changeSpayedFieldInMap(Field loadedField) {
        loadedField.getPolygon()
                   .setStrokeColor(Color.YELLOW);
        rotateAndColorChangeAnimator.end();
        saveRecordText.setVisibility(View.GONE);
    }

    private boolean isUserCloseToOrganicField(Location userLocation, Polygon polygon) {
        List<LatLng> polygonCoordinates = polygon.getPoints();

        for (LatLng coordinate : polygonCoordinates) {
            double fieldLat = coordinate.latitude;
            double fieldLng = coordinate.longitude;

            Location fieldLocation = new Location("Field");
            fieldLocation.setLatitude(fieldLat);
            fieldLocation.setLongitude(fieldLng);

            float distanceInMeters = userLocation.distanceTo(fieldLocation);
            Log.e("Location", "Distance calc (ORGANIC FIELD): " + distanceInMeters);

            if (distanceInMeters <= 20) {
                return true;
            }
        }
        return false;
    }

    private boolean isUserCloseToNonOrganicField(Location userLocation, Polygon polygon) {
        List<LatLng> polygonCoordinates = polygon.getPoints();

        for (LatLng coordinate : polygonCoordinates) {
            double fieldLat = coordinate.latitude;
            double fieldLng = coordinate.longitude;

            Location fieldLocation = new Location("Field");
            fieldLocation.setLatitude(fieldLat);
            fieldLocation.setLongitude(fieldLng);

            float distanceInMeters = userLocation.distanceTo(fieldLocation);
            Log.e("Location", "Distance calc (NON ORGANIC FIELD): " + distanceInMeters);

            if (distanceInMeters <= 10) {
                return true;
            }
        }
        return false;
    }

    /* -------------------Create a notification------------------- */
    private void createNotification(String title, String content) {
        Log.e("Notification", "CreateNotification called");
        String channel_ID = "CHANNEL_ID_NOTIFICATION";
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getApplicationContext(), channel_ID);
        builder.setSmallIcon(R.drawable.logo_best_resol_cropped)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                0, intent, PendingIntent.FLAG_MUTABLE);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = notificationChannel = notificationManager.getNotificationChannel(channel_ID);
            if(notificationChannel == null) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                notificationChannel = new NotificationChannel(channel_ID, "Some description", importance);
                notificationChannel.setLightColor(Color.GREEN);
                notificationChannel.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        notificationManager.notify(0, builder.build());
    }
}