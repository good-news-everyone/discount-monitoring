alter table users
    add column is_blocked_by boolean default false;
alter table users
    add column contact varchar;

create table messages
(
    id        bigserial,
    message   text,
    user_id   bigint references users,
    direction varchar,
    timestamp timestamp
)
