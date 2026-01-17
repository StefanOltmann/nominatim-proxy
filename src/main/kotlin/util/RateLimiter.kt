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

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Global rate limiter for outbound requests.
 */
object RateLimiter {

    /* Single mutex = single queue for all upstream calls. */
    private val mutex = Mutex()

    /* Timestamp of the last upstream call granted by this limiter. */
    private var lastCallEpochMs: Long = 0

    /**
     * Enforces a global minimum interval between upstream calls.
     *
     * Requests wait in a single queue; when the time budget is exceeded,
     * the call is rejected so the service can fail fast under load.
     */
    suspend fun awaitPermit(
        minIntervalMs: Long,
        maxWaitMs: Long
    ): Boolean {

        /*
         * This function is called for every request that wants to hit the upstream API.
         * We only allow ONE request at a time and we enforce a gap of minIntervalMs
         * between two allowed requests.
         */

        /* Record when this request entered the queue so we can enforce maxWaitMs. */
        val enqueueTimeMs = System.currentTimeMillis()

        /*
         * The mutex creates a single-file queue. Every caller waits here until
         * it reaches the front of the line. Once inside, we decide if it is
         * still allowed to wait and how long it must delay to respect minIntervalMs.
         */
        return mutex.withLock {

            val nowMs = System.currentTimeMillis()

            /* Time already spent waiting for the lock counts against maxWaitMs. */
            val waitedForLockMs = nowMs - enqueueTimeMs

            val remainingWaitMs = maxWaitMs - waitedForLockMs

            /*
             * If this request already waited too long in the queue,
             * we fail fast so the caller can retry later.
             */
            if (remainingWaitMs < 0)
                return@withLock false

            /* Compute how long we still need to wait to keep minIntervalMs. */
            val requiredDelayMs = (lastCallEpochMs + minIntervalMs - nowMs).coerceAtLeast(0)

            /*
             * If the required delay exceeds the remaining wait budget,
             * we reject the request to keep latency bounded.
             */
            if (requiredDelayMs > remainingWaitMs)
                return@withLock false

            /*
             * We are allowed to wait. Delay here while still holding the lock,
             * so no other request can pass ahead of us.
             */
            if (requiredDelayMs > 0)
                delay(requiredDelayMs)

            /* Mark this call as the most recent granted permit. */
            lastCallEpochMs = System.currentTimeMillis()

            /*
             * Permit granted. The caller may now perform the upstream request.
             */
            true
        }
    }

    /**
     * Resets internal state so tests can run deterministically.
     */
    internal suspend fun resetForTests() =
        mutex.withLock {
            lastCallEpochMs = 0
        }
}
