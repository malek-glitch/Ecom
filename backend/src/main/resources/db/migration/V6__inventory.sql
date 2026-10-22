-- Stock management (the inventory module, switched by the inventory.enabled setting).

alter table products
    -- false: the product is always available (made to order, digital...) and its stock is ignored.
    add column track_inventory     boolean not null default true,
    -- null: the store-wide threshold (setting inventory.lowStockThreshold).
    add column low_stock_threshold integer check (low_stock_threshold >= 0),
    -- What one unit costs the store, for the stock valuation. Admin only.
    add column cost_cents          bigint  check (cost_cents >= 0);

-- Every change of a product's stock, append-only. `quantity` is signed (+ in, - out) and
-- `stock_after` is the level right after the change, so the history reads like a bank statement.
create table stock_movements (
    id           uuid primary key,
    product_id   uuid         not null references products (id) on delete cascade,
    type         varchar(20)  not null,
    quantity     integer      not null,
    stock_after  integer      not null,
    note         varchar(300) not null default '',
    order_id     uuid references orders (id) on delete set null,
    order_number bigint,
    -- Snapshots, so the history survives deleted accounts.
    actor_id     uuid,
    actor_name   varchar(120),
    created_at   timestamptz  not null default now(),
    version      bigint       not null default 0
);

create index idx_stock_movements_product on stock_movements (product_id, created_at desc);
create index idx_stock_movements_created on stock_movements (created_at desc);

-- Opening balance for existing products, so each history starts from a known level.
insert into stock_movements (id, product_id, type, quantity, stock_after, note, created_at)
select gen_random_uuid(), id, 'INITIAL', stock, stock, 'Opening balance', now()
from products;
