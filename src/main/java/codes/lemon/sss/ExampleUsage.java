package codes.lemon.sss;

import codes.lemon.sss.hunters.HunterFactory;
import codes.lemon.sss.scrapers.DiskScraper;

public class ExampleUsage {
    private final Scavenger scavenger;

    public ExampleUsage() {

        //scavenger = new Scavenger();
        scavenger = new Scavenger.Builder().setScraper(new DiskScraper()).build();
    }
    /***
     * Find 50 images that our Hunter modules have flagged as potentially containing sensitive information.
     * Save and print results.
     */
    public void run() {
        int imagesFound = 0;
        while (imagesFound<50) {
            // TODO: implement scavenger.hasNextImage() to check the scraper hasn't dried up.
            //       When the scraper doesn't have another image to return it could throw an error,
            //       When the scavenger detects this it can return false for hasNextImage().
            //       Still need to consider what to do if images run out during loadNextHuntedImage();
            //       Could hunt an image in advance and store as nextHuntedImage in scavenger. When images
            //       run out hasNextImage() can be false before another call to nextHuntedImage()l
            scavenger.loadNextImage();
            imagesFound++;
            System.out.printf("Flagged image with ID: %s\tTotal images flagged: %d%n", scavenger.getCurrentImageID(), imagesFound);
        }
        scavenger.printResultsAndExit();
    }

    public static void main(String[] args) {


        ExampleUsage example = new ExampleUsage();
        example.run();
    }
}