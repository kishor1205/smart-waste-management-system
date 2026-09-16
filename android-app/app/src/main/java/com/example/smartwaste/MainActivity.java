package com.example.smartwaste;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    EditText description, locationEditText, imageUrl;
    Button submitBtn, cameraBtn;
    ImageView imagePreview;

    FusedLocationProviderClient fusedLocationClient;
    Bitmap capturedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        description = findViewById(R.id.description);
        locationEditText = findViewById(R.id.location);

        submitBtn = findViewById(R.id.submitBtn);
        cameraBtn = findViewById(R.id.cameraBtn);
        imagePreview = findViewById(R.id.imagePreview);

        // 📍 GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        } else {
            getLocation();
        }

        // 📸 CAMERA PERMISSION
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    2);
        }

        // 📸 CAMERA BUTTON
        cameraBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, 100);
            } else {
                Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
            }
        });

        // 🚀 SUBMIT
        submitBtn.setOnClickListener(v -> {
            sendData();
        });
    }

    // 📍 GPS
    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        double lat = loc.getLatitude();
                        double lon = loc.getLongitude();

                        android.location.Geocoder geocoder =
                                new android.location.Geocoder(this, java.util.Locale.getDefault());

                        try {
                            java.util.List<android.location.Address> addresses =
                                    geocoder.getFromLocation(lat, lon, 1);

                            if (addresses != null && !addresses.isEmpty()) {

                                android.location.Address address = addresses.get(0);

                                String subLocality = address.getSubLocality();
                                String locality = address.getLocality();
                                String road = address.getThoroughfare();
                                String full = address.getAddressLine(0);

                                StringBuilder finalAddress = new StringBuilder();

                                // 🔥 Fix wrong nearby area names
                                if (subLocality != null) {
                                    String area = subLocality.toLowerCase();

                                    if (area.contains("sunkadakatte") || area.contains("sanjeevini")) {
                                        finalAddress.append("Hegganahalli");
                                    } else {
                                        finalAddress.append(subLocality);
                                    }
                                }

                                // Add road if available
                                if (road != null && !road.isEmpty()) {
                                    if (finalAddress.length() > 0) finalAddress.append(", ");
                                    finalAddress.append(road);
                                }

                                // Add city
                                if (locality != null && !locality.isEmpty()) {
                                    if (finalAddress.length() > 0) finalAddress.append(", ");
                                    finalAddress.append(locality);
                                }

                                // Fallback if empty
                                if (finalAddress.toString().trim().isEmpty()) {
                                    finalAddress.append(full);
                                }

                                // ✅ Always add coordinates (VERY IMPORTANT)
                                finalAddress.append("\n(")
                                        .append(lat)
                                        .append(", ")
                                        .append(lon)
                                        .append(")");

                                locationEditText.setText(finalAddress.toString());

                            } else {
                                locationEditText.setText(lat + ", " + lon);
                            }

                        } catch (Exception e) {
                            locationEditText.setText(lat + ", " + lon);
                        }
                    }
                });
    }
    // 📍 PERMISSION RESULT
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // 📸 CAMERA RESULT
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {

            capturedImage = (Bitmap) data.getExtras().get("data");

            // show preview
            imagePreview.setImageBitmap(capturedImage);

            Toast.makeText(this, "Image Captured!", Toast.LENGTH_SHORT).show();
        }
    }

    // 🔥 Convert image → Base64
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    // 🚀 SEND DATA
    private void sendData() {
        new Thread(() -> {
            try {
                URL url = new URL("http://10.12.121.230:8080/api/complaints");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject json = new JSONObject();

                // ✅ description
                json.put("description", description.getText().toString());

                // ✅ location (already correct)
                json.put("location", locationEditText.getText().toString());

                // ✅ IMPORTANT (add this)
                json.put("status", "PENDING");

                // ✅ FIX image handling
                if (capturedImage != null) {
                    json.put("imageUrl", "data:image/jpeg;base64," + bitmapToBase64(capturedImage));
                } else {
                    json.put("imageUrl", "");
                }

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("utf-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                runOnUiThread(() -> {
                    if (responseCode == 200 || responseCode == 201) {
                        Toast.makeText(this, "✅ Sent Successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "❌ Failed: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(() ->
                        Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
}