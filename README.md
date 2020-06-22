# Look4Sat: Radio satellite tracker

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.rtbishop.look4sat/)
[<img src="https://play.google.com/intl/en_gb/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80">](https://play.google.com/store/apps/details?id=com.rtbishop.look4sat)

Amateur radio and weather satellite tracker and passes predictor for Android.

The very app creation and design is hugely inspired by the open-source [Gpredict](http://gpredict.oz9aec.net/) desktop satellite tracking application, created by [Alexandru Csete, OZ9AEC](https://github.com/csete) and contributors, supported by the [Libre Space Foundation](https://libre.space/).

The [Libre Space Foundation](https://libre.space/) team is also behind the epic [SatNOGS](https://satnogs.org/) project that provides an extremely easy to use [API](https://db.satnogs.org/api/) and [DB](https://db.satnogs.org/) with a huge amount of information about satellites, their telemetry and transmitters, which the app uses under the hood.

For TLE data calculation and passes prediction Look4Sat uses the mavenized version of [predict4java](https://github.com/davidmoten/predict4java) library, created by [David A. B. Johnson, G4DPZ](https://github.com/g4dpz) and [Dave Moten](https://github.com/davidmoten). Thank you guys for your hard work making this lib efficient and easy to use!

Also, I'd like to mention Dr T.S. Kelso for his invaluable contribution to the industry and for his [Celestrak](https://celestrak.com) website, that stores, updates and provides access to TLE data.

The app is built using Dagger2, Retrofit2, Kotlin and Kotlin coroutines, Architecture Components and Jetpack Navigation.

<p float="left">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="250"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="250"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="250"/>
</p>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="750">

## Main features:

*  Calculating satellite passes for up to one week (168 hours)
*  Calculating passes for the current or manually entered location
*  Showing the list of currently active and upcoming satellite passes
*  Showing the active pass progress, polar trajectory and transceivers info
*  Showing the satellite positional data, footprint and ground track on a map
*  Offline first: passes predictions are made offline.
Weekly updates of TLEs and transceivers DB are recommended.
