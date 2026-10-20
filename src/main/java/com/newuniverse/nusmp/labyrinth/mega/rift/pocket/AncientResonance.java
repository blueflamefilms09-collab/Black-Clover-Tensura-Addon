package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.item.ElsdociaEvents;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Why: the canon says the treasury of an ancient tomb holds lost ancient magic, and that "sometimes an ancient spell resonates with a mage and inscribes itself into their grimoire". When the seal
 * of a tomb opens, every mage nearby whose grimoire holds one of the magics the tomb resonates with ({@link PocketDef#resonance()}) has a chance ({@link PocketRules#RESONANCE_CHANCE}) of being
 * written into: the grimoire's mastery grows by a fifth of its bar, which is what opens pages in this mod (pages open by mastery, {@code multiverse.MasteryPages} checks every two seconds), so the
 * ancient spell shows up as the next page of the book arriving early. It happens at most once per mage per tomb, never costs anything, and does nothing for someone without a grimoire or with
 * another magic. Written against the grimoire's public skill instance only; any failure is logged and swallowed, the opening of the seal never depends on it.
 */
public final class AncientResonance {
    private AncientResonance() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** The share of the mastery bar one resonance writes. */
    public static final double SHARE = 0.20;

    /** Tries to inscribe the tomb's ancient spell into this mage's grimoire; true when it did. {@code roll} is 0 to 1. */
    public static boolean inscribe(ServerPlayer p, PocketDef def, double roll) {
        try {
            if (roll >= PocketRules.RESONANCE_CHANCE) return false;
            String magic = ElsdociaEvents.magicOf(p);
            if (!PocketRules.resonates(magic, def.resonance())) return false;
            Optional<ManasSkillInstance> grim = GrimoirePages.grimoireOf(p);
            if (grim.isEmpty()) return false;
            ManasSkillInstance inst = grim.get();
            int max = inst.getMaxMastery();
            if (max <= 0) return false;
            inst.setMastery(Math.min(max, inst.getMastery() + max * SHARE));
            inst.markDirty();
            p.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.pocket.resonance",
                    "An ancient spell of %s resonates with your %s grimoire and inscribes itself into it.", Component.translatableWithFallback("pocket.nusmp." + def.id() + ".name", def.name()), magic)
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC), false);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.7f);
            RukhFx.aura(p.serverLevel(), p, RukhFx.Alignment.LIGHT);
            return true;
        } catch (RuntimeException | LinkageError ex) {
            LOG.warn("[nusmp] The resonance of {} could not be written into {}'s grimoire: {}", def.id(), p.getGameProfile().getName(), ex.toString());
            return false;
        }
    }
}
