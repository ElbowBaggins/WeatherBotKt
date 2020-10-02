package net.mythoclast.weatherbotkt

import com.github.dvdme.ForecastIOLib.FIOCurrently
import com.github.dvdme.ForecastIOLib.FIODaily
import com.github.dvdme.ForecastIOLib.FIOFlags
import com.github.dvdme.ForecastIOLib.ForecastIO
import net.mythoclast.weatherbotkt.ConfigFile.Core.FORECAST_KEY
import kotlin.math.abs

internal object WeatherBotData {

  private const val ERROR_MSG = "An error occurred while attempting to contact Dark Sky."
  private val DIR_ARRAY = arrayOf(
      "due North", "° NNE", "° NE", "° ENE", "due East", "° ESE", "° SE", "° SSE",
      "due South", "° SSW", "° SW", "° WSW", "due West", "° WNW", "° NW", "° NNW"
  )

  private fun convertBearing(bearing: Double): String {
    var bearingDirection = getBearingDirection(bearing)

    if (bearingDirection.startsWith("due")) {
      if (1 > bearing % 90.0) {
        return bearingDirection
      }
      bearingDirection = getBearingDirection(bearing + 22.5)
    }
    return "${getAdjustedBearing(bearing)}$bearingDirection"
  }

  private fun getBearingDirection(bearing: Double): String {
    return DIR_ARRAY[(((bearing / 22.5) + 0.5) % 16).toInt()]
  }

  private fun getAdjustedBearing(bearing: Double): Int {
    return abs(bearing - (((bearing / 45) % 9).toInt() * 45)).toInt()
  }

  private fun getFormattedSummary(
      units: String,
      location: String,
      complete: Boolean,
      multiline: Boolean = false
  ): String {

    val locationInfo =
        Locator.getLatLongAndAddressForName(location) ?: return "Could not get location data for \"$location\""

    val requestData = ForecastIO(
        locationInfo.left.toString(),
        locationInfo.middle.toString(),
        units,
        ForecastIO.LANG_ENGLISH,
        FORECAST_KEY
    )

    val unitType = FIOFlags(requestData).units() ?: return ERROR_MSG

    var degreeUnit = "°F"
    var windUnit = "mph"
    var distanceUnit = "mi."
    var pressureUnit = "mb"
    var precipIntensityUnit = "in./hr"
    var precipAccumUnit = "in."

    val divider = if (multiline) "\n" else " || "

    if (ForecastIO.UNITS_US != unitType) {
      degreeUnit = "°C"

      // Kilometres per hour are used in Canada, SI territories use metres per second.
      // The UK still uses miles per hour for some reason, so don't change it for them.
      if (ForecastIO.UNITS_CA == unitType) {
        windUnit = "km/h"
      } else if (ForecastIO.UNITS_UK != unitType) {
        windUnit = "m/s"
      }

      // Everywhere that isn't the US uses SI units for this, apparently.
      distanceUnit = "km"
      pressureUnit = "hPa"
      precipIntensityUnit = "mm/hr"
      precipAccumUnit = "cm"
    }

    // Get the actual data point, return the error message if something goes wrong.
    val currently = FIOCurrently(requestData).get() ?: return ERROR_MSG
    val daily = FIODaily(requestData).getDay(0) ?: currently

    // Build response String
    val dataBuilder = StringBuilder()
    dataBuilder.append("Weather data for ${locationInfo.right}${divider}Current Conditions: ")
        .append(currently.summary() ?: "Unknown")

    if (complete) {
      dataBuilder.append("${divider}Today's Forecast: ${daily.summary() ?: "Unknown"}")
    }

    // We're going to have to make sure *nothing* is null if we want this to look even remotely nice.
    // Don't show the temperature block at all if it's not available.
    currently.temperature()?.let { currentRealTemp ->
      dataBuilder.append("${divider}Current Temperature: $currentRealTemp$degreeUnit")

      // Don't show a null "Feels like" block.
      currently.apparentTemperature()?.let { currentFeelsLikeTemp ->
        dataBuilder.append(" (Feels like $currentFeelsLikeTemp$degreeUnit)")
      }
    }

    daily.temperatureMax()?.let { realMax ->
      dataBuilder.append("${divider}Hi: $realMax$degreeUnit${
        daily.temperatureMin()?.let { "/Lo: $it$degreeUnit" } ?: ""
      }")

      daily.apparentTemperatureMax()?.let { feelsLikeMax ->
        dataBuilder.append(" ($feelsLikeMax$degreeUnit${
          daily.temperatureMin()?.let { "/$it$degreeUnit" } ?: ""
        })")
      }
    }


