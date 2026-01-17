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
package util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GeohashUtilTest {

    @Test
    fun testEncode() {

        /* Brandenburger Tor */
        assertEquals(
            expected = "u33db2m3",
            actual = GeohashUtil.encode(52.516340, 13.377616)
        )
    }

    @Test
    fun testDecode() {

        assertEquals(
            expected = GeohashUtil.GpsCoordinate(52.51628, 13.37774),
            actual = GeohashUtil.decodeToCenter("u33db2m3")
        )
    }

    @Test
    fun testDecodeToCenterRejectsInvalidInput() {
        assertFailsWith<IllegalArgumentException> { GeohashUtil.decodeToCenter("") }
        assertFailsWith<IllegalArgumentException> { GeohashUtil.decodeToCenter("u4pru*") }
    }
}
