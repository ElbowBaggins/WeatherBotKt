package net.mythoclast.weatherbotkt

import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.UnsupportedMimeTypeException
import java.io.IOException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import java.util.regex.Pattern
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object URLTitler {

  private val URLPattern = Pattern.compile("((https?):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?+-=\\\\.&]*)")

  private fun getURLSFromMessage(message: String): List<String> {
    val urlList = ArrayList<String>()
    val urlMatcher = URLPattern.matcher(message)

    while (urlMatcher.find()) {
      urlList.add(message.substring(urlMatcher.start(0), urlMatcher.end(0)))
    }

    return urlList
  }

  fun getListOfTitlesForMessage(message: String): String? {
    val urlList = getURLSFromMessage(message)
    return if (urlList.isEmpty()) {
      null
    } else {
      val titleListBuilder = StringBuilder()
      for (i in urlList.indices) {
        titleListBuilder.append(getTitleForURL(urlList[i]).let {
            "${
                if (urlList.size > 1) {
                    "(${i + 1})"
                } else {
                    ""
                }
            } $it\n"
        })
      }
      titleListBuilder.toString()
    }
  }

  private fun getTitleForURL(URL: String): String {
    var result: String
    try {
      result = Jsoup.connect(URL)
          ?.userAgent(
              arrayOf(
                  "Mozilla/5.0 ",
                  "(Windows NT 10.0; Win64; x64) ",
                  "AppleWebKit/537.36 (KHTML, like Gecko) ",
                  "Chrome/85.0.4183.121 Safari/537.36"
              ).joinToString("")
          )
          ?.referrer("http://www.google.com")
          ?.timeout(12000)
          ?.sslSocketFactory(SSLContext.getInstance("SSL").also {
              it.init(
                  null,
                  arrayOf(
                      object : X509TrustManager {
                          override fun checkClientTrusted(
                              chain: Array<out X509Certificate>?,
                              authType: String?
                          ) {
                          }

                          override fun checkServerTrusted(
                              chain: Array<out X509Certificate>?,
                              authType: String?
                          ) {
                          }

                          override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                      }
                  ),
                  SecureRandom()
              )
          }.socketFactory)
          ?.get()
          ?.title() ?: "An internal error occurred while attempting to construct your request."
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

    if (result == "") {
      result = "The page at the address specified did not contain a top-level title tag, " +
          "and that means that the title is hidden away in some kind of fucked up iframe bullshit " +
          "because that makes it WEB SCALE and breaks compatibility with basic human decency."
    }

    return result
  }
}
