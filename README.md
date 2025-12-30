# Look4Sat: Satellite tracker

[![Look4Sat CI](https://github.com/rt-bishop/Look4Sat/actions/workflows/main.yml/badge.svg)](https://github.com/rt-bishop/Look4Sat/actions/workflows/main.yml)


# [cf 反代加速worker.js](/wty_add_whitelist_worker.js)
### 将卫星数据更新添加了代理，方便访问

### 修改了包名com.rtbishop_wty.look4sat

### Radio satellite tracker and pass predictor for Android, inspired by Gpredict

<p float="left">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="180"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="180"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="180"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="180">
</p>

### Track satellite passes with ease!

Thanks to the huge database provided by [Celestrak](https://celestrak.com/) and [SatNOGS](https://satnogs.org/) you have access to more than 5000 active satellites orbiting Earth. You can search the entire DB by satellite name or by NORAD catnum.

Satellite positions and passes are calculated relative to your location. To get reliable info make sure to set the observation position using GPS or QTH Locator in the Settings menu.

The application is built using Kotlin, Coroutines, Architecture Components and Jetpack Navigation. It is now and always will be completely ad-free and open-source.

Huge thanks to [DownloadAstro](https://appoftheday.downloadastro.com/app/look4sat-satellite-tracker/) team for their interest to the app and the interview published.

## Main features:

*  Predicting satellite positions and passes for up to a week
*  Showing the list of currently active and upcoming satellite passes
*  Showing the active pass progress, polar trajectory and transceivers info
*  Showing the satellite positional data, footprint and ground track on a map
*  Custom TLE data import is available via files with TXT or TLE extensions
*  Offline first: calculations are made offline. Weekly update of TLE data is recommended.
