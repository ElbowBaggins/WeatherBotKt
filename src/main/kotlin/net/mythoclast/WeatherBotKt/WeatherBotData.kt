package net.mythoclast.WeatherBotKt

import com.github.dvdme.ForecastIOLib.*

object WeatherBotData {

    private val ERROR_MSG = "An error occurred while attempting to contact Dark Sky."

    private fun convertBearing(bearing: Double) : String {
        val direction = getBearingDirection(bearing)
        if(!direction.startsWith("due")) {
            return getBearingDegrees(bearing).toString() + direction;
        } else {
            return direction;
        }
    }

    private fun getBearingDirection(bearing: Double) : String {
        val adjustedBearing = ((bearing / 22.5) + 0.5)
        val dirArray = arrayOf("due North", "° NNE", "° NE", "° ENE",
                "due East", "° ESE", "° SE", "° SSE",
                "due South", "° SSW", "° SW", "° WSW",
                "due West", "° WNW", "° NW", "° NNW")
        return dirArray[adjustedBearing.toInt() % 16]
    }

    private fun getBearingDegrees(bearing: Double) : Double {
        val adjustedBearing = ((bearing / 22.5) + 0.5)
        val bearingArray = doubleArrayOf(0.0, 22.5, 45.0, 67.5,
                90.0, 112.5, 135.0, 157.5,
                180.0, 202.5, 225.0, 247.5,
                270.0, 292.5, 315.0, 337.5)
        return Math.abs(bearingArray[Math.max(Math.round(adjustedBearing % 16).toInt() - 1, 0)] - bearing)
    }

    private fun getFormattedSummary(units: String, location: String, complete: Boolean): String {
        val locationInfo = Locator.getLatLongAndAddressForName(location) ?:
                return "Could not get location data for \"$location\""

        val requestData = ForecastIO(locationInfo.left.toString(),
                locationInfo.middle.toString(),
                units,
                ForecastIO.LANG_ENGLISH,
                ConfigFile.forecastKey)

        val unitType = FIOFlags(requestData).units() ?: return ERROR_MSG

        var degreeUnit = "°F"
        var windUnit = "mph"
        var distanceUnit = "mi."
        var pressureUnit = "mb"
        var precipIntensityUnit = "in./hr"
        var precipAccumUnit = "in."

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
        dataBuilder.append("Weather data for ${locationInfo.right} || Current Conditions: ")
                   .append(currently.summary() ?: "Unknown")

        if(complete) {
            dataBuilder.append(" || Today's Forecast: ${daily.summary() ?: "Unknown"}")
        }

        // We're going to have to make sure *nothing* is null if we want this to look even remotely nice.
        // Don't show the temperature block at all if it's not available.
        if (null != currently.temperature()) {
            dataBuilder.append(" || Current Temperature: ${currently.apparentTemperature()}$degreeUnit")

            // Don't show a null "Feels like" block.
            if (null != currently.apparentTemperature() && currently.apparentTemperature() != currently.temperature()) {
                dataBuilder.append(" (Feels like ${currently.apparentTemperature()}$degreeUnit)")
            }
        }

        // Don't show the hi/lo forecast if unavailable.
        if (null != daily.temperatureMax() && null != daily.temperatureMin()) {
            dataBuilder.append(" || Hi/Lo: ${daily.temperatureMax()}/${daily.temperatureMin()}$degreeUnit")

            // Don't show the hi/lo "Feels like" if unavailable.
            if (null != daily.apparentTemperatureMax() && null != daily.apparentTemperatureMin()) {
                dataBuilder.append(" (${daily.apparentTemperatureMax()}/${daily.apparentTemperatureMin()})")
            }
        }

        // Figure it out.
        if (null != currently.dewPoint()) {
            dataBuilder.append(" || Dew Point: ${currently.dewPoint()}$degreeUnit")
        }

        // I mean really.
        if (null != currently.humidity()) {
            dataBuilder.append(" || Humidity: ${String.format("%.0f", currently.humidity() * 100)}%")
        }

        // Duh.
        if (null != currently.windSpeed()) {

            if (0.0 == currently.windSpeed()) {
                dataBuilder.append(" || Wind: Still")
            } else {

                // Only show numeric wind speed if it is windy
                dataBuilder.append(" || Wind Speed: ${currently.windSpeed()} $windUnit")

                // Only show the wind bearing if we got it in our response
                if (null != currently.windBearing()) {
                    dataBuilder.append(", ${convertBearing(currently.windBearing())}")
                }
            }
        }

        // Only show fields in here if we asked for everything available
        if (complete) {

            // This does exactly what you think.
            if (null != currently.visibility()) {
                dataBuilder.append(" || Visibility: ${currently.visibility()} $distanceUnit")
            }

            // So does this
            if (null != currently.pressure()) {
                dataBuilder.append(" || Pressure: ${currently.pressure()} $pressureUnit")
            }

            // This only shows precipitation intensity if something very skywatery is happenng.
            if (null != currently.precipIntensity() && null != currently.summary() &&
                    (currently.summary().contains("rain") || currently.summary().contains("storm") ||
                            currently.summary().contains("snow") || currently.summary().contains("Rain") ||
                            currently.summary().contains("Snow") || currently.summary().contains("Storm"))) {

                dataBuilder.append(" || Precipitation Intensity: ${currently.precipIntensity()}$precipIntensityUnit")
            }

            // This only shows skypowder accumulation if
            if (null != currently.precipAccumulation() && null != currently.summary() &&
                    (currently.summary().contains("snow") || currently.summary().contains("Snow"))) {

                dataBuilder.append(" || Precipitation Accumulation: ${currently.precipAccumulation()} $precipAccumUnit")
            }

            if (null != currently.cloudCover()) {
                dataBuilder.append(" || Cloud Cover: ${String.format("%.0f", currently.cloudCover() * 100.0)}%")
            }

            if(null != currently.nearestStormDistance() && null != currently.nearestStormBearing()) {
                dataBuilder.append(" || The nearest storm is ")
                           .append("${String.format("%.0f", currently.nearestStormDistance())}$distanceUnit away, ")
                           .append("${convertBearing(currently.nearestStormBearing())}.")
            }
        }

        return dataBuilder.toString()
    }

    fun autoSummary(location: String): String {
        return getFormattedSummary(ForecastIO.UNITS_AUTO, location, false)
    }

    fun fahrenheitSummary(location: String): String {
        return getFormattedSummary(ForecastIO.UNITS_US, location, false)
    }

    fun canadaSummary(location: String): String {
        return getFormattedSummary(ForecastIO.UNITS_CA, location, false)
    }

    fun ukSummary(location: String): String {
        return getFormattedSummary(ForecastIO.UNITS_UK, location, false)
    }

    fun intlSummary(location: String): String {
        return getFormattedSummary(ForecastIO.UNITS_SI, location, false)
    }

    fun longAutoSummary(location: String): String {
        return getFormattedSummary(ForecastIO.UNITS_AUTO, location, true)
    }

    fun longFahrenheitSummary(location: String): String {
        return getFormattedSummary(ForecastIO.UNITS_US, location, true)
    }

    fun longCanadaSummary(location: String): String {
        return getFormattedSummary(ForecastIO.UNITS_CA, location, true)
    }

    fun longUKSummary(location: String): String {
        return getFormattedSummary(ForecastIO.UNITS_UK, location, true)
    }

    fun longIntlSummary(location: String): String {
        return getFormattedSummary(ForecastIO.UNITS_SI, location, true)
    }
}
