package codes.lemon.sss.scrapers;

import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/***
 * This class represents a prntsc image. The class stores a copy of an image and some metadata.
 * Requires a valid id for a prntsc image. Upon initialisation a PrntscImage
 * object will fetch the absolute url for the image the id refers to.
 * This url will then be accessed to download a copy of the image.
 * The image id, url and the image itself are all stored within the
 * PrntscImage object and accessor methods are provided for future access.
 *
 */
final class PrntscImage {
    private static int rateLimitFailures = 0;  // keeps track of failed image downloads potentially caused by prnt.sc rate limiting connections
    private static final int failuresBeforeWarning = 5; // the number of rate limit failures allowed before a warning is printed
    private final String imageID;
    private final String imageUrl;
    private final BufferedImage imageContent;

    /***
     * Requires a valid prnt.sc image id.
     * This image id is used to retrieve the absolute url pointing to the
     * corresponding image. This image is then downloaded.
     * @param id a valid id for a prnt.sc image
     */
    public PrntscImage(String id) {
        // assume imageID is valid since validity checks are carried out in PrntscCodeGenerator
        imageID = id;
        imageUrl = DataRetrieval.getImageSourceURL(imageID);
        imageContent = DataRetrieval.downloadImage(imageUrl);
    }



    /***
     * Returns true if the image has been successfully downloaded, else false.
     * @return true if the image has been successfully downloaded, else false.
     */
    public boolean containsValidImage() {
        if( imageContent != null) {
            return true;
        }
        return false;
    }

    /***
     * Returns the id of the image stored
     * @return id of stored image
     */
    public String getImageID() {
        return imageID;
    }

    /***
     * Returns the url that the stored image was downloaded from
     * @return the url that was used to download the stored image
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /***
     * Returns the contents of the stored image as a BufferedImage object
     * @return the stored image as a BufferedImage object
     */
    public BufferedImage getImageContent() {
        return imageContent;
    }


    /***
     * A static nested class to retrieve data (image urls and image data) from prnt.sc servers.
     * These methods operate as stateless functions. They make use of local variables only so should
     * be safe to use concurrently.
     * These methods were made static to reduce the overhead of creating PrntscImage objects since a large
     * number of these objects will be created. The static nested DataRetrieval class was added to strengthen
     * class cohesion of the outer (PrntscImage) class.
     */
    private static class DataRetrieval {
        private static final String URL_BASE = "https://prnt.sc/";
        private static final String HTTP_CLIENT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36"; // Chrome 74 on Windows 10

        /***
         * Given a 6 digit prnt.sc image id this method will download the
         * web page associated with that image ID and extract the absolute URL
         * to the image.
         * @return absolute URL to the image associated with image ID on prnt.sc else null
         */
        private static String getImageSourceURL(String imageID) {   // TODO: throws imageSourceUrlException
            try {
                Document doc = Jsoup.connect(URL_BASE + imageID).get();
                Elements media = doc.select("[src]");

                for (Element src : media) {
                    if (src.normalName().equals("img")) {
                        Element img = src.getElementById("screenshot-image");
                        if (img != null) {
                            String imgURL = img.absUrl("src");
                            //System.out.println(imgURL);
                            return imgURL;
                        }
                    }
                }
            } catch (IOException e) {
                // no need to count potential rate limiting/blocking errors here since they will be counted
                // in downloadImage()

                //e.printStackTrace();
            }
            return null;
        }

        /**
         * Downloads a PNG image when given an absolute url to an image.
         * This image is then written into a BufferedImage object.
         * A custom user agent is used because prnt.sc blocks non-browser user agents.
         * @param imageUrl an absolute url to a PNG file
         * @return an Image object containing the image located at imageUrl else null
         */
        private static BufferedImage downloadImage(String imageUrl) { //TODO: throw exception
            HttpsURLConnection connection  = null;
            try {
                URL url = new URL(imageUrl);
                connection = (HttpsURLConnection) url.openConnection();
                connection.setConnectTimeout(1500);
                connection.setReadTimeout(10000);
                connection.setRequestProperty(
                        "User-Agent",
                        HTTP_CLIENT_USER_AGENT);

                BufferedImage image = ImageIO.read(connection.getInputStream());
                connection.disconnect();
                return image;

            } catch (IOException e) {
                // Keeps track of failed image downloads potentially caused by prnt.sc rate limiting connections and ip blocking
                // We only count this exception because it is thrown when a HTTP 403 response is received (ip blocking via cloudlfare in action)
                // Although this exception could be thrown for other reasons, in this use case rate limiting/blocking is the most probable cause
                rateLimitFailures+=1;
                if (rateLimitFailures % failuresBeforeWarning == 0) {
                    System.out.format("WARNING: It appears your ip address may have been blocked by prnt.sc. %d images have failed to download%n", rateLimitFailures);
                }
                //e.printStackTrace();
                if (connection != null) {
                    connection.disconnect();
                }
                return null;


            } catch (Exception e) {
                if (connection != null) {
                    connection.disconnect();
                }
                return null;
            }
        }

    }
}
