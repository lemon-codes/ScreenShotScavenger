package codes.lemon.sss.hunters;

import java.util.List;

/***
 * Implementations of this interface provide access to initialised
 * Hunter instances. A default implementation is provided.
 */
public interface HunterFactory {
    // initialise the default implementation of this interface once
    HunterFactory DEFAULT_IMPLEMENTATION = new HunterFactoryDefaultImp();

    /***
     * Returns an initialised instance of the default implementation of this interface
     * @return an instance which implements this interface
     */
    static HunterFactory getDefaultHunterFactoryInstance() { return DEFAULT_IMPLEMENTATION; }


    /***
     * Provides access to an initialized instances of all pre-defined
     * Hunter modules. Instances are returned as a list
     * @return a list of initialized instances of all pre-defined Hunter modules.
     */
    List<Hunter> getInitializedHunters();


}
