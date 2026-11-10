import java.util.Properties

plugins {
    id("com.android.application")
}

// API 키는 저장소에 올리지 않는 secrets.properties에서 읽습니다(secrets.example.properties 참고).
val secrets = Properties().apply {
    rootProject.file("secrets.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

// -PnoSecrets 를 붙이면 키를 넣지 않습니다(차량 기본 음성과 기본 이야기로 동작).
val withSecrets = !project.hasProperty("noSecrets")

fun secret(name: String): String =
    "\"" + (if (withSecrets) secrets.getProperty(name, "") else "").trim().replace("\\", "").replace("\"", "") + "\""

// release 서명: 저장소에 올리지 않는 keystore.properties(storeFile, storePassword, keyAlias, keyPassword)가 있으면 그 키로,
// 없으면 안드로이드 디버그 키로 서명합니다. 그래서 내려받은 그대로도 assembleRelease가 됩니다.
val keystore = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.ajikmeoreosseo.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ajikmeoreosseo.app"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.2.0"

        buildConfigField("String", "ELEVENLABS_API_KEY", secret("ELEVENLABS_API_KEY"))
        buildConfigField("String", "ELEVENLABS_VOICE_ID", secret("ELEVENLABS_VOICE_ID"))
        buildConfigField("String", "ELEVENLABS_MODEL", secret("ELEVENLABS_MODEL"))
        buildConfigField("String", "OPENAI_API_KEY", secret("OPENAI_API_KEY"))
        buildConfigField("String", "OPENAI_MODEL", secret("OPENAI_MODEL"))
    }

    signingConfigs {
        if (keystore.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // 시험용 도구(DEBUG_* 방송, 시험용 음성 넣기)와 경로 요청 권한은 release에 들어가지 않습니다.
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
}
