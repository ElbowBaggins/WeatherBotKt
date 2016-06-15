package net.mythoclast.WeatherBotKt

import com.github.salomonbrys.kotson.*
import com.google.common.io.Files
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.IOException
import java.util.logging.Logger
import kotlin.system.exitProcess

class ConfigFile private constructor() {
    companion object {
        private val obj = readFile()
        val geocodeKey: String by obj.byString
        val forecastKey: String by obj.byString

        val name: String by obj.byString
        val nickservPassword: String by obj.byString

        val login: String by obj.byString
        val realName: String by obj.byString
        val finger: String by obj.byString
        val partMessage: String by obj.byString

        val server: String by obj.byString
        val port: Int by obj.byInt

        val autoReconnectAttempts: Int by obj.byInt
        val ssl: Boolean by obj.byBool
        val adminUser: String by obj.byString
        val channels: JsonArray by obj.byArray

        private fun readFile(): JsonObject {
            try {
                return JsonParser().parse(Files.toString(File("config.json"), Charsets.UTF_8)).asJsonObject
            } catch (e: IOException) {
                Logger.getLogger("WeatherBot").severe("Could not access configuration, or configuration is malformed.")
                exitProcess(-1)
            }
        }
    }
}