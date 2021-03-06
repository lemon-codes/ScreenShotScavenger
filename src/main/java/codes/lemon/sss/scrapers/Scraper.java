package codes.lemon.sss.scrapers;
import java.awt.image.BufferedImage;

/**
 * Scrapes/gathers images from a source (implementation dependent) and provides
 * clients access to these images.
 * The only requirements are that any implementations generate unique IDs for
 * each image and provide access to those images as BufferedImage instances.
 * It is the scrapers duty to load a valid image. If the scraper cannot do so
 * instantly then it must work/wait until it can. If the scraper will not be
 * able to provide any more images then it must throw a NoImageAvailableException
 * to notify the client. A scraper must have a valid image loaded upon initialisation.
 */
public interface Scraper {
    /***
     * Load the next image into the scraper. It is the scrapers duty to load a valid image.
     * If the scraper cannot do so instantly then it must work/wait until it can.
     * If the scraper will not be able to provide any more images then it must throw
     * a NoImageAvailableException to notify the client.
     * @throws NoImageAvailableException if a next image cannot be loaded
     */
    void nextImage() throws NoImageAvailableException;

    /***
     * Returns a unique ID for the image currently loaded
     * in the Scraper
     * @return the unique ID for the currently loaded image. Must not return null.
     */
    String getImageID();

    /***
     * Provides access to the currently loaded image
     * @return the currently loaded image as a BufferedImage. Must not return null.
     */
    BufferedImage getImageContent();

    /**
     * Free resources and finish up
     */
    void shutdown();


    //void registerObserver(Observer O);

}
