-- Runtime settings: a row exists only when an admin changed the default (see Setting.kt).
create table app_settings (
    key        varchar(100) primary key,
    value      text        not null,
    updated_at timestamptz not null,
    updated_by uuid
);

-- Append-only audit trail. No foreign keys: history must survive user deletion.
create table audit_events (
    id           uuid primary key,
    occurred_at  timestamptz not null,
    actor_id     uuid,
    actor_email  varchar(254),
    action       varchar(64) not null,
    target_type  varchar(32),
    target_id    varchar(64),
    target_label varchar(254),
    ip_address   varchar(64),
    details      text,
    version      bigint      not null default 0
);

create index idx_audit_events_occurred on audit_events (occurred_at desc);
create index idx_audit_events_action on audit_events (action);
