plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "cn.wgc.keyboard.library"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("io.github.wgc0303", "keyboard", "0.0.1")

    pom {
        name.set("Keyboard")
        description.set("A custom Android keyboard library.")
        inceptionYear.set("2026")
        url.set("https://github.com/wgc0303/Keyboard")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("wgc0303")
                name.set("wgc0303")
                url.set("https://github.com/wgc0303")
            }
        }

        scm {
            url.set("https://github.com/wgc0303/Keyboard")
            connection.set("scm:git:git://github.com/wgc0303/Keyboard.git")
            developerConnection.set("scm:git:ssh://git@github.com/wgc0303/Keyboard.git")
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
