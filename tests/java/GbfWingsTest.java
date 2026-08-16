import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Headless regression test for Wings Shall Deliver You (GBF) Saga chapter I:
 * "I — Return target instant or sorcery card from your graveyard to your hand."
 *
 * Original bug (fixed in 0.0.1.7): ValidTgts was
 *   Instant.YourGraveyard,Sorcery.YourGraveyard
 * "YourGraveyard" is NOT a valid CardProperty (the engine only knows
 * wasCastFromYourGraveyard and the sharesNameWith/doesNotShareNameWith
 * special cases), so every candidate card failed the restriction and the
 * chapter could never choose a legal target — a silent no-op.
 * Fixed with the official Zombify-family pattern (soul_salvage /
 * raise_the_draugr): ValidTgts$ Instant.YouOwn,Sorcery.YouOwn combined with
 * Origin$ Graveyard (which constrains targeting to the graveyard zone).
 *
 * The test drives the real scripted chapter ability (AbilityFactory
 * getAbility on the DBI SVar) and asserts the restriction semantics
 * directly against real graveyard cards: your instant matches, the
 * opponent's does not, and the old "YourGraveyard" spelling matches nothing.
 */
public class GbfWingsTest extends GbfTestBase {

    public static void main(String[] args) {
        init();

        boolean ok = true;
        ok &= testChapterITargetsOwnGraveyardOnly();
        ok &= testOldBrokenSpellingMatchesNothing();
        System.out.println(ok ? "ALL PASS" : "SOME FAILED");
        System.exit(ok ? 0 : 1);
    }

    /** Your instant in your graveyard is a legal target; the opponent's is not. */
    private static boolean testChapterITargetsOwnGraveyardOnly() {
        Game game = newGame();
        Player p = game.getPlayers().get(1);
        Player q = game.getPlayers().get(0);

        Card wings = makeCard("Wings Shall Deliver You", p, game);
        addToBattlefield(wings);

        SpellAbility dbi = AbilityFactory.getAbility(wings, "DBI");
        if (dbi == null) {
            System.out.println("[ChapterI] DBI ability not found -> FAIL");
            return false;
        }

        Card boltP = makeCard("Lightning Bolt", p, game); // p's graveyard instant
        game.getAction().changeZone(null, p.getZone(ZoneType.Graveyard), boltP, null, null);
        Card boltQ = makeCard("Lightning Bolt", q, game); // q's graveyard instant
        game.getAction().changeZone(null, q.getZone(ZoneType.Graveyard), boltQ, null, null);

        boolean ownOk = boltP.isValid("Instant.YouOwn", p, wings, dbi);
        boolean oppOk = !boltQ.isValid("Instant.YouOwn", p, wings, dbi);
        boolean zoneOk = dbi.getParam("Origin") != null && dbi.getParam("Origin").equals("Graveyard");
        boolean ok = ownOk && oppOk && zoneOk;
        System.out.println("[ChapterI] ownGraveInstant=" + ownOk + " oppGraveInstantExcluded=" + oppOk
                + " origin=Graveyard:" + zoneOk + " -> " + (ok ? "PASS" : "FAIL"));
        return ok;
    }

    /** The old spelling "Instant.YourGraveyard" must match NOTHING (regression proof). */
    private static boolean testOldBrokenSpellingMatchesNothing() {
        Game game = newGame();
        Player p = game.getPlayers().get(1);

        Card wings = makeCard("Wings Shall Deliver You", p, game);
        addToBattlefield(wings);

        SpellAbility dbi = AbilityFactory.getAbility(wings, "DBI");

        Card boltP = makeCard("Lightning Bolt", p, game);
        game.getAction().changeZone(null, p.getZone(ZoneType.Graveyard), boltP, null, null);

        boolean oldSpelling = boltP.isValid("Instant.YourGraveyard", p, wings, dbi);
        boolean ok = !oldSpelling;
        System.out.println("[OldSpelling] 'Instant.YourGraveyard' matches own instant: " + oldSpelling
                + " (must be false) -> " + (ok ? "PASS" : "FAIL"));
        return ok;
    }
}
