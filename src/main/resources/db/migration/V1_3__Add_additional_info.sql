alter table items
    add column additional_info jsonb default '{}';

create table if not exists additional_info_changelog
(
    id           bigserial primary key,
    item_id      bigint,
    time_checked timestamp,
    info_before  jsonb,
    info_now     jsonb
);

alter table additional_info_changelog
    add constraint additional_info_item_id_fkey foreign key (item_id)
        references items (id) match simple
        on update cascade on delete cascade;
