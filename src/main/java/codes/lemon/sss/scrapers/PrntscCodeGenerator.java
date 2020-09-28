package codes.lemon.sss.scrapers;
import java.math.*;


/***
 * This code generator produces valid 6 digit image identifier codes
 * for printsc images. These 6 digit codes can contain a mixture of letters 
 * a-z (case sensitive), and numbers 0 - 9. The code generator requires a base 
 * code to be supplied by the user upon initialisation. By treating the 6 digit 
 * code as a base36 ([0-9] + [a-z]) integer we can increment the code by 1 to find the 
 * next valid code. This implementation takes advantage of this to generate IDs sequentially.
 * The class checks the base code is valid, if it isn't then the class fixes the code as best
 * it can.
 * @author lemon
 */
class PrntscCodeGenerator {
    private static final int RADIX = 36;  // len([a-z] + [0-9]) = base36
    private static final int VALID_CODE_LENGTH = 6;
    private String codeCurrent;

    // TODO: implement random code generation for base code.
    public PrntscCodeGenerator(String baseCode) {
        codeCurrent = fixCode(baseCode);
    }

    /***
     * Returns the current code (the code last generated by a call to getNextCode()).
     * If getNextCode has not been called, then the baseCode supplied upon initialisation
     * of an instance of this class will be returned.
     * @return the last code generated by this generator
     */
    public String getCurrentCode() {
        return codeCurrent;
    }

    /***
     * Returns a newly generated image id code for a prnt.sc image.
     * The new id code is generated by incrementing the last generated code.
     * This newly generated code will be store within the code generator, and can be
     * retrieved again with a call to getCurrentCode().
     * Image IDs are generated sequentially with each new call to getNextCode().
     * Image IDs are composed of six base36(0-9,a-z) digits.
     * @return a newly generated valid prnt.sc image id
     */
    public String getNextCode() {
        BigInteger codeOld = new BigInteger(codeCurrent, RADIX); // convert to base 10
        BigInteger codeNext = codeOld.add(BigInteger.valueOf(1));
        String code = codeNext.toString(RADIX);
        codeCurrent = fixCode(code);
        return codeCurrent;
    }

    /**
     * Performs some checks to try and ensure the validity of codeToFix.
     * Removes white space, punctuation, converts uppercase to lowercase,
     * pads to the left with 0 if code length is too short, removes most
     * significant digits if code is too long.
     * @param codeToFix code to be fixed (if it contains errors)
     */
    private String fixCode(String codeToFix) {
        String code = codeToFix.replaceAll("\\p{Punct}", "").replaceAll(" ", "").toLowerCase().trim();

        // Ensure code is of valid length
        while (code.length() > VALID_CODE_LENGTH) {
            // remove the most significant character to reduce length
            code = code.substring(1);
        }

        // check for missing leading 0's which are lost when converting RADIX
        while (code.length() < VALID_CODE_LENGTH) {
            code = "0" + code;
        }

        return code;
    }


}