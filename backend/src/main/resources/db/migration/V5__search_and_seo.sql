-- Store search: full-text matching (with prefixes) plus trigram similarity for typos, both
-- accent-insensitive. Both extensions ship with PostgreSQL and are "trusted" (no superuser needed).
create extension if not exists pg_trgm;
create extension if not exists unaccent;

-- unaccent() is only STABLE because its dictionary could change; pinning the dictionary makes it
-- usable in generated columns and indexes. Schema-qualified so it works whatever the search_path.
create or replace function immutable_unaccent(text) returns text
    language sql immutable parallel safe strict
as $$ select public.unaccent('public.unaccent'::regdictionary, $1) $$;

alter table products
    -- Optional overrides for search engines; empty means "use the name and description".
    add column seo_title       varchar(70),
    add column seo_description varchar(200),
    -- Lower-cased, accent-free name and SKU: what typo-tolerant matching compares against.
    add column search_text     text generated always as (lower(immutable_unaccent(name || ' ' || coalesce(sku, '')))) stored,
    -- Words of the name and SKU (weight A) and description (weight C). 'simple' doesn't stem,
    -- which keeps it language-neutral (French, English and Arabic share one catalog).
    add column search_vector   tsvector generated always as (
        setweight(to_tsvector('simple', immutable_unaccent(name)), 'A') ||
        setweight(to_tsvector('simple', immutable_unaccent(coalesce(sku, ''))), 'A') ||
        setweight(to_tsvector('simple', immutable_unaccent(description)), 'C')
    ) stored;

create index idx_products_search_vector on products using gin (search_vector);
create index idx_products_search_text_trgm on products using gin (search_text gin_trgm_ops);
create index idx_products_active_price on products (active, price_cents);

alter table categories
    add column seo_title       varchar(70),
    add column seo_description varchar(200);

-- What shoppers search for, to spot missing products ("searches with no results").
create table search_queries (
    query             varchar(100) primary key,
    searches          bigint      not null default 0,
    last_result_count integer     not null default 0,
    first_searched_at timestamptz not null default now(),
    last_searched_at  timestamptz not null default now()
);

create index idx_search_queries_last on search_queries (last_searched_at desc);
