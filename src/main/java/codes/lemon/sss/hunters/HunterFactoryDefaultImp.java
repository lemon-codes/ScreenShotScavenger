package codes.lemon.sss.hunters;

import java.util.ArrayList;
import java.util.List;

/***
 * Provides access to initialized instances of all pre-defined Hunter modules.
 * Because Hunters should act as stateless functions, only one instance of each
 * Hunter module is ever initialized. Successive calls to getInitializedHunters()
 * will return the same list of instances.
 * This functionality is provided through this class in order to keep
 * Hunter module implementation details within the sss.hunters package.
 */

class HunterFactoryDefaultImp implements HunterFactory {

    // Eagerly initialise hunters once.
    private static final List<Hunter> HUNTERS = initializeHunters();


    /***
     * Initializes one instance of each defined Hunter module and returns
     * these instances in a List.
     * @return a list of the newly initialised Hunter instances
     */
    private static List<Hunter> initializeHunters() {
        List<Hunter> hunterModules = new ArrayList<>();
        hunterModules.add(new PatternMatchingHunter());
        hunterModules.add(new SensitiveKeywordHunter());
        assert hunterModules.size() > 0 : "no hunter modules initialized";
        return hunterModules;

        // below are now deprecated
        //hunterModules.add(new PasswordHunter());
        //hunterModules.add(new PrivateHunter());
        //hunterModules.add(new EmailAddressHunter());
    }

    /***
     * Provides access to an initialized instances of all pre-defined
     * Hunter modules. Instances are returned as a list
     * @return a list of initialised instances of all pre-definded Hunter modules.
     */
    public List<Hunter> getInitializedHunters() {
        return HUNTERS;
    }

}
