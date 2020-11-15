# ScreenShotScavenger v0.9
Notice: Version 0.9. Project is still in early development and APIs may be subject to 
change before final release.
## Overview
ScreenShotScavenger is a recon tool designed to identify leaks of sensitive information 
in screenshot images. Ocular Chatacter Recognition (OCR) is used to extract visible text 
from images to aid analysis. The modular design makes it simple to obtain images from a 
custom source, incorporate new technologies and to hunt for specific types of information.

ScreenShotScavenger is multithreaded. Images are obtained and analysed continuously in 
background threads allowing us to have results preloaded before the client requests them.
This greatly improves ScreenShotScavenger's response time to client requests.  

A Scavenger instance is constructed using a Builder, allowing custom components to be 
supplied by the client and for certain features to be disabled when not required.

The scavenger is stateful. It holds the details of one flagged image at a time, until 
the client requests the next flagged image be loaded. All result details are passed to 
the result manager in the background. 

ScreenShotScavenger makes use of a scraper to obtain images for analysis. A buffer sits 
between the Scraper and the Scavenger to reduce the impact of any latency issues caused 
by Scraper implementations potential reliance on network I/O. New scrapers can be written 
and used by ScreenShotScavenger to obtain images from a custom source. The default 
implementation obtains screenshot images hosted publicly at prnt.sc (LightShot). It is 
multithreaded, however rate limiting performed on requests by LightShot servers limit 
the potential. It was trivial to write a Scraper for images hosted at prnt.sc as the images
 are identified by 6 digit codes which can be iterated through to obtain consecutive images. 
 This is something the LightShot team should seriously consider changing to reduce the chances 
 of information leaks being found by malicious actors. 

Ocular Character Recognition is utilised to extract visible text from images provided by the 
scraper. The default implementation uses the Tesseract Engine (via Tess4J) to perform OCR. 
No pre-processing is performed on images as Tesseract is able to accurately extract text 
from screenshot images without pre-processing. ScreenShotScavengers modular design makes 
it trivial to utilise a different OCR engine or to perform pre-processing on images for 
increased accuracy if the use case requires it. 

Info leaks are found by Hunter modules. Hunter modules are provided access to each image 
and the text extracted via OCR. Multiple Hunter modules can be ran against each image. 
New Hunter modules can be written and used by ScreenShotScavenger in order to utilise 
new methods of analysis and to hunt for specific types of information leaks. The provided 
Hunter modules look for multiple keywords and patterns in the extracted text to find a variety 
of sensitive data. A Hunter module which makes use of TemplateMatching to identify logos inside 
screenshots may be provided soon.  

Results are stored by the ResultManager. New ResultManagers can be written to accommodate 
varying use cases. The default implementation stores flagged images to disk in a folder 
named "HuntedImages" and stores abbreviated result details on disk in a CSV file names 
"AbbreviatedResults.csv". Abbreviated results include imageID, the unique name of hunter 
module that flagged image, reason for being flagged. 

## Requirements/Dependencies
Environment dependencies:
* Tesseract (any version supported by Tess4J. Tested with Tesseract v3 & v4)
* Java SE 14
* Maven

Package dependencies managed by Maven:
* Tess4J v4.5.1 - A Tesseract wrapper for java, used to perform Ocular Character Recognition (OCR)
* jSoup v1.13.1 - Used by the prnt.sc Scraper to extract absolute image urls from web pages.
* OpenCSV v5.2 - Used by the results manager to save results to disk




##  Supported File Formats
Supports all file formats supported by Tess4J:
* PNG
* JPEG
* GIF 
* TIFF
* BMP

## Usage
To run an example usage which hunts for 50 images hosted at prnt.sc before printing and 
saving results use:

    ExampleUsage example = new ExampleUsage();<br />
    example.run();

To manually instantiate a new Scavenger instance, the Builder must be used. This allows the 
client to supply custom implementations of key components (Scraper, OCREngine, Hunters, 
ResultManager) for use by the Scavenger. It also 
allows for certain functionality to be disabled when not required. In cases where 
functionality is enabled but no custom implementation has been supplied, default 
implementations will be used. Default implementations are initialised just in time when 
build() is called. This allows us to initialise only the default implementations needed at 
run time and to free resources which will not be used. Image and result buffer sizes can also
be set. 

    // Create instance with all functionality enabled and default implementations used <br />
    Scavenger scavenger = new Scavenger.Builder().build();  
    
    // Create instance with custom scraper and image buffer size. Hunting and report manager disabled.
    Scavenger scavenger = new Scavenger.Builder().setScraper(new DiskScraper()).setImageBufferSize(8).enableHunting(false).enableReportsManager(false).build();

