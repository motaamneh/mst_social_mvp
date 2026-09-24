# mst_social_mvp

Instagram account-control verification MVP (in progress).

## Run locally

Use Java 21 and a PostgreSQL database named `mst_social_db_mvp`. In your IDE run configuration, set:

```text
SPRING_PROFILES_ACTIVE=local
MST_DB_PASSWORD=<your local PostgreSQL password>
MST_LOCAL_API_KEY=<a generated test key>
```

Generate the local API key once with:

```bash
printf 'mst_test_%s\n' "$(openssl rand -hex 32)"
```

Keep the key in your local run configuration and API client; do not commit it. The `local` profile creates a development tenant and stores only the key digest. Flyway applies `V1__create_initial_schema.sql` automatically at startup; do not run the migration SQL by hand.

Run `./mvnw clean verify` with Docker running. Integration tests use their own PostgreSQL Testcontainer, separate from your local database.

## Current HTTP operations

Send the generated key as `Authorization: Bearer <key>`.

```http
POST /api/v1/verifications
Content-Type: application/json

{"subjectId":"user-123","username":"mothe.techguy"}
```

The creation response contains a one-time marker and a `verificationId`. Read its status with:

```http
GET /api/v1/verifications/{verificationId}
```

The status response never contains the marker. Challenges expire after 15 minutes and allow at most five unsuccessful observations.

## Test the complete local flow

Import [the Postman collection](postman/mst-social-mvp-local.postman_collection.json), set its `apiKey` variable to the same `MST_LOCAL_API_KEY` used by the running application, and run its requests in order. Use a fresh `username` if you already verified that handle in this tenant. The collection creates a challenge, puts a simulated biography into the local-only provider, checks a mismatch, adds the marker to the simulated biography, verifies the challenge, and confirms that status does not reveal the marker. The local fixture endpoint is:

```http
PUT /api/v1/local/instagram-profiles/{username}
Authorization: Bearer <mst-api-key>
Content-Type: application/json

{"biography":"Example bio mst_..."}
```

Then call `POST /api/v1/verifications/{verificationId}/verify` with the same bearer key and no request body. A match returns `VERIFIED` and creates a social identity. A mismatch returns `MARKER_NOT_FOUND` and stays `PENDING`. Provider unavailability returns HTTP 503 and does not use an attempt. The fixture endpoint exists only in the `local` profile and stores biographies in memory; it is never a live Instagram check. Restarting the app clears those fixtures. Flyway applies V2 automatically to add the verification lease column.

Without a configured external provider, non-local profiles fail closed with `PROVIDER_UNAVAILABLE`. The `fake` mode is only for local testing; do not put a marker in a real Instagram bio for that test.

## Use SearchAPI for the MVP

The temporary SearchAPI adapter reads a public Instagram profile biography by username. This repository reads its key from `MST_SEARCHAPI_API_KEY`; the local key is stored in the ignored, owner-readable `.env.searchapi` file. The application does not automatically load `.env` files. Export that file in the same shell before starting the app, or set both variables in the IDE run configuration:

```bash
set -a
source .env.searchapi
set +a
```

Also set the database, local tenant API key, and other required variables described above. `MST_INSTAGRAM_PROVIDER_MODE=searchapi` selects the live adapter in any profile, including `local`; the simulated biography endpoint is available only in `fake` mode. Each live verification check can consume a SearchAPI request, so monitor the account's remaining credits and keep billing disabled if you intend to stay within free usage. A provider error or inaccessible biography returns `PROVIDER_UNAVAILABLE` or `PROFILE_INACCESSIBLE` without treating it as a marker mismatch.

Import [the live Postman collection](postman/mst-social-mvp-live.postman_collection.json) and set its `apiKey` to your application's `MST_LOCAL_API_KEY`, not the SearchAPI key. Set `username` to a public Instagram account you control. Run requests 1-3, copy the marker from request 2 into that account's biography, then run requests 4-5 after the biography is visible. Run these requests individually so you can update the biography between creation and verification. The challenge expires after 15 minutes.

If challenge creation returns HTTP 401, check that Postman sends `Authorization: Bearer <MST_LOCAL_API_KEY>` and that the same `MST_LOCAL_API_KEY` is set in the IntelliJ run configuration before the app starts. The request also needs a nonempty `subjectId`; after authentication, an empty value returns HTTP 400.

The verification service uses the `BioProvider` interface, so the temporary adapter can be replaced when an official API key and the required account access are available. Whether an official Meta API can read the biography for the target account type must be confirmed before switching; arbitrary personal/private biography access is not assumed.

## Connect a trusted biography service

The optional HTTP adapter expects a service you operate or are authorized to use. Set `MST_INSTAGRAM_PROVIDER_MODE=http`, `MST_INSTAGRAM_PROVIDER_BASE_URL=https://your-provider.example/v1`, `MST_INSTAGRAM_PROVIDER_ALLOWED_HOST=your-provider.example`, and `MST_INSTAGRAM_PROVIDER_TOKEN=<secret>`. The adapter calls `GET {baseUrl}/profiles/{normalizedUsername}` with `Authorization: Bearer <token>` and `Accept: application/json`. The base URL is operator-configured, must use HTTPS, and must match the allowlisted host. Redirects are not followed; the connect timeout is 2 seconds, request timeout 5 seconds, and response is limited to 16 KiB.

Expected HTTP 200 body:

```json
{
  "username": "mothe.techguy",
  "biography": "The current biography text",
  "accountId": "stable-provider-id-if-available",
  "observedAt": "2026-09-24T18:00:00Z"
}
```

`accountId` may be omitted. `observedAt` must be when the biography was actually read, not when a cached response was served. HTTP 404 maps to `PROFILE_NOT_FOUND`, 403 to `PROFILE_INACCESSIBLE`, and other failures to `PROVIDER_UNAVAILABLE`. The adapter never accepts a biography in the public `/verify` request. Its evidence is labelled `EXTERNAL_BIO_PROVIDER` / `PROVIDER_REPORTED`; this label does not assert that any particular Instagram account type is supported. Confirm the source's account coverage, authorization, and terms before enabling it. Idempotency, rate limiting, and OpenAPI documentation remain for release hardening.
