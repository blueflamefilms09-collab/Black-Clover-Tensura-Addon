package com.newuniverse.nusmp.rebirth;

import com.newuniverse.nusmp.blackclover.GrimoireAcceptance;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.star.StarRules;
import com.newuniverse.nusmp.wizard.MagicFlavors;
import com.newuniverse.nusmp.wizard.Questionnaire;
import com.newuniverse.nusmp.wizard.WizardLibrary;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Why: the questionnaire of the Tower Wizard is another system's code, so the rebirth feature reaches it only through these few small calls (one or two lines each in {@code WizardSystem}). Everything
 * here runs once per answered questionnaire on the server: it lets a marked player be questioned although they carry a grimoire, takes the first book's magic out of the pool for the second book, grants
 * the second book without touching the first, and (setting {@code twoBooksOnFusion}) turns a strong fused result into two real grimoires.
 */
public final class RebirthWizardHooks {
    private RebirthWizardHooks() {}

    /** Is this player marked for a questionnaire (RESET with no grimoire left, or SECOND with exactly one)? Used to let the wizard question a player who already carries a grimoire. */
    public static boolean pending(ServerPlayer p) {
        RebirthRules.Mode m = RebirthData.mode(p);
        return (m == RebirthRules.Mode.SECOND || m == RebirthRules.Mode.RESET) && RebirthRules.waitsForQuestionnaire(m, GrimoireErasure.owned(p).size());
    }

    /** True for the Yuno rite: the questionnaire adds a book and leaves kingdom, blood and class alone. */
    public static boolean isSecond(ServerPlayer p) { return RebirthData.mode(p) == RebirthRules.Mode.SECOND && pending(p); }

    /** The questionnaire pool: for the second book the first book's magic is taken out, so the result is a different attribute. Otherwise the pool unchanged. */
    public static List<MagicType> poolFor(ServerPlayer p, List<MagicType> pool) {
        if (!isSecond(p)) return pool;
        var first = GrimoirePages.grimoireOf(p);
        return first.isEmpty() ? pool : RebirthRules.poolWithout(pool, GrimoirePages.magicOf(first.get()));
    }

    /** Setting {@code twoBooksOnFusion}: a strong fused result of four leaves or more becomes two grimoires (the plan the tower already knows from Star: the first parent, then the second). */
    public static StarRules.DualPlan twinPlan(StarRules.DualPlan dual, Questionnaire.Result r, GrimoireCover cover) {
        if (dual.extra() != null || !RebirthRules.twoBooksOnFusion(RebirthConfig.twoBooksOnFusion(), r.fused(), r.leaves(), cover.isForbidden())) return dual;
        return new StarRules.DualPlan(r.parentA().name(), r.parentB().name(), false);
    }

    /**
     * Called first thing when the grimoire has floated to the player. RESET: the mark becomes DONE and the normal flow runs (returns false). SECOND: grants the second book beside the first with the first's cover
     * and does the whole ending itself (returns true, the normal flow must stop). Any other player: false, nothing happens.
     */
    public static boolean onArrive(ServerPlayer p, Questionnaire.Result r) {
        RebirthRules.Mode m = RebirthData.mode(p);
        if (m == RebirthRules.Mode.RESET) { RebirthData.setMode(p, RebirthRules.afterQuestionnaire(m)); return false; }
        if (m != RebirthRules.Mode.SECOND) return false;
        var first = GrimoirePages.grimoireOf(p);
        if (first.isEmpty() || !pending(p)) return false;
        MagicType firstMagic = GrimoirePages.magicOf(first.get());
        String name = RebirthRules.secondMagic(r.primary().name(), r.parentA() == null ? null : r.parentA().name(), r.parentB() == null ? null : r.parentB().name(), firstMagic.name());
        MagicType second = name == null ? null : MagicType.valueOf(name);
        if (second == null) for (MagicType t : MagicFlavors.pool()) if (t != firstMagic) { second = t; break; }
        GrimoireAcceptance.grantExtra(p, GrimoirePages.coverOf(first.get()), second);
        if (GrimoireErasure.owned(p).size() < 2) {                                            // another mod refused the skill: the mark and the summons stay, the player may ask again
            p.sendSystemMessage(Component.literal("The second book did not come. Speak to the wizard again.").withStyle(ChatFormatting.GRAY));
            return true;
        }
        for (int i = 0; i < p.getInventory().getContainerSize(); i++)                      // the summons has done its work
            if (p.getInventory().getItem(i).is(NUItems.GRIMOIRE_SUMMONS.get())) p.getInventory().setItem(i, ItemStack.EMPTY);
        RebirthData.setMode(p, RebirthRules.afterQuestionnaire(m));
        p.sendSystemMessage(Component.literal("A second grimoire answers you: " + second.displayName + ". You carry two grimoires.").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        p.sendSystemMessage(Component.literal("Your first grimoire is unchanged. Both books are in your pack.").withStyle(ChatFormatting.GREEN));
        if (WizardLibrary.inLibrary(p)) {
            p.sendSystemMessage(Component.literal("The wizard will send you home in a few seconds.").withStyle(ChatFormatting.GRAY));
            SpellRuntime.later(p.serverLevel(), 160, () -> { if (p.isAlive()) WizardLibrary.leave(p); });
        }
        return true;
    }
}
