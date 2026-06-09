import oracledb
from typing import List, Iterator, Tuple, Any, Optional, Union, TextIO
from dataclasses import dataclass

@dataclass
class DBConfig:
    user: str
    password: str
    dsn: str
    target_table: str
    id_column: str
    clob_column: str
    gtt_name: str = "GTT_IDS"
    query: Optional[str] = None
    filename_column: Optional[str] = None
    id_query: Optional[str] = None

class OracleConnector:
    """Manages connection lifecycle and executes SQL."""

    def __init__(self):
        self.conn: Optional[oracledb.Connection] = None
        self.config: Optional[DBConfig] = None
        self._cached_update_sql: Optional[str] = None

    def connect(self, config: DBConfig):
        """Establishes connection using python-oracledb."""
        self.config = config
        self._cached_update_sql = None
        try:
            self.conn = oracledb.connect(
                user=config.user,
                password=config.password,
                dsn=config.dsn
            )
        except oracledb.Error as e:
            raise RuntimeError(f"Failed to connect to Oracle database: {e}") from e

    def close(self):
        """Closes the database connection."""
        self._cached_update_sql = None
        if self.conn:
            self.conn.close()
            self.conn = None

    def create_gtt(self, ids: List[str]):
        """Populates the Global Temporary Table with IDs."""
        if not self.conn or not self.config:
            raise RuntimeError("Database not connected")

        with self.conn.cursor() as cursor:
            # Check if GTT exists
            cursor.execute("SELECT count(*) FROM user_tables WHERE table_name = :1", (self.config.gtt_name.upper(),))
            exists = cursor.fetchone()[0] > 0

            if not exists:
                cursor.execute(f"""
                    CREATE GLOBAL TEMPORARY TABLE {self.config.gtt_name} (
                        ID_VAL VARCHAR2(255)
                    ) ON COMMIT PRESERVE ROWS
                """)

            # Clear GTT for the current session
            cursor.execute(f"DELETE FROM {self.config.gtt_name}")

            # Bulk insert IDs
            data = [(id_val,) for id_val in ids]
            cursor.executemany(f"INSERT INTO {self.config.gtt_name} (ID_VAL) VALUES (:1)", data)

    def fetch_clobs_join(self) -> Iterator[Tuple[str, Any, Optional[str]]]:
        """Executes the JOIN query and yields (ID, LOB, Filename)."""
        if not self.conn or not self.config:
            raise RuntimeError("Database not connected")

        source = f"({self.config.query})" if self.config.query else self.config.target_table

        columns = [f"t.{self.config.id_column}", f"t.{self.config.clob_column}"]
        if self.config.filename_column:
            columns.append(f"t.{self.config.filename_column}")

        sql = f"""
            SELECT {', '.join(columns)}
            FROM {source} t
            JOIN {self.config.gtt_name} g ON t.{self.config.id_column} = g.ID_VAL
        """

        with self.conn.cursor() as cursor:
            cursor.execute(sql)
            while True:
                rows = cursor.fetchmany()
                if not rows:
                    break
                for row in rows:
                    yield row

    def fetch_ids(self, query: str) -> List[str]:
        """Fetches a list of IDs from the database using the provided query."""
        if not self.conn:
            raise RuntimeError("Database not connected")

        ids = []
        with self.conn.cursor() as cursor:
            cursor.execute(query)
            for row in cursor:
                if row and row[0]:
                    ids.append(str(row[0]))
        return ids

    def fetch_clobs_in(self, ids: List[str]) -> Iterator[Tuple[str, Any, Optional[str]]]:
        """Executes query with IN clause and yields (ID, LOB, Filename)."""
        if not self.conn or not self.config:
            raise RuntimeError("Database not connected")

        if not ids:
            return

        source = f"({self.config.query})" if self.config.query else self.config.target_table
        binds = [f":{i+1}" for i in range(len(ids))]

        columns = [self.config.id_column, self.config.clob_column]
        if self.config.filename_column:
            columns.append(self.config.filename_column)

        sql = f"""
            SELECT {', '.join(columns)}
            FROM {source}
            WHERE {self.config.id_column} IN ({', '.join(binds)})
        """

        with self.conn.cursor() as cursor:
            cursor.execute(sql, ids)
            while True:
                rows = cursor.fetchmany()
                if not rows:
                    break
                for row in rows:
                    yield row

    def update_lob(self, id: str, content: Union[str, bytes, TextIO, Any]) -> int:
        """Updates a specific record with new LOB data.
        Accepts a string, bytes, or a file-like object for streaming.
        Returns the number of rows affected."""
        if not self.conn or not self.config:
            raise RuntimeError("Database not connected")

        sql = self._get_update_sql()
        with self.conn.cursor() as cursor:
            cursor.execute(sql, (content, id))
            return cursor.rowcount

    def update_clob(self, id: str, content: Union[str, TextIO]) -> int:
        """Alias for update_lob for backward compatibility.
        Returns the number of rows affected."""
        return self.update_lob(id, content)

    def get_lob_column_type(self) -> "oracledb.DbType":
        """Determines the database type of the LOB column."""
        if not self.conn or not self.config:
            raise RuntimeError("Database not connected")

        source = f"({self.config.query})" if self.config.query else self.config.target_table
        sql = f"SELECT {self.config.clob_column} FROM {source} WHERE 1=0"
        with self.conn.cursor() as cursor:
            cursor.execute(sql)
            return cursor.description[0][1]

    def get_lob_column_type_name(self) -> str:
        """Determines the database type name of the LOB column."""
        if not self.conn or not self.config:
            raise RuntimeError("Database not connected")

        source = f"({self.config.query})" if self.config.query else self.config.target_table
        sql = f"SELECT {self.config.clob_column} FROM {source} WHERE 1=0"
        with self.conn.cursor() as cursor:
            cursor.execute(sql)
            # cursor.description[0][1] is the type.
            db_type = cursor.description[0][1]
            # oracledb DbType objects have a name property or can be stringified.
            if hasattr(db_type, "name"):
                return db_type.name
            return str(db_type)

    def commit(self):
        """Commits the current transaction."""
        if self.conn:
            self.conn.commit()

    def _get_update_sql(self) -> str:
        if self._cached_update_sql:
            return self._cached_update_sql

        if not self.conn or not self.config:
            raise RuntimeError("Database not connected")

        db_type = self.get_lob_column_type()
        is_xml_type = False
        if hasattr(oracledb, "DB_TYPE_XMLTYPE") and db_type == oracledb.DB_TYPE_XMLTYPE:
            is_xml_type = True
        elif str(db_type).upper().endswith("XMLTYPE"):
            is_xml_type = True

        if is_xml_type:
            self._cached_update_sql = (
                f"UPDATE {self.config.target_table} "
                f"SET {self.config.clob_column} = XMLTYPE(:1) "
                f"WHERE {self.config.id_column} = :2"
            )
        else:
            self._cached_update_sql = (
                f"UPDATE {self.config.target_table} "
                f"SET {self.config.clob_column} = :1 "
                f"WHERE {self.config.id_column} = :2"
            )
        return self._cached_update_sql
