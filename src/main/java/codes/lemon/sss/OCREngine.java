package codes.lemon.sss;
import java.awt.image.BufferedImage;

/***
 * Extracts text from a given image using OCR. Implementations may alter the contents of any
 * provided images in order to improve results so it is recommended that clients provide deep copies
 * of images for processing if they wish to maintain an unaltered copy of the image.
 */
public interface OCREngine {

    static OCREngine EMPTY_OCR_ENGINE = new OCREngine() {
        @Override
        public String getText(BufferedImage image) {
            return "OCR has been disabled";
        }
    };

    /***
     * Extracts text visible in the image using Ocular Character Recognition (OCR).
     * If the image contains no recognisable text or an error occurs, an empty string
     * is returned.
     * @param image an image
     * @return text extracted from the image, else an empty string. Never null.
     */
    String getText(BufferedImage image);
}
