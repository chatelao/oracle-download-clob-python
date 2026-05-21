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

class OracleConnector:
    """Manages connection lifecycle and executes SQL."""

    def __init__(self):
        self.conn: Optional[oracledb.Connection] = None
        self.config: Optional[DBConfig] = None

    def connect(self, config: DBConfig):
        """Establishes connection using python-oracledb."""
        self.config = config
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
        if self.conn:
            self.conn.close()
            self.conn = None

    def create_gtt(self, ids: List[str]):
        """Populates the Global Temporary Table with IDs."""
        if not self.conn or not self.config:
            raise RuntimeError("Database not connected")

        with self.conn.cursor() as cursor:
            # Attempt to create GTT if it doesn't exist
            try:
                cursor.execute(f"""
                    CREATE GLOBAL TEMPORARY TABLE {self.config.gtt_name} (
                        {self.config.id_column} VARCHAR2(255)
                    ) ON COMMIT PRESERVE ROWS
                """)
            except oracledb.DatabaseError as e:
                error_obj, = e.args
                if error_obj.code != 955: # ORA-00955: name is already used by an existing object
                    raise

            # Clear GTT for the current session
            cursor.execute(f"DELETE FROM {self.config.gtt_name}")

            # Bulk insert IDs
            data = [(id_val,) for id_val in ids]
            cursor.executemany(f"INSERT INTO {self.config.gtt_name} ({self.config.id_column}) VALUES (:1)", data)

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
            JOIN {self.config.gtt_name} g ON t.{self.config.id_column} = g.{self.config.id_column}
        """

        with self.conn.cursor() as cursor:
            cursor.execute(sql)
            while True:
                rows = cursor.fetchmany()
                if not rows:
                    break
                for row in rows:
                    yield row

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

    def update_clob(self, id: str, content: Union[str, TextIO]):
        """Updates a specific record with new CLOB data.
        Accepts either a string or a file-like object for streaming."""
        if not self.conn or not self.config:
            raise RuntimeError("Database not connected")

        sql = f"UPDATE {self.config.target_table} SET {self.config.clob_column} = :1 WHERE {self.config.id_column} = :2"
        with self.conn.cursor() as cursor:
            cursor.execute(sql, (content, id))

    def commit(self):
        """Commits the current transaction."""
        if self.conn:
            self.conn.commit()
