package com.newuniverse.nusmp.rebirth;

import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.multiverse.MultiverseConfig;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Why: the one place where a sigil click is decided, on the server. First click (or a click that is not a sneak, or one after the window) only warns; the sneaking second click inside the window checks
 * everything again ({@link RebirthRules#verdict}), consumes one sigil and does the rite. One-off work per click: a handful of NBT reads, one inventory sweep, one ring of at most 24 particles and one sound;
 * nothing runs per tick.
 */
public final class RebirthService {
    private RebirthService() {}

    /** A sigil was used (server side). */
    public static void use(ServerPlayer p, ItemStack stack, RebirthRules.Rite rite) {
        long now = p.getServer().overworld().getGameTime();
        RebirthRules.Verdict v = verdict(p, rite, now);
        if (v != RebirthRules.Verdict.OK) { refuse(p, v, now); return; }
        if (!RebirthRules.confirmed(RebirthState.warnedAt(p.getUUID(), rite), now, RebirthConfig.windowTicks(), p.isShiftKeyDown())) {
            RebirthState.warn(p.getUUID(), rite, now);
            warn(p, rite);
            return;
        }
        RebirthState.forget(p.getUUID());
        if (!p.getAbilities().instabuild) stack.shrink(1);
        if (RebirthConfig.xpCost() > 0 && !p.getAbilities().instabuild) p.giveExperienceLevels(-RebirthConfig.xpCost());
        RebirthData.setLastAt(p, now);
        RebirthData.setMode(p, RebirthRules.afterUse(rite));
        RebirthData.markSummoned(p);
        if (rite == RebirthRules.Rite.REBIRTH) {
            int books = GrimoireErasure.everything(p);
            say(p, "Your grimoire turns to ash in your hands" + (books > 1 ? " (" + books + " books)" : "") + ". Your magic is gone. What you have learned and what you are stay with you.", ChatFormatting.DARK_PURPLE);
        } else {
            say(p, "The clover leaves of your grimoire turn toward you. A second book is willing to answer.", ChatFormatting.GOLD);
        }
        giveSummons(p);
        say(p, "NEXT: right-click the Grimoire Summons in your pack. It carries you to the Tower Wizard's library, where you answer his questions.", ChatFormatting.YELLOW);
        ritual(p, rite);
    }

    static RebirthRules.Verdict verdict(ServerPlayer p, RebirthRules.Rite rite, long now) {
        var owned = GrimoireErasure.owned(p);
        RebirthRules.Mode mode = RebirthData.mode(p);
        return RebirthRules.verdict(rite, RebirthConfig.enabled(), now, RebirthData.lastAt(p), RebirthConfig.cooldownDays(), owned.size(), GrimoireErasure.bestTier(p),
                RebirthConfig.secondMinLeaves(), RebirthData.resolve(p), RebirthConfig.secondMinResolve(), MultiverseConfig.get(MultiverseConfig.WIZARD_MODE),
                p.experienceLevel, RebirthConfig.xpCost(), (mode == RebirthRules.Mode.RESET || mode == RebirthRules.Mode.SECOND) && hasSummons(p));   // a lost summons frees the player to use a new sigil
    }

    private static void warn(ServerPlayer p, RebirthRules.Rite rite) {
        int secs = RebirthConfig.windowTicks() / 20;
        if (rite == RebirthRules.Rite.REBIRTH)
            say(p, "Your grimoire(s), their pages and magic types will be erased. Sneak + right-click again within " + secs + " seconds to be reborn.", ChatFormatting.RED);
        else
            say(p, "A second grimoire will be asked for. Your first book stays. Sneak + right-click again within " + secs + " seconds to begin.", ChatFormatting.GOLD);
    }

    private static void refuse(ServerPlayer p, RebirthRules.Verdict v, long now) {
        String msg = switch (v) {
            case DISABLED -> "The rites of rebirth are sealed on this server.";
            case NO_GRIMOIRE -> "You carry no grimoire. There is nothing to be reborn from.";
            case ALREADY_WAITING -> "A rite is already waiting for you: right-click your Grimoire Summons.";
            case COOLDOWN -> "The rite needs rest: " + RebirthRules.daysLeft(RebirthRules.cooldownLeft(now, RebirthData.lastAt(p), RebirthConfig.cooldownDays())) + " more game day(s).";
            case NO_WIZARD -> "The Tower Wizard no longer sets questions here; the second book cannot be asked for.";
            case ALREADY_TWO -> "You already carry two grimoires.";
            case TIER_TOO_LOW -> "Only a grimoire of " + RebirthConfig.secondMinLeaves() + " leaves or more can call a second book.";
            case RESOLVE_TOO_LOW -> "Your resolve is not strong enough yet to carry a second book.";
            case NOT_ENOUGH_XP -> "The rite costs " + RebirthConfig.xpCost() + " experience levels.";
            case OK -> "";
        };
        p.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.GRAY), true);
        RebirthState.forget(p.getUUID());
    }

    private static boolean hasSummons(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++)
            if (p.getInventory().getItem(i).is(NUItems.GRIMOIRE_SUMMONS.get())) return true;
        return false;
    }

    private static void giveSummons(ServerPlayer p) {
        if (hasSummons(p)) return;
        p.getInventory().placeItemBackInInventory(new ItemStack(NUItems.GRIMOIRE_SUMMONS.get()));
    }

    private static void say(ServerPlayer p, String text, ChatFormatting colour) {
        p.sendSystemMessage(Component.literal(text).withStyle(colour));
    }

    /** One ritual: a ring of 24 motes of the wizard theme and one sound. */
    private static void ritual(ServerPlayer p, RebirthRules.Rite rite) {
        ServerLevel sl = p.serverLevel();
        Fx.ring(sl, FxThemes.WIZARD, rite == RebirthRules.Rite.REBIRTH ? FxTone.DARK : FxTone.LIGHT, p.getX(), p.getY() + 0.1, p.getZ(), 1.3, 24);
        sl.playSound(null, p.blockPosition(), rite == RebirthRules.Rite.REBIRTH ? SoundEvents.TOTEM_USE : SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9f, rite == RebirthRules.Rite.REBIRTH ? 0.8f : 1.2f);
    }
}
