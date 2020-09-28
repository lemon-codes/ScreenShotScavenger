package codes.lemon.sss.hunters;

import java.awt.image.BufferedImage;

/***
 * Abstract class implementing base functionality for Hunter plugins.
 * Hunter plugins will analyse either the image,
 * the extracted text, or both; in order to determine if the image contains
 * some form of sensitive data, eg passwords, encryption keys, cc data.
 * This abstract class gives all subclasses access to the image and text
 * they require.
 */
public interface Hunter {

    /***
     * Analyse the image and/or text to find an indication of sensitive data.
     * An imageID is also supplied to allow Hunter module implementations to
     * record specific details of the results related to that implementation
     * and index them using consistent image IDs.
     * A comment containing details of the result is returned if we believe
     * we have found an indicator of sensitive data. This comment will be logged.
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