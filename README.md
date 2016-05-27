# WeatherBotKt
Kotlin rewrite of WeatherBot, linking against [dvdme's ForecastIO-Lib-Java](https://github.com/dvdme/forecastio-lib-java "ForecastIO-Lib-Java") for weather data and [TheLQ's PircBotX](https://github.com/TheLQ/pircbotx "PircBotX") for IRC capabilities. Provided under the WTFPL.

## Configuration
Presently you must configure WeatherBot by actually modifying WeatherBot.kt. This will be outsourced to a JSON file in the very near future.

## Building
You should be able to build via Maven using nothing more than the pom.xml provided. Running mvn package ought to do the trick. The JAR you want is the jar-with-dependencies.

## Running
Use the jar command to run the JAR. That's it.
To stop WeatherBot, send SIGTERM to the appropriate process. (Determining this is up to you, for now. I will provide the script I use in the future.) It will shut down and part from all joined channels gracefully.
