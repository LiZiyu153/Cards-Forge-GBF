import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Headless behavioral test for Le Jardin de Fleurs (GBF):
 * its death trigger is limited by ResolvedLimit$ 2 -> only two Spirit Blossom
 * tokens per turn, and the limit resets on the next turn.
 */
public class GbfLeJardinTest extends GbfTestBase {

    public static void main(String[] args) {
        init();

        boolean ok = true;
        ok &= testTwicePerTurn();
        ok &= testResetNextTurn();
        System.out.println(ok ? "ALL PASS" : "SOME FAILED");
        System.exit(ok ? 0 : 1);
    }

    private static int countTokens(Game game) {
        int n = 0;
        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if ("Spirit Blossom Token".equals(c.getName())) {
                n++;
            }
        }
        return n;
    }

    /** Move a creature from battlefield to graveyard and let the stack fully resolve. */
    private static void kill(Game game, Player p, Card c) {
        game.getAction().moveTo(p.getZone(ZoneType.Graveyard), c, null);
        game.getTriggerHandler().runWaitingTriggers();
        playUntilStackClear(game);
    }

    /** 3 deaths in the same turn must produce exactly 2 tokens (ResolvedLimit$ 2). */
    private static boolean testTwicePerTurn() {
        Game game = newGame();
        Player p = game.getPlayers().get(1);

        Card jardin = makeCard("Le Jardin de Fleurs", p, game);
        game.getAction().changeZone(null, p.getZone(ZoneType.Battlefield), jardin, null, null);
        game.getTriggerHandler().registerActiveTrigger(jardin, false);

        Card c1 = makeCard("Elvish Mystic", p, game);
        Card c2 = makeCard("Elvish Mystic", p, game);
        Card c3 = makeCard("Elvish Mystic", p, game);
        addToBattlefield(c1);
        addToBattlefield(c2);
        addToBattlefield(c3);

        kill(game, p, c1);
        kill(game, p, c2);
        kill(game, p, c3);

        int tokens = countTokens(game);
        boolean ok = tokens == 2;
        System.out.println("[TwicePerTurn] 3 deaths in one turn -> spirit blossom tokens = " + tokens
                + " (expect 2) -> " + (ok ? "PASS" : "FAIL"));
        return ok;
    }

    /** The twice-per-turn limit resets on the next turn. */
    private static boolean testResetNextTurn() {
        Game game = newGame();
        Player p = game.getPlayers().get(1);

        Card jardin = makeCard("Le Jardin de Fleurs", p, game);
        game.getAction().changeZone(null, p.getZone(ZoneType.Battlefield), jardin, null, null);
        game.getTriggerHandler().registerActiveTrigger(jardin, false);

        Card c1 = makeCard("Elvish Mystic", p, game);
        Card c2 = makeCard("Elvish Mystic", p, game);
        Card c3 = makeCard("Elvish Mystic", p, game);
        Card c4 = makeCard("Elvish Mystic", p, game);
        addToBattlefield(c1);
        addToBattlefield(c2);
        addToBattlefield(c3);
        addToBattlefield(c4);

        kill(game, p, c1);
        kill(game, p, c2);
        kill(game, p, c3);
        int tokensTurn1 = countTokens(game);

        // run the real cleanup-phase reset (what the CLEANUP phase does in a real game),
        // then start a fresh turn in dev mode
        int resolvedBefore = jardin.getAbilityResolvedThisTurnActivators(
                jardin.getTriggers().get(0).getOverridingAbility()).count(p);
        game.onCleanupPhase();
        int resolvedAfter = jardin.getAbilityResolvedThisTurnActivators(
                jardin.getTriggers().get(0).getOverridingAbility()).count(p);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p, 2);
        System.out.println("[ResetNextTurn] resolved-before-cleanup=" + resolvedBefore
                + " resolved-after-cleanup=" + resolvedAfter);

        kill(game, p, c4);
        int tokensTurn2 = countTokens(game);

        boolean ok = tokensTurn1 == 2 && tokensTurn2 == 3;
        System.out.println("[ResetNextTurn] turn1 tokens = " + tokensTurn1 + " (expect 2), after next turn's death tokens = "
                + tokensTurn2 + " (expect 3) -> " + (ok ? "PASS" : "FAIL"));
        return ok;
    }
}
