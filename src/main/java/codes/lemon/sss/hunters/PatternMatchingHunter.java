package codes.lemon.sss.hunters;

import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * Hunts for patterns within the text of an image which may indicate the presence
 * of sensitive data. Patterns currently supported are:
 *  - a regex to identify potential email address
 *  - a regex to identify possible ip addresses
 * If a pattern is identified in an image we consider the hunt successful
 * and flag it to the client.
 */
class PatternMatchingHunter implements Hunter{
    private static final String MODULE_NAME = "PATTERN_MATCHING_HUNTER";
    private static final String REGEX_EMAIL_ADDRESS = "[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+";
    private static final String REGEX_IP_ADDRESS = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

    // We only ever require one instance of each immutable Pattern.
    // Creating new Pattern instances is expensive
    // Since Pattern objects are immutable we can safely reuse the same instances
    private static final Pattern PATTERN_EMAIL_ADDRESS = Pattern.compile(REGEX_EMAIL_ADDRESS);
    private static final Pattern PATTERN_IP_ADDRESS = Pattern.compile(REGEX_IP_ADDRESS);
    private static final Pattern[] PATTERN_SET = {PATTERN_EMAIL_ADDRESS, PATTERN_IP_ADDRESS};


    public PatternMatchingHunter() {
        assert(PATTERN_SET.length > 0) : "No Patterns compiled in PatternMatchingHunter";
    }

    /***
     * Analyses the text extracted from the image and attempts to identify
     * patterns in the text which may indicate sensitive data is present.
     * @param imageID an image ID for the image being analysed
     * @param content the image to be analysed
     * @param OCRText text extracted from the image using OCR techniques
     * @return the text which matched the pattern, else null if no matches.
     */
    @Override
    public String hunt(String imageID, BufferedImage content, String OCRText) {
        // use precompiled patterns for efficiency
        for (Pattern pattern : PATTERN_SET) {
            Matcher m = pattern.matcher(OCRText);
            if (m.find()) {
                // successful match. Return details of the text and pattern which matched.
                String matchingText = m.group();
                String matchingPattern = pattern.toString();
                return "\"" + matchingText + "\"" + " matched with regex: " + matchingPattern;
            }
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
