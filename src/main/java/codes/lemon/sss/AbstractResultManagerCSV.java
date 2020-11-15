package codes.lemon.sss;

import com.opencsv.CSVWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/***
 * A abstract results manager which accepts one result at a time.
 * Results can be added continuously and all results and details about those
 * results are stored in memory and on disk by default. A copy of each image
 * is saved to disk as a png file in a folder named "huntedImages" located in
 * the current working directory. Details of each result are written to a CSV file
 * named "Results.csv" also in the current working directory.
 * By default hunted images and result details are saved to disk and stored in
 * memory. Exactly what details are written to Results.csv and printed are
 * determined by the subclasses which implement the abstract methods provided
 * by this class.
 * */
abstract class AbstractResultManagerCSV implements ResultManager {
    private static final boolean SAVE_IMAGES_TO_DISK = true;
    private static final boolean SAVE_RESULTS_TO_DISK = true;
    private static final boolean STORE_RESULTS_IN_MEMORY = true;  // set to false to reduce memory footprint. printing results will no longer work
    private static final String OUT_PATH = "./huntedImages/";
    private static final String RESULTS_FILE_NAME = "Results.csv";
    private final List<ResultData> results;
    private CSVWriter resultWriter;

    public AbstractResultManagerCSV() {
        results = new ArrayList<>();
        if (SAVE_IMAGES_TO_DISK) {
            createOutFolder();
        }
        if (SAVE_RESULTS_TO_DISK) {
            prepareResultsFile();
        }
    }

    /**
     * Checks if the output destination folder(directory) already exists.
     * If it doesn't already exist we create the directory.
     * The user is warned of any problems.
     * TODO: throw an exception instead
     */
    private void createOutFolder() {
        File f = new File(OUT_PATH);
        if (!f.isDirectory()) {
            boolean directoryCreated = f.mkdirs();
            if (!directoryCreated) {
                // there was a problem creating the directory. Warn the user and continue
                System.out.println("WARNING: there was a problem when creating the output directory. ");
                System.out.println("No images will be saved to storage.");
            }
        }
    }

    /**
     * Creates a new CSV file in the current working directory to store result details.
     * Writes a header to the CSV file. The contents of the header is determined by
     * subclasses which implement getCSVHeader().
     * If a file with the same name already exists it is overwritten.
     */
    private void prepareResultsFile() {
        File resultsFile = new File(RESULTS_FILE_NAME);
        try {
            FileWriter writer = new FileWriter(resultsFile);
            resultWriter = new CSVWriter(writer);
            String[] header = getCSVHeader();// {"Image ID", "Result Author", "Result Details"};
            resultWriter.writeNext(header);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        assert(resultWriter != null) : "CSVWriter was not initialized";
    }

    /***
     * Stores the details of a single result. Validity checks are performed on all data supplied
     * before storing the result. Any results which do not indicate an image has been flagged are discarded.
     * If all data supplied is determined to be valid, the details of the result are stored in memory
     * along with the image. If enabled, the image is saved to disk in the directory defined by OUT_PATH
     * with the resultID as the filename. If enabled, details of the result are written to the results.csv
     * file. Subclasses which implement getResultDetailsToSave() determine which details are written to disk.
     * @param result a result to be stored.
     */
    @Override
    public void addResult(ResultData result) {
        if (verifyResult(result)) {
            if (STORE_RESULTS_IN_MEMORY) {
                results.add(result);
            }
            if (SAVE_IMAGES_TO_DISK) {
                saveImageToDisk(result.getImageID(), result.getImageContent());
            }
            if (SAVE_RESULTS_TO_DISK) {
                // header = {"Image ID", "Result Author", "Result Details"}
                String[] data = Objects.requireNonNull(getResultDetailsToSave(result));//{result.getImageID(), result.getAuthor(), result.getDetails()};
                resultWriter.writeNext(data);
            }
        }
    }


    /***
     * Verifies result data by checking for null values.
     * @param result result data
     * @return true if the data contains no null values.
     */
    private boolean verifyResult(ResultData result) {
        // return false upon the first detection of a null value
        if (result.getImageID() == null) {
            return false;
        }
        if (result.getImageContent() == null) {
            return false;
        }
        if (result.getImageText() == null) {
            return false;
        }
        if (result.getAuthor() == null) {
            return false;
        }
        if (result.getDetails() == null) {
            return false;
        }

        // result contains no null values
        return true;
    }

    /***
     * Saves an image to disk in the directory defined by OUT_PATH.
     * The image is saved with the supplied filename.
     * Assumes image is not null.
     * @param filename the filename to be used when saving the image to disk
     * @param image the image to be saved to disk as a BufferedImage. Assumed not null.
     */
    // TODO: throw exception
    private void saveImageToDisk(String filename, BufferedImage image) {
        File file = new File(OUT_PATH + filename + ".png");
        try {
            assert (image != null) : "image is null";
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     * Prints out details of every result being stored.
     * Exactly what details are printed is determined by subclasses which
     * implement getHeaderToPrint() and getResultDetailsToPrint()
     */
    @Override
    public void printResults() {
        Arrays.stream(getHeaderToPrint()).map(f -> f.concat(" | ")).forEach(System.out::print);
        System.out.println();
        for (ResultData result : results) {
            String[] details = Objects.requireNonNull(getResultDetailsToPrint(result));
            Arrays.stream(details).map(f -> f.concat(" | ")).forEach(System.out::print);
            System.out.println();
            //System.out.println(result.getImageID() + " | " + result.getAuthor() + " | " + result.getDetails());
        }
    }

    /***
     * Exits gracefully by closing open resources
     */
    @Override
    public void exit() {
        if (resultWriter != null) {
            try {
                resultWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        resultWriter = null; // dereference in memory so any contained objects will be garbage collected

    }

    /**
     * Returns the header row at the top of the CSV file.
     * @return an array of column titles. Each element in the array represents a column title.
     */
    protected abstract String[] getCSVHeader();

    /**
     * Extracts the result details to be written to disk from the given ResultData instance.
     * @param result result data. Must not be null
     * @return an array where each element contains a detail extracted from result.
     */
    protected abstract String[] getResultDetailsToSave(ResultData result);


    /**
     * Returns the header row to be printed above result listings. Each element represents
     * the title of a column in the printed results.
     * @return an array of column titles. Each element in the array represents a column title.
     */
    protected abstract String[] getHeaderToPrint();

    /**
     * Extracts the result details to be printed from the given ResultData instance.
     * @param result result data. Must not be null
     * @return an array where each element contains a detail extracted from result.
     */
    protected abstract String[] getResultDetailsToPrint(ResultData result);


}
