package com.s362106.app_mappe_3;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    protected GoogleMap mMap;
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        geocoder = new Geocoder(this, Locale.getDefault());
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
        new getJSON().execute("https://dave3600.cs.oslomet.no/~s362106/jsonPlace.php");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMap.clear();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        View view = LayoutInflater.from(this).inflate(R.layout.bottomsheet_layout, null);
        TextInputEditText titleInput = view.findViewById(R.id.title_input);
        TextInputEditText descriptionInput = view.findViewById(R.id.description_input);
        Button finishButton = view.findViewById(R.id.createBtn);

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();

        finishButton.setOnClickListener(v -> {
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
            bottomSheetDialog.dismiss();

            new PostJSONTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, titleString, descriptionString, address, String.valueOf(lat), String.valueOf(lng));
        });
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        showMarkerDetails(marker);
        return true;
    }

    private void showMarkerDetails(Marker marker) {
        View detailsView = LayoutInflater.from(this).inflate(R.layout.marker_details_dialog, null);

        TextView titleView = detailsView.findViewById(R.id.titleView);
        TextView descriptionView = detailsView.findViewById(R.id.descriptionView);
        TextView addressView = detailsView.findViewById(R.id.addressView);
        TextView coordinateView = detailsView.findViewById(R.id.coordinateView);

        JSONObject info = (JSONObject) marker.getTag();

        if (info != null) {
            try {
                titleView.setText(info.getString("title"));
                descriptionView.setText(info.getString("description"));
                addressView.setText(info.getString("address"));
                String coordinates = "(" + marker.getPosition().latitude + ", " + marker.getPosition().longitude + ")";
                coordinateView.setText(coordinates);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
            if (addresses != null && addresses.size() > 0) {
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
                if (responseCode == HttpURLConnection.HTTP_OK) {
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

                    if (i == 0) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
                    }
                }
            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(), "Error: " + e, Toast.LENGTH_SHORT).show();
            }

        }
    }
}