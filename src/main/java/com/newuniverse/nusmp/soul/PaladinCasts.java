package com.newuniverse.nusmp.soul;

import com.newuniverse.nusmp.book.GrimoireBook;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/**
 * Why: the grimoire pages are {@code BookPage.Cast} lambdas and the book classes are generic, so the Soul pages of Lucius Zogratis
 * need a home of their own that owns the casting rules and nothing else. {@link #conversion} aims, checks the rules, takes the shared
 * control gate (a permanent mind-control is the strongest control the mod has, so it spends the caster's one 12-second control
 * cooldown like Red Thread does) and starts the {@link PaladinChannel}. {@link #recall} brings every loaded Paladin to the caster; with
 * the caster sneaking it dismisses instead (the Paladin under the crosshair, or all of them if none is aimed at), which keeps the
 * book to two pages. Mana is spent by the book before the cast, so a refused page costs nothing and a broken channel costs the mana.
 */
public final class PaladinCasts {
    private PaladinCasts() {}

    public static final String CONVERSION = "paladin_conversion", RECALL = "paladin_recall";
    private static final double RANGE = 16.0;

    public static boolean conversion(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!SoulConfig.enabled()) { GrimoireBook.fail(p, "Paladin Conversion is disabled on this server."); return false; }
        if (PaladinChannel.active(p.getUUID())) { GrimoireBook.fail(p, "You are already purifying a soul."); return false; }
        LivingEntity t = GrimoireBook.target(p, RANGE);
        if (t == null) { GrimoireBook.fail(p, "No soul to purify."); return false; }
        if (!(t instanceof Mob m)) {
            GrimoireBook.fail(p, t instanceof net.minecraft.world.entity.player.Player ? PaladinRules.refusal(PaladinRules.Verdict.PLAYER) : "That is not a creature with a soul to turn.");
            return false;
        }
        PaladinRules.Verdict v = PaladinRules.verdict(Paladins.subject(p, m));
        if (v != PaladinRules.Verdict.OK) { GrimoireBook.fail(p, PaladinRules.refusal(v)); return false; }
        if (PaladinChannel.targeted(m.getUUID())) { GrimoireBook.fail(p, "Someone is already purifying that soul."); return false; }
        if (!GrimoireBook.control(p)) return false;
        double mastery = b.masteryFrac(i);
        PaladinChannel.start(p, m, mastery, SoulConfig.limit(mastery * 100.0));
        b.castCircle(p, 0.8f);
        return true;
    }

    public static boolean recall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        return p.isShiftKeyDown() ? dismiss(b, p) : bring(b, p);
    }

    private static boolean bring(GrimoireBook b, ServerPlayer p) {
        List<Mob> mine = Paladins.of(p.server, p.getUUID());
        if (mine.isEmpty()) { GrimoireBook.fail(p, "You have no Paladins to call."); return false; }
        int n = 0;
        for (Mob m : mine) if (Paladins.teleportNear(m, p)) n++;
        if (n == 0) { GrimoireBook.fail(p, "Your Paladins could not reach you."); return false; }
        b.castCircle(p, 0.6f);
        p.displayClientMessage(Component.literal(n + (n == 1 ? " Paladin answers your call." : " Paladins answer your call.")).withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    private static boolean dismiss(GrimoireBook b, ServerPlayer p) {
        List<Mob> mine = Paladins.of(p.server, p.getUUID());
        if (mine.isEmpty()) { GrimoireBook.fail(p, "You have no Paladins to release."); return false; }
        LivingEntity aimed = GrimoireBook.target(p, RANGE);
        if (aimed instanceof Mob m && mine.contains(m)) {
            Paladins.release(m, Paladins.Reason.DISMISSED);
            return true;
        }
        for (Mob m : mine) Paladins.release(m, Paladins.Reason.DISMISSED);
        p.displayClientMessage(Component.literal("You release every Paladin.").withStyle(ChatFormatting.GRAY), true);
        return true;
    }
}
