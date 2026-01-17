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

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class RateLimiterTest {

    @Test
    fun testPermitsFirstRequestImmediately() = runBlocking {

        RateLimiter.resetForTests()

        val permitted = RateLimiter.awaitPermit(
            minIntervalMs = 100,
            maxWaitMs = 100
        )

        Assert.assertTrue(permitted)
    }

    @Test
    fun testRejectsWhenRemainingWaitBudgetIsTooSmall() = runBlocking {

        RateLimiter.resetForTests()

        val first = RateLimiter.awaitPermit(
            minIntervalMs = 100,
            maxWaitMs = 100
        )

        Assert.assertTrue(first)

        val second = RateLimiter.awaitPermit(
            minIntervalMs = 50,
            maxWaitMs = 0
        )

        Assert.assertFalse(second)
    }
}
