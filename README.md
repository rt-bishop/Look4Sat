# Look4Sat: Radio satellite tracker

[<img src="https://play.google.com/intl/en_gb/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80">](https://play.google.com/store/apps/details?id=com.rtbishop.look4sat)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.rtbishop.look4sat/)

### Open-source amateur radio satellite tracker and pass predictor for Android

<p float="left">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="200"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="200"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="200"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="200">
</p>

### Let this app look for satellites for you!

Thanks to the huge database provided by Celestrak and SatNOGS you have access to more than 3000
active satellites orbiting Earth at this very moment. You can search the entire DB by satellite
name or by NORAD catalog number. Track them with ease!

Satellite positions and passes are calculated relative to your location. In order to get reliable
predictions set the observation position using GPS or QTH Locator in the Settings menu.

The application is built using predict4java library, Kotlin, Coroutines, Dagger2, Retrofit2, Moshi,
Architecture Components and Jetpack Navigation. It is now and always will be completely ad-free
and open-source.

## Main features:

*  Predicting satellite positions and passes for up to 4 days (96 hours)
*  Showing the list of currently active and upcoming satellite passes
*  Showing the active pass progress, polar trajectory and transceivers info
*  Showing the satellite positional data, footprint and ground track on a map
*  Custom TLE data import is available via files with TXT or TLE extensions
*  Offline first: calculations are made offline. Weekly update of TLE data is recommended.
