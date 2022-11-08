package com.example.androidfit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.androidfit.databinding.ActivityMainBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.*;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;
    private static final String TAG = "MainActivity";
    private TextView counter;
    private TextView weekCounter;
    private AppBarConfiguration appBarConfiguration;
    private Context context;
    static DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder()
            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .setType(DataSource.TYPE_DERIVED)
            .setStreamName("estimated_steps")
            .setAppPackageName("com.google.android.gms")
            .build();


    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        counter = findViewById(R.id.counter);
        weekCounter = findViewById(R.id.week_counter);

        if (hasFitPermission()) {
            readStepCountDelta();
            readHistoricStepCount();
        } else {
            requestFitnessPermission();
        }
    }

    private boolean hasFitPermission() {
        FitnessOptions fitnessOptions = getFitnessSignInOptions();
        return GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions);
    }
    private void requestFitnessPermission() {
        GoogleSignIn.requestPermissions(
                this,
                REQUEST_OAUTH_REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(this),
                getFitnessSignInOptions());
    }

    private FitnessOptions getFitnessSignInOptions() {
        return FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA)
                .build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                Log.i(TAG, "Fitness permission granted");
                subscribeStepCount();
                readStepCountDelta(); // Read today's data
                readHistoricStepCount(); // Read last weeks data
            }
        } else {
            Log.i(TAG, "Fitness permission denied");
        }
    }
    private void subscribeStepCount() {
        Fitness.getRecordingClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE);
    }
    private void readStepCountDelta() {
        if (!hasFitPermission()) {
            requestFitnessPermission();
            return;
        }

        Fitness.getHistoryClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .readDailyTotal(DataType.AGGREGATE_STEP_COUNT_DELTA)
                .addOnSuccessListener(
                        dataSet -> {
                            long steps =
                                    dataSet.isEmpty()
                                            ? 0
                                            : dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                            Log.d(TAG, "Total steps: " + steps);
                            counter.setText(String.format(Locale.ENGLISH, "%d", steps));
                        })
                .addOnFailureListener(
                        e -> Log.w(TAG, "Unable to count steps.", e));


    }
    private void readHistoricStepCount() {
        if (!hasFitPermission()) {
            requestFitnessPermission();
            return;
        }

        Fitness.getHistoryClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .readData(queryFitnessData())
                .addOnSuccessListener(
                        this::printData)
                .addOnFailureListener(
                        e -> Log.e(TAG, "Unable to count weekly steps data.", e));
    }

    public static DataReadRequest queryFitnessData() {
        DateTime dt = new DateTime().withTimeAtStartOfDay();
        long endTime = dt.getMillis();
        long startTime = dt.minusWeeks(1).getMillis();

        return new DataReadRequest.Builder()
                .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
    }

    public void printData(DataReadResponse dataReadResult) {
        StringBuilder result = new StringBuilder();
        if (dataReadResult.getBuckets().size() > 0) {
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    result.append(formatDataSet(dataSet));
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                result.append(formatDataSet(dataSet));
            }
        }
        Log.d(TAG, "result: " + result);
        weekCounter.setText(result);
    }
    private static String formatDataSet(DataSet dataSet) {
        StringBuilder result = new StringBuilder();

        for (DataPoint dp : dataSet.getDataPoints()) {
            org.joda.time.DateTime sDT = new org.joda.time.DateTime(dp.getStartTime(TimeUnit.MILLISECONDS));
            org.joda.time.DateTime eDT = new org.joda.time.DateTime(dp.getEndTime(TimeUnit.MILLISECONDS));

            result.append(
                    String.format(
                            Locale.ENGLISH,
                            "%s %s to %s %s\n",
                            sDT.dayOfWeek().getAsShortText(),
                            sDT.toLocalTime().toString("HH:mm"),
                            eDT.dayOfWeek().getAsShortText(),
                            eDT.toLocalTime().toString("HH:mm")
                    )
            );

            result.append(
                    String.format(
                            Locale.ENGLISH,
                            "%s: %s %s\n",
                            sDT.dayOfWeek().getAsShortText(),
                            dp.getValue(dp.getDataType().getFields().get(0)),
                            dp.getDataType().getFields().get(0).getName()));
        }

        return String.valueOf(result);
    }
    public boolean isConnectedToNetwork() {

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED;

    }

    public boolean isGPSon()
    {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE );
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.action_read_data) {
//            readStepCountDelta();
//            return true;
//        } else if (id == R.id.action_read_historic_data) {
//            readHistoricStepCount();
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
