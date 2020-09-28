package codes.lemon.sss;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.*;
import java.awt.image.BufferedImage;
import codes.lemon.sss.hunters.*;
import codes.lemon.sss.scrapers.*;

/***
 * An interactive scavenger which hunts for sensitive data visible in images/screenshots.
 * Images are provided by a scraper. This class performs basic validity checks on data supplied by the scraper.
 * Each image is processed by an OCR engine which extracts text visible in images.
 * A buffer is used to store pre-processed images to improve performance as OCR is not an
 * instantaneous process.
 * The scavenger works with the data for one image at a time. The client can opt to load the
 * next image in the order provided by the scraper if they wish to inspect each image manually.
 * To help filter out junk images (images with no sensitive data) we use hunter modules.
 * Instances of built-in Hunter modules are provided by HunterFactory in the hunters package.
 * Additional Hunter modules can be added at run time by the client.
 * Each hunter module performs its own analysis on the next image and the text extracted from that image.
 * A hunter module will report wither it has found anything which indicates the next image contains
 * sensitive info. If any hunter module has success, the image which was analysed becomes the current image
 * loaded in the scavenger. If no hunters report success, then we load the next image for the hunter modules
 * to process next. We use these hunters in this way to allow the client to load the next image we suspect
 * has sensitive info, skipping those which don't.
 */
public class Scavenger {
    private static final int BUFFER_SIZE = 20;
    private static final boolean OCR_ENABLED = true;
    private final OCREngine ocrEngine;
    private final Queue<ImageData> images;
    private final ResultsManager results;
    private List<Hunter> hunters;
    private Scraper scraper;
    private ImageData currentImage;
    private boolean scraperIsEmpty;

    public Scavenger() {
        // initialise the scraper
        scraper = new PrntscScraper();
        //scraper = new DiskScraper();
        scraperIsEmpty = false;
        // initialise OCR engine
        ocrEngine = new OCRTess4J();
        // initialise hunters
        hunters = new ArrayList<>();
        loadHunters();
        // preload images
        images = new LinkedList<>();
        initBuffer();
        // initialise results manager
        results = new ResultsManagerCSV();
    }

    /***
     * Loads the Hunter modules which will be used to hunt for
     * sensitive information in images. Instances of Hunter modules
     * are obtained from codes.lemon.sss.hunters.HunterFactory
     */
    private void loadHunters() {
        hunters = Objects.requireNonNull(HunterFactory.getHunterFactoryInstance().getInitializedHunters());
    }

    /***
     * Initialises the buffer for the first time by processing the required number
     * of images to fill the buffer. Once the buffer is full we load the first
     * image in the buffer as the current image and then refill the buffer.
     */
    private void initBuffer() {
        fillBufferWithImages(); // fill the buffer with some initial data
        currentImage = images.poll();  // load an initial image from the buffer
        fillBufferWithImages(); // replace the image we just removed from the buffer
    }

    /***
     * If the buffer is not currently full we process more images
     * and fill it. Validity checks are carried out to ensure
     * only valid images with IDs are put in the buffer.
     * If the scraper runs out of images, we do not attempt to obtain
     * any more images from it.
     */
    private void fillBufferWithImages() {
        // check if scraper has already been marked empty before attempting to obtain images from it
        if (scraperIsEmpty) {
            return;
        }

        assert(images != null) : "image collection in Scavenger == null";
        while (images.size() < BUFFER_SIZE) {
            String id = Objects.requireNonNull(scraper.getImageID());
            BufferedImage img = Objects.requireNonNull(scraper.getImageContent());
            String text = Objects.requireNonNull(getTextFromImageUsingOCR(img));

            ImageData image = new ImageData(id, img, text);
            images.add(image);

            try {
                scraper.nextImage(); // load next image in scraper for future use
            } catch (NoImageAvailableException e) {
                // the scraper has ran out of images so mark it as empty
                scraperIsEmpty = true;
            }
        }
    }

