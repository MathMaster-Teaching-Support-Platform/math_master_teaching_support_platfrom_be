CREATE TABLE IF NOT EXISTS diagram_cache (
    id          UUID        NOT NULL,
    latex_hash  VARCHAR(64) NOT NULL,
    latex_content TEXT,
    image_url   TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_diagram_cache PRIMARY KEY (id),
    CONSTRAINT uq_diagram_cache_hash UNIQUE (latex_hash)
);

CREATE INDEX IF NOT EXISTS idx_diagram_cache_hash ON diagram_cache (latex_hash);
