-- The shop: catalog (categories, products) and orders. Prices are integer minor units
-- (cents) in the store currency (setting `shop.currency`); orders keep their own copy.

create table categories (
    id          uuid primary key,
    slug        varchar(80)  not null unique,
    name        varchar(120) not null,
    description text         not null default '',
    position    integer      not null default 0,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now(),
    version     bigint       not null default 0
);

create table products (
    id                     uuid primary key,
    slug                   varchar(120) not null unique,
    name                   varchar(200) not null,
    description            text         not null default '',
    price_cents            bigint       not null check (price_cents >= 0),
    -- "Was" price shown struck through; null when the product isn't on sale.
    compare_at_price_cents bigint check (compare_at_price_cents >= 0),
    sku                    varchar(64) unique,
    stock                  integer      not null default 0 check (stock >= 0),
    active                 boolean      not null default true,
    featured               boolean      not null default false,
    category_id            uuid references categories (id) on delete set null,
    image_key              varchar(512),
    created_at             timestamptz  not null default now(),
    updated_at             timestamptz  not null default now(),
    version                bigint       not null default 0
);

create index idx_products_category on products (category_id);
create index idx_products_active_created on products (active, created_at desc);

-- Human-friendly order numbers (#1001, #1002...), separate from the uuid primary key.
create sequence order_number_seq start with 1001;

create table orders (
    id                uuid primary key,
    number            bigint       not null unique,
    -- Kept when the account is deleted: orders are business records.
    customer_id       uuid references users (id) on delete set null,
    email             varchar(254) not null,
    status            varchar(20)  not null,
    payment_method    varchar(30)  not null,
    currency          varchar(3)   not null,
    subtotal_cents    bigint       not null,
    shipping_cents    bigint       not null,
    total_cents       bigint       not null,
    ship_full_name    varchar(120) not null,
    ship_phone        varchar(40)  not null default '',
    ship_line1        varchar(200) not null,
    ship_line2        varchar(200) not null default '',
    ship_city         varchar(120) not null,
    ship_postal_code  varchar(20)  not null,
    ship_country      varchar(80)  not null,
    customer_note     varchar(1000) not null default '',
    created_at        timestamptz  not null default now(),
    updated_at        timestamptz  not null default now(),
    version           bigint       not null default 0
);

create index idx_orders_customer_created on orders (customer_id, created_at desc);
create index idx_orders_status_created on orders (status, created_at desc);

create table order_items (
    id               uuid primary key,
    order_id         uuid         not null references orders (id) on delete cascade,
    -- A snapshot: the product may change or disappear later.
    product_id       uuid references products (id) on delete set null,
    product_name     varchar(200) not null,
    product_slug     varchar(120) not null,
    sku              varchar(64),
    unit_price_cents bigint       not null,
    quantity         integer      not null check (quantity > 0),
    line_total_cents bigint       not null,
    position         integer      not null,
    version          bigint       not null default 0
);

create index idx_order_items_order on order_items (order_id);
create index idx_order_items_product on order_items (product_id);
