package com.rtbishop.look4sat.interactors

import com.rtbishop.look4sat.data.SatelliteRepo

class UpdateEntriesFromWeb(private val satelliteRepo: SatelliteRepo) {

    private val defaultSources = listOf(
        "https://celestrak.com/NORAD/elements/active.txt",
        "https://amsat.org/tle/current/nasabare.txt",
        "https://www.prismnet.com/~mmccants/tles/classfd.zip",
        "https://www.prismnet.com/~mmccants/tles/inttles.zip"
    )

    suspend operator fun invoke(sources: List<String> = defaultSources) {
        satelliteRepo.updateEntriesFromWeb(sources)
    }
}
