package org.yuktisetu.core.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Cloudinary attachment-name encoding.
 *
 * This is the kind of rule that cannot be re-derived from reading the code: the
 * dot in the filename must be written %252E, and every plausible alternative
 * fails in a way that still looks successful or breaks outright. Verified
 * against the live API:
 *
 *   fl_attachment:asha_resume.pdf      -> 200, Content-Disposition: inline   (flag dropped)
 *   fl_attachment:asha_resume%2Epdf    -> 200, inline                        (decoded too early)
 *   fl_attachment:asha%20resume.pdf    -> 400
 *   fl_attachment:'asha_resume.pdf'    -> 400
 *   fl_attachment:asha_resume%252Epdf  -> 200, filename="asha_resume.pdf"    (correct)
 *
 * Note the two failures that return 200. Nothing downstream would notice the
 * regression; it would surface months later as a recruiter saying the resumes
 * in the applicant sheet download as unopenable files.
 */
class FileStorageDownloadUrlTest {

    private static final String BASE =
            "https://res.cloudinary.com/demo/raw/upload/v1790087471/yuktisetu/students/208/resume/a3f9c2";

    private static String flagIn(String url) {
        int i = url.indexOf("fl_attachment:");
        int j = url.indexOf('/', i);
        return url.substring(i, j);
    }

    @Test
    @DisplayName("the dot is double-encoded, so the recruiter gets a real filename")
    void dotIsDoubleEncoded() {
        assertEquals("fl_attachment:asha_resume%252Epdf",
                flagIn(FileStorageService.downloadUrl(BASE, "asha_resume.pdf")));
    }

    @Test
    @DisplayName("the flag is inserted before the version, leaving the public_id untouched")
    void publicIdSurvivesUnchanged() {
        String url = FileStorageService.downloadUrl(BASE, "cv.pdf");
        assertEquals("https://res.cloudinary.com/demo/raw/upload/fl_attachment:cv%252Epdf"
                + "/v1790087471/yuktisetu/students/208/resume/a3f9c2", url);
        assertTrue(url.endsWith("/a3f9c2"), "the stored key must not gain an extension -- that is what 401s PDFs");
    }

    /** Spaces, quotes and commas are 400s, not cosmetic problems. */
    @Test
    @DisplayName("characters Cloudinary rejects are replaced rather than passed through")
    void unsafeCharactersAreReplaced() {
        assertEquals("fl_attachment:Asha_Patil_Resume_final%252Epdf",
                flagIn(FileStorageService.downloadUrl(BASE, "Asha Patil, Resume (final).pdf")));
        assertEquals("fl_attachment:b%252Epdf",
                flagIn(FileStorageService.downloadUrl(BASE, "a/b.pdf")),
                "a path separator would split the transformation component");
    }

    @Test
    @DisplayName("a Windows client's full path contributes only its last segment")
    void windowsPathIsStripped() {
        assertEquals("fl_attachment:resume%252Edocx",
                flagIn(FileStorageService.downloadUrl(BASE, "C:\\Users\\asha\\Desktop\\resume.docx")));
    }

    @Test
    @DisplayName("a missing or unusable name still yields a valid flag")
    void degradesToDocument() {
        assertEquals("fl_attachment:document", flagIn(FileStorageService.downloadUrl(BASE, null)));
        assertEquals("fl_attachment:document", flagIn(FileStorageService.downloadUrl(BASE, "###")));
        assertEquals("fl_attachment:resume", flagIn(FileStorageService.downloadUrl(BASE, "resume")));
        assertEquals("fl_attachment:hidden", flagIn(FileStorageService.downloadUrl(BASE, ".hidden")));
    }

    @Test
    @DisplayName("an over-long name is truncated rather than producing an unusable URL")
    void longNameIsTruncated() {
        String flag = flagIn(FileStorageService.downloadUrl(BASE, "a".repeat(300) + ".pdf"));
        assertEquals("fl_attachment:" + "a".repeat(80) + "%252Epdf", flag);
    }

    @Test
    @DisplayName("a URL that is not a raw delivery URL is returned untouched")
    void nonRawUrlIsLeftAlone() {
        assertEquals("https://example.test/x.pdf",
                FileStorageService.downloadUrl("https://example.test/x.pdf", "x.pdf"));
        assertEquals(null, FileStorageService.downloadUrl(null, "x.pdf"));
    }
}
