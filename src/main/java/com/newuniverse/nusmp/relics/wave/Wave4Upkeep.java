package com.newuniverse.nusmp.relics.wave;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import io.github.manasmods.tensura.data.TensuraTags;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Wave 4: the once-a-second upkeep of the neck relics, run for each online player at its own second (staggered by entity id) and a
 * bounded sweep of the charmed. Everything here is cheap and guarded: a player is only looked at in depth when one of the wave's
 * switches (flags) is on for them or a mark of the wave is open on them.
 * <ul>
 *   <li>Forest blessing: attribute modifiers and ambient effects of the chosen spirit; raccoon shakes off illusions, owl charm.</li>
 *   <li>Dreamer gills: Water Breathing and full swim speed while the gills are open.</li>
 *   <li>Truesight: invisible creatures near the wearer are outlined, blindness and darkness are shaken off.</li>
 *   <li>Echoed Souls: the spectral warriors are pointed at the wearer's foe.</li>
 * </ul>
 */
final class Wave4Upkeep {
    private Wave4Upkeep() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static boolean warned;

    static final String FLAG_FOREST = "w4_forest";
    static final String FLAG_DREAMER = "w4_dreamer";
    static final String FLAG_SEA = "w4_sea";
    static final String FOREST_ID = "amulet_of_the_forest";
    static final String DREAMER_ID = "pendant_of_the_dreamer";
    static final double SIGHT_RADIUS = 24.0;
    static final int SIGHT_MAX = 16;
    static final double WARRIOR_REACH = 14.0;
    /** A lent Tensura skill is renewed every second for this many ticks, so it is gone a few seconds after the relic stops working. */
    static final int SKILL_LEASE = 100;
    /** Truesight lends these Tensura skills (both exist in the skill registry; an absent one is skipped). */
    static final String[] SIGHT_SKILLS = {"tensura:magic_sense", "tensura:sage"};

