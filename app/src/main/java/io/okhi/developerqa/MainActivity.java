package io.okhi.developerqa;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import io.okhi.android_core.OkHi;
import io.okhi.android_core.interfaces.OkHiRequestHandler;
import io.okhi.android_core.models.OkHiException;
import io.okhi.android_core.models.OkHiLocation;
import io.okhi.android_core.models.OkHiUser;
import io.okhi.android_okcollect.OkCollect;
import io.okhi.android_okcollect.callbacks.OkCollectCallback;
import io.okhi.android_okcollect.utilities.OkHiConfig;
import io.okhi.android_okcollect.utilities.OkHiTheme;
import io.okhi.android_okverify.OkVerify;
import io.okhi.android_okverify.interfaces.OkVerifyCallback;
import io.okhi.android_okverify.models.OkHiNotification;

public class MainActivity extends AppCompatActivity {

    private OkCollect okCollect;
    private OkVerify okVerify;
    private OkHi okhi;

    // define a theme that'll be applied to OkCollect
    private final OkHiTheme theme = new OkHiTheme.Builder("#ba0c2f").setAppBarLogo("https://cdn.okhi.co/icon.png").setAppBarColor("#ba0c2f").build();

    // configure any optional features you'd like
    private final OkHiConfig config = new OkHiConfig.Builder().withStreetView().build();

    // define a user
    private final OkHiUser user = new OkHiUser.Builder("+254712345678").withFirstName("Julius").withLastName("Kiano").build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // initiate okcollect using the values defined above
        try {
            okhi = new OkHi(this);
            okCollect = new OkCollect.Builder(this).withTheme(theme).withConfig(config).build();
            okVerify = new OkVerify.Builder(this).build();
            // Should be invoked one time on app start.
            // (optional) OkHiNotification, use to start a foreground service to transmit verification signals to OkHi servers
            int importance = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? NotificationManager.IMPORTANCE_DEFAULT : 3;
            OkVerify.init(getApplicationContext(), new OkHiNotification(
                "Verifying your address",
                "We're currently verifying your address. This won't take long",
                "OkHi",
                "OkHi Address Verification",
                "Alerts related to any address verification updates",
                importance,
                1, // notificationId
                2 // notification request code
            ));
        } catch (OkHiException exception) {
            exception.printStackTrace();
        }
    }

    public void onCreateAddressPress (View v) {
        launchOkCollect();
    }

    private void launchOkCollect() {
        boolean canStartOkCollect = canStartAddressVerification();
        if (canStartOkCollect) {
            // launch okcollect
            okCollect.launch(user, new OkCollectCallback<OkHiUser, OkHiLocation>() {
                @Override
                public void onSuccess(OkHiUser user, OkHiLocation location) {
                    showMessage("Address created "+user.getPhone()+" "+location.getId());
                    startAddressVerification(user, location);
                }
                @Override
                public void onError(OkHiException e) {
                    showMessage("Error "+e.getMessage());
                }
            });
        }
    }

    private void startAddressVerification(OkHiUser user, OkHiLocation location) {
        okVerify.start(user, location, new OkVerifyCallback<String>() {
            @Override
            public void onSuccess(String result) {
                showMessage("Successfully started verification for: " + result);
            }
            @Override
            public void onError(OkHiException e) {
                showMessage("Something went wrong: " + e.getCode());
            }
        });
    }

    //handler class that extends OkHiRequestHandler
    class Handler implements OkHiRequestHandler<Boolean> {
        @Override
        public void onResult(Boolean result) {
            if (result) launchOkCollect();
        }
        @Override
        public void onError(OkHiException exception) {
            showMessage(exception.getMessage());
        }
    }

    // Define a method you'll use to check if conditions are met to start address creation
    private boolean canStartAddressVerification() {
        Handler requestHandler = new Handler();
        // Check and request user to enable location services
        if (!OkHi.isLocationServicesEnabled(getApplicationContext())) {
            okhi.requestEnableLocationServices(requestHandler);
        } else if (!OkHi.isGooglePlayServicesAvailable(getApplicationContext())) {
            // Check and request user to enable google play services
            okhi.requestEnableGooglePlayServices(requestHandler);
        } else if (!OkHi.isBackgroundLocationPermissionGranted(getApplicationContext())) {
            // Check and request user to grant location permission
            okhi.requestBackgroundLocationPermission("Hey we need location permissions", "Pretty please..", requestHandler);
        } else {
            return true;
        }
        return false;
    }

    private void showMessage (String message) {
        Log.v("MainActivity", message);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Pass permission results to okcollect
        okhi.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Pass activity results results to okcollect
        okhi.onActivityResult(requestCode, resultCode, data);
    }
}