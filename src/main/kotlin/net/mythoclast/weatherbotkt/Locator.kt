package net.mythoclast.weatherbotkt

import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import net.mythoclast.weatherbotkt.ConfigFile.Core.GEOCODE_KEY
import org.apache.commons.lang3.tuple.Triple

internal object Locator {
  private val CONTEXT = GeoApiContext.Builder().apiKey(GEOCODE_KEY).build()

  fun getLatLongAndAddressForName(locationName: String): Triple<Double, Double, String>? {
    val results = GeocodingApi.geocode(CONTEXT, locationName).awaitIgnoreError()
    if (null == results || results.isEmpty()) {
      return null
    }
    return results[0].let {
      Triple.of(it.geometry.location.lat, it.geometry.location.lng, it.formattedAddress)
    }
  }

}
