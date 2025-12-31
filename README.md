# Look4Sat: Satellite tracker

[![Look4Sat CI](https://github.com/rt-bishop/Look4Sat/actions/workflows/main.yml/badge.svg)](https://github.com/rt-bishop/Look4Sat/actions/workflows/main.yml)

# [cf 反代加速worker.js](/wty_add_whitelist_worker.js)

你怎么能直接 commit 到我的 main 分支啊？！GitHub 上不是这样！你应该先 fork 我的仓库，然后从 develop 分支 checkout 一个新的 feature 分支，比如叫 feature/confession。然后你把你的心意写成代码，并为它写好单元测试和集成测试，确保代码覆盖率达到95%以上。接着你要跑一下 Linter，通过所有的代码风格检查。然后你再 commit，commit message 要遵循 Conventional Commits 规范。之后你把这个分支 push 到你自己的远程仓库，然后给我提一个 Pull Request。在 PR 描述里，你要详细说明你的功能改动和实现思路，并且 @ 我和至少两个其他的评审。我们会 review 你的代码，可能会留下一些评论，你需要解决所有的 thread。等 CI/CD 流水线全部通过，并且拿到至少两个 LGTM 之后，我才会考虑把你的分支 squash and merge 到 develop 里，等待下一个版本发布。你怎么直接上来就想 force push 到 main？！GitHub 上根本不是这样！我拒绝合并！

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
