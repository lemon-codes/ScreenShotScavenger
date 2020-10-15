package codes.lemon.sss;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.Objects;

/***
 * This class is used to store details of an individual image.
 * It stores: - an ID which identifies the image
 *            - the contents of the image. Access provided through the BufferedImage class
 *            - text visible in the image (extracted using OCR)
 *
 *  TODO: Consider returning deep copy of BufferedImage in getImageContent to
 *        make instances of ImageData immutable.
 */

final class ImageData {
    private final String imageID;
    private final BufferedImage imageContent;
    private final String imageText;

    /***
     * An immutable class which stores data related to an individual image. This class
     * returns a deep copy of its containing image to ensure its immutability.
     * This allows clients to make alterations to the returned image while maintaining
     * the integrity of the ImageData instance.
     * Null values should never be supplied.
     * @param imageID an identifier for the image
     * @param imageContent the content of the image accessible as a BufferedImage
     * @param imageText text which has been extracted from the image using OCR
     */
    public ImageData(String imageID, BufferedImage imageContent, String imageText) {
        //assert(imageID != null) : "null imageID supplied to ImageData";
        this.imageID = Objects.requireNonNull(imageID, "imageID must not be null");

        //assert(imageContent != null) : "null imageContent supplied to ImageData";
        this.imageContent = Objects.requireNonNull(imageContent, "imageContent must not be null");

        //assert(imageText != null) : "null imageText supplied to ImageData";
        this.imageText = Objects.requireNonNull(imageText, "imageText must not be null");
    }

    /***
     * Returns the ID of the image this object represents.
     * @return the ID of the image
     */
    public String getID() { return imageID; }



    /***
     * Returns the text which has previously been extracted from the image using
     * OCR techniques.
     * @return text contained in the image
     */
    public String getText() { return imageText; }


    /***
     * Provides access to the contents of the image as a BufferedImage.
     * A deep copy is returned allowing clients to make alterations
     * to the returned image while maintaining the integrity of the
     * ImageData instance.
     * @return the contents of the image as a BufferedImage
     */
    public BufferedImage getContent() { return getDeepCopyOfImage(imageContent); }


    /***
     * Returns a deep copy of a BufferedImage. A deep copy is useful when we want to make alterations to a
     * BufferedImage while maintaining an unmodified copy of the original.
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
