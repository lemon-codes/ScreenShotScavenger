package codes.lemon.sss.hunters;
import java.awt.image.BufferedImage;

/***
 * Contains a set of keywords which may indicate the image contains sensitive data.
 * Hunts for an instance of any of the defined keywords within the image text.
 */
class SensitiveKeywordHunter implements Hunter{
    private static final String MODULE_NAME = "SENSITIVE_KEYWORD_HUNTER";

    // case & whitespace insensitive
    private static final String[] KEYWORDS = {"private", "key", "pgp", "wallet", "password", "ip address", "database",
                                                "passwd", "pwd", "pass", "ssh", "ftp", "smb", "root", "remember",
                                                "authentication", "user", "balance", "address", "token", "secret",
                                                "db_login", "session", "code"};

    /***
     * Analyse the text contained in the image and search for keywords which may indicate
     * the presence of sensitive data.
     * The detection of any keyword in the image text is considered a successful hunt
     * and the image is flagged to the client (return true).
     * @param imageID an image ID for the image being analysed
     * @param content the image to be analysed
     * @param OCRText text extracted from the image using OCR techniques
     * @return the keyword which was found, else null if nothing found
     */
    @Override
    public String hunt(String imageID, BufferedImage content, String OCRText) {
        if (OCRText == null) {
            // image contains no text
            return null;
        }

        for (String keyword : KEYWORDS) {
            // identical operations performed to keywords and OCRtext before comparison
            // to prevent character case and whitespace from interfering with comparison
            keyword = keyword.toLowerCase().trim();
            if (OCRText.toLowerCase().trim().contains(keyword)) {
                // a keyword was found. Return details of this.
                return "Detected keyword: \"" + keyword + "\"";
            }
        }

        // no keywords matched. Hunt was not successful
        return null;
    }

    /***
     * Returns a unique name for each hunter module implementation. This can be used to identify
     * which hunter has flagged an image.
     * @return a name which identifies a hunter module implementation
     */
    @Override
    public String getHunterModuleName() {
        return MODULE_NAME;
    }
}
