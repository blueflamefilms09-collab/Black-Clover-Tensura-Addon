package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.WikiBooks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.UUID;

/**
 * The secondary Black Clover magic a cooked monster meal attunes its eater to for a while (docs/living_dungeon.md section 4). While the
 * attunement lasts, the player's spell damage carries the rider of that magic as well as their own grimoire's: a slowing gel, a
 * shoving iron, venom, a hunger that feeds. One game-bus handler does it ({@link #onDamage}): it recognises a spell hit of the attuned
 * player, applies {@code WikiBooks.rider(magic)} to the target once a second at most, and cannot be re-entered by its own effects.
 */
public final class MealAttunement {
    private MealAttunement() {}

    private static final String PART = "Attunement";
    private record Key(UUID caster, UUID target) {}
    /** A rider acts at most once a second on a target, however many times a spell hits it. */
    private static final Cooldowns<Key> RIDERS = new Cooldowns<>(20);
    private static boolean applying;

    /** Attunes the player to {@code magic} for {@code ticks}; a new meal replaces the attunement of the old one. */
    public static void give(Player p, MagicType magic, int ticks) {
        if (ticks <= 0) return;
        CompoundTag t = SkillsData.part(p, PART);
        t.putString("Magic", magic.name());
        t.putLong("Until", p.level().getGameTime() + ticks);
    }

    /** The magic the player is attuned to now, or null. */
    public static MagicType active(Player p) {
        CompoundTag t = SkillsData.peek(p, PART);
        return t == null || p.level().getGameTime() >= t.getLong("Until") ? null : parse(t.getString("Magic"));
    }

    /** Ticks of the attunement left, 0 if there is none. */
    public static long left(Player p) {
        CompoundTag t = SkillsData.peek(p, PART);
        return t == null ? 0L : Math.max(0L, t.getLong("Until") - p.level().getGameTime());
    }

    /** The magic with this constant name, or null if there is none (a table entry the game does not know does nothing). */
    public static MagicType parse(String name) {
        try {
            return MagicType.valueOf(name);
        } catch (IllegalArgumentException unknown) { /* no such magic */ }
        return null;
    }

    public static void clear() { RIDERS.clear(); }

    // ---------------------------------------------------------------- game bus
    public static void onDamage(LivingDamageEvent.Post e) {
        if (applying || e.getNewDamage() <= 0f) return;
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer p)) return;
        LivingEntity target = e.getEntity();
        if (target == p || !target.isAlive() || target.isAlliedTo(p)) return;
        MagicType magic = active(p);
        if (magic == null || src.is(DamageTypes.THORNS) || !AntiMagic.isMagic(src)) return;
        if (!RIDERS.tryUse(new Key(p.getUUID(), target.getUUID()), p.level().getGameTime())) return;
        applying = true;
        try {
            WikiBooks.rider(magic).apply(target, p);
        } catch (RuntimeException riderFailed) { /* a rider must never break the hit that carried it */
        } finally {
            applying = false;
        }
    }
}
