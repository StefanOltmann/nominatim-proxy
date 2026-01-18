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
import data.LocationCache
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import model.NominatimResponse
import model.ReverseGeocodeResponse
import util.GeohashUtil
import util.RateLimiter

private val httpClient = HttpClient()

/**
 * Installs routes and wraps initialization with error logging.
 */
@OptIn(ExperimentalSerializationApi::class)
fun Application.configureRouting() {

    try {

        configureRoutingInternal()

    } catch (ex: Throwable) {

        log("Starting server $VERSION failed.")
        log(ex)
    }
}

@OptIn(ExperimentalSerializationApi::class, ExperimentalTime::class, ExperimentalUuidApi::class)
private fun Application.configureRoutingInternal() {

    val startTime = Clock.System.now().toEpochMilliseconds()

    log("[INIT] Starting Server at version $VERSION")

    install(ContentNegotiation) {
        json(Json)
    }

    /*
     * Wildcard CORS
     */
    install(CORS) {

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)

        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.ContentType)

        anyHost()
    }

    routing {

        get("/") {

            val uptimeMinutes = (Clock.System.now().toEpochMilliseconds() - startTime) / 1000 / 60

            val uptimeHours = uptimeMinutes / 60
            val minutes = uptimeMinutes % 60

            call.respondText("Nominatim Proxy $VERSION (up since $uptimeHours hours and $minutes minutes)")
        }

        /*
         * This does reverse geocoding.
         */
        get("/reverse") {

            try {

                if (!ensureValidApiKey(call))
                    return@get

                val latitude = call.queryParameters["lat"]?.toDoubleOrNull()
                val longitude = call.queryParameters["lon"]?.toDoubleOrNull()

                val geohashParam = call.queryParameters["geohash"]

                val language = call.queryParameters["lang"]

                /*
                 * Ensure we accept the language code
                 *
                 * Unsupported language codes will make Nominatim respond in the
                 * current locale, which could mess up our dataset.
                 */
                if (language == null || !Config.acceptedLanguageCodes.contains(language)) {

                    call.respondText(
                        text = "Parameter 'lang' must be a valid value, but was '$language'.",
                        contentType = ContentType.Text.Plain,
                        status = HttpStatusCode.BadRequest
                    )

                    return@get
                }

                /*
                 * Decide which lookup variant to use.
                 */
                when {

                    geohashParam != null ->
                        handleReverseByGeohash(
                            call = call,
                            geohashParam = geohashParam,
                            language = language
                        )

                    latitude != null && longitude != null ->
                        handleReverseByCoordinates(
                            call = call,
                            latitude = latitude,
                            longitude = longitude,
                            language = language
                        )

                    else ->
                        call.respondText(
                            text = "Parameters must include either 'geohash' or a 'lat' & 'lon' pair.",
                            contentType = ContentType.Text.Plain,
                            status = HttpStatusCode.BadRequest
                        )
                }

            } catch (ex: Throwable) {

                /*
                 * Log
                 */
                log("Reverse geocoding failed")
                log(ex)

                /*
                 * Let the client know.
                 */
                call.respondText(
                    text = "Internal server error: ${ex.message}",
                    contentType = ContentType.Text.Plain,
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
    }
}

/**
 * Builds the Nominatim reverse lookup URL with fixed parameters.
 */
private fun createReverseUrl(
    latitude: Double,
    longitude: Double,
): String =
    "${Config.nominatimUrl}/reverse" +
        "?lat=$latitude&lon=$longitude" +
        "&format=${Config.PARAM_FORMAT}" +
        "&layer=${Config.PARAM_LAYER}" +
        "&adressdetails=${Config.PARAM_ADDRESS_DETAILS}" +
        "&zoom=${Config.PARAM_ZOOM}" +
        "&email=${Config.email}"

/**
 * Checks the API key.
 * Returns "true" when everything is fine, "false" on error.
 */
private suspend fun ensureValidApiKey(
    call: ApplicationCall
): Boolean {

    val givenApiKey = call.request.header("x-api-key")

    if (givenApiKey != Config.apiKey) {

        call.respondText(
            status = HttpStatusCode.Unauthorized,
            contentType = ContentType.Text.Plain,
            text = "Please provide a valid API key."
        )

        return false
    }

    return true
}

/**
 * Handles a reverse lookup based on coordinates.
 * Always converts coordinates into a geohash for cache consistency.
 */
private suspend fun handleReverseByCoordinates(
    call: ApplicationCall,
    latitude: Double,
    longitude: Double,
    language: String
) {

    if (!isValidCoordinateRange(latitude, longitude)) {

        call.respondText(
            text = "Parameters 'lat' & 'lon' must be within valid ranges. Got $latitude|$longitude",
            contentType = ContentType.Text.Plain,
            status = HttpStatusCode.BadRequest
        )

        return
    }

    val geohash = GeohashUtil.encode(latitude, longitude)

    handleReverseByGeohash(
        call = call,
        geohashParam = geohash,
        language = language
    )
}

/**
 * Handles a reverse lookup based on geohash.
 * Always resolves the center coordinate for upstream requests.
 */
private suspend fun handleReverseByGeohash(
    call: ApplicationCall,
    geohashParam: String,
    language: String
) {

    if (geohashParam.length != 8) {

        call.respondText(
            text = "Parameter 'geohash' must be 8 characters.",
            contentType = ContentType.Text.Plain,
            status = HttpStatusCode.BadRequest
        )

        return
    }

    val geohash = geohashParam.lowercase()

    if (geohash == "s0000000") {

        call.respondText(
            text = "Parameter 'geohash' is invalid. You can't query for Null Island.",
            contentType = ContentType.Text.Plain,
            status = HttpStatusCode.BadRequest
        )

        return
    }

    val cachedAddress = LocationCache.findAddress(
        geohash = geohash,
        language = language
    )

    if (cachedAddress != null) {

        log("Cache hit for $geohash ($language)")

        call.respond(
            status = HttpStatusCode.OK,
            message = ReverseGeocodeResponse(
                geohash = geohash,
                address = cachedAddress
            )
        )

        return
    }

    /*
     * Ensure we have a valid GPS coordinate before asking the rate limiter for a slot.
     */
    val centerCoordinate = try {
        GeohashUtil.decodeToCenter(geohash)
    } catch (ex: IllegalArgumentException) {

        call.respondText(
            text = ex.message ?: "Parameter 'geohash' is invalid.",
            contentType = ContentType.Text.Plain,
            status = HttpStatusCode.BadRequest
        )

        return
    }

    val permitGranted = RateLimiter.awaitPermit(
        minIntervalMs = Config.rateLimitMs,
        maxWaitMs = Config.rateLimitMaxWaitMs
    )

    if (!permitGranted) {

        log("Rate limit exceeded.")

        call.respondText(
            text = "Too many requests. Please try again later.",
            contentType = ContentType.Text.Plain,
            status = HttpStatusCode.TooManyRequests
        )

        return
    }

    log("Queried API for $geohash (${centerCoordinate.latitude}|${centerCoordinate.longitude}) ($language)")

    val httpResponse = httpClient.get(
        urlString = createReverseUrl(
            latitude = centerCoordinate.latitude,
            longitude = centerCoordinate.longitude
        )
    ) {
        header("User-Agent", Config.userAgent)
        header("Accept-Language", language)
    }

    if (httpResponse.status != HttpStatusCode.OK) {

        log("API returned HTTP error ${httpResponse.status}: ${httpResponse.bodyAsText()}")

        call.respondText(
            text = "API returned HTTP error: ${httpResponse.status}",
            contentType = ContentType.Text.Plain,
            status = HttpStatusCode.BadGateway
        )

        return
    }

    val responseText = httpResponse.bodyAsText()

    val nominatimResponse = Json.decodeFromString<NominatimResponse>(responseText)

    LocationCache.saveAddress(
        geohash = geohash,
        language = language,
        address = nominatimResponse.address
    )

    call.respond(
        status = HttpStatusCode.OK,
        message = ReverseGeocodeResponse(
            geohash = geohash,
            address = nominatimResponse.address
        )
    )
}

private fun log(message: String) =
    println(message)

private fun log(ex: Throwable) =
    ex.printStackTrace()

private fun isValidCoordinateRange(
    latitude: Double,
    longitude: Double
): Boolean =
    latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
        latitude != 0.0 && longitude != 0.0
