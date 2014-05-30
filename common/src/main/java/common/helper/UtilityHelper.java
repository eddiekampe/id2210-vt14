package common.helper;

/**
 * id2210-vt14 - common.helper
 * User: eddkam
 * Date: 5/26/14
 */
public class UtilityHelper {

    public static Float calculateUtility(int numFreeCpus, int freeMemoryInMbs) {

        if (freeMemoryInMbs == 0) {
            freeMemoryInMbs = 1;
        }
        return numFreeCpus + (1.0f - 1.0f / freeMemoryInMbs);
    }
}
