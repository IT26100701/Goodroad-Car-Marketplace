# Goodroad Car Marketplace

Spring Boot marketplace for the SE1020 group project. It includes a responsive web UI and same-origin REST API for user accounts, car listings and search, purchase orders, wishlists, reviews, and administrator tools.

## Run

Requirements: Java 17+ and Maven 3.6+.

```bash
mvn spring-boot:run
```

The API listens on port `5000` by default. Set `PORT` to change it.
Session cookies are not marked Secure by default so login works on `http://localhost`. When serving over HTTPS, set `SESSION_COOKIE_SECURE=true`.

Open `/` to use the marketplace. Buyers can search listings, save cars, request purchases, and review completed orders. Sellers can manage listings and purchase requests. Administrators can manage accounts and reviews and view marketplace metrics. The web UI uses the same session cookie and CSRF flow described below.

The backend stores human-readable JSON files in `./data` by default:

- `users.json`
- `cars.json`
- `orders.json`
- `wishlists.json`
- `reviews.json`
- `audit.json`

Uploaded car photos are saved as files in `data/images/` (JPG, PNG or WebP, up to 5 MB each, 6 per listing; the first is the cover). Set `APP_DATA_DIR` to use another directory. Writes use a temporary file and atomic replacement where supported. Passwords are stored as BCrypt hashes. The files are appropriate for the single-instance course project; this file-backed approach is not safe for multiple application instances or high-concurrency production use.

## First administrator

Public registration only creates buyer or seller accounts. Before creating the first administrator, set an environment variable named `APP_ADMIN_SETUP_TOKEN` with a long random value (at least 32 characters). Then call `POST /api/admin/bootstrap` once with that token and the initial admin's details. The endpoint disables itself after an admin account exists. Never commit the token or send it in a chat message.

Later administrators are created by a signed-in administrator at `POST /api/admin/admins`. Passwords must be at least 12 characters for admin accounts.

## Session and CSRF

Authentication uses an HTTP-only session cookie. Before any `POST`, `PUT`, `PATCH`, or `DELETE` request, fetch `GET /api/auth/csrf`, keep the returned cookie, and send its `token` value in the `X-XSRF-TOKEN` header. Login rotates the CSRF token, so fetch a fresh one after signing in. The API is intended to be used same-origin with the future UI.

Example registration using curl:

```bash
curl -c cookies.txt http://localhost:5000/api/auth/csrf
# Copy the token from the response.
curl -b cookies.txt -c cookies.txt \
  -H 'Content-Type: application/json' \
  -H 'X-XSRF-TOKEN: <csrf-token>' \
  -d '{"email":"buyer@example.com","password":"example-password","fullName":"Example Buyer","phone":"","role":"BUYER"}' \
  http://localhost:5000/api/auth/register
```

After registration, sign in through `POST /api/auth/login` with the same cookie and a fresh CSRF token. Fetch another CSRF token after login before making further changes. Do not use the example account details as real credentials.

## Main API routes

| Area | Routes |
| --- | --- |
| Health | `GET /api/health` |
| Auth | `GET /api/auth/csrf`, `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout` |
| Profile | `GET/PUT/DELETE /api/users/me`, `GET /api/users/me/cars` (seller) |
| Cars | `GET /api/cars`, `GET /api/cars/{id}`, `POST /api/cars`, `PUT/DELETE /api/cars/{id}` |
| Car photos | `POST /api/cars/{id}/images` (multipart field `files`, seller), `DELETE /api/cars/{id}/images/{fileName}` (seller), `GET /api/images/{fileName}` (public) |
| Orders | `POST /api/orders`, `GET /api/orders`, `PATCH /api/orders/{id}/status` |
| Wishlist | `GET /api/wishlist`, `POST/DELETE /api/wishlist/{carId}` |
| Reviews | `GET/POST /api/cars/{carId}/reviews`, `PUT/DELETE /api/reviews/{id}` |
| Admin | `/api/admin/admins`, `/api/admin/users`, `/api/admin/overview`, `/api/admin/audit`, `/api/admin/reviews` |

Car search supports `q` (every word must match brand, model, year, body style or description, e.g. `toyota aqua`), `vehicleType` (`SEDAN`, `HATCHBACK`, `SUV`, `TRUCK`, `VAN`, `OTHER`), `minPrice`, `maxPrice`, `minYear`, `maxYear`, `minMileage`, `maxMileage`, and `sort`. Sort values: `newest`, `price_asc`, `price_desc`, `year_desc`, `mileage_asc`. Omitting filters resets them.

Orders move from `PENDING` to `APPROVED` to `COMPLETED`; buyers can cancel pending orders and sellers can cancel pending or approved orders. Completing a purchase marks the car sold and cancels other open orders for it. A buyer must have a completed purchase to publish one review for a car. Admins can hide or republish reviews.

## OOP and storage notes

Buyer, seller, and administrator accounts inherit shared encapsulated account data from `Account`; role-specific account subclasses supply their role polymorphically. Data repositories read and write separate JSON collections and return API response objects that never contain password hashes.

## Sample data (Phase 2)

Demo data is in `sample-data/` (users, cars, orders, wishlists, reviews, audit). **It loads automatically on first start** when `data/users.json` is missing or empty, so `mvn spring-boot:run` gives a populated marketplace with working logins (see `sample-data/README.md`). Existing data is never overwritten.

- Set `APP_SEED_SAMPLE_DATA=false` to disable this (do so for any real deployment: the demo passwords are public).
- To reset to the demo state, stop the app, delete `data/`, and start again.
- Prices are shown in LKR (Rs.) and mileage in km.

## Diagrams (Phase 3+)

Design diagrams are in `diagrams/`:

| Type | Files |
|------|-------|
| Class diagrams | `01-domain-class-diagram.*`, `02-domain-overview.mmd`, `03-package-structure.mmd` |
| Use case diagram | `04-use-case-diagram.mmd` / `.puml` |
| Sequence diagrams | `05` Login, `06` Place Order, `07` List Car, `08` Write Review, `09` Approve Order |

Render Mermaid at https://mermaid.live and PlantUML at https://www.plantuml.com/plantuml. See `diagrams/README.md` for details and suggested report captions.

## UI theme (light / dark + motion)

The web UI ships with a light and a dark theme (toggle in the header; follows the OS setting until the visitor chooses, and the choice is remembered). Files in `src/main/resources/static/`:

- `theme.css` – design tokens for both themes plus the glass / aurora / animation skin (loads after `app.css`).
- `fx.js` – theme switch with circular reveal, scroll reveals, card tilt, count-up numbers. Respects `prefers-reduced-motion`.
- Delete the `theme.css` / `fx.js` lines in `index.html` to return to the original look.
