import java.util.ArrayList;
import java.util.List;

import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.item.PaperToken;
import forge.model.FModel;

/**
 * Headless parse test for GBF token scripts (res/tokenscripts).
 *
 * <p>GbfParseTest only covers the [cards] of the GBF edition — token scripts
 * are resolved through TokenDb (StaticData) at token-creation time in a real
 * game, so a broken token script would not show up in GbfParseTest. This test
 * parses every GBF token through the SAME path a game uses:
 * {@code TokenDb.getToken(name, "GBF") -> PaperToken -> CardFactory.getCard(...)}
 * (the identical chain as VentureEffect / TokenInfo in a real game).
 *
 * <p>Usage: {@code java -Dfile.encoding=UTF-8 -cp "<install jar>;<classes>" GbfTokenParseTest [tokenScript...]}
 * (defaults to the 5 tokens registered in res/editions/Granblue Fantasy.txt [tokens]).
 * Run from the install dir; see GbfTestBase for the run rules.
 */
public class GbfTokenParseTest extends GbfTestBase {

    private static final String[] GBF_TOKENS = {
        "c_0_0_a_construct_flying_artifactcount",   // Mahira (GBF #126)
        "b_1_1_dog_ally_lifelink_deathtouch",       // Vajra (GBF #127)
        "b_1_2_dog_ally_lifelink",                  // Vajra (GBF #127)
        "rg_2_1_boar",                              // Kumbhira (GBF #128)
        "b_0_1_rat",                                // Vikala (GBF #129)
        "cerberus_enchantment",                     // Hadean Watchdog,Cerberus (GBF #55)
    };

    public static void main(String[] args) {
        init();

        String[] tokens = args.length > 0 ? args : GBF_TOKENS;

        int ok = 0;
        List<String> fails = new ArrayList<>();
        for (String name : tokens) {
            try {
                PaperToken pt = FModel.getMagicDb().getAllTokens().getToken(name, "GBF");
                if (pt == null) {
                    fails.add(name + " (no paper token found for edition GBF)");
                    continue;
                }
                Card c = CardFactory.getCard(pt, null, 1, null);
                if (c == null) {
                    fails.add(name + " (null card)");
                    continue;
                }
                ok++;
                System.out.println("TOKEN OK: " + name + " -> " + c.getName()
                        + " | " + c.getType() + " | " + c.getNetPower() + "/" + c.getNetToughness()
                        + " | keywords=" + c.getKeywords());
            } catch (Throwable t) {
                fails.add(name + " -> " + t);
                t.printStackTrace(System.out);
            }
        }

        System.out.println("TOKEN RESULT: ok=" + ok + " fail=" + fails.size());
        for (String f : fails) {
            System.out.println("FAIL: " + f);
        }
        System.exit(fails.isEmpty() ? 0 : 1);
    }
}
