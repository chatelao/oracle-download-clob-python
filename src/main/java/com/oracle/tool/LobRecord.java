package com.oracle.tool;

/**
 * Represents a record retrieved from the database containing an ID and a LOB (CLOB or BLOB).
 */
public record LobRecord(String id, Object lob) {
}
