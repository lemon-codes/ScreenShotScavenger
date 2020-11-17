package codes.lemon.sss.results;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.Objects;

/***
 * An immutable class which stores result details. Result details include result author, result details provided
 * by Hunter, image ID, image content and the text extracted via OCR.
 * Deep copies are provided for mutable return types allowing clients to modify mutable objects
 * returned by this implementation without altering this implementations copy.
 * This class provides accessors to all details it contains.
 */
public final class ResultDataImp implements ResultData {
    private final String imageID;
    private final String imageText;
    private final BufferedImage imagecontent;
    private final String author;
    private final String details;

    /***
     * Stores details of a result, the author of that result, and details of the image the result refers to.
     * @param resultAuthor the name of the Hunter module which flagged the ImageData instance
     * @param resultDetails additional details related to the result supplied by the Hunter module
     *                         which flagged the image as a result.
     * @param imageID the unique ID of the image contained in this result
     * @param imageContent the image contained in this result
     * @param imageText the text extracted from the image contained in this result
     */
    public ResultDataImp(String resultAuthor, String resultDetails, String imageID, BufferedImage imageContent,
                         String imageText) {

        this.author = Objects.requireNonNull(resultAuthor);
        this.details = Objects.requireNonNull(resultDetails);
        this.imageID = Objects.requireNonNull(imageID);
        this.imagecontent = Objects.requireNonNull(imageContent);
        this.imageText = Objects.requireNonNull(imageText);
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
        return imageID;
    }

    /***
     * Returns the contents of the image which has been flagged as a result.
     * The image contents are provided as a BufferedImage. A deep copy is
     * provided.
     * @return the image contents of the image contained in this result
     */
    public BufferedImage getImageContent() {
        return getDeepCopyOfImage(imagecontent);
    }

    /***
     * Returns the text extracted from the image in this result.
     * @return the text extracted from the image in this result
     */
    public String getImageText() {
        return imageText;
    }

    /***
     * Returns a deep copy of a BufferedImage.
     * Since BufferedImage instances are mutable, a deep copy is required to allow clients
     * to access and modify a BufferedImage instance without altering the original instance.
     * This can be used to help ensure the immutability of the parent class.
     * @param originalImage the image to be copied
     * @return a deep copy (value copy) of originalImage
     */
    private BufferedImage getDeepCopyOfImage(BufferedImage originalImage) {
        // should never happen since null checks are performed when images are loaded from Scraper
        assert (originalImage != null) : "null passed to getDeepCopyOfImage";
        ColorModel cm = originalImage.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = originalImage.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
}