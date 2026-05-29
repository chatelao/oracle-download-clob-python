package com.oracle.tool;

/**
 * Encapsulates data for a LOB update operation.
 *
 * @param id      The record ID to update.
 * @param content The LOB content (can be a Reader, InputStream, or other Object).
 */
public record LobUpdate(String id, Object content) {
}
