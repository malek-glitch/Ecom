# The shop

Liftoff ships with a small, complete online store: a public catalog, a cart, checkout, order
history for customers and order handling for administrators. It is meant to be adapted, so this
page explains how it fits together and where to change it.

## What customers see

| Page | Path | Notes |
|---|---|---|
| Store home | `/` | Featured products, newest products, categories. Server-rendered. |
| Catalog and search | `/products?q=&sort=&min=&max=&stock=1&page=` | Search results or every product, with filters, sort and pagination. Server-rendered. |
| Category | `/categories/<slug>` | The same list for one category, with its own title and description. Server-rendered. |
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

## Stock management (optional)

**Store → Inventory** shows every product's stock, what it is worth (at cost and at selling
price), and the history of every change. It's a module you can switch off: with
`inventory.enabled` off, the store sells without limits and shows no stock.

- **Adjustments**: *Receive* (a delivery), *Remove* (damaged, lost...) or *Count* (a stock-take:
  enter the real figure, the difference is recorded), each with an optional note.
- **History**: every change is a stock movement (opening stock, received, sold, returned, removed,
  counted) with the quantity, the level right after, the order or the person behind it, and the
  note. The movements of a product always add up to its stock.
- **Per product**: track stock or not (made-to-order or digital products are always available),
  its own low-stock level, and its unit cost for the stock value.
- **Alerts**: administrators get an email when a product reaches its low-stock level or sells out
  (`inventory.lowStockEmails`).
- **Export**: a CSV of the stock levels, for a stock-take or a spreadsheet. Cells that would start
  a formula are escaped.

Stock only changes through `InventoryService`, with atomic SQL updates: the `stock` column is
read-only for JPA. The product form sets the initial stock of a new product; afterwards stock is
changed on the inventory page, so a form saved late can never overwrite a sale.
For shoppers, a product's `stock` is `null` when it isn't tracked, and `lowStock` says when to
show "only N left".

## Search

The search box in the header suggests products and categories while the shopper types, offers a
correction for misspellings and remembers the last searches on the device. The results page adds
filters (category with counts, price range, in stock) and sorting by relevance.

It runs in PostgreSQL, with no extra service (`ProductSearchService`, migration `V5`):

- **Full text** on the name, SKU and description, each word matched as a prefix: "note" finds
  "notebook" while it's being typed. The `simple` text configuration doesn't stem words, which
  keeps it neutral across French, English and Arabic.
- **Typo tolerance** with trigram similarity (`pg_trgm`) on the name and SKU: "skilet" finds
  "Cast Iron Skillet". The threshold is `SearchTerms.TYPO_THRESHOLD`.
- **Accents ignored** both ways (`unaccent`): "creme" finds "Crème".
- **Category names count**: "kitchen" lists the kitchen products.
- **Ranking**: full-text rank, then closeness of the name, names starting with the query first,
  products in stock slightly ahead.
- **Did you mean**: when nothing matches, each word is replaced by the closest word of the
  catalog's vocabulary, and the correction is offered only if it finds something.
- **Insights**: each search (first page only) is counted by normalized query, with no personal
  data. The admin overview lists the top searches and the searches that found nothing; rows unseen
  for 180 days are deleted by the nightly job.

Words are reduced to letters and digits before reaching SQL, always as bound parameters. For very
large catalogs, synonyms or per-language stemming, put a dedicated engine (Meilisearch,
OpenSearch...) behind `ProductSearchService`: the API and the pages don't change.

## Search engines (SEO)

- Store pages are server-rendered, with a title, description, canonical URL, Open Graph and
  Twitter tags on each (`lib/components/app/Seo.svelte`).
- Products and categories have optional **SEO title and description** fields; the admin form shows
  a preview of the search result.
- Structured data (JSON-LD): `Product` with its offer, price and availability, `BreadcrumbList`
  on product and category pages, `ItemList` on the catalog, `Organization` and `WebSite` with a
  search box on the home page.
- `/sitemap.xml` lists the home page, the catalog, categories and products (with photos), built
  from the live catalog; `/robots.txt` points to it and keeps crawlers out of private pages.
- Search results, filtered lists, the cart, checkout and every signed-in page are `noindex`.
- Categories have clean URLs (`/categories/<slug>`); old `/products?category=` links redirect.

## Appearance

**Store → Appearance** sets the brand colour, corner roundness, heading font, product photo shape,
logo, announcement bar, home page texts and picture, footer text and social links, with a live
preview. They are runtime settings (see [CONFIGURATION.md](CONFIGURATION.md#runtime-settings)):
`lib/theme/theme.ts` turns them into CSS variables in the page head at render time, so there is no
rebuild and no flash of the default colours.

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
4. With stock tracking on, stock is taken with one conditional `UPDATE ... SET stock = stock - n
   WHERE stock >= n` per tracked product, inside the order's transaction, and recorded as a `SOLD`
   movement. If any line is short, the whole order rolls back with `409 order.out_of_stock`. Two
   customers can't buy the last item.
5. Cancelling an order (by the customer while `PENDING`, or by an admin) puts back what the order
   took, going by its movements, so it is right even if tracking was switched since.

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
| `/api/v1/admin/inventory/summary`, `/products`, `/products/{id}/adjustments`, `/products/{id}/settings`, `/movements`, `/export` | ADMIN |

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
