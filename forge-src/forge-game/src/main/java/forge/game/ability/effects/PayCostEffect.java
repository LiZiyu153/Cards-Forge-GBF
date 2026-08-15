package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.spellability.SpellAbility;

/**
 * PayCost — a payment node inside an effect chain.
 *
 * Usage: DB$ PayCost | Cost$ <cost> | SubAbility$ DBX | UnpaidSubAbility$ DBY
 *
 * When resolved, the payer (Defined$ or the activating player) is offered the
 * chance to pay Cost. If the cost is paid, the regular SubAbility$ chain
 * continues; if the player declines, UnpaidSubAbility$ (optional) runs instead
 * and the regular chain is skipped. The actual payment flow lives in
 * AbilityUtils.handlePayCost so that the SubAbility chain can be conditionally
 * skipped; the effect class only provides the stack description.
 */
public class PayCostEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final String cost = sa.getParam("Cost");
        if (cost != null) {
            sb.append("Pay ").append(cost).append(".");
        }
        return sb.toString();
    }

    // Payment handling is done in AbilityUtils.handlePayCost, which needs to
    // control whether the SubAbility chain continues after the payment.
    @Override
    public void resolve(final SpellAbility sa) {
        // no-op; see AbilityUtils.handlePayCost
    }
}