    /** From the server tick: every player once a second at their own offset, and the charmed once a second. */
    static void run(MinecraftServer server) {
        int tick = server.getTickCount();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if ((tick + p.getId()) % 20 != 0 || !p.isAlive()) continue;
            try {
                long now = p.level().getGameTime();
                forest(p);
                dreamer(p);
                sight(p, now, ((tick + p.getId()) / 20) % 2 == 0);
                echo(p, now);
            } catch (RuntimeException ex) {
                if (!warned) { warned = true; LOG.warn("Wave 4 relic upkeep failed (further failures are not logged)", ex); }
            }
        }
        if (tick % 20 == 7 && Wave4State.CHARM.size() > 0) {
            try {
                sweepCharmed(server);
            } catch (RuntimeException ex) {
                if (!warned) { warned = true; LOG.warn("Wave 4 charm sweep failed (further failures are not logged)", ex); }
            }
        }
    }

    // ---------------------------------------------------------------- Amulet of the Forest
    static void forest(ServerPlayer p) {
        UUID id = p.getUUID();
        Wave4Forest on = null;
        if (RelicFlags.has(p, FLAG_FOREST)) {
            ItemStack st = Wave4Store.find(p, FOREST_ID);
            if (!st.isEmpty()) {
                CompoundTag t = Wave4Store.read(st);
                Wave4Forest s = Wave4Forest.byId(t.getString(Wave4Store.K_SPIRIT));
                if (s != null) {
                    if (Wave4Forest.active(t.getLong(Wave4Store.K_BLESSED_UNTIL), p.level().getGameTime())) {
                        on = s;
                    } else {
                        Wave4Store.edit(st, x -> { x.remove(Wave4Store.K_SPIRIT); x.remove(Wave4Store.K_BLESSED_UNTIL); });
                        Fx.say(p, "The " + s.label + " spirit's day is over: its blessing leaves you.");
                    }
                }
            }
        }
        Wave4Forest was = Wave4State.FOREST.get(id);
        if (was != null && was != on) strip(p, was);
        if (on == null) { Wave4State.FOREST.remove(id); return; }
        Wave4State.FOREST.put(id, on);
        for (Wave4Forest.Mod m : on.mods) {
            Holder<Attribute> h = attribute(m.attr());
            if (h == null) continue;
            AttributeInstance ai = p.getAttribute(h);
            ResourceLocation mid = modId(on, m);
            if (ai != null && !ai.hasModifier(mid)) ai.addOrUpdateTransientModifier(new AttributeModifier(mid, m.amount(), operation(m.op())));
        }
        for (String e : on.effects) Wave4Look.ambient(p, e, 100, 0);
        if (on.skills.length > 0) Fx.lendSkills(p, SKILL_LEASE, on.skills);              // the spirit's Tensura gift, on a short lease kept fresh while it blesses you
        if (on == Wave4Forest.RACCOON) {
            p.removeEffect(MobEffects.BLINDNESS);
            p.removeEffect(MobEffects.DARKNESS);
            p.removeEffect(MobEffects.CONFUSION);
        } else if (on == Wave4Forest.OWL) {
            shakeCharm(p);
        }
    }

    private static void strip(ServerPlayer p, Wave4Forest s) {
        for (Wave4Forest.Mod m : s.mods) {
            Holder<Attribute> h = attribute(m.attr());
            AttributeInstance ai = h == null ? null : p.getAttribute(h);
            if (ai != null) ai.removeModifier(modId(s, m));
        }
    }

    private static ResourceLocation modId(Wave4Forest s, Wave4Forest.Mod m) {
        return ResourceLocation.fromNamespaceAndPath("nusmp", "relic_w4_forest_" + s.id + "_" + m.attr());
    }

    private static AttributeModifier.Operation operation(int op) {
        return op == Wave4Forest.Mod.MULT_BASE ? AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                : op == Wave4Forest.Mod.MULT_TOTAL ? AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL : AttributeModifier.Operation.ADD_VALUE;
    }

    private static Holder<Attribute> attribute(String key) {
        return switch (key) {
            case "scale" -> Attributes.SCALE;
            case "attack_damage" -> Attributes.ATTACK_DAMAGE;
            case "knockback_resistance" -> Attributes.KNOCKBACK_RESISTANCE;
            case "max_health" -> Attributes.MAX_HEALTH;
            case "movement_speed" -> Attributes.MOVEMENT_SPEED;
            case "safe_fall_distance" -> Attributes.SAFE_FALL_DISTANCE;
            case "jump_strength" -> Attributes.JUMP_STRENGTH;
            case "armor" -> Attributes.ARMOR;
            case "armor_toughness" -> Attributes.ARMOR_TOUGHNESS;
            case "luck" -> Attributes.LUCK;
            case "water_movement_efficiency" -> Attributes.WATER_MOVEMENT_EFFICIENCY;
            default -> null;
        };
    }

    /** The owl cannot be charmed: any effect whose id names a charm or mind control is stripped. */
    private static void shakeCharm(ServerPlayer p) {
        List<Holder<MobEffect>> bad = null;
        for (MobEffectInstance inst : p.getActiveEffects()) {
            String path = inst.getEffect().unwrapKey().map(k -> k.location().getPath()).orElse("");
            if (path.contains("charm") || path.contains("mind_control") || path.contains("domination")) {
                if (bad == null) bad = new ArrayList<>();
                bad.add(inst.getEffect());
            }
        }
        if (bad != null) for (Holder<MobEffect> h : bad) p.removeEffect(h);
    }

    // ---------------------------------------------------------------- Pendant of the Dreamer
    static void dreamer(ServerPlayer p) {
        UUID id = p.getUUID();
        boolean gills = false;
        if (RelicFlags.has(p, FLAG_DREAMER)) {
            ItemStack st = Wave4Store.find(p, DREAMER_ID);
            gills = !st.isEmpty() && Wave4Store.read(st).getLong(Wave4Store.K_GILLS) > p.level().getGameTime();
        }
        AttributeInstance swim = p.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
        ResourceLocation mid = ResourceLocation.fromNamespaceAndPath("nusmp", "relic_w4_dreamer_gills");
        if (gills) {
            Wave4State.GILLS.add(id);
            Wave4Look.ambient(p, "minecraft:water_breathing", 100, 0);
            if (swim != null && !swim.hasModifier(mid)) swim.addOrUpdateTransientModifier(new AttributeModifier(mid, 1.0, AttributeModifier.Operation.ADD_VALUE));
        } else if (Wave4State.GILLS.remove(id) && swim != null) {
            swim.removeModifier(mid);
        }
    }

    // ---------------------------------------------------------------- Amulet of the Sea of Stars
    static void sight(ServerPlayer p, long now, boolean outline) {
        if (Wave4State.SIGHT.size() == 0) return;
        UUID id = p.getUUID();
        if (!Wave4State.SIGHT.active(id, now)) return;
        if (!RelicFlags.has(p, FLAG_SEA)) { Wave4State.SIGHT.remove(id); return; }
        p.removeEffect(MobEffects.BLINDNESS);
        p.removeEffect(MobEffects.DARKNESS);
        Wave4Look.ambient(p, "minecraft:night_vision", 300, 0);
        Fx.lendSkills(p, SKILL_LEASE, SIGHT_SKILLS);                                      // Truesight reads the magicules of everything near: Magic Sense and Sage
        if (!outline) return;
        ServerLevel lv = p.serverLevel();
        List<LivingEntity> hidden = lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(SIGHT_RADIUS), e -> e != p && e.isAlive() && e.isInvisible() && Wave4Look.plain(e));
        int n = 0;
        for (LivingEntity e : hidden) {
            if (n++ >= SIGHT_MAX) break;
            Fx.addEffect(e, "minecraft:glowing", 60, 0);
        }
    }

    // ---------------------------------------------------------------- Pendant of Echoed Souls
    static void echo(ServerPlayer p, long now) {
        UUID id = p.getUUID();
        if (!Wave4Echo.has(id)) return;
        ServerLevel lv = p.serverLevel();
        if (!Wave4Echo.guarded(id, now, w -> warriorAlive(lv, w))) {
            Fx.say(p, "The echoes fall silent.");
            return;
        }
        shakeSkillDebuffs(p);
        LivingEntity prey = p.getLastHurtByMob();
        if (prey == null || !prey.isAlive() || p.tickCount - p.getLastHurtByMobTimestamp() > 200 || prey.level() != p.level() || !Wave4Look.plain(prey) || prey instanceof Player) {
            prey = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity foe : Fx.foesAround(p, WARRIOR_REACH)) {
                if (foe instanceof Player || !Wave4Look.plain(foe)) continue;
                double d = foe.distanceToSqr(p);
                if (d < best) { best = d; prey = foe; }
            }
        }
        if (prey == null) return;
        for (UUID w : Wave4Echo.warriors(id)) {
            Entity e = lv.getEntity(w);
            if (e instanceof Mob m && m.isAlive() && m.getTarget() != prey) m.setTarget(prey);
        }
    }

    /** "Cannot be the target of a spell effect": while the guard stands, every Tensura skill debuff (fear, paralysis, silence...) is shaken off. */
    static void shakeSkillDebuffs(ServerPlayer p) {
        List<Holder<MobEffect>> bad = null;
        for (MobEffectInstance inst : p.getActiveEffects()) {
            if (inst.getEffect().is(TensuraTags.MobEffects.SKILL_DEBUFF)) {
                if (bad == null) bad = new ArrayList<>();
                bad.add(inst.getEffect());
            }
        }
        if (bad != null) for (Holder<MobEffect> h : bad) p.removeEffect(h);
    }

    static boolean warriorAlive(ServerLevel lv, UUID w) {
        Entity e = lv.getEntity(w);
        return e != null && e.isAlive();
    }

    // ---------------------------------------------------------------- the charmed
    /** Makes a creature forget what it was after (the same calls the dungeon's mantle uses on brain-driven mobs). */
    static void forget(Mob m) {
        try {
            m.setTarget(null);
            m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            m.getNavigation().stop();
        } catch (RuntimeException | LinkageError otherBrain) {
            // a brain of another shape: the plain target slot is already clear
        }
    }

    private static LivingEntity brainTarget(Mob m) {
        try {
            return m.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        } catch (RuntimeException | LinkageError otherBrain) {
            return null;
        }
    }

    private static void sweepCharmed(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (UUID id : Wave4State.CHARM.ids()) {
            if (!Wave4State.CHARM.active(id, now)) continue;
            for (ServerLevel l : server.getAllLevels()) {
                Entity e = l.getEntity(id);
                if (e == null) continue;
                if (e instanceof Mob m && m.isAlive() && (m.getTarget() instanceof Player || brainTarget(m) instanceof Player)) forget(m);
                break;
            }
        }
    }
}