    /***
     * Extracts text from an image using Ocular Character Recognition.
     * If OCR has been disabled we return a string notifying any clients
     * that OCR has been disabled.
     * If no text is present in the image or an error occurs when performing OCR,
     * an empty string is returned instead.
     * @param image the image to be analysed as a BufferedImage
     * @return returns the text extracted from image
     */
    private String getTextFromImageUsingOCR(BufferedImage image) {
        String imageText;
        if (OCR_ENABLED) {
            // a deep copy of the image is passed to OCR Engine to allow the OCR engine to
            // alter the image if necessary to improve results.
            imageText = ocrEngine.getText(getDeepCopyOfImage(image)); // returns empty string ("") if no text
        }
        else {
            imageText = "NOTICE: OCR HAS BEEN DISABLED. PLEASE RE-ENABLE OCR IN SCAVENGER IF YOU WISH TO PERFORM TEXTUAL ANALYSIS";
        }
        assert (imageText != null) : "image text should not be null";
        return imageText;
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



    /***
     * Loads the next image in the order the scraper has provided them in.
     * If there is no next image available we stop execution.
     */
    public void loadNextImage() {
        if (images.size() > 0) {
            currentImage = images.poll();
            //System.out.println(currentImage.getID());
            fillBufferWithImages();  // ensures buffer does not become empty
        }
        else {
            System.out.println("We have ran out of images to process." +
                                "The scraper in use has indicated it cannot provide any more images.");
            System.out.println("We will print out the results and finish up now!");
            printResultsAndExit();
        }
    }

    /***
     * Iterates through images and uses all loaded hunter modules to analyse each image.
     * We stops when a hunter has found something which indicates an image contains
     * sensitive data. This image then becomes the current image. Getters can be used to
     * retrieve the details of this image.
     */
    public void loadNextHuntedImage() {
        while (true) {
            // iterate through images
            loadNextImage(); // shuts down if no next image
            // allow all hunters to process the current image. Stop if a hunter is successful
            for (Hunter hunter : hunters) {
                //returns null if nothing found
                String resultDetails = hunter.hunt(currentImage.getID(), currentImage.getContent(), currentImage.getText());
                if (resultDetails != null) {
                    // a hunter has found something.
                    // save the name of the hunter which has flagged this image as a result
                    String resultAuthor = hunter.getHunterModuleName();

                    // store the details of this find in the results manager
                    ResultData result = new ResultDataImp(currentImage, resultAuthor, resultDetails);
                    results.addResult(result);
                    return; // no need to continue processing images
                }
            }
        }
    }

    /***
     * Returns an identifier for the image currently loaded
     * @return ID of the current image
     */
    public String getCurrentImageID() {
        return currentImage.getID();
    }

    /***
     * Returns the content of the current image as a BufferedImage
     * @return current image as a BufferedImage
     */
    public BufferedImage getCurrentImage() {
        return currentImage.getContent();
    }

    /***
     * Returns the text which has been extracted from the image using
     * an OCR engine
     * @return all text visible in the image
     */
    public String getCurrentImageOCRText() {
        return currentImage.getText();
    }

    /***
     * Add a hunter module to the scavenger. This module will
     * be used to analyse any future images for indicators of sensitive
     * data
     * // TODO: USE GENERICS
     * @param h a hunter module
     */
    public void addHunter(Hunter h) {
        if (h != null) {
            hunters.add(h);
        }
        else {
            throw new IllegalArgumentException("Hunter h cannot be null");
        }
    }

    /***
     * Removes a hunter module from the scavenger
     * // TODO: USE GENERICS
     * @param h a hunter module already loaded in the scavenger
     * @return true if the hunter module was successfully removed,
     *          false if the hunter module is not currently loaded
     *          and therefore cannot be removed.
     */
    public boolean removeHunter(Hunter h) {
        // TODO: use instanceOf() to compare the run time class type of each hunter
        //      in the collection with the run time class type of Hunter h
        if (hunters.contains(h)) {
            hunters.remove(h);
            return true;
        }
        return false;
    }

    /***
     * Accepts a Scraper as a parameter and sets this as the scraper to
     * be used as the source of all future images. Any subsequent requests
     * to loadNextImage() or loadNextHuntedImage() will use images obtained
     * from newScraper.
     * // TODO: USE GENERICS
     * @param newScraper a scraper which will be used as a source for future images.
     *                   Must not be null or an IllegalArgumentException will be thrown.
     */
    public void loadNewScraper(Scraper newScraper) {
        if (newScraper != null) {
            scraper = newScraper;
            scraperIsEmpty = false; // mark the new scraper as not being empty
            images.clear();  // clear the image buffer to remove reference to any images from the old scraper
            fillBufferWithImages();  // refill image buffer with images from new scraper
        }
        else {
            // illegal null-parameter passed as newScraper
            throw new IllegalArgumentException("Scraper newScraper cannot be null");
        }
    }

    /***
     * Prints results before cleaning up and exiting.
     */
    public void printResultsAndExit() {
        results.printResults();
        results.exit();
        System.exit(0);
    }

    /***
     * Cleans up and exits
     */
    public void exit() {
        results.exit();
        System.exit(0);
    }
}

