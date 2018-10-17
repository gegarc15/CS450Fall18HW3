package edu.stlawu.locationgps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Observable;
import java.util.Observer;

// TODO: doesn't connect to currentLocation right away and crashes out of the gate

public class MainActivity  extends AppCompatActivity implements Observer {

    private double totalDistance;
    private double speed;
    private double averageSpeed;
    private double startTime;
    Location startLocation;
    Location currentLocation;
    Location prevLocation;

    Button updateButton;
    Button startButton;
    Button resetButton;

    TextView total;
    TextView speedText;
    TextView avg_speed;


    // private Observable location;
    private LocationHandler handler = null;
    private final static int PERMISSION_REQUEST_CODE = 987;

    private boolean permissions_granted;
    private final static String LOGTAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(handler == null) {
            handler = new LocationHandler(this);
            this.handler.addObserver(this);
        }

        // check permissions
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }

        resetButton = findViewById(R.id.reset);
        updateButton =  findViewById(R.id.update);
        startButton =  findViewById(R.id.start);
        total = findViewById(R.id.total);
        speedText = findViewById(R.id.speed);
        avg_speed = findViewById(R.id.avg_speed);


        // can't update without starting
        updateButton.setEnabled(false);
        updateButton.setBackgroundColor(Color.WHITE);
        resetButton.setEnabled(false);

        resetButton.setBackgroundColor(Color.WHITE);

        updateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // check if update position button is checked
                updatePosition();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                updateButton.setEnabled(true);
                updateButton.setBackgroundColor(getResources().getColor(R.color.buttongrey));
                resetButton.setEnabled(true);
                resetButton.setBackgroundColor(getResources().getColor(R.color.buttongrey));
                startButton.setEnabled(false);
                startButton.setBackgroundColor(Color.WHITE);

                prevLocation = currentLocation;
                startLocation = currentLocation;
                totalDistance = 0;
                startTime = currentLocation.getElapsedRealtimeNanos();
                Log.i(LOGTAG,String.format("%f",startTime));
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout newLayout = findViewById(R.id.scroll_layout_distance);
                LinearLayout newLayout2 = findViewById(R.id.scroll_layout_speed);
                newLayout.removeAllViews();
                newLayout2.removeAllViews();
                dataTitles(newLayout, newLayout2);
                totalDistance = 0;
                updateTotal(totalDistance);

                updateButton.setEnabled(false);
                updateButton.setBackgroundColor(Color.WHITE);
                resetButton.setEnabled(false);
                resetButton.setBackgroundColor(Color.WHITE);
                startButton.setEnabled(true);
                startButton.setBackgroundColor(getResources().getColor(R.color.buttongrey));
            }
        });
    }

    public boolean isPermissions_granted() {
        return permissions_granted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSION_REQUEST_CODE){
            // only asked for fine location
            if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                this.permissions_granted = true;
                Log.i(LOGTAG, "Fine location permission granted.");
            }else{
                this.permissions_granted = false;
                Log.i(LOGTAG, "Fine location permission not granted.");
            }
        }
    }

    @Override
    public void update(Observable o, final Object arg) {
        if (o instanceof LocationHandler){
            currentLocation = (Location) arg;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // current speed
                    speedText.setText(String.format("%.2f", (((Location) arg).getSpeed())*3.6));
                }
            });
        }
    }

    public void updatePosition() {
        // distance since last update
        double distance = findDistance(prevLocation, currentLocation);
        updateTotal(distance);

        // speed going between points
        speed = speedBetweenPoints(prevLocation, currentLocation, distance);

        // add to view
        addDistanceView(distance, speed);

        // save location as the prev
        prevLocation = currentLocation;
    }

    // find speed in km/h
    public double speedBetweenPoints(Location prevLocation, Location currentLocation, double distance){
        double speedPrev = prevLocation.getElapsedRealtimeNanos();
        double speedCurr = currentLocation.getElapsedRealtimeNanos();

        if(prevLocation == currentLocation){
            return 0.00;
        }else {
            return (3.6e9) * (distance / (speedCurr - speedPrev));
        }
    }

    // returns distance in meters
    public double findDistance(Location location1, Location location2){
        return location1.distanceTo(location2);
    }

    // https://developer.android.com/reference/android/location/Location
    // adds a textview with the latest distance and speed to the scrollview
    // distance is in meters, and the speed is in kilometers/hour
    private void addDistanceView(double distance, double speed){
        View linearLayoutDistance =  findViewById(R.id.scroll_layout_distance);
        View linearLayoutSpeed = findViewById(R.id.scroll_layout_speed);

        TextView newDistance = new TextView(this);
        newDistance.setText(String.format("%.1f", distance));
        newDistance.setTextSize(20);
        newDistance.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ((LinearLayout) linearLayoutDistance).addView(newDistance);

        // attempt to add speed to view
        TextView newSpeed = new TextView(this);
        newSpeed.setText(String.format("%.2f", speed));
        newSpeed.setTextSize(20);
        newSpeed.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ((LinearLayout) linearLayoutSpeed).addView(newSpeed);
    }

    // displays the distance in kilometers
    private void updateTotal(double distance){
        totalDistance += distance;
        averageSpeed = (totalDistance * 3.6e9) / (currentLocation.getElapsedRealtimeNanos() - startTime);

        averageSpeed = averageVelocity(startLocation, currentLocation, startTime);

        Log.i(LOGTAG,String.format("%f",averageSpeed));

        total.setText(String.format("%.2f", (totalDistance/1000)));
        if(totalDistance == 0){
            avg_speed.setText(String.format("%.2f", 0.00));
        }else {
            avg_speed.setText(String.format("%.2f", Math.abs(averageSpeed)));
        }
    }

    private void dataTitles(LinearLayout linearLayoutOne, LinearLayout linearLayoutTwo){
        TextView newText = new TextView(this);
        newText.setText(R.string.distance_meters);
        newText.setTextColor(Color.GRAY);
        newText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        linearLayoutOne.addView(newText);

        TextView newText2 = new TextView(this);
        newText2.setText(R.string.speed_km_h);
        newText2.setTextColor(Color.GRAY);
        newText2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        linearLayoutTwo.addView(newText2);
    }

    private double averageVelocity(Location start, Location current, double startTime){
        double distance = findDistance(start, current);
        return (distance* 3.6e9) / (currentLocation.getElapsedRealtimeNanos() - startTime);
    }
}