# Nominatim Proxy

![Kotlin](https://img.shields.io/badge/kotlin-2.3.0-blue.svg?logo=kotlin)
[![License: AGPL-3.0](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
![JVM](https://img.shields.io/badge/-JVM-gray.svg?style=flat)
[![GitHub Sponsors](https://img.shields.io/badge/Sponsor-gray?&logo=GitHub-Sponsors&logoColor=EA4AAA)](https://github.com/sponsors/StefanOltmann)

Reverse geocoding proxy for a Nominatim instance with SQLite caching and a strict upstream rate limit.

## Features

- Caches reverse geocoding responses in a local SQLite database.
- Normalizes cache keys by encoding coordinates to geohash (precision 8).
- Requests upstream using the center point of the geohash cell.
- Includes a rate limiter for the upstream API.
- Bounded queueing so callers fail fast under load.
- Simple API key check to protect the proxy endpoint.

If you use this proxy with the official Nominatim instance, you must adhere to https://operations.osmfoundation.org/policies/nominatim/.

## Configuration

All configuration is done via environment variables.

| Variable                 | Required | Default               | Description                                                               |
|--------------------------|----------|-----------------------|---------------------------------------------------------------------------|
| `API_KEY`                | yes      | -                     | API key required in `x-api-key` header.                                   |
| `NOMINATIM_URL`          | yes      | -                     | Base URL of the Nominatim instance, e.g. `https://nominatim.example.com`. |
| `USER_AGENT`             | yes      | -                     | User-Agent header for Nominatim requests.                                 |
| `EMAIL`                  | yes      | -                     | Contact email required by Nominatim usage policy.                         |
| `CACHE_DB_PATH`          | no       | `data/geodata.sqlite` | Path to the SQLite cache file. Parent directory is created if missing.    |
| `RATE_LIMIT_MS`          | no       | `2000`                | Minimum delay between upstream requests in milliseconds.                  |
| `RATE_LIMIT_MAX_WAIT_MS` | no       | `5000`                | Maximum time a request may wait in the queue before failing.              |

## API

### `GET /`

Health endpoint. Returns a short uptime string.

### `GET /reverse`

Reverse geocoding endpoint. Two distinct supported use cases:

Coordinate lookup requires `lat`, `lon`, and `lang`.

Geohash lookup requires `geohash` and `lang`. The server always uses the center point of the geohash cell for the upstream request.

Parameter details:

- `lat`: Latitude as decimal number (only for coordinate lookup).
- `lon`: Longitude as decimal number (only for coordinate lookup).
- `geohash`: Geohash (precision 8) (only for geohash lookup).
- `lang`: ISO 639-1 language code. Unsupported values return `400`.

Headers:

- `x-api-key` (required): Must match `API_KEY`.

Responses:

- `200 OK`: JSON response with `geohash` and address object.
- `400 Bad Request`: Missing or invalid parameters.
- `401 Unauthorized`: Missing or invalid API key.
- `429 Too Many Requests`: Rate limit queue overflow.
- `502 Bad Gateway`: Upstream error.

## Example

Request:

```bash
curl "http://localhost:8080/reverse?lat=40.7484&lon=-73.9857&lang=en" \
  -H "x-api-key: super-secret"
```

Response:

```json
{
    "geohash": "dr5ru7re",
    "address": {
        "road": "5th Avenue",
        "city": "New York",
        "postcode": "10118",
        "country": "United States"
    }
}
```

Geohash request:

```bash
curl "http://localhost:8080/reverse?geohash=dr5ru7re&lang=en" \
  -H "x-api-key: super-secret"
```

## Running locally

```bash
export API_KEY="super-secret"
export NOMINATIM_URL="https://nominatim.example.com"
export USER_AGENT="nominatim-proxy/1.0"
export EMAIL="ops@example.com"
./gradlew run
```

The server listens on `0.0.0.0:8080` by default.

## Build and run the jar

```bash
./gradlew buildFatJar
java -jar build/libs/*-all.jar
```

## Docker

Build the image:

```bash
docker build -t nominatim-proxy .
```

Run the container:

```bash
docker run --rm -p 8080:8080 \
  -e API_KEY="super-secret" \
  -e NOMINATIM_URL="https://nominatim.example.com" \
  -e USER_AGENT="nominatim-proxy/1.0" \
  -e EMAIL="ops@example.com" \
  -e CACHE_DB_PATH="/data/geodata.sqlite" \
  -e RATE_LIMIT_MS="1000" \
  -e RATE_LIMIT_MAX_WAIT_MS="5000" \
  -v "$(pwd)/data:/data" \
  nominatim-proxy
```
