import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Headless regression test for Perpetual Tailwind (GBF) CDA P/T:
 * "Perpetual Tailwind's power is equal to the number of enchantments in your
 * graveyard plus enchantments you control. Its toughness is equal to that
 * number plus 1." (printed stat line: star-over-star plus one)
 *
 * Original bug (fixed in 0.0.1.6): SVar:P/T was
 *   Count$Valid Enchantment.YouCtrl,Enchantment.YourGrave
 * Count$Valid only iterates BATTLEFIELD cards (xCount default zone) and
 * "YourGrave" is not a CardProperty at all, so graveyard enchantments were
 * NEVER counted: the P/T only ever saw the battlefield half.
 * Fixed with the official multi-zone pattern (calamitous_cave_in /
 * gargantuan_leech / runaway_trash_bot):
 *   Count$ValidGraveyard,Battlefield Enchantment.YouCtrl
 * (graveyard cards match YouCtrl because getController() falls back to the
 * owner, so only YOUR graveyard's enchantments are counted, not the
 * opponent's.) Also added CharacteristicDefining$ True per the official CDA
 * template (tarmogoyf) for the star-over-star stat line.
 *
 * The CDA is a static ability, so the test calls
 * game.getAction().checkStaticAbilities() before asserting getNetPower()/
 * getNetToughness() (statics are applied lazily, not on zone.add).
 */
public class GbfPerpetualTailwindTest extends GbfTestBase {

    public static void main(String[] args) {
        init();

        boolean ok = true;
        ok &= testCountsGraveyardPlusControlled();
        ok &= testIgnoresOpponentGraveyard();
        ok &= testIgnoresOpponentControlled();
        System.out.println(ok ? "ALL PASS" : "SOME FAILED");
        System.exit(ok ? 0 : 1);
    }

    /** Tailwind + 1 controlled enchantment on the battlefield + 2 enchantments in own graveyard -> P=4, T=5. */
    private static boolean testCountsGraveyardPlusControlled() {
        Game game = newGame();
        Player p = game.getPlayers().get(1);

        Card tailwind = makeCard("Perpetual Tailwind", p, game);
        addToBattlefield(tailwind); // itself is an enchantment you control

        Card oRing = makeCard("Oblivion Ring", p, game); // enchantment you control (no pump effects)
        addToBattlefield(oRing);

        Card pacifism = makeCard("Pacifism", p, game); // enchantment in YOUR graveyard
        game.getAction().changeZone(null, p.getZone(ZoneType.Graveyard), pacifism, null, null);
        Card journey = makeCard("Journey to Nowhere", p, game); // enchantment in YOUR graveyard
        game.getAction().changeZone(null, p.getZone(ZoneType.Graveyard), journey, null, null);

        game.getAction().checkStaticAbilities();

        boolean ok = tailwind.getNetPower() == 4 && tailwind.getNetToughness() == 5;
        System.out.println("[Grave+Ctrl] tailwind PT=" + tailwind.getNetPower() + "/" + tailwind.getNetToughness()
                + " (expect 4/5: 2 controlled + 2 in own graveyard) -> " + (ok ? "PASS" : "FAIL"));
        return ok;
    }

    /** Only enchantments in the OPPONENT's graveyard -> P=1, T=2 (self only). */
    private static boolean testIgnoresOpponentGraveyard() {
        Game game = newGame();
        Player p = game.getPlayers().get(1);
        Player q = game.getPlayers().get(0);

        Card tailwind = makeCard("Perpetual Tailwind", p, game);
        addToBattlefield(tailwind);

        Card pacifism = makeCard("Pacifism", q, game); // q's graveyard
        game.getAction().changeZone(null, q.getZone(ZoneType.Graveyard), pacifism, null, null);
        Card journey = makeCard("Journey to Nowhere", q, game); // q's graveyard
        game.getAction().changeZone(null, q.getZone(ZoneType.Graveyard), journey, null, null);

        game.getAction().checkStaticAbilities();

        boolean ok = tailwind.getNetPower() == 1 && tailwind.getNetToughness() == 2;
        System.out.println("[OppGrave] tailwind PT=" + tailwind.getNetPower() + "/" + tailwind.getNetToughness()
                + " (expect 1/2: opponent's graveyard must not count) -> " + (ok ? "PASS" : "FAIL"));
        return ok;
    }

    /** Enchantment controlled by the OPPONENT (plus one in opponent's graveyard) -> P=1, T=2. */
    private static boolean testIgnoresOpponentControlled() {
        Game game = newGame();
        Player p = game.getPlayers().get(1);
        Player q = game.getPlayers().get(0);

        Card tailwind = makeCard("Perpetual Tailwind", p, game);
        addToBattlefield(tailwind);

        Card oRing = makeCard("Oblivion Ring", q, game); // q controls it
        addToBattlefield(oRing);

        Card pacifism = makeCard("Pacifism", q, game); // q's graveyard
        game.getAction().changeZone(null, q.getZone(ZoneType.Graveyard), pacifism, null, null);

        game.getAction().checkStaticAbilities();

        boolean ok = tailwind.getNetPower() == 1 && tailwind.getNetToughness() == 2;
        System.out.println("[OppCtrl] tailwind PT=" + tailwind.getNetPower() + "/" + tailwind.getNetToughness()
                + " (expect 1/2: opponent's battlefield enchantment must not count) -> " + (ok ? "PASS" : "FAIL"));
        return ok;
    }
}
