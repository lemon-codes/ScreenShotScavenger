# ScreenShotScavenger v0.9
Notice: Version 0.9. Project is still in early development and APIs may be subject to 
change before final release.

## Introduction
ScreenShotScavenger is a recon tool designed to identify leaks of sensitive information 
in screenshot images. Ocular Character Recognition (OCR) is used to extract visible text 
from images to aid analysis. The modular design makes it simple to obtain images from a 
custom source, to hunt for specific types of information, incorporate new technologies in
the hunting process and to alter how results are processed.

## Overview
The Scavenger is stateful. It holds the details of one result (flagged image) at a time
and provides clients with accessors to access details of that result. The 
Scavengers state is updated when the client requests the next result be loaded.

A scraper provides images for analysis. Ocular Character Recognition (OCR) is utilized to 
extract visible text from each image. Hunter modules then analyse each image and
accompanying text for indicators of sensitive data. We have a result when a Hunter finds 
sensitive data. This result can then be loaded by the client. Each result loaded by the
client is passed to a result manager for logging. Custom implementations of all components
mentioned above can be used to tailor ScreenShotScavenger to suit each client's use-case.

ScreenShotScavenger is multithreaded. Images are obtained and analysed continuously in 
background threads allowing us to have results preloaded before the client requests them.
This greatly improves ScreenShotScavenger's response time to client requests.  

A Scavenger instance is constructed using a Builder, allowing custom components to be 
supplied by the client and for certain features to be disabled when not required.
Default implementations are provided for all required components.

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
Before using ScreenShotScavenger, a client must import the required classes/interfaces.
Exactly which imports are required will vary with each use case. Shown below is list
of the imports required to execute the usage examples which follow. For a complete list
of ScreenShotScavenger's exports, see the javadoc.

    import codes.lemon.sss.Scavenger; // ScreenShotScavenger
    import ExampleUsage;  // a basic example client
    import codes.lemon.sss.scrapers.DiskScraper;  // supplies images stored on disk
    import codes.lemon.sss.results.ResultData;  // stores details of a result
    
Other noteworthy classes/interfaces exported by ScreenShotScavenger are:

    import codes.lemon.sss.scraper.Scraper;  // implementations of this interface can be used to supply images to Scavenger 
    import codes.lemon.sss.hunter.HunterFactory; // implementations of this interface can be used to supply Hunter instances to Scavenger
    import codes.lemon.sss.hunter.Hunter;  // implementations of this interface can be used by Scavanger to analyse images for sensitive data
    import codes.lemon.sss.result.ResultManager;  // implementations of this interface can be used by Scavenger to process and log results
    import codes.lemon sss.OCREngine; // implementations of this interface can be used by Scavenger to extract text visible in images.
    
As you may have noticed, some components are contained within subpackages of
`codes.lemon.sss`. These are components which are likely to grow in complexity and 
in number of implementations. The subpackages are as follows; `codes.lemon.sss.scrapers`,
`codes.lemon.sss.hunters` and `codes.lemon.sss.results`. For full details see documentation.

To run an example usage which hunts for 20 images hosted at prnt.sc before printing and 
saving results use:

    ExampleUsage example = new ExampleUsage();
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

    // Create instance with all functionality enabled and default implementations used
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

The Scraper also provides access to the codes.lemon.sss.results.ResultData instance which 
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

## Implementation Details
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
of sensitive data (including private keys & login credentials). A Hunter module which 
makes use of TemplateMatching to identify logos inside screenshots may be provided soon.  

Results are stored by the ResultManager. New ResultManagers can be written to accommodate 
varying use cases. The default implementation stores flagged images to disk in a folder 
named "HuntedImages" and stores abbreviated result details on disk in a CSV file names 
"AbbreviatedResults.csv". Abbreviated results include imageID, the unique name of hunter 
module that flagged image, reason for being flagged. 

## Known Defects

Tesseract may print warnings to STDOUT when it encounters a malformed image, or an image
which is too small (eg 3x3). This could be prevented by analysing all images manually to
ensure they meet Tesseract's preconditions and are valid images before attempting OCR. 
However, since ScreenShotScavenger 
is expected to process large volumes of images in a timely manner, the performance 
penalty from performing such additional checks has been deemed unacceptable. Particularly 
since it is expected to be a rare occurance that such invalid images will be encountered.
## Credit

This project was inspired by [shotlooter](https://github.com/utkusen/shotlooter)