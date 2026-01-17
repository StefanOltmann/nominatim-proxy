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
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.File

fun main() {

    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

/**
 * Application entrypoint used by Ktor for wiring dependencies and routes.
 */
fun Application.module() {

    /* Create the necessary directories if missing. */
    File(Config.cacheDbPath).parentFile.mkdirs()

    /* Initialize the database */
    LocationCache.init(Config.cacheDbPath)

    /* Configure the web server. */
    configureRouting()
}
