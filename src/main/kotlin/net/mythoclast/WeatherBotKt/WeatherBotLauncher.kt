package net.mythoclast.WeatherBotKt

import kotlin.concurrent.thread

fun main(args: Array<String>) {
   thread(start = true, isDaemon = false, block = {
       WeatherBotIRC.start()
   })
}