# Look4Sat: Satellite tracker

[![Look4Sat CI](https://github.com/rt-bishop/Look4Sat/actions/workflows/release.yml/badge.svg)](https://github.com/rt-bishop/Look4Sat/actions/workflows/release.yml)

[<img src="https://play.google.com/intl/en_gb/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80">](https://play.google.com/store/apps/details?id=com.rtbishop.look4sat)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.rtbishop.look4sat/)

### Radio satellite tracker and pass predictor for Android, inspired by Gpredict

<p float="left">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="192"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="192"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="192"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="192">
</p>

### Track satellite passes with ease!

Thanks to [Celestrak](https://celestrak.com/) and [SatNOGS](https://satnogs.org/) you have access to over 9000 satellites.\
You can search the entire database by NORAD Catalog Number or the satellite's name.

Orbital positions and passes are calculated relative to your location.\
To get reliable data make sure to set the station position via the app Settings.

The application is built using Kotlin, Coroutines, Jetpack Compose and Navigation.\
It is now and always will be completely ad-free and open-source.

## Main features:

*  Predicting satellite positions and passes for up to 10 days
*  Showing the list of currently active and upcoming satellite passes
*  Showing the active pass progress, polar trajectory and transceivers info
*  Showing the satellite positional data, footprint and ground track on the map
*  Custom TLE satellite data import is available via Three Line Element .txt files
*  Offline first: calculations are made offline. Weekly TLE data update is recommended.
