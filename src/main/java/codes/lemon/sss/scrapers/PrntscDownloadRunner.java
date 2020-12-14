package codes.lemon.sss.scrapers;
import java.util.concurrent.BlockingQueue;

/***
 * Downloads the image for the given image ID.
 * If the download was successful and we have a valid image
 * we attempt to add it to the buffer. If the attempt fails we
 * discard the image and return.
 */
class PrntscDownloadRunner implements Runnable{
    private final String imageID;
    private final BlockingQueue<PrntscImage> buffer;

    public PrntscDownloadRunner(String imageID, BlockingQueue<PrntscImage> buffer) {
        this.imageID = imageID;
        this.buffer = buffer;
    }

    /***
     * Downloads the image for the given image ID.
     * If the download was successful and we have a valid image
     * we attempt to add it to the buffer. If the attempt fails we
     * discard the image and return.
     */
    @Override
    public void run() {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        PrntscImage image = new PrntscImage(imageID);
        if (image.containsValidImage()) {
            try {
                buffer.put(image);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // task is finishing anyway so no need to manually exit
            }
        }

    }
}
