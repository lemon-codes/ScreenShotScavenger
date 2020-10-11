package codes.lemon.sss;

import java.util.List;

/***
 * A results manager accepts one result at a time. How the result data is processed
 * thereafter is up to the implementation. The only requirement is that the result
 * manager can print the details of previously stored results when the client calls
 * printResults()
 */
public interface ResultsManager {
    ResultsManager EMPTY_RESULT_MANAGER = new ResultsManagerEmpty();

    /***
     * Stores the details of a single result.
     * @param result a result to be stored.
     */
    void addResult(ResultData result);

    /***
     * Prints out the abbreviated details of every result being stored.
     */
    void printResults();


    /***
     * Exits gracefully by closing open resources
     */
    void exit();
}
