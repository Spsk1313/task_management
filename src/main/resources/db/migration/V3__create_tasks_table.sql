CREATE TYPE task_priority AS ENUM ('HIGH', 'MEDIUM', 'LOW');

CREATE TYPE task_status AS ENUM ('TODO', 'IN_PROGRESS', 'BLOCKED', 'DONE');

CREATE TABLE tasks (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title varchar(255) NOT NULL,
    description TEXT,
    project_id bigint NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    priority task_priority NOT NULL,
    status task_status NOT NULL,
    due_date DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);