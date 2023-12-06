package com.s362106.app_mappe_3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.AsyncTask;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener{

    protected GoogleMap mMap;
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getJSON task = new getJSON();
        task.execute(new String[]{"https://dave3600.cs.oslomet.no/~s362106/jsonPlace.php"});

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getJSON task = new getJSON();
        task.execute(new String[]{"https://dave3600.cs.oslomet.no/~s362106/jsonPlace.php"});
    }

    @Override
    public void onMapClick(LatLng latLng) {
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog, null);
        TextInputEditText titleInput = view.findViewById(R.id.title_input);
        TextInputEditText descriptionInput = view.findViewById(R.id.description_input);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("New Marker");
        builder.setView(view);

        builder.setPositiveButton("Ferdig", (DialogInterface dialog, int which) -> {
            String titleString = titleInput.getText().toString();
            String descriptionString = descriptionInput.getText().toString();
            double lat = latLng.latitude;
            double lng = latLng.longitude;

            Marker marker = mMap.addMarker(new MarkerOptions().position(latLng));
            String address = getAddress(marker.getPosition());

            try {
                marker.setTag(new JSONObject()
                        .put("title", titleString)
                        .put("description", descriptionString)
                        .put("address", address));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            new postJSON(titleString, descriptionString, address, lat, lng).execute();
        });
        builder.show();
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        showMarkerDetails(marker);
        return true;
    }

    private void showMarkerDetails(Marker marker) {
        View detailsView = LayoutInflater.from(MainActivity.this).inflate(R.layout.marker_details_dialog, null);

        TextView titleView = detailsView.findViewById(R.id.titleView);
        TextView descriptionView = detailsView.findViewById(R.id.descriptionView);
        TextView addressView = detailsView.findViewById(R.id.addressView);
        TextView coordinateView = detailsView.findViewById(R.id.coordinateView);

        JSONObject info = (JSONObject) marker.getTag();

        if(info != null) {
            try {
                titleView.setText(info.getString("title"));
                descriptionView.setText(info.getString("description"));
                addressView.setText(info.getString("address"));
                String coordinates = "(" + marker.getPosition().latitude + ", " + marker.getPosition().longitude + ")";
                coordinateView.setText(coordinates);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setView(detailsView);

                builder.show();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    private String getAddress(LatLng latLng) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if(addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                return address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Address not found";
    }


    private class getJSON extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if(responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String s;
                    while ((s = br.readLine()) != null) {
                        result.append(s);
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                return "Something went wrong" + e.getMessage();
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String dbResponse) {
            try {
                JSONArray jsonArray = new JSONArray(dbResponse);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject data = jsonArray.getJSONObject(i);

                    String titleString = data.getString("title");
                    String descriptionString = data.getString("description");
                    String addressString = data.getString("street_address");
                    double latitude = data.getDouble("latitude");
                    double longitude = data.getDouble("longitude");

                    LatLng position = new LatLng(latitude, longitude);

                    Marker marker = mMap.addMarker(new MarkerOptions().position(position));

                    try {
                        marker.setTag(new JSONObject()
                                .put("title", titleString)
                                .put("description", descriptionString)
                                .put("address", addressString));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if(i == 0) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
                    }
                }
            } catch (JSONException e) {
                Toast.makeText(MainActivity.this, "Error med JSON: " + e, Toast.LENGTH_SHORT).show();
            }

        }
    }

    private class postJSON extends AsyncTask<Void, Void, String> {
        private final String tittel;
        private final String beskrivelse;
        private final String adresse;
        private final double latitude;
        private final double longitude;

        public postJSON(String tittel, String beskrivelse, String adresse, double latitude, double longitude) {
            this.tittel = tittel;
            this.beskrivelse = beskrivelse;
            this.adresse = adresse;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL("https://dave3600.cs.oslomet.no/~s362106/jsonPI.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String data = "Title=" + URLEncoder.encode(tittel, "UTF-8") +
                        "&Description=" + URLEncoder.encode(beskrivelse, "UTF-8") +
                        "&Address=" + URLEncoder.encode(adresse, "UTF-8") +
                        "&Latitude=" + latitude + "&Longitude=" + longitude;

                OutputStream outputStream = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                writer.write(data);
                writer.flush();
                writer.close();
                outputStream.close();

                int responseCode = conn.getResponseCode();
                return String.valueOf(responseCode);
            } catch (IOException e) {
                return "Error: " + e.getMessage();
            }
        }
    }
}