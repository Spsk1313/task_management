CREATE TABLE projects
(
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        varchar(100) NOT NULL,
    owner_id    bigint       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    UNIQUE (owner_id, name)
);