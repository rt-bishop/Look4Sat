package com.rtbishop.lookingsat.repo

import com.github.amsacode.predict4java.SatPassTime

data class SatPass(val satName: String, val catNum: Int, val pass: SatPassTime)