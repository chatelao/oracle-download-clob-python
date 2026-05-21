import oracledb
import sys

try:
    print("Attempting to connect with LDAP DSN...")
    oracledb.connect(
        user="system",
        password="password",
        dsn="ldap://localhost:10389/FREEPDB1,cn=OracleContext,dc=example,dc=com"
    )
except Exception as e:
    print(f"Caught expected exception: {e}")
    # If it tried to connect to LDAP, the error should mention it.
