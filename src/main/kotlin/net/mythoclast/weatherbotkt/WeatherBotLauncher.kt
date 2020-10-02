package net.mythoclast.weatherbotkt

import kotlin.concurrent.thread

fun main(args: Array<String>) {

  if (args.size != 1) {
    println(
        "WeatherBot requires exactly one of the following arguments: " +
            "-IRC (for IRC mode), -TS3 (for TeamSpeak 3 mode), or -BOTH (for both)."
    )
  }

  val upperCaseArg = args[0].toUpperCase()

  if (upperCaseArg == "-BOTH" || upperCaseArg == "-IRC") {
    thread(start = true, isDaemon = false, block = { WeatherBotIRC.start() })
  }

  if (upperCaseArg == "-BOTH" || upperCaseArg == "-TS3") {
    thread(start = true, isDaemon = false, block = { WeatherBotTS3 })
  }

  if (upperCaseArg != "-BOTH" && upperCaseArg != "-IRC" && upperCaseArg != "-TS3") {
    println("'$upperCaseArg' is not a valid argument.")
  }
}
