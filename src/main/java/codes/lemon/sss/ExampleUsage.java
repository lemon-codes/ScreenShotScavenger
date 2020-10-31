package codes.lemon.sss;

import codes.lemon.sss.scrapers.DiskScraper;
import codes.lemon.sss.scrapers.PrntscScraper;

import java.time.Duration;
import java.time.Instant;

public class ExampleUsage {
    private final Scavenger scavenger;

    public ExampleUsage() {
        //scavenger = new Scavenger.Builder().build();

        //scavenger = new Scavenger.Builder().setScraper(new PrntscScraper("aaaaaa")).build();
        scavenger = new Scavenger.Builder().setScraper(new DiskScraper()).enableHunting(false).build();
        //scavenger = new Scavenger.Builder().setScraper(new DiskScraper()).enableResultsManager(false).build();
    }
    /***
     * Find 50 images that our Hunter modules have flagged as potentially containing sensitive information.
     * Save and print results.
     */
    public void run() {
        int imagesFound = 0;
        Instant start = Instant.now();
        while (imagesFound < 500) {
            while (scavenger.hasNextResult()) {
                scavenger.loadNextResult();
                imagesFound++;
                System.out.printf("Flagged image with ID: %s\tTotal images flagged: %d%n", scavenger.getCurrentImageID(), imagesFound);
            }
        }
        Instant end = Instant.now();
        scavenger.printResultsAndExit();

        // execution time
        long executionTime = Duration.between(start, end).toSeconds();
        System.out.println("Execution time: " + executionTime + " seconds");
    }

    public static void main(String[] args) {


        ExampleUsage example = new ExampleUsage();
        example.run();
    }
}