    // Figure it out.
    currently.dewPoint()?.let {
      dataBuilder.append("${divider}Dew Point: $it$degreeUnit")
    }

    // I mean really.
    currently.humidity()?.let {
      dataBuilder.append("${divider}Humidity: ${String.format("%.0f", it * 100)}%")
    }

    currently.windSpeed()?.let { windSpeed ->
      dataBuilder.append(divider)
      when (windSpeed) {
        0.0 -> dataBuilder.append("Wind: Still")
        else -> {
          // Only show numeric wind speed if it is windy
          dataBuilder.append("Wind Speed: $windSpeed $windUnit")
        }
      }

      currently.windBearing()?.let {
        dataBuilder.append(", ${convertBearing(it)}")
      }
    }

    // Only show fields in here if we asked for everything available
    if (complete) {

      // This does exactly what you think.
      if (null != currently.visibility()) {
        dataBuilder.append("${divider}Visibility: ${currently.visibility()} $distanceUnit")
      }

      // So does this
      if (null != currently.pressure()) {
        dataBuilder.append("${divider}Pressure: ${currently.pressure()} $pressureUnit")
      }

      // This only shows precipitation intensity if something very skywatery is happening.
      if (null != currently.precipIntensity() && currently.precipIntensity() > 0) {
        dataBuilder.append("${divider}Precipitation Intensity: ${currently.precipIntensity()}$precipIntensityUnit")
      }

      // This only shows skypowder accumulation if
      if (currently.precipAccumulation() > 0) {
        dataBuilder.append("${divider}Precipitation Accumulation: ${currently.precipAccumulation()} $precipAccumUnit")
      }

      if (null != currently.cloudCover()) {
        dataBuilder.append("${divider}Cloud Cover: ${String.format("%.0f", currently.cloudCover() * 100.0)}%")
      }
    }

    // Always show storm distance info last
    if (null != currently.nearestStormDistance() && null != currently.nearestStormBearing()) {
      dataBuilder.append("${divider}The nearest storm is ")
      if (currently.nearestStormDistance() >= 0) {
        dataBuilder.append(
            "${
              String.format(
                  "%.0f",
                  currently.nearestStormDistance()
              )
            }$distanceUnit away, "
        )
      }
      dataBuilder.append("${convertBearing(currently.nearestStormBearing())}.")
    }

    return dataBuilder.toString()
  }

  fun autoSummary(location: String, multiline: Boolean = false): String {
    return getFormattedSummary(ForecastIO.UNITS_AUTO, location, false, multiline)
  }

  fun fahrenheitSummary(location: String, multiline: Boolean = false): String {
    return getFormattedSummary(ForecastIO.UNITS_US, location, false, multiline)
  }

  fun canadaSummary(location: String, multiline: Boolean = false): String {
    return getFormattedSummary(ForecastIO.UNITS_CA, location, false, multiline)
  }

  fun ukSummary(location: String, multiline: Boolean = false): String {
    return getFormattedSummary(ForecastIO.UNITS_UK, location, false, multiline)
  }

  fun intlSummary(location: String, multiline: Boolean = false): String {
    return getFormattedSummary(ForecastIO.UNITS_SI, location, false, multiline)
  }

  fun longAutoSummary(location: String, multiline: Boolean = false): String {
    return getFormattedSummary(ForecastIO.UNITS_AUTO, location, true, multiline)
  }

  fun longFahrenheitSummary(location: String, multiline: Boolean = false): String {
    return getFormattedSummary(ForecastIO.UNITS_US, location, true, multiline)
  }

  fun longCanadaSummary(location: String, multiline: Boolean = false): String {
    return getFormattedSummary(ForecastIO.UNITS_CA, location, true, multiline)
  }

  fun longUKSummary(location: String, multiline: Boolean = false): String {
    return getFormattedSummary(ForecastIO.UNITS_UK, location, true, multiline)
  }

  fun longIntlSummary(location: String, multiline: Boolean = false): String {
    return getFormattedSummary(ForecastIO.UNITS_SI, location, true, multiline)
  }
}
