package codes.lemon.sss;
import java.util.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

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
 * sensitive info. If no hunters report success, then we load the next image for the hunter modules
 * to process next. This allows clients to load the next image we suspect has sensitive info, skipping
 * those which don't. If any hunter module has success, the Scavengers state is updated to contain the
 * image which was analysed along with details related to the result. Getters can be used to
 * access result details. The getters in Scavenger returns deep copies of objects making it safe for clients
 * to modify data obtained from Scavenger instances.
 */
public class Scavenger {


    /***
     * Implements the builder design pattern to construct Scavenger instances.
     * Using the builder pattern here ensures that the Scavenger is initialised to a valid state,
     * rather than providing setters to the client which may cause a poorly programmed client to
     * alter a scavenger instance in such a way that its state becomes invalid resulting in
     * nondeterministic behaviour.
     * Upper bound wildcards are used to allow subclasses of components to be supplied.
     */
    public static class Builder {

        private Scraper scraper;
        private OCREngine ocrEngine;
        private List<Hunter> hunters;
        private ResultsManager resultsManager;
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
            this.resultsManagerEnabled = resultsManagerEnabled;
            return this;
        }


        /**
         * Releases resources if they will not be used. "Empty" implementations which
         * do nothing are provided allowing the original implementation to be garbage collected.
         * The use of "empty" implementations prevents Scavenger code being littered with null checks
         * wherever a potentially unused resource is accessed.
         */
        private void releaseUnusedResources() {
            ocrEngine = ocrEnabled ? ocrEngine : OCREngine.EMPTY_OCR_ENGINE;
            resultsManager = resultsManagerEnabled ? resultsManager : ResultsManager.EMPTY_RESULT_MANAGER;

            if (!huntingEnabled) {
                hunters = new ArrayList<>();
                hunters.add(Hunter.EMPTY_HUNTER);  // flags every image which simulates hunting being disabled
            }
        }

        /***
         * Initialises default implementations of required components which
         * have not been supplied by the client. These default components are
         * not subject to inline field initialisation because initialising them
         * can be resource expensive, so we wait until we know if they are required
         */
        private void initialiseRequiredDefaults() {
            scraper = (scraper == null) ? new PrntscScraper() : scraper;

            if (ocrEnabled && (ocrEngine == null)) {
                ocrEngine = new OCRTess4J();
            }

            if (huntingEnabled && (hunters == null)) {
                hunters = HunterFactory.getDefaultHunterFactoryInstance().getInitializedHunters();
            }

            if (resultsManagerEnabled && (resultsManager == null)) {
                resultsManager = new ResultsManagerCSV();
            }
        }

        public Scavenger build() {
            releaseUnusedResources();
            initialiseRequiredDefaults();
            return new Scavenger(this);
        }
    }

    private final ResultsManager resultsManager;
    private final ExecutorService imageBufferExecutor;
    private final ExecutorService huntingExecutor;
    private final BlockingQueue<ResultData> resultBuffer;
    private ResultData currentResult;

    private Scavenger(Builder builder) {
        imageBufferExecutor = Executors.newSingleThreadExecutor();
        BlockingQueue<ImageData> imageBuffer = new LinkedBlockingQueue<>();
        imageBufferExecutor.submit(new ImageDataBufferTask(builder.bufferSize, builder.scraper, builder.ocrEngine, imageBuffer));

        huntingExecutor = Executors.newSingleThreadExecutor();
        resultBuffer = new LinkedBlockingQueue<>();
        huntingExecutor.submit(new HuntingTask(imageBuffer, builder.hunters, resultBuffer));

        resultsManager = builder.resultsManager;
        loadInitialResult(); // sets currentResult
    }

    /**
     * Blocks until an initial result is available to represent the scavengers state.
     * This ensures that the Scavenger has a valid state upon initialisation.
     */
    private void loadInitialResult() {
        try {
            // blocks until a result is available
            currentResult = resultBuffer.take();
            resultsManager.addResult(currentResult);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /***
     * A state-testing method. Indicates that the next result is ready to be
     * loaded. This allows clients to test if a call to <i>loadNextResult()</i>
     * will succeed.
     * @return true if it is safe to call <i>loadNextResult()</i>. False if Scavenger
     *          is unable to load any more results.
     */
    public boolean hasNextResult() {
        return (resultBuffer.size() > 0 ? true : false);
    }


    /***
     *  Updates Scavengers state to contain details of the next result. Getters can be used to query for details.
     *  If hunting is disabled, we consider the next image provided by the scraper to be the current result.
     *  If hunting is enabled, we consider the next image flagged by a Hunter to be the current result.
     *  Details of the new current result are passed to the Results Manager.
     *  hasNextImage() is a state-checking method which is to be used to ensure successful calls
     *  to this method.
     *  @Throws IllegalStateException if the Scavenger is unable to load a result.

     */
    public void loadNextResult() {
        try {
            currentResult = resultBuffer.remove();
            resultsManager.addResult(currentResult);
        } catch (NoSuchElementException e) {
            throw new IllegalStateException();
        }

    }



    /***
     * Returns an identifier for the image currently loaded
     * @return ID of the current image
     */
    public String getCurrentImageID() {
        return currentResult.getImageID();
    }

    /***
     * Returns the content of the current result image as a BufferedImage.
     * A deep copy is returned allowing clients to safely modify the returned
     * image without affecting the integrity of Scavengers copy.
      * @return current image as a BufferedImage
     */
    public BufferedImage getCurrentImage() {
        return currentResult.getImageContent();
    }

    /***
     * Returns the text which has been extracted from the image using
     * an OCR engine
     * @return all text visible in the image
     */
    public String getCurrentImageOCRText() {
        return currentResult.getImageText();
    }

    /***
     * Returns the ResultData instance. ResultData instances are immutable
     * making it safe to provide the client direct access to the Scavengers
     * instance of ResultData.
     * @return ResUltData instance containing details of the most recent result
     */
    public ResultData getCurrentResultData() { return currentResult; }


    /**
     * Prints results that have been logged since initialisation.
     */
    public void printResults() {
        resultsManager.printResults();
    }


    /***
     * Prints results before cleaning up and exiting.
     */
    public void printResultsAndExit() {
        resultsManager.printResults();
        resultsManager.exit();
        imageBufferExecutor.shutdownNow();
        huntingExecutor.shutdownNow();
    }

    /***
     * Cleans up and exits
     */
    public void exit() {
        resultsManager.exit();
        System.exit(0);
    }
}

