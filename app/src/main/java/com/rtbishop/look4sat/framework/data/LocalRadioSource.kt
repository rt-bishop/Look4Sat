/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.framework.data

import com.rtbishop.look4sat.domain.model.SatRadio as DomainRadio
import com.rtbishop.look4sat.framework.model.SatRadio as FrameworkRadio
import com.rtbishop.look4sat.domain.data.ILocalRadioSource
import com.rtbishop.look4sat.framework.data.dao.RadiosDao

class LocalRadioSource(private val radiosDao: RadiosDao) : ILocalRadioSource {

    override fun getRadiosTotal() = radiosDao.getRadiosTotal()

    override suspend fun getRadiosWithId(id: Int): List<DomainRadio> {
        return radiosDao.getRadiosWithId(id).toDomainRadios()
    }

    override suspend fun insertRadios(radios: List<DomainRadio>) {
        radiosDao.deleteRadios()
        radiosDao.insertRadios(radios.toFrameworkRadios())
    }

    override suspend fun deleteRadios() = radiosDao.deleteRadios()

    private fun DomainRadio.toFramework() = FrameworkRadio(
        this.uuid, this.info, this.isAlive, this.downlink, this.uplink,
        this.mode, this.isInverted, this.catnum, this.comment
    )

    private fun FrameworkRadio.toDomain() = DomainRadio(
        this.uuid, this.info, this.isAlive, this.downlink, this.uplink,
        this.mode, this.isInverted, this.catnum, this.comment
    )

    private fun List<DomainRadio>.toFrameworkRadios() = this.map { radio -> radio.toFramework() }

    private fun List<FrameworkRadio>.toDomainRadios() = this.map { radio -> radio.toDomain() }
}
