package org.yuktisetu.core.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import org.yuktisetu.core.exception.BadRequestException;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Uploads a file and returns its durable key plus a delivery URL.
 *
 * Delivery is deliberately PUBLIC and non-expiring. Documents are routinely sent
 * on to companies and HR contacts who have no portal login, and the exported
 * applicant sheet exists to make that sharing easy. A signed or expiring link
 * eventually leaves a recruiter looking at a dead URL, and the student then asks
 * why their document could not be opened when it was needed -- a worse and far
 * less answerable failure than the exposure. Accountability is handled by
 * logging every time a URL is handed out, not by gating the file.
 *
 * `resource_type: raw`, not `auto`, and this was learned the hard way. With
 * `auto` Cloudinary classifies a .pdf as an IMAGE and parses it -- a file it
 * dislikes is rejected outright with "Invalid PDF file", so a slightly
 * malformed but perfectly readable resume would bounce. Worse, PDF delivery
 * under the image type is blocked by default on many accounts, which would
 * break every link after a successful upload.
 *
 * `raw` stores the bytes verbatim, delivers them untouched, and never parses or
 * re-encodes. For a document store that is exactly the contract wanted: what the
 * student uploaded is what the recruiter downloads. The cost is no server-side
 * thumbnails, which nothing here needs.
 *
 * The public_id is randomised rather than derived from the student's name, so
 * one leaked URL cannot be edited into another student's document.
 */
@Slf4j
public class FileStorageService {

    private final Cloudinary cloudinary;
    private final StorageProperties properties;

    public FileStorageService(Cloudinary cloudinary, StorageProperties properties) {
        this.cloudinary = cloudinary;
        this.properties = properties;
    }

    /**
     * @param folder logical sub-folder, e.g. "students/208/resume" or "postings/12/jd"
     * @return the stored file's key and delivery URL
     */
    public StoredFile upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was provided.");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BadRequestException(String.format(
                    "File is %.1f MB; the limit is %.1f MB.",
                    file.getSize() / 1048576.0, properties.getMaxFileSizeBytes() / 1048576.0));
        }

        String publicId = UUID.randomUUID().toString().replace("-", "");
        String fullFolder = properties.getRootFolder() + "/" + folder;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", fullFolder,
                            "public_id", publicId,
                            "resource_type", "raw",
                            // Keep the original bytes: a resume must download as the
                            // PDF the student uploaded, not a re-encoded derivative.
                            "use_filename", false,
                            "unique_filename", false,
                            "overwrite", false));

            String storageKey = String.valueOf(result.get("public_id"));
            // The named-download form is what gets persisted and handed out: every
            // consumer of this URL (applicant sheet, admin view, student portal) is
            // sending a document to a human who needs to be able to open it.
            String url = downloadUrl(String.valueOf(result.get("secure_url")), file.getOriginalFilename());

            log.info("Stored file key={} folder={} bytes={} contentType={}",
                    storageKey, fullFolder, file.getSize(), file.getContentType());

            return new StoredFile(storageKey, url, file.getContentType(),
                    file.getSize(), file.getOriginalFilename());

        } catch (IOException e) {
            log.error("Upload failed for folder={} name={}: {}",
                    fullFolder, file.getOriginalFilename(), e.getMessage(), e);
            throw new IllegalStateException("Could not store the file. Please try again.", e);
        }
    }

    /**
     * Rewrites a delivery URL so the file arrives with its real name.
     *
     * A raw asset is stored under a random, extensionless public_id, so a plain
     * click downloads a file literally named "a3f9c2..." with no extension and a
     * content type of application/octet-stream. A recruiter who has been sent an
     * applicant sheet then has a folder of unopenable blobs. Cloudinary's
     * `fl_attachment:<name>` flag sets the Content-Disposition filename, which
     * fixes both.
     *
     * The dot must be written as %252E — double-encoded. Cloudinary decodes the
     * transformation component once before parsing it, so a literal "." is read
     * as the start of a format extension and the flag is dropped silently (the
     * response comes back 200, but inline and unnamed). A single %2E decodes to
     * "." too early and fails the same way. Spaces, quotes and commas in the name
     * are worse than useless: they produce a 400.
     *
     * The extension is kept OUT of the public_id on purpose. Storing a key that
     * ends in .pdf makes Cloudinary classify the asset as a PDF, and PDF/ZIP
     * delivery is blocked by default at the account level — every resume link
     * would 401 while the upload itself reported success.
     */
    public static String downloadUrl(String deliveryUrl, String originalFileName) {
        if (deliveryUrl == null || !deliveryUrl.contains(RAW_UPLOAD)) {
            return deliveryUrl;
        }
        return deliveryUrl.replace(RAW_UPLOAD, RAW_UPLOAD + "fl_attachment:" + attachmentName(originalFileName) + "/");
    }

    private static final String RAW_UPLOAD = "/raw/upload/";

    /** Builds a Cloudinary-safe "<base>%252E<ext>" attachment name. */
    private static String attachmentName(String originalFileName) {
        String name = originalFileName == null ? "" : originalFileName;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);

        String base = name, ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            base = name.substring(0, dot);
            ext = name.substring(dot + 1).toLowerCase().replaceAll("[^a-z0-9]", "");
        }

        base = base.replaceAll("[^A-Za-z0-9_-]+", "_").replaceAll("_{2,}", "_").replaceAll("^_|_$", "");
        if (base.isEmpty()) base = "document";
        if (base.length() > 80) base = base.substring(0, 80);
        if (ext.length() > 8) ext = ext.substring(0, 8);

        return ext.isEmpty() ? base : base + "%252E" + ext;
    }

    /**
     * Best-effort removal. Never throws: a document row is deleted for the user
     * whether or not the provider cooperated, and a file left behind is an
     * orphan to sweep, not a failed request.
     */
    public void delete(String storageKey) {
        try {
            cloudinary.uploader().destroy(storageKey,
                    ObjectUtils.asMap("resource_type", "raw", "invalidate", true));
            log.info("Deleted stored file key={}", storageKey);
        } catch (Exception e) {
            log.warn("Could not delete stored file key={} (left orphaned): {}", storageKey, e.getMessage());
        }
    }
}
