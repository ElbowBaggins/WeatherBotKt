package net.mythoclast.WeatherBotKt

import org.jsoup.Jsoup
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
        val result: String
        try {
            result = Jsoup.connect(URL)?.get()?.title() ?: "Could not reach the given address."
        } catch (e: Exception) {
            result = "Could not reach the given address."
        }

        return result
    }
}