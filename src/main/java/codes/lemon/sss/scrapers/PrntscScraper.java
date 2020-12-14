package codes.lemon.sss.scrapers;
import java.awt.image.BufferedImage;
import java.util.concurrent.*;

/***
 * Implements a scraper targeted at the web interface of prnt.sc.
 * This scraper provides access to publicly accessible prnt.sc screenshots.
 * A code generator (PrntscCodeGenerator) is used to increment through valid prnt.sc
 * image ids which are then used to download the image that ID refers to.
 * The base code used by the code generator is stored as a class variable in this class.
 * A thread pool is used to allow concurrent downloading of images.
 * Downloaded images are stored in a buffer as a PrntscImage object.
 * This scraper provides access to the ID and content of one image at a time.
 * The client dictates when we load the next image.
 * As the client loads new images from the buffer, we refill the buffer with new images.
 * To prevent the buffer growing too large, we wait until the buffer has reached a
 * lower limit on the number of elements it contains before downloading new images to
 * refill the buffer. Images are downloaded in batches in order to keep the CPU
 * free to focus on OCR.
 * NOTE: If HTTP 503 error response is encountered repeatedly, reduce THREADS.
 * @author lemon
 */
public class PrntscScraper implements Scraper{
    private static final int MIN_IMAGES_IN_BUFFER = 8; // 24
    private static final int THREADS = 2;
    private static final int BATCH_DOWNLOAD_SIZE = 4; // 12
    private static final long MAX_TIME_TO_WAIT = 10; // max time (in seconds) we will wait for an image to become available

    private final ExecutorService pool;
    private final BlockingQueue<PrntscImage> imageBuffer;
    private final PrntscCodeGenerator codeGenerator;
    private PrntscImage currentImage;

    // default constructor
    public PrntscScraper() {
        pool = Executors.newFixedThreadPool(THREADS);
        // LinkedBlockingQueue was chosen because it blocks on read when the queue is empty.
        // Eliminating the need for synchronization
        imageBuffer = new LinkedBlockingQueue<PrntscImage>();
        codeGenerator = new PrntscCodeGenerator();
        initBuffer();
    }

    /**
     * Constructs a PrntscScraper instance whose state upon initialisation will contain details of
     * the prntsc image that corresponds to baseImageID. This baseImageID is incremented to provide
     * subsequent images.
     * @param baseImageID a valid prntsc image ID which will be incremented to identify subsequent images
     */
    public PrntscScraper(String baseImageID) {
        // cannot call this() because codeGenerator is assigned and used within parameterless constructor
        pool = Executors.newFixedThreadPool(THREADS);
        imageBuffer = new LinkedBlockingQueue<PrntscImage>();
        codeGenerator = new PrntscCodeGenerator(baseImageID);
        initBuffer();
    }

    /***
     * Initialises the buffer and loads the first image.
     */
    private void initBuffer() {
        downloadNewBatchToBuffer();
        try {
            nextImage();
        } catch (NoImageAvailableException e) {
            e.printStackTrace();
        }
    }

    /***
     * Loads the next image into the scraper.
     * This method may block for a maximum of 10 seconds while waiting for an image
     * to become available at which point an exception is thrown to notify the client
     * that we failed to load an image.
     * @throws NoImageAvailableException if a next image cannot be loaded
     */
    public void nextImage() throws NoImageAvailableException {
        // isInterrupted() preserves the current interrupted status of the thread
        if (!Thread.currentThread().isInterrupted()) {
            downloadNewBatchToBuffer(); // ensure the buffer doesn't become empty
        } else {
            // thread has been interrupted - treat as a cancellation
            shutdown(); // may have been called previously - has no side effects if called repeatedly
            throw new NoImageAvailableException(); // let the client know that no image is available
        }

        PrntscImage nextCurrentImage = null;
        try {
            // returns null if timeout is reached
            nextCurrentImage = imageBuffer.poll(MAX_TIME_TO_WAIT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // treat interrupt as a cancel
            shutdown();                             // free resources
            Thread.currentThread().interrupt();     // set thread as interrupted
            throw new NoImageAvailableException();  // let the client know that no image is available
        }
        if (nextCurrentImage != null) {
            currentImage = nextCurrentImage;
        } else {
            // we reached the max time to wait before an image became available
            throw new NoImageAvailableException();
        }
    }

    /***
     * Downloads a batch of newly scraped prntsc images to the buffer.
     * We wait until the buffer is low before replenishing to save resources and prevent
     * the buffer becoming larger than necessary.
     * We download the images in batches so we can take full advantage of the thread pool.
     * Image downloads can be costly time wise, so we want to keep the buffer well fed.
     */
    private void downloadNewBatchToBuffer() {
        if (imageBuffer.size() <= MIN_IMAGES_IN_BUFFER) {
            // start download jobs in batches
            for (int i=0; i<BATCH_DOWNLOAD_SIZE; i++) {
                startNextImageDownload();
            }
        }
    }

    /***
     * Starts a new DownloadRunner which runs in a thread from our thread pool.
     * The DownloadRunner attempts to download an image using the next image ID
     * provided by the code generator. If the image is downloaded successfully it
     * is added to the imageBuffer. If not, it gives up.
     */
    private void startNextImageDownload() {
        String nextImageID = codeGenerator.getNextCode();
        PrntscDownloadRunner dlr = new PrntscDownloadRunner(nextImageID, imageBuffer);
        pool.execute(dlr);
    }

    /**
     * Stops downloading any further images and releases resources.
     */
    public void shutdown() {
        if (!pool.isShutdown()) {
            pool.shutdownNow();
        }
        //while (!pool.isTerminated()) {
            // block until all tasks have finished
        //}
    }

    /***
     * Returns a unique ID for the image currently loaded
     * in the Scraper
     * @return the unique ID for the currently loaded image
     */
    public String getImageID() {
        return currentImage.getImageID();
    }

    /***
     * Provides access to the contents of the currently loaded image
     * as a BufferedImage.
     * @return the currently loaded image as a BufferedImage
     */
    public BufferedImage getImageContent() {
        return currentImage.getImageContent();
    }
}
