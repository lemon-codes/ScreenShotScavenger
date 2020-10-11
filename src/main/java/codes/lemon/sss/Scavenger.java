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
 *
 *
 */
public class Scavenger {


    /***
     * Implements the builder design pattern to construct Scavenger instances.
     * Using the builder pattern here ensures that the Scavenger is initialised to a valid state,
     * rather than providing setters to the client which may cause a poorly programmed client to
     * alter a scavenger instance in such a way that its state becomes invalid resulting in
     * nondeterministic behaviour.
     * Lower bound wildcards are used to allow subclasses of components to be supplied.
     */
    public static class Builder {

        //private Scraper scraper = new PrntscScraper();
        private Scraper scraper = new DiskScraper();
        private OCREngine ocrEngine = new OCRTess4J();
        //private HunterFactory hunterFactory = HunterFactory.getDefaultHunterFactoryInstance();
        private List<Hunter> hunters = HunterFactory.getDefaultHunterFactoryInstance().getInitializedHunters();
        private ResultsManager resultsManager = new ResultsManagerCSV();
        private int bufferSize = 20;
        private boolean ocrEnabled = true;
        private boolean huntingEnabled = true;
        private boolean resultsManagerEnabled = true;


        // upper bound wildcard used to allow subclasses of Scraper implementations to be used
        public <T extends Scraper> Builder setScraper(T scraper) {
            this.scraper = Objects.requireNonNull(scraper);
            return this;
        }

        public <T extends OCREngine> Builder setOCREnging(T ocrEngine) {
            this.ocrEngine = Objects.requireNonNull(ocrEngine);
            return this;
        }

        public <T extends HunterFactory> Builder setHunterFactory(T hunterFactory) {
            HunterFactory factory = Objects.requireNonNull(hunterFactory);
            // new list to prevent client modifying list during execution. Hunters are immutable so no need to make deep copies
            List<Hunter> suppliedHunters = new ArrayList<>();
            for (Hunter hunter : Objects.requireNonNull(factory.getInitializedHunters())) {
                // Objects.requireNull used rather than discarding null values because we want to fail fast
                // if a hunter implementation hasn't been initialised properly rather than obscure that fact
                suppliedHunters.add(Objects.requireNonNull(hunter));
            }
            this.hunters = suppliedHunters;
            return this;
        }

        public <T extends ResultsManager> Builder setResultsManager(T resultsManager) {
            this.resultsManager = Objects.requireNonNull(resultsManager);
            return this;
        }

        public Builder setBufferSize(int bufferSize) {
            if (bufferSize > 0) {
                this.bufferSize = bufferSize;
            }
            return this;
        }

        public Builder enableOCR(boolean ocrEnabled) {
            this.ocrEnabled = ocrEnabled;

            return this;
        }

        public Builder enableHunting(boolean huntingEnabled) {
            this.huntingEnabled = huntingEnabled;
            return this;
        }

        public Builder enableResultsManager(boolean resultsManagerEnabled) {
            this.resultsManagerEnabled = Objects.requireNonNull(resultsManagerEnabled);
            return this;
        }

        private void releaseUnusedResources() {
            // allow ocrEngine to be garbage collected if OCR is disabled
            ocrEngine = ocrEnabled ? ocrEngine : OCREngine.EMPTY_OCR_ENGINE;

            // Allow Hunter instances to be garbage collected if hunting disabled
            // Empty list returned rather than null to prevent null checks littering code
            hunters = huntingEnabled ? hunters : Collections.EMPTY_LIST;

            // If resultsManager is disabled, replace results manager with one which discards all results.
            // This allows the original resultsManager resources to be garbage collected,
            if (!resultsManagerEnabled) {
                // EMPTY_RESULT_MANAGER is used rather than null to prevent Scavenger code being littered with null checks.
                resultsManager = ResultsManager.EMPTY_RESULT_MANAGER;
            }
        }

        public Scavenger build() {
            releaseUnusedResources();
            return new Scavenger(this);
        }
    }

    private Scraper scraper; // TODO: final
    private final OCREngine ocrEngine;
    //private final HunterFactory hunterFactory;
    private final ResultsManager results;
    private final int bufferSize;
    private final boolean ocrEnabled;
    private final boolean huntingEnabled;
    private final boolean resultsManagerEnabled;
    private final Queue<ImageData> images;

    private List<Hunter> hunters;  // TODO: final
    private ImageData currentImage;
    private boolean scraperIsEmpty;

    private Scavenger(Builder builder) {
        bufferSize = builder.bufferSize;
        scraper = builder.scraper;
        ocrEnabled = builder.ocrEnabled;
        ocrEngine = builder.ocrEngine;
        huntingEnabled = builder.huntingEnabled;
        hunters = builder.hunters;

        resultsManagerEnabled = builder.resultsManagerEnabled;
        results = builder.resultsManager;
        images = new LinkedList<>();
        initBuffer();
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
        while (images.size() < bufferSize) {
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
        if (ocrEnabled) {
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

        // TODO: Refactor so currentImage is not changed until hunted image found.
        //       change loadNextImage() to getNextImage(). Return image instead of updating currentImage.
        //       change this method name to "loadNextImage()". Check huntingEnabled.
        //       If false. update currentImage to image returned by getNextImage().
        //       If true, perform hunting. Once image hunted, update currentImage.
        //       This will ensure Scavenger stays in a consistent state.
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
     * @param hunter a hunter module. Must not be null
     */
    public <T extends Hunter> void addHunter(T hunter) {
        hunters.add(Objects.requireNonNull(hunter));
    }

    /***
     * Removes a hunter module from the scavenger. Comparison is based on class name.
     * @param hunter a hunter module already loaded in the scavenger
     * @return true if the hunter module was successfully removed,
     *          false if the hunter module is not currently loaded
     *          and therefore cannot be removed.
     */
    public <T extends Hunter> boolean removeHunter(T hunter) {
        // TODO: offer base class for Hunters to extend. Base class will implement equals() and
        //       hashcode(). All Hunter implementations will extend base class. List.contains()
        //       uses objects equals() equals method to compare elements. By overriding equals(),
        //       we can ensure hunters are compared using their unique name field. This will allow
        //       hunters.contains(Hunter h) to be used to remove modules with same unique name as h.
        final Hunter target = Objects.requireNonNull(hunter);

        for (Hunter candidate : hunters) {
            // comparison is based on class name
            if (candidate.getClass().equals(target.getClass())) {
                hunters.remove(candidate);
                return true;
            }
        }
        return false;
    }

    /***
     * Accepts a Scraper as a parameter and sets this as the scraper to
     * be used as the source of all future images. Any subsequent requests
     * to loadNextImage() or loadNextHuntedImage() will use images obtained
     * from newScraper.
     * @param newScraper a scraper (or subclass) which will be used as a source for future images.
     *                   Must not be null.
     */
    public <T extends Scraper> void loadNewScraper(T newScraper) {
        scraper = Objects.requireNonNull(newScraper);
        scraperIsEmpty = false; // mark the new scraper as not being empty
        images.clear();  // clear the image buffer to remove reference to any images from the old scraper
        fillBufferWithImages();  // refill image buffer with images from new scraper
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

