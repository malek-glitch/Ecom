# The shop

Liftoff ships with a small, complete online store: a public catalog, a cart, checkout, order
history for customers and order handling for administrators. It is meant to be adapted, so this
page explains how it fits together and where to change it.

## What customers see

| Page | Path | Notes |
|---|---|---|
| Store home | `/` | Featured products, newest products, categories. Server-rendered. |
| Catalog | `/products?q=&category=&sort=&page=` | Search, category filter, sort, pagination. Server-rendered. |
| Product | `/products/<slug>` | Photo, price (with a struck-through "was" price on sale), stock, add to cart. Server-rendered. |
| Cart | `/cart` | Kept in `localStorage`, so it works without an account. Prices are re-checked with the API on every change. |
| Checkout | `/checkout` | Needs an account: unsigned visitors go through `/login` and come back. Address, payment method, note. |
| My orders | `/orders`, `/orders/<id>` | Status, items, totals, bank details for transfers. A `PENDING` order can be cancelled. |

Emails (in the customer's language): order confirmation, shipped, cancelled.

## What administrators see

Under **Store** in the sidebar:

- **Orders**: search by number, email or name; filter by status; move an order to its next status.
- **Products**: create, edit, hide or delete products; upload a photo (with `STORAGE_ENABLED=true`).
- **Categories**: create, rename, reorder, delete (products stay, without a category).

The admin overview adds revenue and order counts for the last 30 days, orders waiting to be
handled and low-stock products. Store settings (currency, shipping fee, free-shipping threshold,
bank details) are in **App settings** ([CONFIGURATION.md](CONFIGURATION.md#runtime-settings)).
Product, category and order-status changes are written to the audit log.

## How it works

### Money

Amounts are integers in the currency's **minor units** (cents for EUR and USD, whole yen for JPY,
millimes for TND), named `...Cents` in the API. There is no floating point anywhere. The store
currency is the `shop.currency` setting; each order copies the currency it was placed in.
`formatMoney(minor, currency)` (`lib/i18n/format.ts`) and `parseAmount` (`lib/shop/money.ts`) do
the conversions in the frontend.

### Cart and checkout

1. The cart is a list of `{ productId, quantity }` (plus a name and price snapshot for instant
   display) in `localStorage`, managed by `lib/shop/cart.svelte.ts`.
2. `POST /api/v1/public/shop/cart/quote` prices it from the database: lines, subtotal, shipping,
   total, products no longer sold, and whether every line is in stock (`orderable`).
3. `POST /api/v1/orders` places the order. The server prices everything again and never trusts
   the client's numbers. The client sends the total it showed (`expectedTotalCents`); if prices
   changed meanwhile the order is refused with `409 order.price_changed` rather than charging a
   different amount.
4. Stock is taken with one conditional `UPDATE ... SET stock = stock - n WHERE stock >= n` per
   product, inside the order's transaction. If any line is short, the whole order rolls back with
   `409 order.out_of_stock`. Two customers can't buy the last item.
5. Cancelling an order (by the customer while `PENDING`, or by an admin) puts the items back in
   stock.

### Order statuses

```
PENDING ──► PAID ──► SHIPPED ──► DELIVERED
   │          │
   │          └────► CANCELLED
   ├────────► SHIPPED            (cash on delivery: ship first, get paid at the door)
   └────────► CANCELLED
```

The allowed moves live in `OrderStatus.kt`; the API returns `nextStatuses` with each order so
the admin page only offers valid buttons.

### Orders are snapshots

An order copies the product names, SKUs, prices, the address and the currency. Editing or deleting
a product, or deleting the customer's account, never rewrites past orders (`ON DELETE SET NULL`).
To stop selling a product but keep it linked to past orders, untick **Visible in the store**
instead of deleting it.

## API

| Endpoint | Who |
|---|---|
| `GET /api/v1/public/shop/home`, `/categories`, `/products`, `/products/{slug}` | anyone |
| `POST /api/v1/public/shop/cart/quote` | anyone (rate limit `cart`) |
| `POST /api/v1/orders`, `GET /api/v1/orders`, `GET /api/v1/orders/{id}`, `POST /api/v1/orders/{id}/cancel` | signed-in customer, own orders only (others are 404) |
| `/api/v1/admin/shop/products`, `/categories`, `/orders`, `/orders/{id}/status`, `/stats` | ADMIN |

The full contract is in Swagger UI (`/swagger-ui.html`), tags *Store*, *Orders*, *Admin: catalog*
and *Admin: orders*.

## Demo data

`SHOP_SEED_DEMO=true` (the `.env.example` default) fills an **empty** catalog with four categories
and twelve products at startup, so a fresh install has something to show. It never touches a
catalog that already has products. Leave it off in production.

## Common changes

**Take card payments.** Payment is offline today: cash on delivery or bank transfer, and an admin
marks orders `PAID`. To add Stripe (or PayPal, Mollie...):

1. add a value to `PaymentMethod` and its label in `lib/shop/labels.ts` and the three message files;
2. in `OrderService.place`, create the provider's checkout session or payment intent after the
   order is saved, and return its URL or client secret with the order;
3. on the provider's webhook (a new public, signature-checked endpoint), move the order to `PAID`;
4. add the provider's domains to the CSP in `svelte.config.js` if you use its hosted fields.

Keep the order as the source of truth: create it `PENDING` first, then pay for it.

**Taxes.** Prices are treated as tax-inclusive. For tax added at checkout, compute it in
`PricingService` next to shipping and store it on the order (a new migration column).

**Shipping rules.** `PricingService.shipping` is the only place that decides the fee. Replace the
flat fee with weight, zones or carriers there; the cart and checkout pick it up through the quote.

**Guest checkout.** Orders already keep their own email. To allow guests, accept an email in
`PlaceOrderRequest`, make `customer_id` optional for those orders (it already is in the schema),
add the endpoint to the public paths and give guests an order link with a signed token.

**Several photos per product.** Add a `product_images` table (product id, key, position), move
`ProductImageService` to it and show a gallery on the product page.

**Variants (size, colour).** Add a `product_variants` table with its own SKU, price and stock, put
the variant id in cart lines and order items, and take stock from the variant.
