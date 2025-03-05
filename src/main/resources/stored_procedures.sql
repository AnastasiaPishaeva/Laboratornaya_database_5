
CREATE EXTENSION IF NOT EXISTS dblink;

-- BEGIN SYSTEM PROCEDURES
-- Процедура для создания базы данных (параметр TEXT)
CREATE OR REPLACE PROCEDURE sp_create_database(dbname TEXT)
LANGUAGE plpgsql
AS $$
BEGIN
    -- Передаём параметры аутентификации в строке подключения для dblink_exec
    PERFORM dblink_exec(
      'host=localhost dbname=postgres user=admin_role password=admin',
      'CREATE DATABASE ' || quote_ident(dbname)
    );
END;
$$;

-- Процедура для удаления базы данных (параметр TEXT)
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

-- Назначение прав для системных процедур
GRANT EXECUTE ON PROCEDURE sp_create_database(TEXT) TO admin_role;
GRANT EXECUTE ON PROCEDURE sp_drop_database(TEXT) TO admin_role;
-- END SYSTEM PROCEDURES

-- BEGIN CAR_RENTAL PROCEDURES
-- Процедура для создания таблицы
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

-- Процедура для очистки таблицы
CREATE OR REPLACE PROCEDURE sp_clear_table()
LANGUAGE plpgsql
AS $$
BEGIN
    EXECUTE 'TRUNCATE TABLE cars';
END;
$$;

-- Процедура для вставки новой записи (автомобиля)
CREATE OR REPLACE PROCEDURE sp_insert_car(p_brand TEXT, p_model TEXT, p_year INTEGER, p_price NUMERIC)
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO cars (brand, model, year, price) VALUES (p_brand, p_model, p_year, p_price);
END;
$$;

-- Функция для поиска записей по полю model

DROP FUNCTION IF EXISTS sp_search_car(TEXT);

CREATE OR REPLACE FUNCTION sp_search_car(_model TEXT)
RETURNS TABLE(id INTEGER, brand TEXT, model TEXT, year INTEGER, price NUMERIC) AS $$
BEGIN
    RETURN QUERY
    SELECT cars.id, cars.brand, cars.model, cars.year, cars.price  -- Указываем имя таблицы явно
    FROM cars
    WHERE cars.model = _model;  -- Не путать model с параметром
END;
$$ LANGUAGE plpgsql;



-- Процедура для обновления записи (по id)
CREATE OR REPLACE PROCEDURE sp_update_car(p_id INTEGER, p_brand TEXT, p_model TEXT, p_year INTEGER, p_price NUMERIC)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE cars SET brand = p_brand, model = p_model, year = p_year, price = p_price WHERE id = p_id;
END;
$$;

-- Процедура для удаления записи по значению поля model
CREATE OR REPLACE PROCEDURE sp_delete_car_by_model(p_model TEXT)
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM cars WHERE model = p_model;
END;
$$;

-- Функция для просмотра всех записей из таблицы
CREATE OR REPLACE FUNCTION sp_view_cars()
RETURNS TABLE(id INTEGER, brand TEXT, model TEXT, year INTEGER, price NUMERIC)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY SELECT * FROM cars ORDER BY id;
END;
$$;

-- Назначение прав для процедур и функций аренды автомобилей
GRANT EXECUTE ON PROCEDURE sp_create_table() TO admin_role;
GRANT EXECUTE ON PROCEDURE sp_clear_table() TO admin_role;
GRANT EXECUTE ON PROCEDURE sp_insert_car(TEXT, TEXT, INTEGER, NUMERIC) TO admin_role;
GRANT EXECUTE ON FUNCTION sp_search_car(TEXT) TO admin_role;
GRANT EXECUTE ON PROCEDURE sp_update_car(INTEGER, TEXT, TEXT, INTEGER, NUMERIC) TO admin_role;
GRANT EXECUTE ON PROCEDURE sp_delete_car_by_model(TEXT) TO admin_role;
GRANT EXECUTE ON FUNCTION sp_view_cars() TO admin_role;

GRANT EXECUTE ON FUNCTION sp_search_car(TEXT) TO guest_role;
GRANT EXECUTE ON FUNCTION sp_view_cars() TO guest_role;
-- END CAR_RENTAL PROCEDURES
