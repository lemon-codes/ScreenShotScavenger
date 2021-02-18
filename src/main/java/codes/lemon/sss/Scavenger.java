package codes.lemon.sss;
import java.util.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.*;

import codes.lemon.sss.hunters.*;
import codes.lemon.sss.results.AbbreviatedResultManagerCSV;
import codes.lemon.sss.results.ResultData;
import codes.lemon.sss.results.ResultManager;
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
        private ResultManager resultManager;
        private int imageBufferSize = 16;
        private int resultBufferSize = 8;
        private boolean ocrEnabled = true;
        private boolean huntingEnabled = true;
        private boolean resultManagerEnabled = true;

        /**
         *  Sets the scraper that will be used to obtain images for analysis
         * @param scraper the scraper
         * @param <T> Scraper or subtype of Scraper
         * @return This builder
         */
        // upper bound wildcard used to allow subclasses of Scraper implementations to be used
        public <T extends Scraper> Builder setScraper(T scraper) {
            this.scraper = Objects.requireNonNull(scraper);
            return this;
        }

        /**
         * Sets the OCR engine that will be used to extract text from images
         * @param ocrEngine the ocrEngine
         * @param <T> OCREngine or a subtype of OCREngine
         * @return This builder
         */
        public <T extends OCREngine> Builder setOCREngine(T ocrEngine) {
            this.ocrEngine = Objects.requireNonNull(ocrEngine);
            return this;
        }

        /**
         * Sets the HunterFactory that will provide Hunter implementations to
         * analyse images & extracted text
         * @param hunterFactory the HunterFactory
         * @param <T> HunterFactory or a subtype of HunterFactory
         * @return This builder
         */
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

        /**
         * Sets the results manager which will log results.
         * @param resultManager the results manager
         * @param <T> ResultManager or subtype of ResultManager
         * @return This builder
         */
        public <T extends ResultManager> Builder setResultManager(T resultManager) {
            this.resultManager = Objects.requireNonNull(resultManager);
            return this;
        }

        /**
         * Sets the image buffer size. This determines the number of images which will be
         * preloaded and stored in a buffer until they are required.
         * @param bufferSize maximum number of images in buffer. Must be > 0
         * @return This builder
         */
        public Builder setImageBufferSize(int bufferSize) {
            if (bufferSize > 0) {
                this.imageBufferSize = bufferSize;
            }
            return this;
        }

        /**
         * Sets the result buffer size. This determines the number of results which will be
         * found in advance of clients requests in order to reduce load times when clients request the
         * next result be loaded.
         * @param bufferSize maximum number of results in buffer. Must be > 0
         * @return This builder
         */
        public Builder setResultBufferSize(int bufferSize) {
            if (bufferSize > 0) {
                this.resultBufferSize = bufferSize;
            }
            return this;
        }

        /**
         * Enable(default) or disable OCR functionality. Disabling OCR functionality will
         * override <i>setOCREngine()</i>.
         * @param ocrEnabled true to enable, false to disable
         * @return This builder
         */
        public Builder enableOCR(boolean ocrEnabled) {
            this.ocrEnabled = ocrEnabled;
            return this;
        }

        /**
         * Enable(default) or disable hunting functionality. Disabling hunting will override
         * <i>setHunterFactory()</i>. This allows Scavenger to return image data for every image
         * provided by the Scraper.
         * @param huntingEnabled true to enable, false to disable
         * @return This builder.
         */
        public Builder enableHunting(boolean huntingEnabled) {
            this.huntingEnabled = huntingEnabled;
            return this;
        }

        /**
         * Enable(default) or disable results manager functionality. Disabling results manager
         * will override <i>setResultManager()</i>
         * @param resultManagerEnabled true to enable, false to disable
         * @return This builder.
         */
        public Builder enableResultManager(boolean resultManagerEnabled) {
            this.resultManagerEnabled = resultManagerEnabled;
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
            resultManager = resultManagerEnabled ? resultManager : ResultManager.EMPTY_RESULT_MANAGER;

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

            if (resultManagerEnabled && (resultManager == null)) {
                resultManager = new AbbreviatedResultManagerCSV();
            }
        }

        /***
         * Builds and initialises a Scavenger instance to a valid state before providing clients
         * access to the Scavenger instance. Provides default implementations in cases where certain
         * functionality has been enabled but no custom implementation has been provided. Also releases
         * unused resources when certain functionality is disabled.
         * @return a Scavenger instance initialised to a valid state
         */
        public Scavenger build() {
            releaseUnusedResources();
            initialiseRequiredDefaults();
            return new Scavenger(this);
        }
    }




    private final ResultManager resultManager;
    private final ExecutorService imageBufferExecutor;
    private final Future<?> imageBufferStatus;  // indicates that image buffer is no longer being filled as scraper is empty
    private final ExecutorService huntingExecutor;
    private final Future<?> huntingStatus;  // indicates that hunting is finished as there are no more images to process
    private final BlockingQueue<ResultData> resultBuffer;
    private ResultData currentResult;

    private Scavenger(Builder builder) {
        /*
        The following Tasks are limited to 1 thread each as they utilise components
        which are not guaranteed to be thread-safe.
        Thread-safety is achieved through thread-confinement.
         */

        // start a task in a background thread to obtain images from the Scraper
        // and perform OCR before placing image data in a buffer
        imageBufferExecutor = Executors.newSingleThreadExecutor();  // Limited to 1 thread as Scraper is not thread safe
        BlockingQueue<ImageData> imageBuffer = new LinkedBlockingQueue<>();
        imageBufferStatus = imageBufferExecutor.submit(new ImageDataBufferTask(builder.scraper, builder.ocrEngine,
                                                                                builder.imageBufferSize, imageBuffer));

        // start a task in a background thread which takes ImageData instances from the buffer
        // and allows hunters to analyse them. This allows us to maintain a buffer of results
        // to drastically reduce client wait times
        huntingExecutor = Executors.newSingleThreadExecutor();
        resultBuffer = new LinkedBlockingQueue<>();
        huntingStatus = huntingExecutor.submit(new HuntingTask(imageBuffer, imageBufferStatus, builder.hunters,
                                                                builder.resultBufferSize, resultBuffer));

        resultManager = builder.resultManager;
        loadInitialResult(); // sets currentResult. Ensures valid state upon initialisation
    }

    /**
     * Blocks until an initial result is available to represent the scavengers state.
     * This ensures that the Scavenger has a valid state upon initialisation.
     */
    private void loadInitialResult() {
        try {
            // blocks until a result is available
            // TODO: consider doing this lazily upon first requests to getters()
            //       this would reduce the time it takes to return from the constructor
            currentResult = resultBuffer.take();
            resultManager.addResult(currentResult);
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
        return (resultBuffer.size() > 0);
    }


    /***
     *  Updates Scavengers state to contain details of the next result. Getters can be used to query for details.
     *  If hunting is disabled, we consider the next image provided by the scraper to be the current result.
     *  If hunting is enabled, we consider the next image flagged by a Hunter to be the current result.
     *  Details of the new current result are passed to the Results Manager.
     *  hasNextImage() is a state-checking method which is to be used to ensure successful calls
     *  to this method.
     *  @throws IllegalStateException if the Scavenger is unable to load a result.

     */
    public void loadNextResult() {
        try {
            currentResult = resultBuffer.remove();
            resultManager.addResult(currentResult);
        } catch (NoSuchElementException e) {
            throw new IllegalStateException();
        }

    }

    /**
     * Returns true if this Scavenger will not be able to load any new results.
     * This occurs when the Scraper cannot provide any more images and all
     * buffers are empty.
     * @return true if the Scraper is finished, else false if it is still working
     */
    public boolean isFinished() {
        if (hasNextResult()) {
            // we still have results in the buffer for the client to load before we are finished
            return false;
        }

        // buffer is empty. Check if it is being refilled
        if (imageBufferStatus.isDone() && huntingStatus.isDone()) {
            // image buffer is no longer being refilled.
            // all images in buffer have been analysed.
            // we are finished.
            return true;
        }

        // there are still images in the buffer to be processed by hunters
        // we may be able to produce more results so we are not yet finished
        return false;
    }



    /***
     * Returns an identifier for the result image currently loaded
     * @return ID of the current image
     */
    public String getResultImageID() {
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
     * Returns the text which has been extracted (using OCR) from the image contained
     * in the current result
     * @return all text visible in the image
     */
    public String getResultImageText() {
        return currentResult.getImageText();
    }

    /**
     * Returns the name of the Hunter which found and authored the current
     * result.
     * @return the name of the Hunter module that flagged the current result
     */
    public String getResultAuthorName() {
        return currentResult.getAuthor();
    }

    /**
     * Returns a comment provided by the Hunter which flagged this result.
     * This comment details why the image was flagged as a result.
     * @return a comment detailing why the image was flagged as a result
     */
    public String getResultComment() {
        return currentResult.getDetails();
    }

    /***
     * Returns the ResultData instance. ResultData instances are immutable
     * making it safe to provide the client direct access to the Scavengers
     * instance of ResultData.
     * @return ResUltData instance containing details of the most recent result
     */
    public ResultData getResultData() { return currentResult; }



    /**
     * Prints results that have been logged since initialisation.
     */
    public void printResults() {
        resultManager.printResults();
    }


    /***
     * Prints results before cleaning up and exiting.
     */
    public void printResultsAndExit() {
        printResults();
        exit();
    }

    /***
     * Cleans up and exits
     */
    public void exit() {
        resultManager.exit();
        imageBufferStatus.cancel(true);
        imageBufferExecutor.shutdownNow();
        huntingStatus.cancel(true);
        huntingExecutor.shutdownNow();
        // awaitTermination() is not called because we don't want client code to wait
    }
}

