create table if not exists items(
    id bigserial primary key,
    name varchar,
    url varchar,
    type varchar,
    initial_price decimal,
    lowest_price decimal,
    highest_price decimal,
    current_price decimal,
    time_added timestamp default now()
);

create table if not exists price_changelog(
    id bigserial primary key,
    item_id bigint references items(id),
    time_checked timestamp,
    price_change varchar,
    price_before decimal,
    price_now decimal
);

create table if not exists users(
    id bigserial primary key,
    first_name varchar,
    is_bot boolean,
    chat_id bigint,
    last_name varchar,
    user_name varchar
);

create table if not exists item_subscribers(
    id bigserial primary key,
    item_id bigint references items(id),
    user_id bigint references users(id)
);
