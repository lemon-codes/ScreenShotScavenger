package codes.lemon.sss;

import codes.lemon.sss.scrapers.NoImageAvailableException;
import codes.lemon.sss.scrapers.Scraper;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

/***
 * This task interacts with a Scraper instance to ensure the ImageData buffer is continually
 * replenished. Ocular Character Recognition (OCR) is performed on images provided by the
 * Scraper to extract visible text contained in those images. The OCR Engine is passed a deep copy
 * of each image allowing the Engine to make modifications to the image to aid analysis.
 * Each image and the extracted text are bundled into an ImageData instance and place in the client
 * provided buffer. This task will block until it is able to add the image to the buffer to ensure
 * no ImageData instances are discarded.
 * This task will run indefinitely until the Scraper can no longer supply images or until interrupted by
 * the client.
 */
class ImageDataBufferTask implements Runnable {
    private final int bufferSize;
    private final Scraper scraper;
    private final BlockingQueue<ImageData> buffer;
    private final OCREngine ocrEngine;
    private boolean finished = false;

    /***
     *
     * @param scraper a scraper which will provide valid BufferedImage instances
     * @param ocrEngine extracts text from images using Ocular Character Recognition
     * @param bufferSize maximum number of elements in buffer at any one time. must be > 0.
     * @param buffer the buffer which ImageData instances will be added to
     */
    public ImageDataBufferTask(Scraper scraper, OCREngine ocrEngine, int bufferSize, BlockingQueue<ImageData> buffer) {
        assert(scraper != null) : "null value supplied for scraper";
        assert(ocrEngine != null) : "null value supplied for ocrEngine";
        assert(bufferSize > 0) : "bufferSize must be > 0";
        assert(buffer != null) : "null value supplied for imageBuffer";

        this.bufferSize = bufferSize;
        this.scraper = scraper;
        this.buffer = buffer;
        this.ocrEngine = ocrEngine;
    }


    /***
     * Ensures the client supplied buffer is constantly replenished but does not exceed the maximum buffer size.
     */
    @Override
    public void run() {
        while (!finished) {
            while (buffer.size() < bufferSize) {
                String imageID = Objects.requireNonNull(scraper.getImageID());
                BufferedImage imageContent = Objects.requireNonNull(scraper.getImageContent());
                // a deep copy of the image is passed to OCR Engine to allow the OCR engine to
                // alter the image if necessary to improve results.
                String imageText = Objects.requireNonNull(ocrEngine.getText(getDeepCopyOfImage(imageContent)));
                ImageData imageData = new ImageData(imageID, imageContent, imageText);
                try {
                    // put() blocks until element successfully added to queue. This ensures no valid images are discarded
                    buffer.put(imageData);
                    scraper.nextImage();
                } catch (InterruptedException e) {
                    shutdown();
                    break;
                } catch (NoImageAvailableException e) {
                    // scraper can provide no more images
                    shutdown();
                    break;
                }
            }
        }
    }

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


    private void shutdown() {
        finished = true;
    }
}
