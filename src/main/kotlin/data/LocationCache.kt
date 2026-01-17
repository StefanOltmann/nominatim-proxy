/*
 * Nominatim Proxy
 * Copyright (C) 2026 Stefan Oltmann
 * https://github.com/StefanOltmann/nominatim-proxy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package data

import model.Address
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * SQLite table that stores cached address lookups.
 */
private object LocationCacheTable : Table("locations") {

    val geohash = text("geohash")
    val language = text("language")

    val createdAt = long("created_at").nullable()

    val road = text("road").nullable()
    val pedestrian = text("pedestrian").nullable()
    val footway = text("footway").nullable()
    val cycleway = text("cycleway").nullable()
    val residential = text("residential").nullable()
    val square = text("square").nullable()
    val place = text("place").nullable()
    val neighbourhood = text("neighbourhood").nullable()
    val suburb = text("suburb").nullable()
    val cityDistrict = text("city_district").nullable()
    val hamlet = text("hamlet").nullable()
    val village = text("village").nullable()
    val town = text("town").nullable()
    val city = text("city").nullable()
    val municipality = text("municipality").nullable()
    val borough = text("borough").nullable()
    val county = text("county").nullable()
    val state = text("state").nullable()
    val province = text("province").nullable()
    val stateDistrict = text("state_district").nullable()
    val postcode = text("postcode").nullable()
    val country = text("country").nullable()
    val countryCode = text("country_code").nullable()

    /* Composite key is the cache key (geohash + language). */
    override val primaryKey = PrimaryKey(geohash, language, name = "pk_locations")
}

/**
 * Cache access layer backed by SQLite + Exposed.
 */
object LocationCache {

    /**
     * Opens the SQLite database and ensures the schema exists.
     */
    fun init(dbPath: String) {

        Database.connect("jdbc:sqlite:$dbPath", driver = "org.sqlite.JDBC")

        transaction {
            SchemaUtils.create(LocationCacheTable)
        }
    }

    /**
     * Looks up a cached address for the given cache key.
     */
    fun findAddress(
        geohash: String,
        language: String
    ): Address? =
        transaction {
            LocationCacheTable
                .selectAll()
                .where {
                    (LocationCacheTable.geohash eq geohash) and
                        (LocationCacheTable.language eq language)
                }
                .limit(1)
                .firstOrNull()
                ?.toAddress()
        }

    /**
     * Stores the address if the cache key does not exist yet.
     */
    fun saveAddress(
        geohash: String,
        language: String,
        address: Address
    ) = transaction {
        LocationCacheTable.insertIgnore {
            it[LocationCacheTable.geohash] = geohash
            it[LocationCacheTable.language] = language
            it[createdAt] = System.currentTimeMillis()
            it[road] = address.road
            it[pedestrian] = address.pedestrian
            it[footway] = address.footway
            it[cycleway] = address.cycleway
            it[residential] = address.residential
            it[square] = address.square
            it[place] = address.place
            it[neighbourhood] = address.neighbourhood
            it[suburb] = address.suburb
            it[cityDistrict] = address.cityDistrict
            it[hamlet] = address.hamlet
            it[village] = address.village
            it[town] = address.town
            it[city] = address.city
            it[municipality] = address.municipality
            it[borough] = address.borough
            it[county] = address.county
            it[state] = address.state
            it[province] = address.province
            it[stateDistrict] = address.stateDistrict
            it[postcode] = address.postcode
            it[country] = address.country
            it[countryCode] = address.countryCode
        }
    }
}

private fun ResultRow.toAddress(): Address =
    Address(
        road = this[LocationCacheTable.road],
        pedestrian = this[LocationCacheTable.pedestrian],
        footway = this[LocationCacheTable.footway],
        cycleway = this[LocationCacheTable.cycleway],
        residential = this[LocationCacheTable.residential],
        square = this[LocationCacheTable.square],
        place = this[LocationCacheTable.place],
        neighbourhood = this[LocationCacheTable.neighbourhood],
        suburb = this[LocationCacheTable.suburb],
        cityDistrict = this[LocationCacheTable.cityDistrict],
        hamlet = this[LocationCacheTable.hamlet],
        village = this[LocationCacheTable.village],
        town = this[LocationCacheTable.town],
        city = this[LocationCacheTable.city],
        municipality = this[LocationCacheTable.municipality],
        borough = this[LocationCacheTable.borough],
        county = this[LocationCacheTable.county],
        state = this[LocationCacheTable.state],
        province = this[LocationCacheTable.province],
        stateDistrict = this[LocationCacheTable.stateDistrict],
        postcode = this[LocationCacheTable.postcode],
        country = this[LocationCacheTable.country],
        countryCode = this[LocationCacheTable.countryCode]
    )
