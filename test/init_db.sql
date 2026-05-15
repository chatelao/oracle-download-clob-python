-- test/init_db.sql
-- Setup test schema for integration testing

SET ECHO ON
SET FEEDBACK ON

-- Create the test user
DECLARE
    l_count NUMBER;
BEGIN
    SELECT count(*) INTO l_count FROM dba_users WHERE username = 'TEST_USER';
    IF l_count > 0 THEN
        EXECUTE IMMEDIATE 'DROP USER TEST_USER CASCADE';
    END IF;
END;
/

CREATE USER TEST_USER IDENTIFIED BY test_password;
GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE TO TEST_USER;
ALTER USER TEST_USER QUOTA UNLIMITED ON USERS;

-- Create target table in TEST_USER schema
CREATE TABLE TEST_USER.CLOB_DATA (
    ID VARCHAR2(255) PRIMARY KEY,
    DATA_CONTENT CLOB
);

-- Insert some initial data
INSERT INTO TEST_USER.CLOB_DATA (ID, DATA_CONTENT) VALUES ('1', 'Initial content for ID 1');
INSERT INTO TEST_USER.CLOB_DATA (ID, DATA_CONTENT) VALUES ('2', 'Initial content for ID 2');
INSERT INTO TEST_USER.CLOB_DATA (ID, DATA_CONTENT) VALUES ('3', 'Initial content for ID 3');

COMMIT;

EXIT;
