package codes.lemon.sss;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;

import java.awt.image.BufferedImage;
import java.io.File;

/***
 * An implementation of OCREngine using Tess4J to perform OCR to extract text from images.
 * Tess4j is a java wrapper for Tesseract, an open source OCR engine.
 * This class makes use of a single static instance of the Tesseract Engine. We limit the number
 * of instances to 1 because the engine is greedy with system resources.
 */
public class OCRTess4J implements OCREngine{
    // tesseract declared as class (static) variable because initialisation & setup
    // are costly and only one instance of the tesseract engine is needed
    private static final Tesseract tesseract = new Tesseract();

    // configure tesseract to a state where it is ready to process images
    static {
        // Extract tessData
        File tessDataFolder = LoadLibs.extractTessResources("tessdata");
        //Set the tessdata path
        tesseract.setDatapath(tessDataFolder.getAbsolutePath());
        // Quick fix for a bug within tess4j related to Environment locale
        CLibrary.INSTANCE.setlocale(CLibrary.LC_ALL, "C");
    }

    /***
     * Extracts text from a given image by using tesseract to perform OCR.
     * If the image contains no recognisable text or an error occurs, an
     * empty string is returned.
     * @param image an image as a BufferedImage
     * @return the text extracted from the image using OCR, else an empty string.
     *
     * */
    @Override
    public String getText(BufferedImage image) {
        // attempt OCR
        String text = null;
        try {
            text = tesseract.doOCR(image);
        }
        catch (TesseractException e) {
            e.printStackTrace();
        }
        
        if (text != null) {
            // OCR was successful
            return text;
        }
        else {
            // image contains no text or error so return empty string instead
            return "";
        }
    }
}
