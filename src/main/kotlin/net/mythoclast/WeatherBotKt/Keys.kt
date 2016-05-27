package net.mythoclast.WeatherBotKt

enum class Keys(val key: String) {
    GEOCODE("YOUR GEOCODE KEY"),
    FORECAST("YOUR FORECAST.IO KEY");

    override fun toString(): String {
        return key;
    }
}