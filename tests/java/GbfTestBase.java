import java.util.List;

import com.google.common.collect.Lists;

import forge.GuiDesktop;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/**
 * Shared headless test harness for GBF behavior/parse tests.
 *
 * <p>Every Gbf*Test class extends this base to reuse the identical boilerplate:
 * FModel initialization (LOAD_CARD_SCRIPTS_LAZILY=false so all card + token
 * rules are parsed), a dev-mode 2-player game, and the standard helpers for
 * creating cards, placing them in zones and clearing the stack.
 *
 * <p><b>Critical run rules (see AGENTS.md 验证方法论):</b>
 * <ul>
 *   <li>Must run with the installed fat jar as classpath:
 *       {@code java -Dfile.encoding=UTF-8 -cp "<install>\forge-gui-desktop-2.0.13-jar-with-dependencies.jar;<this-dir>"}
 *       (unpacked classes report BuildInfo version "GIT" and break Localizer).</li>
 *   <li>Working directory MUST be the install dir (jar's getAssetsDir() returns
 *       "" -> res/languages resolves relative to CWD).</li>
 *   <li>Use {@link #init()} once, at the start of main().</li>
 * </ul>
 *
 * <p>Dev-environment quirks to know (details in docs/dev-mode-limitations.md):
 * <ul>
 *   <li>{@code changeZone(null, zone, card, ...)} only registers <b>extrinsic</b>
 *       triggers; call {@code registerActiveTrigger(card, false)} explicitly and
 *       use {@link #enterBattlefield(Game, Card)} for real zone-change triggers.</li>
 *   <li>CardFactory.getCard leaves the card in a null zone; {@code moveTo(Hand)}
 *       COPIES the card (the original stays in null zone) — use a direct
 *       {@code zone.add(card)} first.</li>
 *   <li>AI target selection prefers the trigger source itself; pin targets with
 *       {@code sa.getTargets().add(target)} when the target matters.</li>
 *   <li>Self-moving LTB triggers, ImmediateTrigger, loyalty-cost payment and
 *       LKI-based Defined$ in sub-ability chains are not exercisable headless —
 *       drive scripted chains with {@code AbilityFactory.getAbility(card, "SVar")}
 *       and verify those paths in a real game.</li>
 * </ul>
 */
public abstract class GbfTestBase {

    protected static int cardId = 1000;

    /** One-time FModel init: lazy=false so ALL card AND token rules are parsed. */
    protected static void init() {
        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, preferences -> {
            preferences.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
            preferences.setPref(FPref.UI_LANGUAGE, "en-US");
            return null;
        });
    }

    /** Dev-mode 2-player (AI) game in MAIN1; returns the game (player 1 = active). */
    protected static Game newGame() {
        List<RegisteredPlayer> players = Lists.newArrayList();
        Deck d1 = new Deck();
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("p2", null)));
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("p1", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Test");
        Game game = new Game(players, rules, match);
        Player p = game.getPlayers().get(1);
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getPhaseHandler().onStackResolved();
        return game;
    }

    /** Create a real card from the DB (parse path identical to a game). */
    protected static Card makeCard(String name, Player owner, Game game) {
        PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name);
        if (pc == null) {
            throw new RuntimeException("Card not found in DB: " + name);
        }
        Card c = CardFactory.getCard(pc, owner, cardId++, game);
        c.setGameTimestamp(game.getNextTimestamp());
        return c;
    }

    /** Direct battlefield placement (no zone-change triggers fire). */
    protected static void addToBattlefield(Card c) {
        c.getController().getZone(ZoneType.Battlefield).add(c);
    }

    /** Direct hand placement (no zone-change triggers fire). */
    protected static void addToHand(Card c) {
        c.getController().getZone(ZoneType.Hand).add(c);
    }

    /**
     * Real entry-to-battlefield path: register intrinsic triggers, put the card
     * in hand first (zone.add, NOT moveTo — moveTo would copy and leave the
     * original in a null zone), then moveTo the battlefield so ChangesZone
     * triggers actually fire and are queued as waiting.
     */
    protected static void enterBattlefield(Game game, Card c) {
        game.getTriggerHandler().registerActiveTrigger(c, false);
        if (game.getZoneOf(c) == null) {
            c.getController().getZone(ZoneType.Hand).add(c);
        }
        game.getAction().moveTo(c.getController().getZone(ZoneType.Battlefield), c, null);
    }

    /** Run the main loop until the stack is empty (guard against infinite loops). */
    protected static void playUntilStackClear(Game game) {
        int guard = 0;
        do {
            game.getPhaseHandler().mainLoopStep();
            guard++;
            if (guard > 800) {
                throw new RuntimeException("Stack did not clear; gameOver=" + game.isGameOver()
                        + " stackSize=" + game.getStack().size());
            }
        } while (!game.isGameOver() && !game.getStack().isEmpty());
    }

    /** Flush waiting triggers, then let the stack fully resolve. */
    protected static void runTriggersAndClear(Game game) {
        game.getTriggerHandler().runWaitingTriggers();
        playUntilStackClear(game);
    }
}
