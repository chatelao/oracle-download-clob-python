package com.oracle.tool;

/**
 * Represents a record retrieved from the database containing an ID, a LOB (CLOB or BLOB),
 * and an optional filename.
 */
public record LobRecord(String id, Object lob, String filename) {
}
