BEGIN;

CREATE TABLE app_user (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login varchar(80) NOT NULL UNIQUE CHECK (length(trim(login)) > 0),
    password_hash text NOT NULL
);

CREATE TABLE product (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name text NOT NULL,
    volume_ml integer NOT NULL CHECK (volume_ml > 0),
    UNIQUE (name, volume_ml)
);

CREATE TABLE batch (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    number varchar(40) NOT NULL UNIQUE CHECK (length(trim(number)) > 0),
    bottled_on date NOT NULL,
    product_id bigint NOT NULL REFERENCES product,
    filled integer NOT NULL CHECK (filled > 0),
    rejected integer NOT NULL CHECK (rejected BETWEEN 0 AND filled)
);

CREATE TABLE quality_check (
    batch_id bigint PRIMARY KEY REFERENCES batch,
    approved boolean NOT NULL,
    comment varchar(300) NOT NULL DEFAULT '',
    checked_at timestamptz NOT NULL DEFAULT now(),
    CHECK (approved OR length(trim(comment)) > 0)
);

CREATE TABLE receipt (
    batch_id bigint PRIMARY KEY REFERENCES batch,
    quantity integer NOT NULL CHECK (quantity > 0),
    received_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO product (name, volume_ml) VALUES
    ('Вода негазированная', 500),
    ('Вода негазированная', 1500);

COMMIT;
