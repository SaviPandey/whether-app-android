package com.lifecodes.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.lifecodes.weatherapp.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private WeatherAdapter weatherAdapter;
    private ArrayList<WeatherModel> weatherModelArrayList;
    private final int PERMISSION_CODE = 44;
    private FusedLocationProviderClient mFusedLocationClient;

    private double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().hide();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        weatherModelArrayList = new ArrayList<>();
        weatherAdapter = new WeatherAdapter(weatherModelArrayList, this);
        binding.weatherRv.setAdapter(weatherAdapter);

        Calendar calendar = Calendar.getInstance();
        String date = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.getTime());
        binding.dateTv.setText(date.substring(0, date.length() - 6));

        getLastLocation();
        getWeatherInfo(latitude, longitude);

        binding.searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String cityName = binding.cityEditTxt.getText().toString();

                if (cityName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter City", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                binding.statusTv.setText("Updating");
                binding.statusTv.setTextColor(getResources().getColor(android.R.color.holo_orange_light));

                LatLng latLng = getLocationFromAddress(cityName);
                if (latLng != null) {
                    getWeatherInfo(latLng.latitude, latLng.longitude);
                    binding.cityEditTxt.setText("");
                    binding.cityEditTxt.clearFocus();
                    binding.cityTv.setText(cityName);
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            Log.d("loc", String.valueOf(location.getLatitude()));
                            Log.d("loc", String.valueOf(location.getLongitude()));
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();

                            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                            List<Address> addresses = null;
                            try {
                                addresses = geocoder.getFromLocation(latitude, longitude, 1);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            String cityName = addresses.get(0).getLocality();
                            binding.cityTv.setText(cityName);
                        }
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on your location!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            getWeatherInfo(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        }
    };

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
            getWeatherInfo(latitude, longitude);
        }
    }

    public void getWeatherInfo(double latitude, double longitude) {

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.openweathermap.org/data/2.5/onecall?lat=" + latitude + "&lon=" + longitude + "&exclude=minutely,daily&units=metric&appid=" + getResources().getString(R.string.api_key);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        binding.statusTv.setText("Updated");
                        binding.statusTv.setTextColor(getResources().getColor(R.color.status_green));
                        weatherModelArrayList.clear();

                        try {
                            JSONObject currObj = response.getJSONObject("current");
                            Double temperature = currObj.getDouble("temp");
                            String weatherMain = currObj.getJSONArray("weather").getJSONObject(0).getString("main");
                            String weatherDesc = currObj.getJSONArray("weather").getJSONObject(0).getString("description");

                            double windSpeed = currObj.getDouble("wind_speed");
                            String humidity = currObj.getString("humidity");
                            String pressure = currObj.getString("pressure");

                            binding.currentTempTv.setText(String.format("%.0f", temperature) + "°");
                            String capitalize = weatherDesc.substring(0, 1).toUpperCase() + weatherDesc.substring(1).toLowerCase();
                            binding.weatherDesc.setText(capitalize);
                            binding.weatherIv.setImageDrawable(getIcon(weatherMain, true));


                            binding.windTv.setText(Math.round(windSpeed * (18 / 5)) + " km/h");
                            binding.humidityTv.setText(humidity + "%");
                            binding.pressureTv.setText(pressure + " hPa");

                            JSONArray hourArray = response.getJSONArray("hourly");

                            for (int i = 0; i < hourArray.length(); i++) {
                                JSONObject hourObject = hourArray.getJSONObject(i);
                                Double temp = hourObject.getDouble("temp");
                                Long time = hourObject.getLong("dt");
                                String weatherIcon = hourObject.getJSONArray("weather").getJSONObject(0).getString("main");

                                weatherModelArrayList.add(new WeatherModel(temp, getIcon(weatherIcon, false), time));
                            }
                            weatherAdapter.notifyDataSetChanged();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Enter valid location", Toast.LENGTH_SHORT).show();
            }
        });

        queue.add(jsonObjectRequest);
    }

    public LatLng getLocationFromAddress(String strAddress) {

        Geocoder coder = new Geocoder(MainActivity.this);
        List<Address> address;
        LatLng values = null;

        try {
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null || address.isEmpty()) {
                Toast.makeText(this, "Enter valid city", Toast.LENGTH_SHORT).show();
                binding.statusTv.setText("Failed");
                binding.statusTv.setTextColor(getResources().getColor(R.color.status_red));
                return null;
            }
            Address location = address.get(0);
            values = new LatLng(location.getLatitude(), location.getLongitude());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return values;
    }

    public Drawable getIcon(String name, boolean type) {
        Drawable icon;
        if (type) {
            switch (name) {
                case "Clouds":
                    icon = getApplicationContext().getResources().getDrawable(R.drawable.sunny);
                    break;
                case "Rain":
                case "Dizzle":
                    icon = getApplicationContext().getResources().getDrawable(R.drawable.rainy);
                    break;
                case "Thunderstorm":
                    icon = getApplicationContext().getResources().getDrawable(R.drawable.thunder);
                    break;
                case "Snow":
                    icon = getApplicationContext().getResources().getDrawable(R.drawable.snow);
                    break;
                default:
                    icon = getApplicationContext().getResources().getDrawable(R.drawable.sunny);
            }
        } else {
            switch (name) {
                case "Clouds":
                    icon = getApplicationContext().getResources().getDrawable(R.drawable.sunny_2d);
                    break;
                case "Rain":
                case "Dizzle":
                    icon = getApplicationContext().getResources().getDrawable(R.drawable.rainy_2d);
                    break;
                case "Thunderstorm":
                    icon = getApplicationContext().getResources().getDrawable(R.drawable.thunder_2d);
                    break;
                case "Snow":
                    icon = getApplicationContext().getResources().getDrawable(R.drawable.snow_2d);
                    break;
                default:
                    icon = getApplicationContext().getResources().getDrawable(R.drawable.sunny_2d);
            }
        }
        return icon;
    }
}