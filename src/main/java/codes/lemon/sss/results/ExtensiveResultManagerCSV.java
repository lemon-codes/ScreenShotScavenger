package codes.lemon.sss.results;

/***
 * A results manager which accepts one result at a time and saves and prints
 * all (extended) result details. The extended results contain image ID, image content
 * (as a BufferedImage instance), text extracted from the image using Ocular Character
 * Recognition (OCR), result author and the result comment which provides a description
 * of why the image was flagged.
 * Results can be added continuously and all results and details about those
 * results are stored in memory and on disk by default. A copy of each image
 * is saved to disk as a png file in a folder named "huntedImages" located in
 * the current working directory. Details of each result are written to a CSV file
 * named "Results.csv" also in the current working directory.
 * By default hunted images and result details are saved to disk and stored in
 * memory.
 * */
public class ExtensiveResultManagerCSV extends AbstractResultManagerCSV {
    private static final String[] CSV_HEADER = {"Image ID", "Result Author", "Result Details", "Extracted Text"};
    private static final String[] PRINT_HEADER = {"Image ID", "Result Author", "Result Details"};
    /**
     * Returns the header row at the top of the CSV file.
     * This implementation provides a header for extensive results in the following format;
     * Image ID, Result Author, Result Details, Extracted Text.
     * @return an array of column titles. Each element in the array represents a column title.
     */
    @Override
    protected String[] getCSVHeader() {
        return CSV_HEADER;
    }

    /**
     * Returns the header row to be printed above result listings. Each element represents
     * the title of a column in the printed results.
     * This implementation provides a header for abbreviated results in the following format;
     * Image ID, Result Author, Result Details.
     * Image text is not included in printout because some images contain a lot of text and printing
     * it all out makes it impossible to decipher results.
     * @return an array of column titles. Each element in the array represents a column title.
     */
    @Override
    protected String[] getHeaderToPrint() {
        return PRINT_HEADER;
    }

    /**
     * Extracts the result details to be written to disk from the given ResultData instance.
     * This implementation provides a header for extensive results in the following format;
     * Image ID, Result Author, Result Details, Extracted Text.
     * @param result result data. Must not be null
     * @return an array where each element contains a detail extracted from result.
     */
    @Override
    protected String[] getResultDetailsToSave(ResultData result) {
        String[] resultDetails = {result.getImageID(), result.getAuthor(), result.getDetails(), result.getImageText()};
        return resultDetails;
    }



    /**
     * Extracts the result details to be printed from the given ResultData instance.
     * This implementation provides a header for abbreviated results in the following format;
     * Image ID, Result Author, Result Details
     * Image text is not included in printout because some images contain a lot of text and printing
     * it all out makes it impossible to decipher results.
     * @param result result data. Must not be null
     * @return an array where each element contains a detail extracted from result.
     */
    @Override
    protected String[] getResultDetailsToPrint(ResultData result) {
        String[] resultDetails = {result.getImageID(), result.getAuthor(), result.getDetails()};
        return resultDetails;
    }
}
