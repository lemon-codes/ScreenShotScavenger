package codes.lemon.sss;

import codes.lemon.sss.scrapers.DiskScraper;
import codes.lemon.sss.scrapers.PrntscScraper;

public class ExampleUsage {
    private final Scavenger scavenger;

    public ExampleUsage() {
        scavenger = new Scavenger.Builder().build();

        //scavenger = new Scavenger.Builder().setScraper(new PrntscScraper("aaaaaa")).build();
        //scavenger = new Scavenger.Builder().setScraper(new DiskScraper()).build();
        //scavenger = new Scavenger.Builder().setScraper(new DiskScraper()).enableResultsManager(false).build();
    }
    /***
     * Find 50 images that our Hunter modules have flagged as potentially containing sensitive information.
     * Save and print results.
     */
    public void run() {
        int imagesFound = 0;
        while (imagesFound<50) {
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