package net.mythoclast.WeatherBotKt

import net.mythoclast.WeatherBotKt.ConfigFile.Core.GEOCODE_KEY

import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import org.apache.commons.lang3.tuple.Triple

internal object Locator {

    fun getLatLongAndAddressForName(locationName: String): Triple<Double, Double, String>? {
        val results = GeocodingApi.geocode(GeoApiContext().setApiKey(GEOCODE_KEY),
                locationName).awaitIgnoreError()
        if (null == results || 0 == results.size) {
            return null
        }
        return Triple.of(results[0].geometry.location.lat, results[0].geometry.location.lng,
                results[0].formattedAddress)
    }

}
