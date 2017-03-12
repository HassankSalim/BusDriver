package akshayatech.travel_on;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    TextView lat, lon, streetId, stateId, countryId, zipId;
    TextView updateTime;
    Button getDetail, startServe, stopServe;

    Double latInDouble, lonInDouble;

    GoogleApiClient mGoogleApiClient;

    String url = "http://192.168.43.142:8000/test";


    Location mLastLocation, mCurrentLocation;
    LocationRequest mLocationRequest;
    PendingResult<LocationSettingsResult> result;

    String mLastUpdateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) this)
                    .addOnConnectionFailedListener((GoogleApiClient.OnConnectionFailedListener) this)
                    .addApi(LocationServices.API)
                    .build();
        }

        createLocationRequest();

        lat = (TextView) findViewById(R.id.latId);
        lon = (TextView) findViewById(R.id.lonId);
        streetId = (TextView) findViewById(R.id.streetView);
        stateId = (TextView) findViewById(R.id.stateView);
        countryId = (TextView) findViewById(R.id.countryView);
        zipId = (TextView) findViewById(R.id.zipView);
        updateTime = (TextView) findViewById(R.id.getCoods);

        getDetail = (Button) findViewById(R.id.getDetails);
        startServe = (Button) findViewById(R.id.startService);
        stopServe = (Button) findViewById(R.id.stopService);

        startServe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(new Intent(MainActivity.this, MyService.class));
            }
        });

        stopServe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(MainActivity.this, MyService.class));
            }
        });

        getDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickUIUpdate();
            }
        });


    }



    protected void createLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    11);

        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                builder.build());


        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                final LocationSettingsStates temp = locationSettingsResult.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    MainActivity.this,
                                    0x1);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;


                }


            }
        });
        startLocationUpdates();

        if (mLastLocation != null) {
            latInDouble = mLastLocation.getLatitude();
            lonInDouble = mLastLocation.getLongitude();
            lat.setText(String.valueOf(mLastLocation.getLatitude()));
            lon.setText(String.valueOf(mLastLocation.getLongitude()));
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, (LocationListener) MainActivity.this);
    }

    @Override
    public void onLocationChanged(Location location)
    {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateLocation(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        Log.e("Reached Here", "1");
        updateUI();
    }

    private void updateUI()
    {

        latInDouble = mCurrentLocation.getLatitude();
        lonInDouble = mCurrentLocation.getLongitude();
        lat.setText(String.valueOf(latInDouble));
        lon.setText(String.valueOf(lonInDouble));
        updateTime.setText(mLastUpdateTime);
    }

    private void clickUIUpdate()
    {
        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());

        List<Address> addresses  = null;
        try {
            addresses = geocoder.getFromLocation(latInDouble,lonInDouble, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String city = addresses.get(0).getLocality();
        city = addresses.get(0).getSubLocality();
        Log.d("AdminArea",""+addresses.get(0).getAdminArea());
        Log.d("SubAdminArea",""+addresses.get(0).getSubAdminArea());
        String state = addresses.get(0).getAdminArea();
        String zip = addresses.get(0).getPostalCode();
        String country = addresses.get(0).getCountryName();

        streetId.setText(city);
        stateId.setText(state);
        zipId.setText(zip);
        countryId.setText(country);

    }

    private void updateLocation(double lat, double lon)
    {
        JSONObject data = new JSONObject();
        JsonObjectRequest request = null;
        try {
            data.put("busnum", 12);
            data.put("lat", lat);
            data.put("lon", lon);
            Log.e("data", ""+data);

            request = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.e("Success", "Success");
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

        AppController.getInstance().addToRequestQueue(request);
    }

}
