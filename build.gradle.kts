import org.gradle.api.file.DuplicatesStrategy.EXCLUDE

val mainClassName = "net.mythoclast.weatherbotkt.WeatherBotLauncherKt"

group = "net.mythoclast"
version = "1.3.1"
description = "WeatherBot for Kotlin"

plugins {
  application
  kotlin("jvm") version "1.4.10"
}

repositories {
  jcenter()
  maven {
    url = uri("https://raw.githubusercontent.com/TheHolyWaffle/TeamSpeak-3-Java-API/mvn-repo/")
  }
}

application {
  mainClass.set(mainClassName)
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.register<Jar>("uberJar") {
  manifest {
    attributes["Main-Class"] = mainClassName
  }
  duplicatesStrategy = EXCLUDE
  archiveClassifier.set("uber")

  from(sourceSets.main.get().output)

  dependsOn(configurations.runtimeClasspath)
  from(
    {
       configurations
         .runtimeClasspath
         .get()
         .filter { it.name.endsWith("jar") }
         .map { zipTree(it) }
    }
  )
}

dependencies {
  implementation("com.google.maps:google-maps-services:0.15.0")
  implementation("org.pircbotx:pircbotx:2.1")
  implementation("com.github.dvdme:ForecastIOLib:1.6.0")
  implementation("org.slf4j:slf4j-jdk14:1.7.30")
  implementation("com.github.salomonbrys.kotson:kotson:2.5.0")
  implementation("com.github.theholywaffle:teamspeak3-api:1.2.0")
  implementation("org.jsoup:jsoup:1.13.1")
}
