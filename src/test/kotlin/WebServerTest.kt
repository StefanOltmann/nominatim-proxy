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
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Some basic tests that can be performed without mocking the upstream service.
 */
class WebServerTest {

    @Test
    fun testHealthEndpoint() = testApplication {

        application {
            configureRouting()
        }

        val response = client.get("/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Nominatim Proxy"))
    }

    @Test
    fun testReverseRequiresApiKey() = testApplication {

        application {
            configureRouting()
        }

        val response = client.get("/reverse?lat=1&lon=2&lang=en")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testReverseRejectsMissingParameters() = testApplication {

        application {
            configureRouting()
        }

        val response = client.get("/reverse?lang=en") {
            header("x-api-key", "test-api-key")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testReverseRejectsInvalidLanguage() = testApplication {

        application {
            configureRouting()
        }

        val response = client.get("/reverse?lat=1&lon=2&lang=zz") {
            header("x-api-key", "test-api-key")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testReverseRejectsOutOfRangeCoordinates() = testApplication {

        application {
            configureRouting()
        }

        val response = client.get("/reverse?lat=91&lon=2&lang=en") {
            header("x-api-key", "test-api-key")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
