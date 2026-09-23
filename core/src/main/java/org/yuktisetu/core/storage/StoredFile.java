package org.yuktisetu.core.storage;

/**
 * The result of a successful upload.
 *
 * `storageKey` is the provider's identifier (Cloudinary's public_id) and is the
 * durable reference — it is what gets persisted. `url` is the delivery address;
 * it is persisted too, so that handing a link to an admin or writing one into an
 * exported sheet costs no call to the provider.
 */
public record StoredFile(
        String storageKey,
        String url,
        String contentType,
        long sizeBytes,
        String originalFileName
) {}
