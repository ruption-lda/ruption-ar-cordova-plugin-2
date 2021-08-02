package com.ruption.ar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.engine.SystemWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.ar.core.examples.java.helloar.HelloArActivity;



/**
 * Basic PhoneGap Wikitude ARchitect Plugin
 *
 * You must add "<plugin name="wikitudeplugin" value="com.wikitude.phonegap.wikitudeplugin"/>"
 * in config.xml to enable this plug-in in your project
 *
 * Also ensure to have wikitudesdk.jar in your libs folder
 *
 * Note:
 * This plug-in is written under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.html
 */
public class RuptionARPlugin extends CordovaPlugin {

    /** PhoneGap-root to Android-app-assets folder ; e.g. use "assets/foo.html" as source if you want to load foo.html from your android-project's assets-folder */
    private static final String	LOCAL_ASSETS_PATH_ROOT		= "assets/";


	/* static action strings */
    /**
     * opens architect-view (add to view stack)
     */
    private static final String	ACTION_OPEN					= "open";

    /**
     * closes architect-view (remove view stack)
     */
    private static final String	ACTION_CLOSE				= "close";

    /**
     * set visibility of helloARRuption to visible (of present)
     */
    private static final String	ACTION_SHOW					= "show";

    /**
     * set visibility of helloARRuption to invisible (of present)
     */
    private static final String	ACTION_HIDE					= "hide";

    /**
     * inject location information
     */
    private static final String	ACTION_SET_LOCATION			= "setLocation";

    /**
     * inject location information
     */
    private static final String	ACTION_CAPTURE_SCREEN			= "captureScreen";

    /**
     * callback for uri-invocations
     */
    private static final String ACTION_ON_URLINVOKE         = "onUrlInvoke";

    /**
     * callback for AR.platform.sendJSONObject
     */
    private static final String ACTION_ON_JSON_RECEIVED     = "onJSONObjectReceived";

    /**
     * life-cycle notification for resume
     */
    private static final String	ACTION_ON_RESUME			= "onResume";

    /**
     * life-cycle notification for pause
     */
    private static final String	ACTION_ON_PAUSE				= "onPause";

    /**
     * check if view is on view-stack (no matter if visible or not)
     */
    private static final String	ACTION_STATE_ISOPEN			= "isOpen";

    /**
     * opens architect-view (add to view stack)
     */

    private static final String ACTION_REQUEST_ACCESS   = "requestAccess";

    private static final String ACTION_OPEN_APP_SETTINGS = "openAppSettings";

    private static final String ACTION_SHOW_ALERT = "showAlert";

    /**
     * check if view is on view-stack (no matter if visible or not)
     */
    private static final String	ACTION_CALL_JAVASCRIPT		= "callJavascript";

    /**
     * Used to set a custom callback that is called once the back button is clicked.
     */
    private static final String ACTION_SET_BACK_BUTTON_CALLBACK = "setBackButtonCallback";

    /**
     * Used as key to see if a local path prefix is given in the strings.xml file or not.
     * If the key is given, it's value will be appended before the `www` substring in the Architect world url.
     */
    private static final String LOCAL_PATH_PREFIX_KEY = "WikitudeCordovaPluginLocalPathPrefix";

    private static final String GET_SDK_BUILD_INFORMATION = "getSDKBuildInformation";

    private static final String GET_SDK_VERSION = "getSDKVersion";

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1;

    private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 2;

    private static final int REQUEST_ACCESS_REQUEST_CODE = 3;

    /**
     * the Wikitude ARchitectview
     */
    private HelloARRuption		helloARRuption;

    /**
     * callback-Id of sendJSONObject method
     */
    private CallbackContext     urlInvokeCallback           = null;

    /**
     * callback-Id of sendJSONObject method
     */
    private CallbackContext jsonObjectReceivedCallback = null;

    /**
     * callback-id of "open"-action method
     */
    private CallbackContext		openCallback				= null;

    /**
     * The custom back button callback id
     */
    private CallbackContext		onBackButtonCallback		= null;
    private CallbackContext requestAccessCallback = null;

    /**
     * last known location of the user, used internally for content-loading after user location was fetched
     */
    protected Location lastKnownLocaton;


