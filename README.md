# WeatherBotKt
Kotlin rewrite of WeatherBot, linking against [dvdme's ForecastIO-Lib-Java](https://github.com/dvdme/forecastio-lib-java "ForecastIO-Lib-Java") for weather data and [TheLQ's PircBotX](https://github.com/TheLQ/pircbotx "PircBotX") for IRC capabilities. Provided under the WTFPL.

## Configuration
Edit the provided config.json file to fit your needs. If autoReconnectAttemps is -1, it will attempt to reconnect forever (up to Int.MAX_VALUE times, anyway). Setting it to zero disables automatic reconnection. The channels field is an array. The rest ought to be self-explanatory.

The config file needs to be in the same folder as the jar.

## Building
You should be able to build via Maven using nothing more than the pom.xml provided. Running mvn package ought to do the trick. The JAR you want is the jar-with-dependencies.

## Running
     java -jar <name of jar file>

That's it.
To stop WeatherBot, send SIGTERM to the appropriate process. (Determining this is up to you, for now. I will provide the script I use in the future.) It will shut down and part from all joined channels gracefully.

## Supported Commands
### Utility Commands
* .wbhelp - Replies with a help message that outlines available commands
* .setlocation <location> - Sets the user's location so that they may invoke any of the following commands without the location parameter
### Weather Data Commands
#### All of the following take one parameter, the location one wishes to see weather data for. If the user has already set their location with .setlocation this parameter is optional.
* .w - Provides data with automatic, region-based units)
* .wf (Provides data with "American" units)
* .wc (Provides data with SI units)
* .wca (Provides data with "Canadian" units)
* .wuk (Provides data with "British" units)
#### All of the following are identical to their preceding siblings but provide more "long-form" detail
* .wlong
* .wflong
* .wclong
* .wcalong
* .wuklong
