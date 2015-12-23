SpacePlayer
====
Android Music Player, Just a project for HMI-Design class.

### Development
* `IDE`: Intellij Idea 15
* `SDK`: Android API 23
* `Buildtools`: 23.0.2
* `Gradle`: 2.2.1-all

### Install
You can download apk file form [***Here***](http://download.koumakan.cc/SpacePlayer "SpacePlayer_release_v.apk")

### Compile and Sign
If you use GUI like Android Studio, use `Build->Generate Signed APK`.
If you use CLI, uncomment follows code in `/app/build.gradle` :

````GROOVY
//	signingConfigs { //gradle assembleRelease
//		release {
//			storeFile file(System.console().readLine("\nKeystore file: "))
//			storePassword System.console().readLine("\nKeystore password: ")
//			keyAlias System.console().readLine("\nKey Alias: ")
//			keyPassword System.console().readLine("\nKey password: ")
//		}
//	}

	buildTypes {
		debug {
			applicationIdSuffix ".debug"
			minifyEnabled false
			manifestPlaceholders = [apk_name: default_apk_name + "_Debug"]
		}
		release {
//			signingConfig signingConfigs.release
			minifyEnabled false
			manifestPlaceholders = [apk_name: default_apk_name]
			zipAlignEnabled true
		}
	}
````

````
$ gradle assembleRelease
````

then input your key, the signed APK will be generated.