package codes.lemon.sss.hunters;

import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * Hunts for email addresses in images. Any image with text which appears
 * to contain an email address is flagged by this Hunter.
 */
class EmailAddressHunter implements Hunter{
    private static final String MODULE_NAME = "EMAIL_ADDRESS_HUNTER";

    /***
     * Analyses the text extracted from the image and attempts to identify
     * the presence of an email address.
     * @param imageID an image ID for the image being analysed
     * @param content the image to be analysed
     * @param OCRText text extracted from the image using OCR techniques
     * @return the email address we have found, else null if nothing found.
     */
    @Override
    public String hunt(String imageID, BufferedImage content, String OCRText) {
        Matcher m = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+").matcher(OCRText);
        if (m.find()) {
            // we have found an email address. We return it.
            return m.group();
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