    /**
     * sample location strategy
     */
    protected ILocationProvider				locationProvider;


    /**
     * location listener receives location updates and must forward them to the helloARRuption
     */
    protected LocationListener locationListener;

    private boolean useCustomLocation						= false;
    private boolean _locationPermissionRequired             = false;
    private boolean _cameraPermissionGranted          		= false;
    private boolean _locationPermissionRequestRequired      = false;
    private boolean loadFailed                              = false;

    private JSONArray openArgs;
    private String action;

    private JSONArray _savedCaptureScreenArgs;
    private CallbackContext _savedCaptureScreenCallbackContext;

    @Override
    public boolean execute( final String action, final JSONArray args, final CallbackContext callContext ) {
        this.action = action;

		/* hide architect-view -> destroy and remove from activity */
        if ( RuptionARPlugin.ACTION_CLOSE.equals( action ) ) {
            if ( this.helloARRuption != null ) {
                this.cordova.getActivity().runOnUiThread( new Runnable() {

                    @Override
                    public void run() {
                        if (RuptionARPlugin.this.locationProvider != null) {
                            RuptionARPlugin.this.locationProvider.onPause();
                        }
                        removeHelloARActivity();
                    }
                } );
                callContext.success( action + ": helloARRuption is present" );
            }
            else {
                callContext.error( action + ": helloARRuption is not present" );
            }
            return true;
        }

		/* return success only if view is opened (no matter if visible or not) */
        if ( RuptionARPlugin.ACTION_STATE_ISOPEN.equals( action ) ) {
            if ( this.helloARRuption != null ) {
                callContext.success( action + ": helloARRuption is present" );
            } else {
                callContext.error( action + ": helloARRuption is not present" );
            }
            return true;
        }

        if (RuptionARPlugin.ACTION_CAPTURE_SCREEN.equals(action) )
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || this.cordova.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                captureScreen(args, callContext);
            } else {
                _savedCaptureScreenArgs = args;
                _savedCaptureScreenCallbackContext = callContext;
                this.cordova.requestPermissions(this, EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE,  new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE});
            }

            return true;
        }

		/* life-cycle's RESUME */
        if ( RuptionARPlugin.ACTION_ON_RESUME.equals( action ) ) {

            if ( this.helloARRuption != null ) {
                this.cordova.getActivity().runOnUiThread( new Runnable() {

                    @Override
                    public void run() {
                        RuptionARPlugin.this.helloARRuption.onResume();
                        callContext.success( action + ": helloARRuption is present" );
                        if (locationProvider != null) {
                            locationProvider.onResume();
                        }
                    }
                } );

                // callContext.success( action + ": helloARRuption is present" );
            } else {
                callContext.error( action + ": helloARRuption is not present" );
            }
            return true;
        }

		/* life-cycle's PAUSE */
        if ( RuptionARPlugin.ACTION_ON_PAUSE.equals( action ) ) {
            if ( helloARRuption != null ) {
                this.cordova.getActivity().runOnUiThread( new Runnable() {

                    @Override
                    public void run() {
                        RuptionARPlugin.this.helloARRuption.onPause();
                        if (locationProvider != null) {
                            locationProvider.onPause();
                        }
                    }
                } );

                callContext.success( action + ": helloARRuption is present" );
            } else {
                callContext.error( action + ": helloARRuption is not present" );
            }
            return true;
        }

        /* set a custom callback that is called when our plugin internally  */
        if ( RuptionARPlugin.ACTION_SET_BACK_BUTTON_CALLBACK.equals( action ) ) {
            this.onBackButtonCallback = callContext;
            final PluginResult result = new PluginResult( PluginResult.Status.NO_RESULT, action + ": registered back button callback");
            result.setKeepCallback(true);
            callContext.sendPluginResult( result );
            return true;
        }

        /* define call-back for url-invocations */
        if ( RuptionARPlugin.ACTION_ON_URLINVOKE.equals( action ) ) {
            this.urlInvokeCallback = callContext;
            final PluginResult result = new PluginResult( PluginResult.Status.NO_RESULT, action + ": registered callback" );
            result.setKeepCallback( true );
            callContext.sendPluginResult( result );
            return true;
        }

        /* define call-back for AR.platform.sendJSONObject */
        if ( RuptionARPlugin.ACTION_ON_JSON_RECEIVED.equals( action ) ) {
            this.jsonObjectReceivedCallback = callContext;
            final PluginResult result = new PluginResult( PluginResult.Status.NO_RESULT, action + ": registered callback" );
            result.setKeepCallback( true );
            callContext.sendPluginResult( result );
            return true;
        }

		

        if ( RuptionARPlugin.ACTION_REQUEST_ACCESS.equals( action ) ) {
            requestAccessCallback = callContext;
            JSONArray jsonArray = null;
            try {
                jsonArray = args.getJSONArray( 0 );
            } catch (JSONException e) {
            }

            int features = convertArFeatures(jsonArray);

            boolean cameraPermissionRequestRequired = !cordova.hasPermission(Manifest.permission.CAMERA);
            _locationPermissionRequestRequired = !cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && !cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            _locationPermissionRequired = (ArchitectStartupConfiguration.Features.Geo & features) == ArchitectStartupConfiguration.Features.Geo;

            if(cameraPermissionRequestRequired && (_locationPermissionRequestRequired && _locationPermissionRequired)) {
                _cameraPermissionGranted = false;
                this.cordova.requestPermissions(this, REQUEST_ACCESS_REQUEST_CODE, new String[] { Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            } else if (cameraPermissionRequestRequired) {
                this.cordova.requestPermission(this, REQUEST_ACCESS_REQUEST_CODE, Manifest.permission.CAMERA);
            } else if (_locationPermissionRequestRequired && _locationPermissionRequired) {
                _cameraPermissionGranted = true;
                this.cordova.requestPermissions(this, REQUEST_ACCESS_REQUEST_CODE, new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            } else {
                callContext.success();
            }
            return true;
        }


        /* initial set-up, show ArchitectView full-screen in current screen/activity */
        if ( RuptionARPlugin.ACTION_OPEN.equals( action ) ) {
            this.openCallback = callContext;

            boolean cameraPermissionRequestRequired = !cordova.hasPermission(Manifest.permission.CAMERA);
            _locationPermissionRequestRequired = !cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && !cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            _locationPermissionRequired = (ArchitectStartupConfiguration.Features.Geo & features) == ArchitectStartupConfiguration.Features.Geo;

            if(cameraPermissionRequestRequired && (_locationPermissionRequestRequired && _locationPermissionRequired)) {
                _cameraPermissionGranted = false;
                this.cordova.requestPermissions(this, CAMERA_PERMISSION_REQUEST_CODE, new String[] { Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            } else if (cameraPermissionRequestRequired) {
                this.cordova.requestPermission(this, CAMERA_PERMISSION_REQUEST_CODE, Manifest.permission.CAMERA);
            } else if (_locationPermissionRequestRequired && _locationPermissionRequired) {
                _cameraPermissionGranted = true;
                this.cordova.requestPermissions(this, CAMERA_PERMISSION_REQUEST_CODE, new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            } else {
                loadWorld();
                changeSurface(1, 1);
                drawFrame();
            }
            return true;
        }

        if ( RuptionARPlugin.ACTION_OPEN_APP_SETTINGS.equals( action ) ) {
            final Intent i = new Intent();
            i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            i.setData(Uri.parse("package:" + cordova.getActivity().getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            cordova.getActivity().startActivity(i);
            return true;
        }

        if ( RuptionARPlugin.ACTION_SHOW_ALERT.equals( action ) ) {
            try {
                final String alertString = args.getString( 0 );
                AlertDialog.Builder builder = new AlertDialog.Builder(cordova.getActivity());
                builder.setMessage(alertString)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            } catch (JSONException e) {
                callContext.error( action + ": exception thrown, " + e != null ? e.getMessage() : "(exception is NULL)" );
                return true;
            }
            return true;
        }

        /* fall-back return value */
        callContext.sendPluginResult( new PluginResult( PluginResult.Status.ERROR, "no such action: " + action ) );
        return false;
    }

    private void loadWorld() {
        PluginResult result = null;
        try {

            this.cordova.getActivity().runOnUiThread( new Runnable() {

                @Override
                public void run() {
                    try {
                        RuptionARPlugin.this.onSurfaceCreated( RuptionARPlugin.this.render );
                    } catch ( Exception e ) {
							/* in case "addArchitectView" threw an exception -> notify callback method asynchronously */
                        openCallback.error( e != null ? e.getMessage() : "Exception is 'null'" );
                    }
                }
            } );

        } catch ( Exception e ) {
            result = new PluginResult( PluginResult.Status.ERROR, action + ": exception thown, " + e != null ? e.getMessage() : "(exception is NULL)" );
            result.setKeepCallback( false );
            this.openCallback.sendPluginResult(result);
        }

			/* adding architect-view is done in separate thread, ensure to setKeepCallback so one can call success-method properly later on */
        result = new PluginResult( PluginResult.Status.NO_RESULT, action + ": no result required, just registered callback-method" );
        result.setKeepCallback( true );
        this.openCallback.sendPluginResult(result);
    }

    private void changeSurface(int width, int height) {
        RuptionARPlugin.this.onSurfaceChanged(RuptionARPlugin.this.render, width, height);
    }

    private void drawFrame() {
        RuptionARPlugin.this.onDrawFrame(RuptionARPlugin.this.render);
    }

    /**
     * hides/removes ARchitect-View completely
     * @return true if successful, false otherwise
     */
    private boolean removeHelloARActivity() {
        if ( this.helloARRuption != null ) {

			/* fake life-cycle calls, because activity is already up and running */
            this.helloARRuption.onPause();
            this.helloARRuption.onDestroy();

            RuptionARPlugin.handleResumeInCordovaWebView(cordova.getActivity().getWindow().getDecorView().findViewById(android.R.id.content));
            RuptionARPlugin.releaseFocusInCordovaWebView(cordova.getActivity().getWindow().getDecorView().findViewById(android.R.id.content));

            return true;
        }
        return false;
    }



    /**
     * add helloARRuption to current screen
     * @param apiKey developers's api key to use (hides watermarking/intro-animation if it matches your package-name)
     * @param filePath the url (starting with http:// for online use; starting with LOCAL_ASSETS_PATH_ROOT if oyu want to load assets within your app-assets folder)
     * @param features Augmented Reality mode ()
     * @throws IOException might be thrown from ARchitect-SDK
     */
    private void addHelloARRuption() throws IOException {
        if ( this.helloARRuption == null ) {

            RuptionARPlugin.releaseFocusInCordovaWebView(cordova.getActivity().getWindow().getDecorView().findViewById(android.R.id.content));

            this.helloARRuption = new HelloARRuption( this.cordova.getActivity() , new HelloARRuption.OnKeyUpDownListener() {

                @Override
                public boolean onKeyUp(int keyCode, KeyEvent event) {
                    if (helloARRuption!=null && keyCode == KeyEvent.KEYCODE_BACK) {
                        if (!loadFailed && helloARRuption.webViewGoBack()) {
                            return false;
                        } else {
                            if ( RuptionARPlugin.this.onBackButtonCallback != null ) {
                                try {
                                    /* pass called url as String to callback-method */
                                    final PluginResult res = new PluginResult( PluginResult.Status.OK);
                                    res.setKeepCallback(true);
                                    RuptionARPlugin.this.onBackButtonCallback.sendPluginResult( res );
                                } catch ( Exception e ) {
                                    RuptionARPlugin.this.onBackButtonCallback.error( "onBackButton result could not be send." );
                                }
                            }
                            return true;
                        }
                    } else {
                        return false;
                    }
                }

                @Override
                public boolean onKeyDown(int keyCode, KeyEvent event) {
                    return helloARRuption!=null && keyCode == KeyEvent.KEYCODE_BACK;
                }
            });

            this.helloARRuption.onWindowFocusChanged();

            this.locationListener = new LocationListener() {

                @Override
                public void onStatusChanged( String provider, int status, Bundle extras ) {
                }

                @Override
                public void onProviderEnabled( String provider ) {
                }

                @Override
                public void onProviderDisabled( String provider ) {
                }
            };

			/* add content view and fake initial life-cycle */
            (this.cordova.getActivity()).addContentView( this.helloARRuption, new ViewGroup.LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT ) );
            (this.cordova.getActivity()).setVolumeControlStream( AudioManager.STREAM_MUSIC );

			/* fake life-cycle calls, because activity is already up and running */
            this.helloARRuption.onCreate( this.helloARRuption );

			/* also a fake-life-cycle call (the last one before it is really shown in UI */
            this.helloARRuption.onResume();
        }

        // hide keyboard when adding AR view on top of views
        InputMethodManager inputManager = (InputMethodManager)
                (this.cordova.getActivity()).getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow((this.cordova.getActivity()).getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }


    private static void releaseFocusInCordovaWebView(View rootView) {
        if (rootView instanceof SystemWebView) {
            ((SystemWebView) rootView).getCordovaWebView().getView().clearFocus();
        } else if (rootView instanceof ViewGroup) {
            final int childCount = ((ViewGroup)rootView).getChildCount();
            for (int i=0; i< childCount; i++) {
                RuptionARPlugin.releaseFocusInCordovaWebView(((ViewGroup)rootView).getChildAt(i));
            }
        }
    }

    /**
     * To avoid JavaScript in Cordova staying paused after CordovaWebView lost focus call "handleResume" of the CordovaView in current Activity
     * @param rootView the root view to search recursively for a CordovaWebView
     */
    private static void handleResumeInCordovaWebView(final View rootView) {
        if (rootView instanceof SystemWebView) {
            ((SystemWebView) rootView).getCordovaWebView().handleResume(true);
        }
        else if (rootView instanceof ViewGroup) {
            final int childCount = ((ViewGroup)rootView).getChildCount();
            for (int i=0; i< childCount; i++) {
                RuptionARPlugin.handleResumeInCordovaWebView(((ViewGroup)rootView).getChildAt(i));
            }
        }
    }


    protected static class HelloARRuption extends HelloArActivity {
        public static interface OnKeyUpDownListener {
            public boolean onKeyDown(int keyCode, KeyEvent event);

            public boolean onKeyUp(int keyCode, KeyEvent event);
        }

        private final OnKeyUpDownListener onKeyUpDownListener;

        @Deprecated
        public HelloARRuption(Context context) {
            super(context);
            this.onKeyUpDownListener = null;
        }

        public HelloARRuption(Context context, OnKeyUpDownListener onKeyUpDownListener) {
            super(context);
            this.onKeyUpDownListener = onKeyUpDownListener;
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            // forward onKeyDown events to listener
            return this.onKeyUpDownListener!=null &&  this.onKeyUpDownListener.onKeyDown(keyCode, event);
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            // forward onKeyUp events to listener
            return this.onKeyUpDownListener!=null &&  this.onKeyUpDownListener.onKeyUp(keyCode, event);
        }

        @Override
        protected void onFocusChanged(boolean gainFocus, int direction,
                                      Rect previouslyFocusedRect) {
            super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

            // ensure helloARRuption does not loose focus on screen orientation changes etc.
            if (!gainFocus) {
                this.requestFocus();
            }
        }
    }

    /**
     * Sample implementation of a locationProvider, feel free to polish this very basic approach (compare http://goo.gl/pvkXV )
     * @author Wikitude GmbH
     *
     */
    private static class LocationProvider implements ILocationProvider {

        /** location listener called on each location update */
        private final LocationListener	locationListener;

        /** system's locationManager allowing access to GPS / Network position */
        private final LocationManager	locationManager;

        /** location updates should fire approximately every second */
        private static final int		LOCATION_UPDATE_MIN_TIME_GPS	= 1000;

        /** location updates should fire, even if last signal is same than current one (0m distance to last location is OK) */
        private static final int		LOCATION_UPDATE_DISTANCE_GPS	= 0;

        /** location updates should fire approximately every second */
        private static final int		LOCATION_UPDATE_MIN_TIME_NW		= 1000;

        /** location updates should fire, even if last signal is same than current one (0m distance to last location is OK) */
        private static final int		LOCATION_UPDATE_DISTANCE_NW		= 0;

        /** to faster access location, even use 10 minute old locations on start-up */
        private static final int		LOCATION_OUTDATED_WHEN_OLDER_MS	= 1000 * 60 * 10;

        /** is gpsProvider and networkProvider enabled in system settings */
        private boolean					gpsProviderEnabled, networkProviderEnabled;

        /** the context in which we're running */
        private final Context			context;


        public LocationProvider( final Context context, LocationListener locationListener ) {
            super();
            this.locationManager = (LocationManager)context.getSystemService( Context.LOCATION_SERVICE );
            this.locationListener = locationListener;
            this.context = context;
            this.gpsProviderEnabled = this.locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER );
            this.networkProviderEnabled = this.locationManager.isProviderEnabled( LocationManager.NETWORK_PROVIDER );
        }

        @Override
        public void onPause() {
            if ( this.locationListener != null && this.locationManager != null && (this.gpsProviderEnabled || this.networkProviderEnabled) ) {
                this.locationManager.removeUpdates( this.locationListener );
            }
        }

        @Override
        public void onResume() {
            if ( this.locationManager != null && this.locationListener != null ) {

                // check which providers are available are available
                this.gpsProviderEnabled = this.locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER );
                this.networkProviderEnabled = this.locationManager.isProviderEnabled( LocationManager.NETWORK_PROVIDER );

                /** is GPS provider enabled? */
                if ( this.gpsProviderEnabled ) {
                    final Location lastKnownGPSLocation = this.locationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER );
                    if ( lastKnownGPSLocation != null && lastKnownGPSLocation.getTime() > System.currentTimeMillis() - LOCATION_OUTDATED_WHEN_OLDER_MS ) {
                        locationListener.onLocationChanged( lastKnownGPSLocation );
                    }
                    this.locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, LOCATION_UPDATE_MIN_TIME_GPS, LOCATION_UPDATE_DISTANCE_GPS, this.locationListener );
                }

                /** is Network / WiFi positioning provider available? */
                if ( this.networkProviderEnabled ) {
                    final Location lastKnownNWLocation = this.locationManager.getLastKnownLocation( LocationManager.NETWORK_PROVIDER );
                    if ( lastKnownNWLocation != null && lastKnownNWLocation.getTime() > System.currentTimeMillis() - LOCATION_OUTDATED_WHEN_OLDER_MS ) {
                        locationListener.onLocationChanged( lastKnownNWLocation );
                    }
                    this.locationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER, LOCATION_UPDATE_MIN_TIME_NW, LOCATION_UPDATE_DISTANCE_NW, this.locationListener );
                }

                /** user didn't check a single positioning in the location settings, recommended: handle this event properly in your app, e.g. forward user directly to location-settings, new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS ) */
                if ( !this.gpsProviderEnabled && !this.networkProviderEnabled ) {
                    Toast.makeText( this.context, "Please enable GPS and Network positioning in your Settings ", Toast.LENGTH_LONG ).show();
                }
            }
        }
    }

    private interface ILocationProvider {

        /**
         * Call when host-activity is resumed (usually within systems life-cycle method)
         */
        public void onResume();

        /**
         * Call when host-activity is paused (usually within systems life-cycle method)
         */
        public void onPause();

    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {

        ArrayList<String> deniedPermissions = new ArrayList<String>();
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            Log.e("Plugin", "permission" + permission);
            if (permission.equals(Manifest.permission.CAMERA)) {
                if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if ((!_locationPermissionRequired || !_locationPermissionRequestRequired) && requestCode != REQUEST_ACCESS_REQUEST_CODE) {
                        this.loadWorld();
                        break;
                    } else {
                        _cameraPermissionGranted = true;
                    }
                } else {
                    if (requestCode == REQUEST_ACCESS_REQUEST_CODE) {
                        deniedPermissions.add(permission);
                    } else {
                        this.openCallback.error("Camera permissions wasn't granted");
                    }
                }
            } else if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) || permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if (_cameraPermissionGranted && requestCode != REQUEST_ACCESS_REQUEST_CODE) {
                        this.loadWorld();
                        break;
                    } else {
                        _locationPermissionRequestRequired = false;
                    }
                } else {
                    if (requestCode == REQUEST_ACCESS_REQUEST_CODE) {
                        deniedPermissions.add(permission);
                    } else {
                        this.openCallback.error("Location permission wasn't granted");
                    }
                }
            } else if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    switch (requestCode) {
                        case EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE:
                            captureScreen(_savedCaptureScreenArgs, _savedCaptureScreenCallbackContext);
                            break;
                    }
                } else {
                    if (requestCode == REQUEST_ACCESS_REQUEST_CODE) {
                        deniedPermissions.add(permission);
                    } else {
                        this.openCallback.error("External Storage permission wasn't granted");
                    }
                }
            }
        }

        if (requestCode == REQUEST_ACCESS_REQUEST_CODE) {
            if (!deniedPermissions.isEmpty()) {
                String permissionsString = deniedPermissions.toString();
                String developerDescription = "The user denied the following permissions: " + permissionsString;

                String userDescription = "The following permissions need to be granted to enable an AR Experience: ";
                for (String permission : deniedPermissions) {
                    if (permission.equals(Manifest.permission.CAMERA)) {
                        userDescription += "Camera ";
                    }
                    if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        userDescription += "Location ";
                    }
                }

                try {
                    requestAccessCallback.error(new JSONObject("{" +
                            "\"userDescription\":\"" + userDescription + "\"," +
                            "\"developerDescription\":\"" + developerDescription + "\"" +
                            "}"));
                } catch (JSONException e) {
                }
            } else {
                requestAccessCallback.success();
            }
        }
    }

    private boolean captureScreen(final JSONArray args, final CallbackContext callContext)
    {
        if (helloARRuption != null)
        {
            int captureMode = ArchitectView.CaptureScreenCallback.CAPTURE_MODE_CAM_AND_WEBVIEW;

            try
            {
                captureMode = ( args.getBoolean( 0 )) ? ArchitectView.CaptureScreenCallback.CAPTURE_MODE_CAM_AND_WEBVIEW : ArchitectView.CaptureScreenCallback.CAPTURE_MODE_CAM;
            }
            catch (Exception e)
            {
                // unexpected error;
            }
            String name = "";
            if (args.length() > 1 && !args.isNull(1))
            {
                try
                {
                    name = args.getString(1);
                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                }
            }

            final String fileName = name;

            helloARRuption.captureScreen(captureMode, new CaptureScreenCallback()
            {
                @Override
                public void onScreenCaptured(Bitmap screenCapture)
                {
                    final ContentResolver resolver = cordova.getActivity().getApplicationContext().getContentResolver();
                    final StringBuilder finalPath = new StringBuilder();
                    try
                    {
                        String path;
                        if (fileName.equals(""))
                        {
                            path = String.valueOf(System.currentTimeMillis());
                        }
                        else
                        {
                            String[] fileNameSplit = fileName.split("\\.");
                            path = fileNameSplit[0];
                        }

                        ContentValues values = new ContentValues();
                        values.put(Images.Media.MIME_TYPE, "image/jpeg");
                        values.put(Images.Media.DISPLAY_NAME, path);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
                            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                        } else {
                            File imageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                            File file = new File(imageDirectory, path + ".jpg");
                            values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
                            finalPath.append(file.getAbsolutePath());
                        }

                        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            finalPath.append(uri.getPath());
                        }

                        try (final OutputStream out = resolver.openOutputStream(uri)) {
                            screenCapture.compress(Bitmap.CompressFormat.JPEG, 90, out);
                            out.flush();
                        }

                        cordova.getActivity().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                callContext.success(finalPath.toString());
                                // 								in case you want to sent the pic to other applications, uncomment these lines (for future use)
                                //								final Intent share = new Intent(Intent.ACTION_SEND);
                                //								share.setType("image/jpg");
                                //								share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(screenCaptureFile));
                                //								final String chooserTitle = "Share Snaphot";
                                //								cordova.getActivity().startActivity(Intent.createChooser(share, chooserTitle));
                            }
                        });
                    }
                    catch (Exception e)
                    {
                        callContext.error(e.getMessage());
                    }
                }
            });
        }
        return true;
    }
}
