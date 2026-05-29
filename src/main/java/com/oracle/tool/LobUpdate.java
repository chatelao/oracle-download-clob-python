package com.oracle.tool;

/**
 * Represents a pending LOB update containing the record ID,
 * the content to upload, and the stream to be closed after processing.
 *
 * @param id      The database record ID.
 * @param content The content (Reader, InputStream, or other).
 * @param stream  The stream to close (usually same as content).
 */
public record LobUpdate(String id, Object content, AutoCloseable stream) {
}
