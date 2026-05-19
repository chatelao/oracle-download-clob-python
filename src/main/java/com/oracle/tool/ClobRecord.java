package com.oracle.tool;

import java.sql.Clob;

/**
 * Represents a record retrieved from the database containing an ID and a CLOB.
 */
public record ClobRecord(String id, Clob clob) {
}
