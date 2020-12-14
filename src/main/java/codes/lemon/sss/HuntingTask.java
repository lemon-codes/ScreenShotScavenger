package codes.lemon.sss;

import codes.lemon.sss.hunters.Hunter;
import codes.lemon.sss.results.ResultData;
import codes.lemon.sss.results.ResultDataImp;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

/***
 * This task hunts for indicators of sensitive data in images when provided an image buffer
 * containing valid imageData instances. Results are stored in a fixed size buffer.
 * When the image buffer is empty this task will block until the image buffer is no longer empty.
 * This task monitors the status of the image buffer to ensure it is being refilled. If it is no
 * longer being refilled and the buffer is empty, we finish up.
 * Flagged images are added to the resultBuffer collection. This task will block until it is
 * able to add the result to the collection to ensure no results are discarded.
 **/
class HuntingTask implements Runnable {
    private final BlockingQueue<ImageData> imageBuffer;
    private final Future<?> imageBufferStatus;
    private final List<Hunter> hunters;
    private final int bufferSize;
    private final BlockingQueue<ResultData> resultBuffer;
    private boolean finished = false;


    /***
     * @param imageBuffer a collection containing valid ImageData instances which will be analysed for sensitive data.
     * @param imageBufferStatus indicates that the image buffer is no longer being refilled.
     * @param hunters a collection of Hunter instances which will each analyse every images in imageBuffer
     * @param bufferSize the number of results which can be stored in the buffer. Must be > 0
     * @param resultBuffer a collection which will contain result details of images flagged by hunters
     */
    public HuntingTask(BlockingQueue<ImageData> imageBuffer, Future<?> imageBufferStatus, List<Hunter> hunters,
                       int bufferSize, BlockingQueue<ResultData> resultBuffer) {

        assert(imageBuffer != null) : "null value supplied for imageBuffer";
        assert(imageBufferStatus != null) : "null value supplied for imageBufferStatus";
        assert(hunters != null) : "null value supplied for hunters";
        assert(bufferSize > 0);
        assert(resultBuffer != null) : "null value supplied for ResultBuffer";


        this.imageBuffer = imageBuffer;
        this.imageBufferStatus = imageBufferStatus;
        this.hunters = hunters;
        this.bufferSize = bufferSize;
        this.resultBuffer = resultBuffer;
    }

    /***
     * Works through images provided by imageBuffer and gives each Hunter the opportunity to analyse
     * each image. When a hunter flags an image as containing sensitive data, that image and details
     * of why it was flagged are added to the resultBuffer as a ResultData instance. When the image
     * buffer is empty this method will wait until it is not empty. When the result buffer is full,
     * we wait for clients to remove results from the buffer before hunting for more results.
     */
    @Override
    public void run() {
        while (!finished) {
            while (resultBuffer.size() < bufferSize) {
                if (imageBuffer.size() == 0 && imageBufferStatus.isDone()) {
                    // buffer is empty and no longer being refilled. We are finished
                    finished = true;
                    break;
                }
                ImageData image = null;
                try {
                    image = imageBuffer.take(); // blocks until an element is available
                } catch (InterruptedException e) {
                    finished = true;
                    // Thread.currentThread().interrupt();
                    break;
                }
                for (Hunter hunter : hunters) {
                    String resultDetails = hunter.hunt(image.getID(), image.getContent(), image.getText());
                    if (resultDetails != null) {
                        // successful hunt
                        String resultAuthor = hunter.getHunterModuleName();
                        ResultData result = new ResultDataImp(resultAuthor, resultDetails, image.getID(), image.getContent(), image.getText());
                        resultBuffer.add(result); // blocks until result is added
                    }
                }
            }
        }
    }
}
