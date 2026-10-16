package com.newuniverse.nusmp.avatar.luck;

import com.newuniverse.nusmp.thread.RougeManager;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Why: "Total Luck" is read from four places that live in four different systems (the vanilla attribute, Tensura's probability skills, Vanessa's Rouge
 * from the Thread grimoire, and Dice Magic), so one class reads them all defensively. Every source is optional: a missing mod, a skill that does not
 * exist or a half-loaded player counts as zero, never as an exception, because a luck read must not be able to break creation.
 */
public final class LuckPoller {
    private LuckPoller() {}

    /** The persistent-data flag Dice Magic's Loaded Dice sets (a game time until which the buff lasts); read here, written by book.DiceBook. */
    private static final String LOADED_DICE_UNTIL = "nusmp_dice_loaded";

    public static double total(ServerPlayer p) {
        double vanilla = 0.0;
        try { vanilla = p.getAttributeValue(Attributes.LUCK); } catch (RuntimeException ignored) { /* no luck attribute: zero */ }
        return LuckMath.total(vanilla, probabilitySkills(p), rougeArmed(p), rougeOut(p), loadedDice(p));
    }

    /** How many learned skills have a luck-like id (Tensura's probability manipulation family, or any add-on's fortune skill). */
    static int probabilitySkills(ServerPlayer p) {
        try {
            int n = 0;
            for (ManasSkillInstance i : SkillAPI.getSkillsFrom(p).getLearnedSkills()) {
                var id = i.getSkill().getRegistryName();
                if (id != null && LuckMath.looksLikeLuckSkill(id.getPath())) n++;
            }
            return n;
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    static boolean rougeArmed(ServerPlayer p) {
        try { return RougeManager.threadBook(p).map(i -> i.getOrCreateTag().getBoolean("RougeArmed")).orElse(false); }
        catch (RuntimeException | LinkageError e) { return false; }
    }

    static boolean rougeOut(ServerPlayer p) {
        try { return RougeManager.of(p.getUUID()) != null; }
        catch (RuntimeException | LinkageError e) { return false; }
    }

    static boolean loadedDice(ServerPlayer p) {
        try { return p.getPersistentData().getLong(LOADED_DICE_UNTIL) > p.level().getGameTime(); }
        catch (RuntimeException e) { return false; }
    }
}
