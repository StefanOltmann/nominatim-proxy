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

/**
 * Runtime configuration loaded from environment variables.
 */
object Config {

    const val PARAM_FORMAT: String = "json"
    const val PARAM_LAYER: String = "address"
    const val PARAM_ADDRESS_DETAILS: String = "1"
    const val PARAM_ZOOM: String = "18"

    /**
     * This default limits the proxy to less than one request per second,
     * which is the absolute allowed maximum for the official hosted Nominatim instance.
     */
    const val DEFAULT_RATE_LIMIT_MS: Long = 2000

    const val DEFAULT_RATE_LIMIT_MAX_WAIT_MS: Long = 5000

    /* We accept all 184 ISO 639-1 language codes. */
    val acceptedLanguageCodes = setOf(
        "aa", "ab", "ae", "af", "ak", "am", "an", "ar", "as", "av", "ay", "az",
        "ba", "be", "bg", "bh", "bi", "bm", "bn", "bo", "br", "bs", "ca", "ce",
        "ch", "co", "cr", "cs", "cu", "cv", "cy", "da", "de", "dv", "dz", "ee",
        "el", "en", "eo", "es", "et", "eu", "fa", "ff", "fi", "fj", "fo", "fr",
        "fy", "ga", "gd", "gl", "gn", "gu", "ha", "he", "hi", "ho", "hr", "ht",
        "hu", "hy", "hz", "ia", "id", "ie", "ig", "ii", "ik", "io", "is", "it",
        "iu", "ja", "jv", "ka", "kg", "ki", "kj", "kk", "kl", "km", "kn", "ko",
        "kr", "ks", "ku", "kv", "kw", "ky", "la", "lb", "lg", "li", "ln", "lo",
        "lt", "lu", "lv", "mg", "mh", "mi", "mk", "ml", "mn", "mr", "ms", "mt",
        "my", "na", "nb", "nd", "ne", "ng", "nl", "nn", "no", "nr", "nv", "ny",
        "oc", "oj", "om", "or", "os", "pa", "pi", "pl", "ps", "pt", "qu", "rm",
        "rn", "ro", "ru", "rw", "sa", "sc", "sd", "se", "sg", "si", "sk", "sl",
        "sm", "sn", "so", "sq", "sr", "ss", "st", "su", "sv", "sw", "ta", "te",
        "tg", "th", "ti", "tk", "tl", "tn", "to", "tr", "ts", "tt", "tw", "ty",
        "ug", "uk", "ur", "uz", "ve", "vi", "vo", "wa", "wo", "xh", "yi", "yo",
        "za", "zh", "zu"
    )

    val apiKey: String =
        System.getenv("API_KEY") ?: error("API_KEY not defined")

    val nominatimUrl: String =
        System.getenv("NOMINATIM_URL") ?: error("NOMINATIM_URL not defined")

    val userAgent: String =
        System.getenv("USER_AGENT") ?: error("USER_AGENT not defined")

    val email: String =
        System.getenv("EMAIL") ?: error("EMAIL not defined")

    val cacheDbPath: String =
        System.getenv("CACHE_DB_PATH") ?: "data/geodata.sqlite"

    val rateLimitMs: Long =
        System.getenv("RATE_LIMIT_MS")?.toLongOrNull() ?: DEFAULT_RATE_LIMIT_MS

    val rateLimitMaxWaitMs: Long =
        System.getenv("RATE_LIMIT_MAX_WAIT_MS")?.toLongOrNull() ?: DEFAULT_RATE_LIMIT_MAX_WAIT_MS
}