See Builder documentation for a full list of customisations available. All methods which 
accept custom components use upper bound wildcards to allow subclasses of implementations 
to be used.
The Builder ensures that all Scavenger are instantiated to a valid and optimal state.

From initialisation the Scavengers state will contain details of a result. The Scavenger's 
state can be queried using getters to obtain details about the currently loaded result.
(Note: full result details are handled by the Result Manager).
    
    BufferedImage image = scavenger.getResultImage();
    String imageID = scavenger.getResultImageID();
    String imageText = scavenger.getResultImageText();
    String resultAuthor = scavenger.getResultAuthor();
    String resultDetails = scavenger.getResultComment();

The Scraper also provides access to the codes.lemon.sss.ResultData instance which 
provides its own accessors to the fields listed above.

    ResultData result = scavenger.getResultData();
    BufferedImage image = result.getImageContent();

Scavenger provides two state-checking methods. The first `boolean hasNextResult()` allows clients 
to check that the Scavenger is ready to load the next result. The second `boolean isFinished()` allows
clients to check if the Scacenger is finished and will be unable to provide more results in the future.
Clients can load the next result by calling `void loadNextResult();`. Clients are required to 
use `hasNextResult()` to ensure they only call `loadNextImage()` at times when it is safe to do so. 
Clients which fail to check`hasNextResult()` before attempting to load the next result risk 
triggering an unchecked IllegalStateException. 
It was decided a checked exception was not appropriate since it should
not expect to be encountered if clients use the state checking method as expected. The use of an 
unchecked exception allows for clean client code (without try catch) as shown below. 

    while (!scavenger.isFinished()) {
        while (scavenger.hasNextResult() {
            scavenger.getNextResult();
            // process result before loading next result when available
        }
    }
It is recommended that clients use the `isFinished()` method as shown above to prevent clients 
hanging whilst waiting for `hasNextImage()` to return true (which will never happen if all 
images have been processed).

To shutdown gracefully and ensure results are written to disk (by closing open resources) call
    
     scavenger.exit()

Alternatively, to print results first before exit, call

    scavenger.printResultsAndExit()

## Example Results
The default ResultManager implementation is ResultManagerCSV. It stores a copy of all hunted
images in a folder named "huntedImages" which is created in the current working directory. 
Result details are stored in CSV format in a file names "AbbreviatedResults.csv", which is
also created in the current working directory. Using Below is an example of results printed to STDOUT
by ResultManagerCSV.

    n6xzpt.png | SENSITIVE_KEYWORD_HUNTER | Detected keyword: "password"
    n6xzmi.png | PATTERN_MATCHING_HUNTER | "redacted@redacted.com" matched with regex: [a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+
    n6xzjg.png | SENSITIVE_KEYWORD_HUNTER | Detected keyword: "token"
    n6xzi4.png | PATTERN_MATCHING_HUNTER | "redacted@redacted.com" matched with regex: [a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+
    n6xzgf.png | SENSITIVE_KEYWORD_HUNTER | Detected keyword: "private"
    n6y01c.png | SENSITIVE_KEYWORD_HUNTER | Detected keyword: "password"
    n6xzmr.png | PATTERN_MATCHING_HUNTER | "redacted@redacted.com" matched with regex: [a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+
    n6y0er.png | SENSITIVE_KEYWORD_HUNTER | Detected keyword: "password"
    n6xzh3.png | SENSITIVE_KEYWORD_HUNTER | Detected keyword: "password"
    n6xzcd.png | PATTERN_MATCHING_HUNTER | "redacted@redacted.com" matched with regex: [a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+
    n6y006.png | SENSITIVE_KEYWORD_HUNTER | Detected keyword: "password"

## Known Defects
Some versions of Tesseract write a warning message to stdout if a dpi value is not present in 
an images metadata. This occurs when images are being scraped from a source which has previously 
stripped all image metadata. This does not appear to effect the end results since Tesseract 
uses a reasonable dpi in such cases and is still able to extract the text accurately. For the 
most part the only impact of  this defect is the unsightly warning messages, however there 
may be edge cases (images with very high or low dpi) where this impacts the accuracy of the 
results. I am unable to fix this without delving into the code base of Tess4J which falls 
outside the scope of this project and will remain so due to time constraints imposed by myself. 
Tesseract v3 does not produce such warning messages and has similarly accurate results as 
Tesseract v4 for this use case. 

## Credit

This project was inspired by [shotlooter](https://github.com/utkusen/shotlooter)