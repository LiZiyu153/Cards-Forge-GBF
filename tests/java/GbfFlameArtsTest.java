import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Headless regression test for Flame Arts (GBF) after the 0.0.1.8 fix:
 * "Flame Arts deals X damage to each of up to X target creatures."
 *
 * Original bug: the script used `DividedAsYouChoose$ X`, which makes the
 * engine DIVIDE X damage among the chosen targets (each target gets its
 * allocated slice). With X targets chosen, the engine's
 * `size == amount` rule gives each target exactly 1 damage — hence "always
 * deals 1 damage regardless of X". The Oracle says each target takes the
 * full X, so DividedAsYouChoose was removed (no division; each of up to X
 * targets takes X).
 *
 * Covered:
 *  1. X=3, two targets: each takes 3 damage (both 2/2 Grizzly Bears die).
 *  2. X=3, one target: the single target takes 3 damage.
 */
public class GbfFlameArtsTest extends GbfTestBase {

    public static void main(String[] args) {
        init();

        boolean ok = true;
        ok &= testX3TwoTargets();
        ok &= testX3OneTarget();
        System.out.println(ok ? "ALL PASS" : "SOME FAILED");
        System.exit(ok ? 0 : 1);
    }

    /** Resolve the Flame Arts spell ability with X paid and explicit targets. */
    private static void resolveFlameArts(Game game, Player caster, Card flame, Card... targets) {
        SpellAbility sa = flame.getFirstSpellAbility();
        sa.setActivatingPlayer(caster);
        sa.setXManaCostPaid(3);
        for (Card t : targets) {
            sa.getTargets().add(t);
        }
        // spells resolve from the Stack zone; put the card there first
        game.getAction().moveToStack(flame, sa);
        game.getStack().add(sa);
        playUntilStackClear(game);
        // one more priority pass so lethal-damage SBAs move the bears to the graveyard
        game.getPhaseHandler().mainLoopStep();
    }

    /** X=3, two 2/2 bears -> each takes 3 damage and dies. */
    private static boolean testX3TwoTargets() {
        Game game = newGame();
        Player p = game.getPlayers().get(1);
        Player q = game.getPlayers().get(0);
        Card flame = makeCard("Flame Arts", p, game);
        Card bear1 = makeCard("Grizzly Bears", q, game);
        addToBattlefield(bear1);
        Card bear2 = makeCard("Grizzly Bears", q, game);
        addToBattlefield(bear2);

        resolveFlameArts(game, p, flame, bear1, bear2);

        boolean bear1Dead = game.getZoneOf(bear1) != null && game.getZoneOf(bear1).is(ZoneType.Graveyard);
        boolean bear2Dead = game.getZoneOf(bear2) != null && game.getZoneOf(bear2).is(ZoneType.Graveyard);
        boolean ok = bear1Dead && bear2Dead;
        System.out.println("[X3TwoTgts] bear1Dead=" + bear1Dead + " bear2Dead=" + bear2Dead
                + " (each should take 3, both die) -> " + (ok ? "PASS" : "FAIL"));
        return ok;
    }

    /** X=3, one 2/2 bear -> the single target takes 3 and dies (no division). */
    private static boolean testX3OneTarget() {
        Game game = newGame();
        Player p = game.getPlayers().get(1);
        Player q = game.getPlayers().get(0);
        Card flame = makeCard("Flame Arts", p, game);
        Card bear = makeCard("Grizzly Bears", q, game);
        addToBattlefield(bear);

        resolveFlameArts(game, p, flame, bear);

        boolean bearDead = game.getZoneOf(bear) != null && game.getZoneOf(bear).is(ZoneType.Graveyard);
        System.out.println("[X3OneTgt] bearDead=" + bearDead
                + " (single target takes full 3) -> " + (bearDead ? "PASS" : "FAIL"));
        return bearDead;
    }
}
