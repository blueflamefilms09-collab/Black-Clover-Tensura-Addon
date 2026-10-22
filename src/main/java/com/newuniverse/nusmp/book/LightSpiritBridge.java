package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Why: the Spirit of Light (package {@code spiritlight}) is a servant of Light Magic, and its beam must hurt exactly like the Light Magic spells do: through {@link LightBook#lightHit} (a light hit: armour
 * ignored, the spirit cut, held back only by an Aura barrier, then the balance law and its caps in {@link GrimoireBook#hurtAs}). That method belongs to this package and needs the caster's own Light book
 * instance, so this small public door lives here and nothing in {@link LightBook} changed. It never lifts a cap and never deals more than the raw number it is given.
 */
public final class LightSpiritBridge {
    private LightSpiritBridge() {}

    /** The caster's Light Magic book instance, or empty when the caster has not got Light Magic. */
    private static Optional<ManasSkillInstance> instance(ServerPlayer p) {
        Optional<ManasSkillInstance> opt = SkillAPI.getSkillsFrom(p).getSkill(NUSkills.BOOK_LIGHT.getId());
        return opt.isPresent() && opt.get().getSkill() instanceof LightBook ? opt : Optional.empty();
    }

    /** Does this player have Light Magic (the book skill)? Only such a caster may link the Spirit of Light. */
    public static boolean hasLightMagic(ServerPlayer p) { return instance(p).isPresent(); }

    /** One light hit of {@code raw} raw damage on a target, as the caster's Light Magic would deal it. False when the caster has no Light Magic (nothing happens). */
    public static boolean hit(ServerPlayer p, LivingEntity target, float raw) {
        Optional<ManasSkillInstance> opt = instance(p);
        if (opt.isEmpty() || target == null || raw <= 0f) return false;
        LightBook.lightHit((LightBook) opt.get().getSkill(), opt.get(), p, target, 0, raw);
        return true;
    }
}
