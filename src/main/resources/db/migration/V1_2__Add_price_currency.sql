alter table items
    add column if not exists price_currency varchar default 'RUB';

ALTER TABLE price_changelog
    DROP CONSTRAINT price_changelog_item_id_fkey;
ALTER TABLE price_changelog
    ADD CONSTRAINT price_changelog_item_id_fkey FOREIGN KEY (item_id)
        REFERENCES items (id) MATCH SIMPLE
        ON UPDATE CASCADE ON DELETE CASCADE;
