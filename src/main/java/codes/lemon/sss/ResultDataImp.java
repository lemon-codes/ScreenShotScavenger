package codes.lemon.sss;

import java.awt.image.BufferedImage;

/***
 * This class acts as wrapper for the ImageData class. It is intended to be used when a Hunter
 * module flags an image. This class stores the original ImageData object plus additional
 * result data related to the image, such as the Author of the result (the name of the Hunter module
 * which flagged the image) and any additional result details supplied by the flagging hunter module.
 * This class provides accessors to all data contained in the supplied ImageData object, as well as the
 * result author and additional result details.
 *
 * TODO: Consider refactoring ResultData to extend ImageData rather than acting as a wrapper
 *
 * TODO: Make class immutable by returning deep copy of BufferedImage in getImageContent(),
 *      Consider performance penalty first.
 */
final class ResultDataImp implements ResultData {
    private final ImageData image;
    private final String author;
    private final String details;

    /***
     * Stores an ImageData instance which has been flagged, alongside details
     * of the result.
     * @param resultImage the ImageData instance which has been flagged as a successful result
     * @param resultAuthor the name of the Hunter module which flagged the ImageData instance
     * @param resultDetails additional details related to the result supplied by the Hunter module
     *                         which flagged the image as a result.
     */
    public ResultDataImp(ImageData resultImage, String resultAuthor, String resultDetails) {
        assert (resultImage != null) : "ImageData supplied to Result should never be null";
        image = resultImage;

        assert (resultAuthor != null) : "resultAuthor should never be null";
        author = resultAuthor;

        assert (resultDetails != null) : "resultDetails should never be null";
        details = resultDetails;

        // null values should never be provided to ImageData objects upon initialization
        // so we should never expect null values to be returned by ImageData objects
        assert(image.getID() != null);
        assert(image.getContent() != null);
        assert(image.getText() != null);
    }

    /***
     * Returns the name of the Hunter module which flagged this result.
     * @return author of this result
     */
    public String getAuthor() {
        return author;
    }

    /***
     * Returns additional details about the result supplied by the Hunter module
     * which flagged the result.
     * @return additional details about the result
     */
    public String getDetails() {
        return details;
    }

    /***
     * Returns the ID of the image contained in this result
     * @return the ID of the image contained in this result
     */
    public String getImageID() {
        return image.getID();
    }

    /***
     * Returns the contents of the image which has been flagged as a result.
     * The image contents are provided as a BufferedImage
     * @return the image contents of the image contained in this result
     */
    public BufferedImage getImageContent() {
        return image.getContent();
    }

    /***
     * Returns the text extracted from the image in this result.
     * @return the text extracted from the image in this result
     */
    public String getImageText() {
        return image.getText();
    }
}
