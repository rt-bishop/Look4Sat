package com.rtbishop.look4sat.domain.source

object Sources {
    const val RADIO_DATA_URL = "https://db.satnogs.org/api/transmitters/?format=json&status=active"
    val satelliteDataUrls = mapOf(
        "All" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=active&FORMAT=csv",
        "Amsat" to "https://amsat.org/tle/current/nasabare.txt",
        "Amateur" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=amateur&FORMAT=csv",
        "Brightest" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=visual&FORMAT=csv",
        "Classified" to "https://www.mmccants.org/tles/classfd.zip",
        "Cubesat" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=cubesat&FORMAT=csv",
        "Education" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=education&FORMAT=csv",
        "Engineer" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=engineering&FORMAT=csv",
        "Geostationary" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=geo&FORMAT=csv",
        "Globalstar" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=globalstar&FORMAT=csv",
        "GNSS" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=gnss&FORMAT=csv",
        "Intelsat" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=intelsat&FORMAT=csv",
        "Iridium" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=iridium-NEXT&FORMAT=csv",
        "McCants" to "https://www.mmccants.org/tles/inttles.zip",
        "Military" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=military&FORMAT=csv",
        "New" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=last-30-days&FORMAT=csv",
        "OneWeb" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=oneweb&FORMAT=csv",
        "Orbcomm" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=orbcomm&FORMAT=csv",
        "R4UAB" to "https://r4uab.ru/satonline.txt",
        "Resource" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=resource&FORMAT=csv",
        "SatNOGS" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=satnogs&FORMAT=csv",
        "Science" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=science&FORMAT=csv",
        "Spire" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=spire&FORMAT=csv",
        "Starlink" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=starlink&FORMAT=csv",
        "Swarm" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=swarm&FORMAT=csv",
        "Weather" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=weather&FORMAT=csv",
        "X-Comm" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=x-comm&FORMAT=csv"
    )
}
