
	/**
	 * Release date: January 2, 2017
	 */

	var RuptionARPlugin = function() {


        /**
         *  Start-up configuration: camera position (front or back).
         */
        this.CameraPositionUndefined = 0;
        this.CameraPositionFront     = 1;
        this.CameraPositionBack      = 2;

        /**
         *  Start-up configuration: camera focus range restriction (for iOS only).
         */
        this.CameraFocusRangeNone = 0;
        this.CameraFocusRangeNear = 1;
        this.CameraFocusRangeFar  = 2;
	};


	/*
	 *	=============================================================================================================================
	 *
	 *	PUBLIC API
	 *
	 *	=============================================================================================================================
	 */

	/* Managing ARchitect world loading */

    /**
     * Use this function to request access to restricted APIs like the camera, gps or photo library.
     *
     * @param {function} successCallback A callback which is called if all required permissions are granted.
     * @param {function} errorCallback A callback which is called if one or more permissions are not granted.
     * @param {function} requiredFeatures An array of strings describing which features of the Wikitude SDK are used so that the plugin can request access to those restricted APIs.
     */
    RuptionARPlugin.prototype.requestAccess = function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "RuptionARPlugin", "requestAccess", []);
    };

	/**
	 *	Use this function to load an ARchitect World.
	 *
     *  @param {function(loadedURL)}  		successCallback		function which is called after a successful launch of the AR world.
     *  @param {function(error)}		 	errorCallback		function which is called after a failed launch of the AR world.
     *	@param {String} 					architectWorldPath	The path to a local ARchitect world or to a ARchitect world on a server or your
	 *  @param {String} 					worldPath			path to an ARchitect world, either on the device or on e.g. your Dropbox.
     *  @param {Array} 						requiredFeatures	augmented reality features: a flags mask for enabling/disabling
     *                                  geographic location-based (RuptionARPlugin.FeatureGeo) or image recognition-based (RuptionARPlugin.FeatureImageTracking) tracking.
	 *  @param {json object} (optional) startupConfiguration	represents the start-up configuration which may look like the following:
	 *									{
	 *                               		"cameraPosition": app.RuptionARPlugin.CameraPositionBack,
	 *                                  	    	"*OptionalPlatform*": {
	 *											"*optionalPlatformKey*": "*optionalPlatformValue*"
	 *                                      	}
	 *                               	}
	 */
	RuptionARPlugin.prototype.loadWorld = function(successCallback, errorCallback, architectWorldPath, requiredFeatures, startupConfiguration) {

		cordova.exec(successCallback, errorCallback, "RuptionARPlugin", "open", []);

		if (this.customBackButtonCallback == null) {
            cordova.exec(this.onBackButton, this.onRuptionARError, "RuptionARPlugin", "setBackButtonCallback", []);
		}

		// We add an event listener on the resume and pause event of the application life-cycle
		document.addEventListener("resume", this.onResume, false);
		document.addEventListener("pause", this.onPause, false);
		document.addEventListener("backbutton", this.onBackButton, false);
	};

	/* Managing the Wikitude SDK Lifecycle */
	/**
	 *	Use this function to stop the Wikitude SDK and to remove it from the screen.
	 */
	RuptionARPlugin.prototype.close = function() {

		document.removeEventListener("pause", this.onPause, false);
		document.removeEventListener("resume", this.onResume, false);
		document.removeEventListener("backbutton", this.onBackButton, false);

		cordova.exec(this.onRuptionAROK, this.onRuptionARError, "RuptionARPlugin", "close", [""]);
	};


	/* Interacting with the Wikitude SDK */





	/**
	 *  Use this function to generate a screenshot from the current Wikitude SDK view.
	 *
     *  @param {function(ur)}  successCallback  function which is called after the screen capturing succeeded.
     *  @param {function(err)} errorCallback    function which is called after capturing the screen has failed.
	 *  @param includeWebView Indicates if the ARchitect web view should be included in the generated screenshot or not.
	 *  @param imagePathInBundleorNullForPhotoLibrary If a file path or file name is given, the generated screenshot will be saved in the application bundle. Passing null will save the photo in the device photo library.
	 */
	RuptionARPlugin.prototype.captureScreen = function(successCallback, errorCallback, includeWebView, imagePathInBundleOrNullForPhotoLibrary)
    {
		cordova.exec(successCallback, errorCallback, "RuptionARPlugin", "captureScreen", [includeWebView, imagePathInBundleOrNullForPhotoLibrary]);
	};

	/**
	 * Use this function to set a callback that is called every time the Wikitude SDK encounters an internal error or warning.
	 * Internal errors can occur at any time and might not be related to any WikitudePlugin function invocation.
	 * An error code and message are passed in form of a JSON object.
	 *
	 *  @param {function(jsonObject)}  errorHandler  function which is called every time the SDK encounters an internal error.
	 *
	 * NOTE: The errorHandler is currently only called by the Wikitude iOS SDK!
	 */
	RuptionARPlugin.prototype.setErrorHandler = function(errorHandler)
	{
		cordova.exec(this.onRuptionAROK, errorHandler, "RuptionARPlugin", "setErrorHandler", []);
	}


	/**
	 * Use this function to set a callback that is called every time the back button is pressed while the Wikitude Cordova Plugin is presented.
	 *
	 * @param {function()} onBackButtonCallback function which is called every time the Android back button is pressed.
	 *
	 * Note: The function is only implemented for Android!
	 */
	RuptionARPlugin.prototype.setBackButtonCallback = function(onBackButtonCallback)
	{
		this.customBackButtonCallback = function() {
			onBackButtonCallback();
			RuptionARPlugin.prototype.close();
		}
		cordova.exec(this.customBackButtonCallback, this.onRuptionARError, "RuptionARPlugin", "setBackButtonCallback", []);
	}

	
    /**
     * Use this function to open the application specific system setting view.
     */
	RuptionARPlugin.prototype.openAppSettings = function() {
    	cordova.exec(this.onRuptionAROK, this.onRuptionARError, "RuptionARPlugin", "openAppSettings", []);
	}

	/**
	 * Use this function to display an alert with a specific message.
	 *
	 * @param alertString The message to display in the alert.
	 */
	RuptionARPlugin.prototype.showAlert = function(alertString) {
		cordova.exec(this.onRuptionAROK, this.onRuptionARError, "RuptionARPlugin", "showAlert", [alertString]);
	};

	/*
	 *	=============================================================================================================================
	 *
	 *	Callbacks of public functions
	 *
	 *	=============================================================================================================================
	 */


	/* Lifecycle updates */
	/**
	 *	This function gets called every time the application did become active.
	 */
	RuptionARPlugin.prototype.onResume = function() {

		// Call the Wikitude SDK that it should resume.
		cordova.exec(this.onRuptionAROK, this.onRuptionARError, "RuptionARPlugin", "onResume", [""]);
	};

	/* Lifecycle updates */
	/**
	 *	This function gets called when the application goes back to main
	 */
	RuptionARPlugin.prototype.onBackButton = function() {

		// Call the Wikitude SDK that it should resume.
		//cordova.exec(this.onRuptionAROK, this.onRuptionARError, "RuptionARPlugin", "close", [""]);
		if (this.customBackButtonCallback != null) {
			this.customBackButtonCallback();
		}
		else {
			RuptionARPlugin.prototype.close();
		}
	};

	/**
	 *	This function gets called every time the application is about to become inactive.
	 */
	RuptionARPlugin.prototype.onPause = function() {

		// Call the Wikitude SDK that the application did become inactive
		cordova.exec(this.onRuptionAROK, this.onRuptionARError, "RuptionARPlugin", "onPause", [""]);
	};

	/**
	 *	A generic success callback used inside this wrapper.
	 */
	RuptionARPlugin.prototype.onRuptionAROK = function() {};

	/**
	 *  A generic error callback used inside this wrapper.
	 */
	RuptionARPlugin.prototype.onRuptionARError = function() {};



	/* Export a new WikitudePlugin instance */
	/*var wikitudePlugin = new WikitudePlugin();
	module.exports = wikitudePlugin;*/

	// Installation constructor that binds WkitudePlugin to window
	RuptionARPlugin.install = function () {
		if (!window.plugins) {
			window.plugins = {};
		}
		window.plugins.ruptionARPlugin = new RuptionARPlugin();
		return window.plugins.ruptionARPlugin;
	};
	cordova.addConstructor(RuptionARPlugin.install);
