object Dependencies {
    //Coil
    const val coil = "io.coil-kt:coil:${Version.coil}"
    const val coilCompose = "io.coil-kt:coil-compose:${Version.coil}"

    //Jetpack Accompanist
    const val accompanistSystemUi = "com.google.accompanist:accompanist-systemuicontroller:${Version.accompanist}"
    const val accompanistInsets = "com.google.accompanist:accompanist-insets:${Version.accompanist}"
    const val accompanistSwipeRefresh = "com.google.accompanist:accompanist-swiperefresh:${Version.accompanist}"

    //Android Upload Service
    const val androidUploadService = "net.gotev:uploadservice:${Version.androidUploadService}"

    //Gson
    const val gson = "com.google.code.gson:gson:${Version.gson}"

    //Firebase & Google
    const val firebaseMessaging = "com.google.firebase:firebase-messaging-ktx:${Version.firebaseMessaging}"
    const val firebaseAnalytics = "com.google.firebase:firebase-analytics-ktx:${Version.firebaseAnalytics}"

    //Third party login
    const val googleAuth = "com.google.android.gms:play-services-auth:${Version.googleAuth}"
    const val microsoftAuth = "com.microsoft.identity.client:msal:${Version.microsoftAuth}"
    const val facebookAuth = "com.facebook.android:facebook-android-sdk:${Version.facebookAuth}"

    //gRPC
    const val okhttp = "com.squareup.okhttp:okhttp:${Version.okhttp}"
    const val grpcStub = "io.grpc:grpc-stub:${Version.grpc}"
    const val grpcOkhttp = "io.grpc:grpc-okhttp:${Version.grpc}"
    const val grpcProtoBufUtil = "io.grpc:grpc-protobuf-lite:${Version.grpc}"
    const val javaxAnnotations = "javax.annotation:javax.annotation-api:${Version.javaxAnnotations}"
    const val protoParser = "com.squareup:protoparser:${Version.protoParser}"

    //Glide
    const val glide = "com.github.bumptech.glide:glide:${Version.glide}"
    const val glideCompiler = "com.github.bumptech.glide:compiler:${Version.glide}"
    const val glideTransformations = "jp.wasabeef:glide-transformations:${Version.glideTransformations}"
    const val toggleImageButton = "net.colindodd:toggleimagebutton:${Version.toggleImageButton}"

    //Coroutines
    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Version.coroutines}"
    const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.coroutines}"

    const val jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Version.kotlin}"
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}"
    const val androidCore = "androidx.core:core-ktx:$${Version.androidCore}"
    const val appCompat = "androidx.appcompat:appcompat:${Version.appCompat}"
    const val appCompatV7 = "com.android.support:appcompat-v7:${Version.appCompatV7}"
    const val materialDesign = "com.google.android.material:material:${Version.materialDesign}"
    const val activity = "androidx.activity:activity-ktx:${Version.activity}"

    //Architecture components
    const val lifecycleLiveData = "androidx.lifecycle:lifecycle-livedata-ktx:${Version.lifecycleLiveData}"
    const val lifecycleLiveDataKtx = "androidx.lifecycle:lifecycle-livedata:${Version.lifecycleLiveData}"
    const val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel:${Version.lifecycleViewModel}"
    const val lifecycleViewModelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Version.lifecycleViewModel}"
    const val lifecycleExtensions = "androidx.lifecycle:lifecycle-extensions:${Version.lifecycleExtensions}"

    //Jetpack Compose
    const val composeActivity = "androidx.activity:activity-compose:1.3.1"
    const val composeCompiler = "androidx.compose.compiler:compiler:${Version.jetpackCompose}"
    const val composeRuntime = "androidx.compose.runtime:runtime:${Version.jetpackCompose}"
    const val composeRuntimeLiveData = "androidx.compose.runtime:runtime-livedata:${Version.jetpackCompose}"
    const val composeNavigation = "androidx.navigation:navigation-compose:${Version.jetpackComposeNav}"
    const val composeUi = "androidx.compose.ui:ui:${Version.jetpackCompose}"
    const val composeMaterialIcons = "androidx.compose.material:material-icons-core:${Version.jetpackCompose}"
    const val composeMaterialIconsExtended = "androidx.compose.material:material-icons-extended:${Version.jetpackCompose}"
    const val composeMaterial = "androidx.compose.material:material:${Version.jetpackCompose}"
    const val composeFoundation = "androidx.compose.foundation:foundation:${Version.jetpackCompose}"
    const val composeFoundationLayout = "androidx.compose.foundation:foundation-layout:${Version.jetpackCompose}"
    const val composeAnimation = "androidx.compose.animation:animation:${Version.jetpackCompose}"
    const val composeConstraintLayout = "androidx.constraintlayout:constraintlayout-compose:${Version.composeConstraintLayout}"
    const val composeUiTooling = "androidx.compose.ui:ui-tooling:${Version.jetpackCompose}"

    //Signal
    const val signal = "org.signal:libsignal-client:${Version.signal}"

    //Hilt
    const val hilt = "com.google.dagger:hilt-android:${Version.hilt}"
    const val dagger = "com.google.dagger:dagger:${Version.hilt}"
    const val daggerAndroidProcessor = "com.google.dagger:dagger-android-processor:${Version.hilt}"
    const val daggerAndroidSupport = "com.google.dagger:dagger-android-support:${Version.hilt}"
    const val daggerCompiler = "com.google.dagger:dagger-compiler:${Version.hilt}"
    const val hiltCompiler = "com.google.dagger:hilt-compiler:${Version.hilt}"
    const val hiltCompilerAndroidx = "androidx.hilt:hilt-compiler:${Version.hiltCompilerAndroidxVersion}"
    const val dagger2InjectAnnotation = "com.squareup.inject:assisted-inject-annotations-dagger2:${Version.assistedInjectVersion}"
    const val dagger2InjectProcessor = "com.squareup.inject:assisted-inject-processor-dagger2:${Version.assistedInjectVersion}"


    //Test dependencies
    const val junit = "junit:junit:${Version.junit}"
    const val junitExt = "androidx.test.ext:junit:${Version.junitExt}"
    const val espresso = "androidx.test.espresso:espresso-core:${Version.espresso}"

    //Room components
    const val roomRuntime = "androidx.room:room-runtime:${Version.room}"
    const val roomKtx = "androidx.room:room-ktx:${Version.room}"
    const val roomCompiler = "androidx.room:room-compiler:${Version.room}"
    const val roomTesting = "androidx.room:room-testing:${Version.room}"

    const val constraintLayout = "androidx.constraintlayout:constraintlayout:${Version.constraintLayout}"
    const val constraint = "com.android.support.constraint:constraint-layout:${Version.constraint}"
    const val libPhoneNumber = "com.googlecode.libphonenumber:libphonenumber:${Version.libPhoneNumber}"
    const val circleImageView = "de.hdodenhof:circleimageview:${Version.circleImageView}"

    const val buildGradle = "com.android.tools.build:gradle:${Version.buildGradle}"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Version.kotlin}"
    const val protobufGradlePlugin = "com.google.protobuf:protobuf-gradle-plugin:0.8.13"
    const val hiltGradlePlugin = "com.google.dagger:hilt-android-gradle-plugin:${Version.hilt}"
    const val googleService = "com.google.gms:google-services:${Version.googleService}"
    //WebRtc
    const val webRtc = "org.webrtc:google-webrtc:${Version.webRtc}"

    //Okhttp3
    const val okhttp3 = "com.squareup.okhttp3:okhttp:${Version.okhttp3}"
    const val okhttp3Logging = "com.squareup.okhttp3:logging-interceptor:${Version.okhttp3Logging}"
}

