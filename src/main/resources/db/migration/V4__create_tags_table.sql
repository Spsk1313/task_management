CREATE TABLE tags
(
    id   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(50) NOT NULL UNIQUE
);

CREATE TABLE task_tags
(
    task_id bigint REFERENCES tasks (id) ON DELETE CASCADE,
    tag_id  bigint REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, tag_id)
);