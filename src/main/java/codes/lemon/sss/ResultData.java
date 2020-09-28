package codes.lemon.sss;

import java.awt.image.BufferedImage;

/***
 * Stores details about results. Used by the scavenger to communicate with the
 * ResultsManager. The implementation of ResultData is not intended to be modified
 * or replaced, only used by other ResultsManager implementations to receive messages
 * from the scavenger.
 */
interface ResultData {


    /***
     * Returns the name of the Hunter module which flagged this result.
     * @return author of this result
     */
    String getAuthor();

    /***
     * Returns additional details about the result supplied by the Hunter module
     * which flagged the result.
     * @return additional details about the result
     */
    String getDetails();

    /***
     * Returns the ID of the image contained in this result
     * @return the ID of the image contained in this result
     */
    String getImageID();

    /***
     * Returns the contents of the image which has been flagged as a result.
     * The image contents are provided as a BufferedImage
     * @return the image contents of the image contained in this result
     */
    BufferedImage getImageContent();

    /***
     * Returns the text extracted from the image in this result.
     * @return the text extracted from the image in this result
     */
    String getImageText();


}
