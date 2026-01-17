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
package model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

/**
 * Address fields as returned by Nominatim.
 *
 * Address parts come from OSM tagging and are not stable or consistent.
 *
 * This model includes only actively used, unique, and commonly emitted fields.
 *
 * See https://nominatim.org/release-docs/latest/api/Output/#json
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class Address(

    /* ---------- Street ---------- */

    /**
     * Street or road name.
     * Examples: "Unter den Linden", "Broadway", "Downing Street"
     */
    val road: String? = null,

    /**
     * Pedestrian street.
     * Examples: "Königstraße", "Third Street Promenade", "Church Street"
     */
    val pedestrian: String? = null,

    /**
     * Footway or path.
     * Examples: "Parkweg", "River Walk", "Thames Path"
     */
    val footway: String? = null,

    /**
     * Cycleway.
     * Examples: "Radweg am Kanal", "Hudson River Greenway", "Bristol and Bath Railway Path"
     */
    val cycleway: String? = null,

    /**
     * Residential street.
     * Examples: "Ahornstraße", "Maple Avenue", "Station Road"
     */
    val residential: String? = null,

    /**
     * Square or plaza.
     * Examples: "Alexanderplatz", "Times Square", "Trafalgar Square"
     */
    val square: String? = null,

    /**
     * Place name used instead of a street.
     * Examples: "Marktplatz", "Union Square", "Parliament Square"
     */
    val place: String? = null,

    /* ---------- Sub-locality ---------- */

    /**
     * Neighbourhood or quarter.
     * Examples: "Prenzlauer Berg", "SoHo", "Shoreditch"
     */
    val neighbourhood: String? = null,

    /**
     * Suburb or district.
     * Examples: "Charlottenburg", "Hollywood", "Camden"
     */
    val suburb: String? = null,

    /**
     * Borough.
     * Examples: "Mitte", "Brooklyn", "Westminster"
     */
    val borough: String? = null,

    /**
     * City district (common for larger cities and administrative subdivisions).
     * Examples: "Friedrichshain-Kreuzberg", "Manhattan", "Greater London"
     */
    @SerialName("city_district")
    val cityDistrict: String? = null,

    /* ---------- Settlement ---------- */

    /**
     * Hamlet.
     * Examples: "Kleinmachnow", "Pine Grove", "Hampton-on-the-Hill"
     */
    val hamlet: String? = null,

    /**
     * Village.
     * Examples: "Wustermark", "Williamsville", "Bibury"
     */
    val village: String? = null,

    /**
     * Town.
     * Examples: "Garmisch-Partenkirchen", "Aspen", "St Ives"
     */
    val town: String? = null,

    /**
     * City.
     * Examples: "Berlin", "New York", "London"
     */
    val city: String? = null,

    /**
     * Municipality.
     * Examples: "Gemeinde Timmendorfer Strand", "City of Santa Monica", "City of Westminster"
     */
    val municipality: String? = null,

    /* ---------- Administrative ---------- */

    /**
     * County or similar second-level administrative unit.
     * Examples: "Landkreis München", "Los Angeles County", "Kent"
     */
    val county: String? = null,

    /**
     * State, province, or first-level administrative unit.
     * Examples: "Bayern", "California", "England"
     */
    val state: String? = null,

    /**
     * Province, used in some countries and sometimes present alongside state.
     * Examples: "Oberbayern", "New England", "Yorkshire"
     */
    val province: String? = null,

    /**
     * Intermediate administrative level.
     * Examples: "Regierungsbezirk Oberbayern", "Southern California", "Greater London"
     */
    @SerialName("state_district")
    val stateDistrict: String? = null,

    /* ---------- Postal ---------- */

    /**
     * Postal code.
     * Examples: "10117", "10001", "SW1A 2AA"
     */
    val postcode: String? = null,

    /* ---------- Country ---------- */

    /**
     * Country name, localized.
     * Examples: "Deutschland", "United States", "United Kingdom"
     */
    val country: String? = null,

    /**
     * ISO 3166-1 alpha-2 country code.
     * Examples: "DE", "US", "GB"
     */
    @SerialName("country_code")
    val countryCode: String? = null

)
