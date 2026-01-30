/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.domain.predict

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class NearEarthObject(data: OrbitalData) : OrbitalObject(data) {

    private val aodp: Double
    private val aycof: Double
    private val c1: Double
    private val c4: Double
    private val c5: Double
    private val cosio: Double
    private var d2 = 0.0
    private var d3 = 0.0
    private var d4 = 0.0
    private val delmo: Double
    private val omgcof: Double
    private val eta: Double
    private val omgdot: Double
    private val sinio: Double
    private val xnodp: Double
    private val sinmo: Double
    private val t2cof: Double
    private var t3cof = 0.0
    private var t4cof = 0.0
    private var t5cof = 0.0
    private val x1mth2: Double
    private val x3thm1: Double
    private val x7thm1: Double
    private val xmcof: Double
    private val xmdot: Double
    private val xnodcf: Double
    private val xnodot: Double
    private val xlcof: Double
    private val sgp4Simple: Boolean

    init {
        // Recover original mean motion (xnodp) and semimajor axis (aodp) from input elements
        val a1 = (XKE / data.xno).pow(TWO_THIRDS)
        cosio = cos(data.xincl)
        val theta2 = sqr(cosio)
        x3thm1 = 3.0 * theta2 - 1.0
        val eo = data.eccn
        val eosq = sqr(eo)
        val betao2 = 1.0 - eosq
        val betao = sqrt(betao2)
        val del1 = 1.5 * CK2 * x3thm1 / (sqr(a1) * betao * betao2)
        val ao = a1 * (1.0 - del1 * (0.5 * TWO_THIRDS + del1 * (1.0 + 134.0 / 81.0 * del1)))
        val delo = 1.5 * CK2 * x3thm1 / (sqr(ao) * betao * betao2)
        xnodp = data.xno / (1.0 + delo)
        aodp = ao / (1.0 - delo)

        // For perigee less than 220 kilometers, the "simple" flag is set
        sgp4Simple = aodp * (1.0 - eo) < 220 / EARTH_RADIUS + 1.0

        // For perigees below 156 km, the values of S and QOMS2T are altered
        setPerigee((aodp * (1.0 - eo) - 1.0) * EARTH_RADIUS)
        val pinvsq = invert(sqr(aodp) * sqr(betao2))
        val tsi = invert(aodp - s4)
        eta = aodp * eo * tsi
        val etasq = eta * eta
        val eeta = eo * eta
        val psisq = abs(1.0 - etasq)
        val coef = qoms24 * tsi.pow(4.0)
        val coef1 = coef / psisq.pow(3.5)
        val bstar = data.bstar
        val c2 = coef1 * xnodp * (aodp * (1.0 + 1.5 * etasq + eeta * (4.0 + etasq)) + 0.75
            * CK2 * tsi / psisq * x3thm1 * (8.0 + 3.0 * etasq * (8.0 + etasq)))
        c1 = bstar * c2
        sinio = sin(data.xincl)
        val a3ovk2 = -J3_HARMONIC / CK2
        val c3 = coef * tsi * a3ovk2 * xnodp * sinio / eo
        x1mth2 = 1.0 - theta2
        val omegao = data.omegao
        c4 = 2 * xnodp * coef1 * aodp * betao2 * (eta * (2.0 + 0.5 * etasq) + eo * (0.5 + 2 * etasq)
            - 2 * CK2 * tsi / (aodp * psisq) * (-3 * x3thm1 * (1.0 - 2 * eeta + etasq
            * (1.5 - 0.5 * eeta)) + 0.75 * x1mth2 * (2.0 * etasq - eeta * (1.0 + etasq))
            * cos(2.0 * omegao)))
        c5 = 2.0 * coef1 * aodp * betao2 * (1.0 + 2.75 * (etasq + eeta) + eeta * etasq)
        val theta4 = sqr(theta2)
        val temp1 = 3.0 * CK2 * pinvsq * xnodp
        val temp2 = temp1 * CK2 * pinvsq
        val temp3 = 1.25 * CK4 * pinvsq * pinvsq * xnodp
        xmdot =
            xnodp + 0.5 * temp1 * betao * x3thm1 + (0.0625 * temp2 * betao * (13.0 - 78.0 * theta2 + 137.0 * theta4))
        val x1m5th = 1.0 - 5.0 * theta2
        omgdot =
            -0.5 * temp1 * x1m5th + 0.0625 * temp2 * (7.0 - 114.0 * theta2 + 395.0 * theta4) + temp3 * (3.0 - 36.0 * theta2 + 49.0 * theta4)
        val xhdot1 = -temp1 * cosio
        xnodot =
            xhdot1 + (0.5 * temp2 * (4.0 - 19.0 * theta2) + 2.0 * temp3 * (3.0 - 7.0 * theta2)) * cosio
        omgcof = bstar * c3 * cos(omegao)
        xmcof = -TWO_THIRDS * coef * bstar / eeta
        xnodcf = 3.5 * betao2 * xhdot1 * c1
        t2cof = 1.5 * c1
        xlcof = 0.125 * a3ovk2 * sinio * (3.0 + 5 * cosio) / (1.0 + cosio)
        aycof = 0.25 * a3ovk2 * sinio
        val xmo = data.xmo
        delmo = (1.0 + eta * cos(xmo)).pow(3.0)
        sinmo = sin(xmo)
        x7thm1 = 7.0 * theta2 - 1
        if (!sgp4Simple) {
            val c1sq = sqr(c1)
            d2 = 4.0 * aodp * tsi * c1sq
            val temp = d2 * tsi * c1 / 3.0
            d3 = (17 * aodp + s4) * temp
            d4 = 0.5 * temp * aodp * tsi * (221 * aodp + 31 * s4) * c1
            t3cof = d2 + 2 * c1sq
            t4cof = 0.25 * (3.0 * d3 + c1 * (12 * d2 + 10 * c1sq))
            t5cof = 0.2 * (3.0 * d4 + 12 * c1 * d3 + 6 * d2 * d2 + 15 * c1sq * (2.0 * d2 + c1sq))
        } else {
            d2 = 0.0
            d3 = 0.0
            d4 = 0.0
            t3cof = 0.0
            t4cof = 0.0
            t5cof = 0.0
        }
    }

    internal fun calculateSGP4(tSince: Double) {
        synchronized(this) {
            val temp = DoubleArray(9)
            val xmdf = data.xmo + xmdot * tSince
            val omgadf = data.omegao + omgdot * tSince
            val xnoddf = data.xnodeo + xnodot * tSince
            var omega = omgadf
            var xmp = xmdf
            val tsq = sqr(tSince)
            val xnode = xnoddf + xnodcf * tsq
            val bstar = data.bstar
            var tempa = 1.0 - c1 * tSince
            var tempe = bstar * c4 * tSince
            var templ = t2cof * tsq
            if (!sgp4Simple) {
                val delomg = omgcof * tSince
                val delm = xmcof * ((1.0 + eta * cos(xmdf)).pow(3.0) - delmo)
                temp[0] = delomg + delm
                xmp = xmdf + temp[0]
                omega = omgadf - temp[0]
                val tcube = tsq * tSince
                val tfour = tSince * tcube
                tempa = tempa - d2 * tsq - d3 * tcube - d4 * tfour
                tempe += bstar * c5 * (sin(xmp) - sinmo)
                templ += t3cof * tcube + tfour * (t4cof + tSince * t5cof)
            }
            val a = aodp * tempa.pow(2.0)
            val eo = data.eccn
            val e = eo - tempe
            val xl = xmp + omega + xnode + xnodp * templ
            val beta = sqrt(1.0 - e * e)
            val xn = XKE / a.pow(1.5)

            // Long period periodics
            val axn = e * cos(omega)
            temp[0] = invert(a * sqr(beta))
            val xll = temp[0] * xlcof * axn
            val aynl = temp[0] * aycof
            val xlt = xl + xll
            val ayn = e * sin(omega) + aynl

            // Solve Kepler's equation
            val capu = mod2PI(xlt - xnode)
            temp[2] = capu
            converge(temp, axn, ayn, capu)
            calculatePosAndVel(temp, xnode, a, xn, axn, ayn)
            calculatePhase(xlt, xnode, omgadf)
        }
    }

    private fun calculatePosAndVel(
        temp: DoubleArray, xnode: Double, a: Double,
        xn: Double, axn: Double, ayn: Double
    ) {
        val ecose = temp[5] + temp[6]
        val esine = temp[3] - temp[4]
        val elsq = sqr(axn) + sqr(ayn)
        temp[0] = 1.0 - elsq
        val pl = a * temp[0]
        val r = a * (1.0 - ecose)
        temp[1] = invert(r)
        val rdot = XKE * sqrt(a) * esine * temp[1]
        val rfdot = XKE * sqrt(pl) * temp[1]
        temp[2] = a * temp[1]
        val betal = sqrt(temp[0])
        temp[3] = invert(1.0 + betal)
        val cosu = temp[2] * (temp[8] - axn + ayn * esine * temp[3])
        val sinu = temp[2] * (temp[7] - ayn - axn * esine * temp[3])
        val u = atan2(sinu, cosu)
        val sin2u = 2.0 * sinu * cosu
        val cos2u = 2.0 * cosu * cosu - 1
        temp[0] = invert(pl)
        temp[1] = CK2 * temp[0]
        temp[2] = temp[1] * temp[0]
        // Update for short periodics
        val rk = r * (1.0 - 1.5 * temp[2] * betal * x3thm1) + 0.5 * temp[1] * x1mth2 * cos2u
        val uk = u - 0.25 * temp[2] * x7thm1 * sin2u
        val xnodek = xnode + 1.5 * temp[2] * cosio * sin2u
        val xinck = data.xincl + 1.5 * temp[2] * cosio * sinio * cos2u
        val rdotk = rdot - xn * temp[1] * x1mth2 * sin2u
        val rfdotk = rfdot + xn * temp[1] * (x1mth2 * cos2u + 1.5 * x3thm1)
        super.calculatePosAndVel(rk, uk, xnodek, xinck, rdotk, rfdotk)
    }
}
