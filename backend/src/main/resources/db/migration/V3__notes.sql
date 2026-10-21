-- Example feature (see note/). Removed by `node scripts/init.mjs --remove-example`.
create table notes (
    id         uuid primary key,
    owner_id   uuid         not null references users (id) on delete cascade,
    title      varchar(200) not null,
    content    text         not null default '',
    pinned     boolean      not null default false,
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now(),
    version    bigint       not null default 0
);

create index idx_notes_owner_updated on notes (owner_id, pinned desc, updated_at desc);
