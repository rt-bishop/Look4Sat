package com.rtbishop.look4sat.utility

import com.rtbishop.look4sat.framework.model.SatEntry
import com.rtbishop.look4sat.framework.model.SatItem
import com.rtbishop.look4sat.framework.model.SatTrans
import com.rtbishop.look4sat.domain.model.SatEntry as DomainEntry
import com.rtbishop.look4sat.domain.model.SatItem as DomainItem
import com.rtbishop.look4sat.domain.model.SatTrans as DomainTrans

object DataMapper {

    // Framework to Domain

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

    fun satTransToDomainTrans(transmitter: SatTrans): DomainTrans {
        return DomainTrans(
            transmitter.uuid, transmitter.info, transmitter.isAlive, transmitter.downlink,
            transmitter.uplink, transmitter.mode, transmitter.isInverted, transmitter.catNum
        )
    }

    fun satTransListToDomainTransList(transmitters: List<SatTrans>): List<DomainTrans> {
        return transmitters.map { transmitter -> satTransToDomainTrans(transmitter) }
    }

    // Domain to Framework

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

    fun domainTransToSatTrans(transmitter: DomainTrans): SatTrans {
        return SatTrans(
            transmitter.uuid, transmitter.info, transmitter.isAlive, transmitter.downlink,
            transmitter.uplink, transmitter.mode, transmitter.isInverted, transmitter.catNum
        )
    }

    fun domainTransListToSatTransList(transmitters: List<DomainTrans>): List<SatTrans> {
        return transmitters.map { transmitter -> domainTransToSatTrans(transmitter) }
    }
}
