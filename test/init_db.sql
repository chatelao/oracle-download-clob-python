-- test/init_db.sql
-- Create a test table for CLOB operations

DROP TABLE CLOB_DATA;

CREATE TABLE CLOB_DATA (
    ID VARCHAR2(255) PRIMARY KEY,
    CONTENT CLOB
);

INSERT INTO CLOB_DATA (ID, CONTENT) VALUES ('1', 'Initial content for ID 1');
INSERT INTO CLOB_DATA (ID, CONTENT) VALUES ('2', 'Initial content for ID 2');
INSERT INTO CLOB_DATA (ID, CONTENT) VALUES ('3', 'Initial content for ID 3');

COMMIT;

EXIT;
