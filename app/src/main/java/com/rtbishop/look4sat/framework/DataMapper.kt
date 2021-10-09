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

import com.rtbishop.look4sat.framework.model.SatEntry
import com.rtbishop.look4sat.framework.model.SatItem
import com.rtbishop.look4sat.framework.model.Transmitter
import com.rtbishop.look4sat.domain.model.SatEntry as DomainEntry
import com.rtbishop.look4sat.domain.model.SatItem as DomainItem
import com.rtbishop.look4sat.domain.model.Transmitter as DomainTrans

object DataMapper {

    // Presentation to Domain

    fun satEntryToDomainEntry(entry: SatEntry): DomainEntry {
        return DomainEntry(entry.tle, entry.catNum, entry.name, entry.isSelected)
    }

    fun satEntriesToDomainEntries(entries: List<SatEntry>): List<DomainEntry> {
        return entries.map { entry -> satEntryToDomainEntry(entry) }
    }

    fun satItemToDomainItem(item: SatItem): DomainItem {
        return DomainItem(item.catNum, item.name, item.isSelected, item.modes)
    }

    fun satItemsToDomainItems(items: List<SatItem>): List<DomainItem> {
        return items.map { item -> satItemToDomainItem(item) }
    }

    fun satTransToDomainTrans(transmitter: Transmitter): DomainTrans {
        return DomainTrans(
            transmitter.uuid, transmitter.info, transmitter.isAlive, transmitter.downlink,
            transmitter.uplink, transmitter.mode, transmitter.isInverted, transmitter.catNum
        )
    }

    fun satTransListToDomainTransList(transmitters: List<Transmitter>): List<DomainTrans> {
        return transmitters.map { transmitter -> satTransToDomainTrans(transmitter) }
    }

    // Domain to Presentation

    fun domainEntryToSatEntry(entry: DomainEntry): SatEntry {
        return SatEntry(entry.tle, entry.catNum, entry.name, entry.isSelected)
    }

    fun domainEntriesToSatEntries(entries: List<DomainEntry>): List<SatEntry> {
        return entries.map { entry -> domainEntryToSatEntry(entry) }
    }

    fun domainItemToSatItem(item: DomainItem): SatItem {
        return SatItem(item.catNum, item.name, item.isSelected, item.modes)
    }

    fun domainItemsToSatItems(items: List<DomainItem>): List<SatItem> {
        return items.map { item -> domainItemToSatItem(item) }
    }

    fun domainTransToSatTrans(transmitter: DomainTrans): Transmitter {
        return Transmitter(
            transmitter.uuid, transmitter.info, transmitter.isAlive, transmitter.downlink,
            transmitter.uplink, transmitter.mode, transmitter.isInverted, transmitter.catNum
        )
    }

    fun domainTransListToSatTransList(transmitters: List<DomainTrans>): List<Transmitter> {
        return transmitters.map { transmitter -> domainTransToSatTrans(transmitter) }
    }
}
