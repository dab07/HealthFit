package com.example.androidfit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.androidfit.databinding.ActivityMainBinding;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.HttpClient;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.DefaultHttpClient;

import net.danlew.android.joda.JodaTimeAndroid;

import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

;


public class MainActivity<authCode> extends AppCompatActivity {
    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;
    private static final String TAG = "Fit";
    private TextView counter;
    private TextView weekCounter;
    private AppBarConfiguration appBarConfiguration;
    private GoogleApiClient mGoogleApiClient;
    private static final int RC_GET_AUTH_CODE = 9003;
    private String mAccessToken;
    private long mTokenExpired;

    private static final String DATA_URL = "https://www.googleapis.com/fitness/v1/users/me/dataSources";
    private static final String USER_AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String ACCESS_TOKEN_URL = "https://oauth2.googleapis.com/token";

    private static final String CLIENT_ID = "323623763070-sqarr6qf4ju9361she7etj71f9ool81a.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "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";

    public GoogleFitDataTypes[] getFitDataTypes() {
        return new GoogleFitDataTypes[] {
                GoogleFitDataTypes.BODY_HEIGHT,
                GoogleFitDataTypes.BODY_WEIGHT,
                GoogleFitDataTypes.CALORIES_BURNED,
                GoogleFitDataTypes.GEOPOSITION,
                GoogleFitDataTypes.HEART_RATE,
                GoogleFitDataTypes.PHYSICAL_ACTIVITY,
                GoogleFitDataTypes.SPEED,
                GoogleFitDataTypes.STEP_COUNT
        };
    }
    public enum GoogleFitDataTypes implements FitDataType {

        BODY_HEIGHT("derived:com.google.height:com.google.android.gms:merge_height"),
        BODY_WEIGHT("derived:com.google.weight:com.google.android.gms:merge_weight"),
        CALORIES_BURNED("derived:com.google.calories.expended:com.google.android.gms:merge_calories_expended"),
        GEOPOSITION("derived:com.google.location.sample:com.google.android.gms:merge_location_samples"),
        HEART_RATE("derived:com.google.heart_rate.bpm:com.google.android.gms:merge_heart_rate_bpm"),
        PHYSICAL_ACTIVITY("derived:com.google.activity.segment:com.google.android.gms:merge_activity_segments"),
        SPEED("derived:com.google.speed:com.google.android.gms:merge_speed"),
        STEP_COUNT("derived:com.google.step_count.delta:com.google.android.gms:merge_step_deltas");

        private final String streamId;

        GoogleFitDataTypes(String streamId) {
            this.streamId = streamId;
        }

        public String getStreamId() {
            return streamId;
        }
    }

    public MainActivity() throws IOException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.example.androidfit.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());
        JodaTimeAndroid.init(this);

        if (hasFitPermission()) {
//            readStepBackEnd();
        } else {
            requestFitnessPermission();
        }
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                .requestServerAuthCode(CLIENT_ID)
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this , (GoogleApiClient.OnConnectionFailedListener) this )
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
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
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA)
                .build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == Activity.RESULT_OK) {
//            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
//                Log.i(TAG, "Fitness permission granted");
//                subscribeStepCount();
//                readStepBackEnd();
////                readStepCountDelta(); // Read today's data
////                readHistoricStepCount(); // Read last weeks data
//            }
//        } else {
//            Log.i(TAG, "Fitness permission denied");
//        }
        if (requestCode == RC_GET_AUTH_CODE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String authCode = account.getServerAuthCode();
                String idToken = account.getIdToken();
                readStepBackEnd(authCode, idToken);

            } catch (ApiException | IOException e) {
                Log.w(TAG, "Sign-in failed", e);
            }
        }
    }

    private void readStepBackEnd(String authCode, String idToken) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        String CLIENT_SECRET_FILE = "/Users/hs/AndroidStudioProjects/client_secret_323623763070-sqarr6qf4ju9361she7etj71f9ool81a.apps.googleusercontent.com.json";
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(
                        JacksonFactory.getDefaultInstance(), new FileReader(CLIENT_SECRET_FILE));
        GoogleTokenResponse tokenResponse =
                new GoogleAuthorizationCodeTokenRequest(
                        new NetHttpTransport(),
                        JacksonFactory.getDefaultInstance(),
                        "https://oauth2.googleapis.com/token",
                        CLIENT_ID,
                        CLIENT_SECRET,
                        authCode,
                        "")  // Specify the same redirect URI that you use with your web
                        // app. If you don't have a web version of your app, you can
                        // specify an empty string.
                        .execute();

        String accessToken = tokenResponse.getAccessToken();
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);


    }

    private void subscribeStepCount() {
        Fitness.getRecordingClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE);
    }


    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}