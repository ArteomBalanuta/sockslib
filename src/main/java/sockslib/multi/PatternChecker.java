package sockslib.multi;

import java.util.ArrayList;
import java.util.List;

public class PatternChecker {
    private static final List<Pattern> patterns = new ArrayList<>();
    static {
//        patterns.add(new Pattern("C10C27", null));
//        patterns.add(new Pattern("C313", null));

        // C107112CE multistrike but this is bound to single mob.. C10711... multiple mobs
        //63A5B35E613AFC
        patterns.add(new Pattern("63A5B3", null));
        patterns.add(new Pattern("63A1D3", null));
    }
    public static boolean check(String payload) {
        for (Pattern pattern : patterns) {
            String postfix = pattern.postfix;

            if (postfix == null) {
                return payload.startsWith(pattern.suffix);
            } else {
                return payload.startsWith(pattern.suffix) && payload.endsWith(pattern.postfix);
            }
        }

        /* noting matches */
        return false;
    }
}
