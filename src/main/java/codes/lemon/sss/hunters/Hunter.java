package codes.lemon.sss.hunters;

import codes.lemon.sss.OCREngine;

import java.awt.image.BufferedImage;

/***
 * Hunter instances will analyse either the image,
 * the extracted text, or both; in order to determine if the image contains
 * some form of sensitive data, eg passwords, encryption keys, cc data.
 * Implementations must provide a unique name for identification.
 * It is the responsibility of any implementation authors to decide which data
 * is relevant to return after a successful hunt.
 */
public interface Hunter {

    /**
     * An empty Hunter which flags every image. Allows Scavenger to operate as a Scraper.
     */
    static Hunter EMPTY_HUNTER = new Hunter() {
        @Override
        public String hunt(String imageID, BufferedImage content, String OCRText) {
            return "HUNTING DISABLED";
        }
        @Override
        public String getHunterModuleName() {
            return "HUNTING DISABLED";
        }
    };

    /***
     * Analyse the image and/or text to find an indication of sensitive data.
     * An imageID is also supplied to allow Hunter module implementations to
     * record specific details of the results related to that implementation
     * and index them using consistent image IDs.
     * A comment containing details (an explanation) of the result is returned if we believe
     * we have found an indicator of sensitive data.
     * It is up to the author of the Hunter implementation to decide what
     * information is relevant to include.
     * If no indicator of sensitive data was found, null should be returned.
     * @param imageID an image ID for the image being analysed
     * @param content the image to be analysed. to be treated as READ ONLY. Perform alterations on a deep copy.
     * @param OCRText text extracted from the image using OCR techniques
     * @return if sensitive data is found, a comment containing details of the result.
     *         Else null if nothing found.
     */
    String hunt(String imageID, BufferedImage content, String OCRText);

    /***
     * Returns a unique name for each hunter module implementation. This can be used to identify
     * which hunter has flagged an image.
     * @return a name which identifies a hunter module implementation
     */
    String getHunterModuleName();
}