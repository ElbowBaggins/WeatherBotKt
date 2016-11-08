package net.mythoclast.WeatherBotKt

import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.UnsupportedMimeTypeException
import java.io.IOException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.util.*
import java.util.regex.Pattern

object URLTitler {

    private val URLPattern = Pattern.compile("((https?):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)")

    fun getURLSFromMessage(message: String) : List<String> {
        val URLList = ArrayList<String>()
        val URLMatcher = URLPattern.matcher(message)

        while(URLMatcher.find()) {
            URLList.add(message.substring(URLMatcher.start(0), URLMatcher.end(0)))
        }

        return URLList
    }

    fun getListOfTitlesForMessage(message: String) : String? {
        val URLList = getURLSFromMessage(message)
        return if(URLList.size < 1) {
            null
        } else {
            val titleListBuilder = StringBuilder()
            for(i in URLList.indices) {
                titleListBuilder.append("${getTitleForURL(URLList[i]).let{
                    "${if(URLList.size > 1) { "(${i + 1})"} else { "" }} $it\n"
                }}")
            }
            titleListBuilder.toString()
        }
    }

    fun getTitleForURL(URL: String) : String {
        var result: String
        try {
            result = Jsoup.connect(URL)
                         ?.userAgent("Mozilla/5.0 (Windows NT 6.1) " +
                                             "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
                         ?.referrer("http://www.google.com")
                         ?.timeout(12000)
                         ?.validateTLSCertificates(false)
                         ?.get()
                         ?.title() ?:
                    "An internal error occurred while attempting to construct your request."
        } catch (e: MalformedURLException) {
            result = "The specified URL, '$URL', is not a HTTP or HTTPS URL, or is otherwise malformed."
        } catch (e: HttpStatusException) {
            result = "The server returned HTTP error code ${e.statusCode}."
        } catch (e: UnsupportedMimeTypeException) {
            result = "The specified URL refers to a file of type, (${e.mimeType})."
        } catch (e: SocketTimeoutException) {
            result = "The connection to the server timed out."
        } catch (e: IOException) {
            result = "An unknown I/O error occurred while processing your request."
        }

        if(result == "") {
            result = "The page at the address specified did not contain a top-level title tag, " +
                    "and that means that the title is hidden away in some kind of fucked up iframe bullshit " +
                    "because that makes it WEB SCALE and breaks compatibility with basic human decency."
        }

        return result
    }
}