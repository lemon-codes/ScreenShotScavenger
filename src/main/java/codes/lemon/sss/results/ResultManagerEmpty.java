package codes.lemon.sss.results;

import codes.lemon.sss.results.ResultManager;
import codes.lemon.sss.results.ResultData;

/***
 * A ResultManager implementation which discards all results passed to it.
 */
class ResultManagerEmpty implements ResultManager {
    /***
     * Discards any results passed in
     * @param result a result to be discarded
     */
    @Override
    public void addResult(ResultData result) {
        //do nothing
    }

    /***
     * Prints out the abbreviated details of every result being stored.
     */
    @Override
    public void printResults() {
        System.out.println("Result Manager has been disabled");
    }

    /***
     * Exits gracefully by closing open resources
     */
    @Override
    public void exit() {
        // do nothing;
    }
}
