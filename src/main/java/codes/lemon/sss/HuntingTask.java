package codes.lemon.sss;

import codes.lemon.sss.hunters.Hunter;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/***
 * This task performs continuous hunting on images when provided an image buffer
 * containing valid imageData instances. Hunting is the process of analysing the image
 * and any text extracted using Ocular Character Recognition (OCR) for indicators of sensitive
 * data.
 * When the image buffer is empty this task will block until the buffer is no longer empty.
 * Flagged images are added to the resultBuffer collection. This task will block until it is
 * able to add the result to the collection to ensure no results are discarded.
 **/
class HuntingTask implements Runnable {
    private final BlockingQueue<ImageData> imageBuffer;
    private final List<Hunter> hunters;
    private final BlockingQueue<ResultData> resultBuffer;
    private boolean finished = false;

    /***
     * @param imageBuffer a collection containing valid ImageData instances which will be analysed for sensitive data.
     * @param hunters a collection of Hunter instances which will each analyse every images in imageBuffer
     * @param resultBuffer a collection which will contain result details of images flagged by hunters
     */
    public HuntingTask(BlockingQueue<ImageData> imageBuffer, List<Hunter> hunters, BlockingQueue<ResultData> resultBuffer) {
        assert(imageBuffer != null) : "null value supplied for imageBuffer";
        assert(hunters != null) : "null value supplied for hunters";
        assert(resultBuffer != null) : "null value supplied for ResultBuffer";

        this.imageBuffer = imageBuffer;
        this.hunters = hunters;
        this.resultBuffer = resultBuffer;
    }

    /***
     * Works through images provided by imageBuffer and gives each Hunter the opportunity to analyse
     * each image. When a hunter flags an image as containing sensitive data, that image and details
     * of why it was flagged are added to the resultBuffer as a ResultData instance. When the buffer is
     * empty this method will wait until it is not empty. This method runs until interrupted.
     */
    @Override
    public void run() {
        while (!finished) {
            ImageData image = null;
            try {
                image = imageBuffer.take(); // blocks until an element is available
            } catch (InterruptedException e) {
                shutdown();
                break;
            }
            for (Hunter hunter : hunters) {
                String resultDetails = hunter.hunt(image.getID(), image.getContent(), image.getText());
                if (resultDetails != null) {
                    // successful hunt
                    String resultAuthor = hunter.getHunterModuleName();
                    ResultData result = new ResultDataImp(image, resultAuthor, resultDetails);
                    resultBuffer.add(result); // blocks until result is added
                }
            }
        }
    }

    private void shutdown() {
        finished = true;
    }
}
