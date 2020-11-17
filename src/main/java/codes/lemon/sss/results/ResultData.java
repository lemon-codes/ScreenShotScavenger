package codes.lemon.sss.results;

import java.awt.image.BufferedImage;

/***
 * Stores details about results. Implementations should be immutable to allow clients
 * so safely modify data provided by a ResultData instance whilst maintinging the integrity
 * of the ResultData instance. Used by the scavenger to represent its state and
 * communicate with the ResultsManager.
 */
public interface ResultData {


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
     * Returns a deep copy of contents of the image which has been flagged as a result.
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
