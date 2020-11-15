package codes.lemon.sss.scrapers;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/***
 * A very basic disk scraper which has been created to load test images
 * during the development of this software.
 * No safety checks or error handling is performed as this class is not
 * intended for production.,
 */
public class DiskScraper implements Scraper {
    private static final String PATH_TO_FOLDER = "./dataset/";
    private Queue<File> images;
    private File currentImage;

    public DiskScraper() {
        images = new LinkedList<>();
        File folder = new File(PATH_TO_FOLDER);
        images.addAll(Arrays.asList(Objects.requireNonNull(folder.listFiles())));
        try {
            nextImage(); // load first image
        } catch (NoImageAvailableException e) {
            System.out.println("Directory \"" + PATH_TO_FOLDER + "\" is empty!");
            e.printStackTrace();
        }
    }
    /***
     * Load the next image into the scraper
     */
    @Override
    public void nextImage() throws NoImageAvailableException {
        if (images.size() > 0) {
            currentImage = images.remove(); // throws error when no images left
        }
        else {
            throw new NoImageAvailableException();
        }

    }

    /***
     * Returns a unique ID for the image currently loaded
     * in the Scraper
     * @return the unique ID for the currently loaded image
     */
    @Override
    public String getImageID() {
        return currentImage.getName();
    }

    /***
     * Provides access to the currently loaded image
     * @return the currently loaded image as a BufferedImage
     */
    @Override
    public BufferedImage getImageContent() {
        BufferedImage image = null;
        try {
            image = ImageIO.read(currentImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    /**
     * Free resources and finish up
     */
    public void shutdown() {
        images = null;
    }
}