object Modules {
    const val app = ":app"
    const val domain = ":domain"
    const val common = ":common"
    const val local = ":data:local"
    const val remote = ":data:remote"
    const val repository = ":data:repository"
    const val featureSplash = ":features:splash"
    const val featureCall = ":features:calls"
    const val featureShared = ":features:shared"
    const val navigation = ":navigation"
    const val janus = ":januswrapper"
    const val srp = ":srp"
    const val featureChat = ":features:chat"
    const val featureAuth = ":features:auth"
}

object Version {
    const val coil = "2.1.0"
    const val accompanist = "0.25.0"

    const val androidUploadService = "4.7.0"

    const val gson = "2.9.1"

    const val firebaseMessaging = "23.0.6"

    const val firebaseAnalytics = "21.1.0"
    const val googleAuth = "20.2.0"

    const val microsoftAuth = "4.0.0"
    const val facebookAuth = "14.1.0"
    const val javaxAnnotations = "1.3.2"

    const val okhttp = "2.7.5"
    const val okhttp3 = "3.5.0"
    const val okhttp3Logging = "3.5.0"

    const val grpc = "1.29.0"
    const val protoParser = "4.0.3"
    const val glide = "4.13.2"

    const val glideTransformations = "4.3.0"
    const val toggleImageButton = "1.2"
    const val coroutines = "1.6.4"

    const val kotlin = "1.7.0"

    const val androidCore = "1.6.0"

    const val appCompat = "1.3.1"
    const val appCompatV7 = "28.0.0"
    const val materialDesign = "1.4.0"
    const val activity = "1.1.0"
    const val lifecycleLiveData = "2.2.0"
    const val lifecycleViewModel = "2.5.1"

    const val lifecycleExtensions = "2.0.0"
    const val jetpackCompose = "1.2.0"

    const val jetpackComposeNav = "2.5.1"
    const val composeConstraintLayout = "1.0.1"
    const val signal = "0.18.1"

    const val hilt = "2.43.2"
    const val googleService = "4.3.13"
    const val hiltCompilerAndroidxVersion = "1.0.0"
    const val assistedInjectVersion = "0.8.1"
    const val circleImageView = "3.1.0"

    const val junit = "4.+"
    const val junitExt = "1.1.2"
    const val espresso = "3.4.0"

    const val room = "2.4.3"

    const val constraintLayout = "2.1.4"
    const val constraint = "1.0.2"
    const val libPhoneNumber = "8.2.0"
    const val webRtc = "1.0.20284"
    const val buildGradle = "7.0.4"
}