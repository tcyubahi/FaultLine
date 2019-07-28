/*

    ESRI Weekend of Innovation 07.25.2019
    Group: Faultline

 */

package com.example.faultline;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;


public class MainActivity extends AppCompatActivity {

    final private String featureServiceUrl = "https://services8.arcgis.com/LLNIdHmmdjO2qQ5q/arcgis/rest/services/Map_WFL1/FeatureServer/0";
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Context context;
    private TelephonyManager telephonyManager;
    private String deviceID;
    private MapView mapView;
    private int currentLevel = 1;
    private FeatureLayer featureLayer;
    private ServiceFeatureTable serviceFeatureTable;
    private Spinner levelSpinner;
    private String [] levels;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<Map<String, Object>> offices = new ArrayList<>();
    private LinearLayout listView, levelView;
    private String userLevel;
    private String userOffice;
    private Double officeScore;
    private double threshholdHigher = 0.252036, threshholdLower = 0.616187;
    private TextView registeredView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        mapView = findViewById(R.id.mapView);
        ArcGISMap map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 34.056295, -117.195800, 20);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        serviceFeatureTable = new ServiceFeatureTable(featureServiceUrl);
        featureLayer = new FeatureLayer(serviceFeatureTable);
        map.getOperationalLayers().add(featureLayer);
        mapView.setMap(map);
        levelSpinner = findViewById(R.id.levelSpinner);
        levels = new String[] {"1", "2", "3"};
        ArrayAdapter<CharSequence> levelAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, levels);
        mapView.setOnTouchListener(new IdentifyFeatureLayerTouchListener(this, mapView, featureLayer));

        recyclerView = findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new OfficeListAdapter(offices, context);
        recyclerView.setAdapter(adapter);

        listView = findViewById(R.id.listView);
        levelView = findViewById(R.id.levelView);
        levelSpinner.setAdapter(levelAdapter);

        registeredView = findViewById(R.id.registeredView);
        levelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentLevel = Integer.parseInt(levels[position]);
                sortByLevel();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        featureLayer.addLoadStatusChangedListener(loadStatusChangedEvent -> {
            if(featureLayer.getLoadStatus().toString().equals("LOADED")) {
                sortByLevel();
            }
        });

        getUIID();
        checkUpdates();
    }

    private void getUIID() {
        Log.e("HERE", "WHAT");
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        deviceID = telephonyManager.getDeviceId();
        checkUserRegistration(deviceID);
        Log.e(getClass().getSimpleName(), deviceID);
    }

    private void checkUpdates() {

        DocumentReference docRef = db.collection("liveFeed").document("redlands");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e("Error", "Listen failed.", e);
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    boolean alert = documentSnapshot.getBoolean("alert");
                    if (alert) {
                        alertUser();
                    }
                    Log.d(getClass().getSimpleName(), "Current data: " + documentSnapshot.getData());
                } else {
                    Log.d("Error", "Current data: null");
                }
            }
        });
    }

    private void alertUser() {
        if (officeScore == null) {
            new AlertDialog.Builder(context).setTitle("Attention")
                    .setMessage("EARTHQUAKE DETECTED\nREMAIN CALM\nASSIST YOURSELF BEFORE ASSISTING OTHERS\nCLEAR WAY FOR EMERGENCY SERVICES\n").setCancelable(false).create().show();
        } else if (officeScore <= threshholdHigher){
            new AlertDialog.Builder(context).setTitle("Attention " + userOffice)
                    .setMessage(" TAKE COVER UNDER DESK\nREMAIN CALM\nASSIST YOURSELF BEFORE ASSISTING OTHERS").setCancelable(false).create().show();
        } else if (officeScore <= threshholdLower){
            new AlertDialog.Builder(context).setTitle("Attention " + userOffice)
                    .setMessage("MOVE OUTDOORS\nREMAIN CALM\nCLEAR WAY FOR EMERGENCY SERVICES").setCancelable(false).create().show();
        }
    }

    private void updateFCMToken() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(getClass().getSimpleName(), "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                        // Log and toast
                        String msg = "Token";
                        Log.d(getClass().getSimpleName(), token);
                        //Toast.makeText(MainActivity.this, token, Toast.LENGTH_SHORT).show();
                        saveToken(token);
                    }
                });
    }

    private void saveToken(final String token) {
        Map<String, Object> office = new HashMap<>();
        office.put("token", token);
        db.collection("users").document(deviceID)
                .set(office, SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {

                }
            }
        });
    }

    private void  checkUserRegistration(final String uuid) {
        DocumentReference docRef = db.collection("users").document(uuid);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        updateFCMToken();
                        checkUpdates();
                        userOffice = document.get("name").toString();
                        userLevel = document.get("level").toString();
                        officeScore = Double.parseDouble(document.get("score").toString());
                        registeredView.setText("Your registered office is " +userOffice + " on Level " + userLevel);
                        recyclerView.setVisibility(View.GONE);
                        levelView.setVisibility(View.GONE);
                        registeredView.setVisibility(View.VISIBLE);
                    } else {

                    }
                } else {
                    Log.d(getClass().getSimpleName(), "get failed with ", task.getException());
                }
            }
        });
    }

    public void registerUser(Map<String, Object> office) {
        office.put("deviceId", deviceID);
        db.collection("users").document(deviceID)
                .set(office, SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    recyclerView.setVisibility(View.GONE);
                    levelView.setVisibility(View.GONE);
                    userOffice = office.get("name").toString();
                    userLevel = office.get("level").toString();
                    officeScore = Double.parseDouble(office.get("score").toString());
                    registeredView.setText("Your registered office is " +userOffice + " on Level " + userLevel);
                    registeredView.setVisibility(View.VISIBLE);
                    updateFCMToken();
                }
            }
        });
    }

    private class IdentifyFeatureLayerTouchListener extends DefaultMapViewOnTouchListener {

        private FeatureLayer layer = null; // reference to the layer to identify features in

        // provide a default constructor
        public IdentifyFeatureLayerTouchListener(Context context, MapView mapView, FeatureLayer layerToIdentify) {
            super(context, mapView);
            layer = layerToIdentify;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // get the screen point where user tapped
            android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());

            Log.e("POINT", "" + e.getX());

            final ListenableFuture<IdentifyLayerResult> identifyFuture =
                    mMapView.identifyLayerAsync(layer, screenPoint, 20, false, 25);

            identifyFuture.addDoneListener(new Runnable() {

                @Override
                public void run() {
                    try {
                        IdentifyLayerResult identifyLayerResult = identifyFuture.get();
                        offices.clear();
                        for ( int i = 0; i < identifyLayerResult.getElements().size(); i++) {
                            Map<String, Object> office = new HashMap<>();
                            office.put("name", identifyLayerResult.getElements().get(i).getAttributes().get("NAME").toString());
                            office.put("score", identifyLayerResult.getElements().get(i).getAttributes().get("FinalScore").toString());
                            office.put("level", identifyLayerResult.getElements().get(i).getAttributes().get("LEVEL_NUMBER").toString());
                            offices.add(office);
                            Log.e("SIZE", ""+offices.size());
                        }
                        adapter.notifyDataSetChanged();

                    } catch (InterruptedException | ExecutionException ex) {

                    }
                }
            });

            return true;
        }
    }

    private void sortByLevel () {
        featureLayer.clearSelection();
        featureLayer.resetFeaturesVisible();
        QueryParameters query = new QueryParameters();
        query.setWhereClause("LEVEL_NUMBER <>" + currentLevel);
        final ListenableFuture<FeatureQueryResult> future = serviceFeatureTable.queryFeaturesAsync(query);
        future.addDoneListener(() -> {
            try {
                FeatureQueryResult result = future.get();
                Iterator<Feature> resultIterator = result.iterator();
                while(((Iterator) resultIterator).hasNext()) {
                    Feature feature = resultIterator.next();
                    featureLayer.setFeatureVisible(feature, false);

                }
            } catch (Exception e) {
                String error = "Feature search failed for: " +  e.getMessage();
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                Log.e("ERROR", error);
            }
        });
    }
}
