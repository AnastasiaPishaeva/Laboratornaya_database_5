CREATE EXTENSION IF NOT EXISTS dblink;

CREATE OR REPLACE PROCEDURE sp_create_database(dbname TEXT)
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM dblink_exec(
      'host=localhost dbname=postgres user=admin_role password=admin',
      'CREATE DATABASE ' || quote_ident(dbname)
    );
END;
$$;

CREATE OR REPLACE PROCEDURE sp_drop_database(dbname TEXT)
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM dblink_exec(
      'host=localhost dbname=postgres user=admin_role password=admin',
      'DROP DATABASE IF EXISTS ' || quote_ident(dbname)
    );
END;
$$;

CREATE OR REPLACE PROCEDURE sp_create_table()
LANGUAGE plpgsql
AS $$
BEGIN
    EXECUTE '
        CREATE TABLE IF NOT EXISTS cars (
            id SERIAL PRIMARY KEY,
            brand TEXT NOT NULL,
            model TEXT NOT NULL,
            year INTEGER NOT NULL,
            price NUMERIC(10,2) NOT NULL
        )';
END;
$$;

CREATE OR REPLACE PROCEDURE sp_clear_table()
LANGUAGE plpgsql
AS $$
BEGIN
    EXECUTE 'TRUNCATE TABLE cars';
END;
$$;


CREATE OR REPLACE PROCEDURE sp_insert_car(p_brand TEXT, p_model TEXT, p_year INTEGER, p_price NUMERIC)
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO cars (brand, model, year, price) VALUES (p_brand, p_model, p_year, p_price);
END;
$$;

DROP FUNCTION IF EXISTS sp_search_car(TEXT);

CREATE OR REPLACE FUNCTION sp_search_car(_model TEXT)
RETURNS TABLE(id INTEGER, brand TEXT, model TEXT, year INTEGER, price NUMERIC) AS $$
BEGIN
    RETURN QUERY
    SELECT cars.id, cars.brand, cars.model, cars.year, cars.price
    FROM cars
    WHERE cars.model = _model;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE PROCEDURE sp_update_car(p_id INTEGER, p_brand TEXT, p_model TEXT, p_year INTEGER, p_price NUMERIC)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE cars SET brand = p_brand, model = p_model, year = p_year, price = p_price WHERE id = p_id;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_delete_car_by_model(p_model TEXT)
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM cars WHERE model = p_model;
END;
$$;

CREATE OR REPLACE FUNCTION sp_view_cars()
RETURNS TABLE(id INTEGER, brand TEXT, model TEXT, year INTEGER, price NUMERIC)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY SELECT * FROM cars ORDER BY id;
END;
$$;

