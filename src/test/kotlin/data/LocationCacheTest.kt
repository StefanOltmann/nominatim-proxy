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

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import model.Address
import util.GeohashUtil

class LocationCacheTest {

    @Test
    fun testInsertAndLookup() {

        val dbPath = createTempDbPath()
        LocationCache.init(dbPath)

        val address = Address(
            road = "5th Avenue",
            city = "New York",
            postcode = "10118",
            country = "United States"
        )

        LocationCache.saveAddress(
            geohash = GeohashUtil.encode(40.7484, -73.9857),
            language = "en",
            address = address
        )

        val cached = LocationCache.findAddress(
            geohash = GeohashUtil.encode(40.7484, -73.9857),
            language = "en"
        )

        assertEquals(address, cached)
    }

    @Test
    fun testDuplicateInsertDoesNotOverwrite() {

        val dbPath = createTempDbPath()
        LocationCache.init(dbPath)

        val first = Address(
            road = "Main Street",
            city = "Sampletown",
            postcode = "12345",
            country = "Exampleland"
        )

        val second = first.copy(road = "New Road")

        LocationCache.saveAddress(
            geohash = GeohashUtil.encode(10.0, 20.0),
            language = "en",
            address = first
        )

        LocationCache.saveAddress(
            geohash = GeohashUtil.encode(10.0, 20.0),
            language = "en",
            address = second
        )

        val cached = LocationCache.findAddress(
            geohash = GeohashUtil.encode(10.0, 20.0),
            language = "en"
        )

        assertEquals(first, cached)
    }

    private fun createTempDbPath(): String {
        val path = Files.createTempFile("nominatim-cache-", ".sqlite")
        path.toFile().deleteOnExit()
        return path.toAbsolutePath().toString()
    }
}
