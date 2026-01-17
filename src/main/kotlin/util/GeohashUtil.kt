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

import kotlin.math.round

/**
 * Minimal geohash encoder/decoder.
 *
 * How it works:
 * - Start with full latitude [-90, 90] and longitude [-180, 180] ranges.
 * - For each bit, split the current range in half.
 * - Alternate longitude and latitude bits (lon, lat, lon, lat...).
 * - Every 5 bits map to one character using the geohash base32 alphabet.
 *
 * Decoding reverses the process by replaying the stored bits to shrink
 * the ranges until the final cell is known. The returned coordinate is
 * the center of that cell, rounded to 5 decimals for stable requests.
 */
object GeohashUtil {

    /*
     * Geohash uses its own base32 alphabet (not RFC 4648).
     * See https://en.wikipedia.org/wiki/Geohash#Algorithm_and_example
     */
    private const val GEOHASH_BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    private const val BITS_PER_CHAR = 5

    private val BIT_MASKS = intArrayOf(16, 8, 4, 2, 1)

    private val base32Lookup: IntArray = IntArray(128) { -1 }.also { lookup ->
        GEOHASH_BASE32.forEachIndexed { index, char ->
            lookup[char.code] = index
        }
    }

    /**
     * Mutable numeric range that is repeatedly bisected while encoding/decoding.
     */
    private data class Range(
        var min: Double,
        var max: Double
    ) {

        /**
         * Splits the range in half and returns the bit for the side that contains the value.
         * 0 means "lower half", 1 means "upper half".
         */
        fun refineForValue(value: Double): Int {

            val mid = (min + max) / 2

            return if (value >= mid) {
                min = mid
                1
            } else {
                max = mid
                0
            }
        }

        /**
         * Splits the range in half based on an incoming bit (1 = upper, 0 = lower).
         * This is the inverse of refineForValue() used during decoding.
         */
        fun refineForBit(isSet: Boolean) {

            val mid = (min + max) / 2

            if (isSet)
                min = mid
            else
                max = mid
        }

        /**
         * Returns the center of the current range, rounded for stable API requests.
         */
        fun center(): Double = roundTo5Decimals((min + max) / 2)
    }

    /**
     * Latitude/longitude pair.
     */
    data class GpsCoordinate(
        val latitude: Double,
        val longitude: Double
    )

    /**
     * Encodes latitude/longitude into a geohash with the given precision.
     *
     * The algorithm splits longitude and latitude ranges in alternating bits:
     * lon bit, lat bit, lon bit, lat bit, and so on.
     *
     * Precision is the number of geohash characters (5 bits each).
     */
    fun encode(
        latitude: Double,
        longitude: Double,
        precision: Int = 8
    ): String {

        require(precision > 0) { "Precision must be positive." }

        val latRange = Range(-90.0, 90.0)
        val lonRange = Range(-180.0, 180.0)

        var isEvenBit = true
        var bitIndex = 0
        var currentBits = 0

        val geohash = StringBuilder(precision)

        while (geohash.length < precision) {

            /* Alternate lon/lat refinement to build up 5-bit characters. */
            currentBits = if (isEvenBit)
                (currentBits shl 1) or lonRange.refineForValue(longitude)
            else
                (currentBits shl 1) or latRange.refineForValue(latitude)

            isEvenBit = !isEvenBit
            bitIndex++

            if (bitIndex == BITS_PER_CHAR) {

                /* Map the 5 bits into the geohash alphabet. */
                geohash.append(GEOHASH_BASE32[currentBits])

                bitIndex = 0
                currentBits = 0
            }
        }

        return geohash.toString()
    }

    /**
     * Decodes a geohash to the center of its rectangle.
     *
     * Each character adds 5 bits of precision. The bits are interpreted
     * in the same alternating lon/lat order as encoding.
     *
     * The returned coordinate is the center of the geohash cell, not the
     * original input coordinate that produced the geohash.
     */
    fun decodeToCenter(geohash: String): GpsCoordinate {

        require(geohash.isNotBlank()) { "Geohash must not be blank." }

        val latRange = Range(-90.0, 90.0)
        val lonRange = Range(-180.0, 180.0)
        var isEvenBit = true

        for (char in geohash.lowercase()) {

            /* Convert a geohash character into its 5-bit value. */
            val value: Int = if (char.code < base32Lookup.size)
                base32Lookup[char.code]
            else
                -1

            require(value >= 0) { "Invalid geohash character '$char'." }

            for (mask in BIT_MASKS) {

                val bit = value and mask

                /*
                 * Reconstruct lon/lat ranges by applying each bit to the current bounds.
                 * Bit order matches the encoding: lon, lat, lon, lat...
                 */
                if (isEvenBit)
                    lonRange.refineForBit(bit != 0)
                else
                    latRange.refineForBit(bit != 0)

                isEvenBit = !isEvenBit
            }
        }

        return GpsCoordinate(
            latitude = latRange.center(),
            longitude = lonRange.center()
        )
    }

    private fun roundTo5Decimals(value: Double): Double =
        round(value * 100000.0) / 100000.0
}
