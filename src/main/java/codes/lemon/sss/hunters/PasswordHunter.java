package codes.lemon.sss.hunters;
import java.awt.image.BufferedImage;

/***
 * Hunts for in instance of the word "password" within the image text.
 * The presence of the word "password" in a screenshot could indicate the
 * password is visible in the image.
 */
class PasswordHunter implements Hunter {
    private static final String MODULE_NAME = "PASSWORD_HUNTER";
    private static final String SUCCESSFUL_HUNT_COMMENT = "Found word: \"password\"";

    /***
     * Analyse the text contained in the image and search for the word "password".
     * If the word "password" is present, it is possible there may be a visible
     * password in the screenshot, so we consider this a successful hunt and return true.
     * @param imageID an image ID for the image being analysed
     * @param content the image to be analysed
     * @param OCRText text extracted from the image using OCR techniques
     * @return a string noting the word "password" was found, else null if it was not found.
     */
    @Override
    public String hunt(String imageID, BufferedImage content, String OCRText) {
        if (OCRText == null) {
            // image contains no text
            return null;
        }
        if (OCRText.toLowerCase().trim().contains("password")) {
            // we found the word password
            return SUCCESSFUL_HUNT_COMMENT;
        }
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