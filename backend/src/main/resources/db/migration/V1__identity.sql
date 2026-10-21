-- Accounts, sessions and single-use email tokens.
-- Migrations are immutable once applied: change the schema with a new V<n>__*.sql file.

create extension if not exists citext;

create table users (
    id                uuid primary key,
    email             citext       not null unique,
    password_hash     varchar(255),
    full_name         varchar(120) not null,
    role              varchar(20)  not null default 'USER' check (role in ('USER', 'ADMIN')),
    locale            varchar(8)   not null default 'fr',
    enabled           boolean      not null default true,
    email_verified_at timestamptz,
    google_sub        varchar(255) unique,
    avatar_key        varchar(512),
    security_version  integer      not null default 0,
    last_login_at     timestamptz,
    created_at        timestamptz  not null default now(),
    updated_at        timestamptz  not null default now(),
    version           bigint       not null default 0
);

create index idx_users_created_at on users (created_at);

-- One row per signed-in device.
create table user_sessions (
    id             uuid primary key,
    user_id        uuid        not null references users (id) on delete cascade,
    created_at     timestamptz not null,
    last_used_at   timestamptz not null,
    expires_at     timestamptz not null,
    user_agent     varchar(512),
    ip_address     varchar(64),
    revoked_at     timestamptz,
    revoked_reason varchar(40),
    version        bigint      not null default 0
);

create index idx_user_sessions_user on user_sessions (user_id);
create index idx_user_sessions_expires on user_sessions (expires_at);

-- Rotating refresh tokens (SHA-256 only). Rotated rows are kept for reuse detection.
create table refresh_tokens (
    id         uuid primary key,
    session_id uuid        not null references user_sessions (id) on delete cascade,
    token_hash varchar(64) not null unique,
    created_at timestamptz not null,
    rotated_at timestamptz,
    version    bigint      not null default 0
);

create index idx_refresh_tokens_session on refresh_tokens (session_id);

-- Email verification, password reset and invitation links (SHA-256 only).
create table user_tokens (
    id         uuid primary key,
    user_id    uuid        not null references users (id) on delete cascade,
    purpose    varchar(32) not null check (purpose in ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'INVITATION')),
    token_hash varchar(64) not null unique,
    created_at timestamptz not null,
    expires_at timestamptz not null,
    used_at    timestamptz,
    version    bigint      not null default 0
);

create index idx_user_tokens_user_purpose on user_tokens (user_id, purpose);
