# expense_manager ![Travis Ci](https://github.com/SouravDas25/Tracko/workflows/Build%20and%20Release%20apk/badge.svg)

A new Flutter project.


## Getting Started

#### Generate ORM Bean Command
del lib\models\*.jorm.dart

flutter packages pub run build_runner build



keytool -genkey -v -keystore ./android-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias android-key
