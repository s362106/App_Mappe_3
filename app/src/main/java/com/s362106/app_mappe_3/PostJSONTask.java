package com.s362106.app_mappe_3;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class PostJSONTask extends AsyncTask<String, Void, Void> {
    private final Context mContext;

    public PostJSONTask(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(String... dataPoints) {

        try {
            URL url = new URL("https://dave3600.cs.oslomet.no/~s362106/jsonPI.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            String title = dataPoints[0];
            String description = dataPoints[1];
            String address = dataPoints[2];
            double latitude = Double.parseDouble(dataPoints[3]);
            double longitude = Double.parseDouble(dataPoints[4]);

            String data = String.format("Title=%s&Description=%s&Address=%s&Latitude=%s&Longitude=%s",
                    URLEncoder.encode(title, "UTF-8"),
                    URLEncoder.encode(description, "UTF-8"),
                    URLEncoder.encode(address, "UTF-8"),
                    latitude, longitude);

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream());
            outputStreamWriter.write(data);
            outputStreamWriter.flush();
            outputStreamWriter.close();

            Log.d("postJSON", "Response Code: " + conn.getResponseCode());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
