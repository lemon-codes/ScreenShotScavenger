package codes.lemon.sss;

import com.opencsv.CSVWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/***
 * A results manager which accepts one result at a time.
 * Results can be added continuously and all results and details about those
 * results are stored in memory and on disk by default. A copy of each image
 * is saved to disk as a png file. Details of each result are written to a CSV file.
 *
 * */
public class ResultsManagerCSV implements ResultsManager {
    private static final boolean SAVE_IMAGES_TO_DISK = true;
    private static final boolean SAVE_RESULTS_TO_DISK = true;
    private static final boolean STORE_RESULTS_IN_MEMORY = true;  // set to false to reduce memory footprint. printing results will no longer work
    private static final String OUT_PATH = "./huntedImages/";
    private static final String RESULTS_FILE_NAME = "AbbreviatedResults.csv";
    private final List<ResultData> results;
    private CSVWriter resultWriter;

    public ResultsManagerCSV() {
        results = new ArrayList<>();
        if (SAVE_IMAGES_TO_DISK) {
            createOutFolder();
        }
        if (SAVE_RESULTS_TO_DISK) {
            // TODO: move to new method
            File resultsFile = new File(RESULTS_FILE_NAME);
            try {
                FileWriter writer = new FileWriter(resultsFile);
                resultWriter = new CSVWriter(writer);
                String[] header = {"Image ID", "Result Author", "Result Details"};
                resultWriter.writeNext(header);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            assert(resultWriter != null) : "CSVWriter was not initialized";
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

    /***
     * Stores the details of a single result. Validity checks are performed on all data supplied
     * before storing the result. Any results which do not indicate an image has been flagged are discarded.
     * If all data supplied is determined to be valid, the details of the result are stored in memory
     * along with the image. If enabled, the image is saved to disk in the directory defined by OUT_PATH
     * with the resultID as the filename.
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
                String[] data = {result.getImageID(), result.getAuthor(), result.getDetails()};
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
     * Prints out the abbreviated details of every result being stored.
     * The abbreviated details include Image ID, Result Author and Result Details.
     * For a non-abbreviated printout of results use printResults();
     */
    @Override
    public void printResults() {
        System.out.println("Image ID | Result Author | Result Details");
        for (ResultData result : results) {
            System.out.println(result.getImageID() + " | " + result.getAuthor() + " | " + result.getDetails());
        }
    }

    /*
     * Returns a raw copy of all results.
     * @return a list of all Results.

    @Override
    public List<ResultData> getResults() {
        return results;
    }
    */

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


}
