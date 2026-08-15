package forge.game.staticability;

import java.util.ArrayList;
import java.util.List;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class StaticAbilityMustTarget {

    /**
     * A single MustTarget restriction. The validTarget may be a card type
     * (e.g. "Flagbearer") or the special "Card.Self" to require targeting the
     * host of the static ability itself (e.g. "all spells that can target me
     * must target me").
     */
    private static final class MustTargetRestriction {
        final String validTarget;
        final ZoneType zone;
        final Card hostCard;

        MustTargetRestriction(String validTarget, ZoneType zone, Card hostCard) {
            this.validTarget = validTarget;
            this.zone = zone;
            this.hostCard = hostCard;
        }

        boolean matches(Card card) {
            if ("Card.Self".equals(validTarget)) {
                return card == hostCard;
            }
            return card.getType().hasStringType(validTarget);
        }
    }

    public static boolean filterMustTargetCards(Player targetingPlayer, List<Card> targets, final SpellAbility spellAbility) {
        //Only applied when the targeting player and controller are the same
        if (targetingPlayer != spellAbility.getHostCard().getController()) {
            return false;
        }

        List<MustTargetRestriction> restrictions = getAllRestrictions(spellAbility);
        return applyMustTargetCardAbility(restrictions, targets, spellAbility);
    }

    public static boolean meetsMustTargetRestriction(final SpellAbility spellAbility) {
        // Copied spell is not affected.
        // (ChangeTarget does not go this path so not checked here.)
        if (spellAbility.isCopied()) return true;

        final Game game = spellAbility.getHostCard().getGame();
        List<MustTargetRestriction> restrictions = getAllRestrictions(spellAbility);

        if (restrictions.isEmpty()) return true;

        SpellAbility currentAbility = spellAbility;
        boolean usesTargeting = false;
        do {
            if (currentAbility.usesTargeting() && !currentAbility.hasParam("TargetingPlayer")) {
                usesTargeting = true;
                // Check if currentAbility can target any MustTarget cards
                TargetRestrictions tgt = currentAbility.getTargetRestrictions();
                List<ZoneType> zone = tgt.getZone();
                List<Card> validCards = CardLists.getValidCards(game.getCardsIn(zone), tgt.getValidTgts(), currentAbility.getActivatingPlayer(), currentAbility.getHostCard(), currentAbility);
                List<Card> choices = CardLists.getTargetableCards(validCards, currentAbility);

                isRestrictionsMet(restrictions, choices, currentAbility);
            }
            currentAbility = currentAbility.getSubAbility();
        } while (currentAbility != null);

        return !usesTargeting || restrictions.isEmpty();
    }

    private static List<MustTargetRestriction> getAllRestrictions(final SpellAbility spellAbility) {
        final Game game = spellAbility.getHostCard().getGame();
        List<MustTargetRestriction> restrictions = new ArrayList<>();

        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.MustTarget) || !stAb.matchesValidParam("ValidSA", spellAbility)) {
                    continue;
                }
                MustTargetRestriction newRestriction = new MustTargetRestriction(
                        stAb.getParam("ValidTarget"), ZoneType.smartValueOf(stAb.getParam("ValidZone")), stAb.getHostCard());
                boolean dup = false;
                for (MustTargetRestriction r : restrictions) {
                    if (r.validTarget.equals(newRestriction.validTarget) && r.zone == newRestriction.zone && r.hostCard == newRestriction.hostCard) {
                        dup = true;
                        break;
                    }
                }
                if (!dup) {
                    restrictions.add(newRestriction);
                }
            }
        }

        return restrictions;
    }

    private static boolean isRestrictionsMet(List<MustTargetRestriction> restrictions, List<Card> targets, final SpellAbility spellAbility) {
        for (int i = restrictions.size() - 1; i >= 0; i--) {
            MustTargetRestriction restriction = restrictions.get(i);
            // First, check satisfied restrictions that is already targeted by spellAbility
            boolean found = false;
            for (final Card card : spellAbility.getTargets().getTargetCards()) {
                if (restriction.matches(card) && card.isInZone(restriction.zone)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                restrictions.remove(i);
                continue;
            }

            // Second check if their are any targetable card with type in zone
            found = false;
            for (final Card card : targets) {
                if (restriction.matches(card) && card.isInZone(restriction.zone)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                restrictions.remove(i);
            }
        }

        return restrictions.isEmpty();
    }

    private static boolean applyMustTargetCardAbility(List<MustTargetRestriction> restrictions, List<Card> targets, final SpellAbility spellAbility) {
        if (isRestrictionsMet(restrictions, targets, spellAbility)) {
            return false;
        }

        // If remaining restrictions are larger than possible target numbers, then all targets are cleared (means not possible to target any one)
        final int maxTargets = spellAbility.getMaxTargets();
        final int targeted = spellAbility.getTargets().size();
        if (restrictions.size() > maxTargets - targeted) {
            targets.clear();
            return true;
        }

        // Filter out all cards not satisfying any of the restrictions
        boolean filtered = false;
        for (int i = targets.size() - 1; i >= 0; i--) {
            final Card card = targets.get(i);
            boolean satisfied = false;
            for (MustTargetRestriction restriction : restrictions) {
                if (restriction.matches(card) && card.isInZone(restriction.zone)) {
                    satisfied = true;
                    break;
                }
            }
            if (!satisfied) {
                targets.remove(i);
                filtered = true;
            }
        }
        return filtered;
    }

}
