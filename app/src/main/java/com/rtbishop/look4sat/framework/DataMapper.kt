/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.framework

import com.rtbishop.look4sat.domain.model.SatEntry as DomainEntry
import com.rtbishop.look4sat.domain.model.SatItem as DomainItem
import com.rtbishop.look4sat.domain.model.SatRadio as DomainRadio
import com.rtbishop.look4sat.framework.model.SatEntry as FrameworkEntry
import com.rtbishop.look4sat.framework.model.SatItem as FrameworkItem
import com.rtbishop.look4sat.framework.model.SatRadio as FrameworkRadio

fun DomainEntry.toFramework() = FrameworkEntry(this.data, this.comment)

fun DomainRadio.toFramework() = FrameworkRadio(
    this.uuid, this.info, this.isAlive, this.downlink, this.uplink,
    this.mode, this.isInverted, this.catnum, this.comment
)

fun FrameworkItem.toDomain() = DomainItem(this.catnum, this.name, this.modes, false)

fun FrameworkRadio.toDomain() = DomainRadio(
    this.uuid, this.info, this.isAlive, this.downlink, this.uplink,
    this.mode, this.isInverted, this.catnum, this.comment
)

fun List<DomainEntry>.toFrameworkEntries() = this.map { it.toFramework() }

fun List<DomainRadio>.toFrameworkRadios() = this.map { it.toFramework() }

fun List<FrameworkItem>.toDomainItems() = this.map { it.toDomain() }

fun List<FrameworkRadio>.toDomainRadios() = this.map { it.toDomain() }
