package com.newuniverse.nusmp.labyrinth.mega.weekly;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Why: puts the week's twists on the hostile creatures of the dungeon floors that earn them. One handler, on the creature's first join to the dungeon dimension (a creature loaded from a save
 * already carries its permanent modifiers, so it is skipped): it reads the floor under the creature, asks {@link WeeklyRules} how many twists that floor has and which, and adds one permanent
 * attribute modifier per twist under a fixed id (so a second join can never stack them). Worst case per creature join: one floor lookup, at most three attribute writes, no entity, no packet,
 * no particle, nothing per tick. The week is read from the UTC calendar day, at most once per 60 s (cached), never per creature.
 */
public final class WeeklyEvents {
    private WeeklyEvents() {}

    private static final String NS = "anime_multiverse";
    private static long cachedAt = Long.MIN_VALUE;
    private static long cachedWeek;

    public static void register(IEventBus bus) { bus.addListener(WeeklyEvents::onJoin); }

    /** The current week number (UTC), re-read from the clock at most once per minute. */
    public static long currentWeek() {
        long now = System.currentTimeMillis();
        if (now - cachedAt > 60_000L || now < cachedAt) {
            cachedWeek = WeeklyRules.weekOf(LocalDate.now(ZoneOffset.UTC).toEpochDay());
            cachedAt = now;
        }
        return cachedWeek;
    }

    /** The twists the current week holds for a floor under the server's thresholds (empty when switched off or the floor is too shallow). */
    public static List<WeeklyRules.Mod> twistsFor(int floor) {
        if (!WeeklyConfig.get(WeeklyConfig.ENABLED)) return List.of();
        int n = WeeklyRules.countFor(floor, WeeklyConfig.get(WeeklyConfig.FLOOR_ONE), WeeklyConfig.get(WeeklyConfig.FLOOR_TWO), WeeklyConfig.get(WeeklyConfig.FLOOR_THREE));
        return n == 0 ? List.of() : WeeklyRules.pick(currentWeek(), n);
    }

    private static void onJoin(EntityJoinLevelEvent e) {
        try {
            Entity en = e.getEntity();
            if (e.loadedFromDisk() || e.getLevel().isClientSide() || !(e.getLevel() instanceof ServerLevel sl) || !(en instanceof Mob m) || !(en instanceof Enemy)) return;
            if (!MegaDungeon.in(sl)) return;
            apply(m, MegaDungeon.floorAt(sl, m.blockPosition()));
        } catch (RuntimeException ex) {
            // the week's twists are a bonus: a fault must never stop a creature from joining
        }
    }

    /** Adds the floor's twists to a creature (idempotent: each twist has one fixed id). Returns how many were added. */
    public static int apply(Mob m, int floor) {
        int scale = WeeklyConfig.get(WeeklyConfig.STRENGTH);
        int added = 0;
        for (WeeklyRules.Mod mod : twistsFor(floor)) {
            int pct = WeeklyRules.scaledPercent(mod, scale);
            if (pct <= 0) continue;
            if (add(m, mod, pct)) added++;
        }
        return added;
    }

    private static boolean add(Mob m, WeeklyRules.Mod mod, int pct) {
        Holder<Attribute> attr = switch (mod) {
            case THICK_HIDE -> Attributes.MAX_HEALTH;
            case SWIFT_FOES -> Attributes.MOVEMENT_SPEED;
            case SAVAGE_BLOWS -> Attributes.ATTACK_DAMAGE;
            case ARMOURED -> Attributes.ARMOR;
            case KEEN_SENSES -> Attributes.FOLLOW_RANGE;
        };
        AttributeInstance inst = m.getAttribute(attr);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(NS, "weekly/" + mod.id());
        if (inst == null || inst.getModifier(id) != null) return false;
        // armour adds points (a percentage of nothing is nothing); everything else multiplies
        AttributeModifier mm = mod == WeeklyRules.Mod.ARMOURED
                ? new AttributeModifier(id, pct / 5.0, AttributeModifier.Operation.ADD_VALUE)
                : new AttributeModifier(id, pct / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        inst.addPermanentModifier(mm);
        if (mod == WeeklyRules.Mod.THICK_HIDE) m.setHealth(m.getMaxHealth());
        return true;
    }
}
