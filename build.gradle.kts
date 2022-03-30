plugins {
    `java-library`
}

dependencies {
    api("junit:junit:4.13.2")
    implementation("junit:junit:4.13.2")
    testImplementation("junit:junit:4.13.2")
    implementation(files("libs/antlr-4.9.3-complete.jar"))
}

configurations {
    implementation {
        resolutionStrategy.failOnVersionConflict()
    }
}

sourceSets {
    main {
        java.srcDir("src/")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
repositories {
    mavenCentral()
}

tasks {
    test {
        testLogging.showExceptions = true
    }
}