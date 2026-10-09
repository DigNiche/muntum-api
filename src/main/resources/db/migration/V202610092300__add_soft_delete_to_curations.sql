ALTER TABLE curations
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN deleted_by BINARY(16) NULL;

ALTER TABLE curations
    ADD COLUMN active_unique_key TINYINT
        GENERATED ALWAYS AS (
            CASE
                WHEN deleted_at IS NULL THEN 1
                ELSE NULL
                END
            ) VIRTUAL,
    ADD UNIQUE INDEX uk_curations_active_program_curator (
        program_id,
        curator_id,
        active_unique_key
    );

ALTER TABLE curations
DROP INDEX uk_program_curations_program_curator;