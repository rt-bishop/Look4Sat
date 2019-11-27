package com.rtbishop.lookingsat.repo

import android.os.Parcelable
import com.github.amsacode.predict4java.PassPredictor
import com.github.amsacode.predict4java.SatPassTime
import com.github.amsacode.predict4java.TLE
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class SatPass(
    val tle: TLE,
    val predictor: @RawValue PassPredictor,
    val pass: @RawValue SatPassTime
) : Parcelable