package com.newuniverse.nusmp.dungeon.sentient;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The Juggernaut of the Dungeon's immune system: a creature of the floor (a bear or an orc of the Catacombs, a megalodon of the Waterways, a
 * greater daemon of the Underworld), born from a wall near the player it has taken for a lethal threat after a long, loud crack, and grown
 * (four times the health up to the attribute's ceiling, more than twice the damage, faster, armoured, hard to knock back), named, outlined
 * and given a red boss bar for the players within 48 blocks. {@link BornDriver} sends it at the nearest player like any born mob, but
 * from 64 blocks. It is bounded: three at a time, and the pending births count.
 */
final class JuggernautSpawner {
    private JuggernautSpawner() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** The crack of a Juggernaut's birth, in ticks; how near and how far from the player its wall is; how far its bar reaches; how long a birth may be waited for. */
    private static final int DELAY = 70, MIN_DIST = 14, MAX_DIST = 22, BAR_RANGE = 48, WAIT_MARGIN = 60;
    /** How many places are looked at for one that is big enough for the Juggernaut. */
    private static final int CANDIDATES = 8;

    private static final class Juggernaut {
        final Mob mob;
        final ServerBossEvent bar;

        Juggernaut(Mob mob, ServerBossEvent bar) {
            this.mob = mob;
            this.bar = bar;
        }
    }

    private static final Map<UUID, Juggernaut> ALIVE = new LinkedHashMap<>();
    /** The game times by which the Juggernauts on their way will have been born or given up: they count against the cap meanwhile. */
    private static final ArrayDeque<Long> PENDING_UNTIL = new ArrayDeque<>();
    private static boolean sorry;

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }

    // ---------------------------------------------------------------- the count
    static int alive() {
        for (Iterator<Juggernaut> it = ALIVE.values().iterator(); it.hasNext(); ) {
            Mob m = it.next().mob;
            if (!m.isAlive() || m.isRemoved()) it.remove();
        }
        return ALIVE.size();
    }

    static int pending(long now) {
        while (!PENDING_UNTIL.isEmpty() && PENDING_UNTIL.peekFirst() <= now) PENDING_UNTIL.pollFirst();
        return PENDING_UNTIL.size();
    }

    // ---------------------------------------------------------------- sending one
    /** Schedules a Juggernaut for the player: false if there is nowhere to birth one or nothing to make it from. The caller has checked the caps. */
    static boolean spawn(ServerPlayer p, String reason) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        BlockPos here = p.blockPosition();
        FloorKind found = FloorKind.at(level, here);
        final FloorKind floor = found.hasRoomBirths() ? found : FloorKind.CATACOMBS;
        List<EntityType<?>> types = typesFor(floor);
        if (types.isEmpty()) {
            if (!sorry) {
                sorry = true;
                LOG.warn("[nusmp] The Dungeon has no creature to make a Juggernaut of on {} (is Tensura installed?)", floor.label());
            }
            return false;
        }
        RandomSource rnd = level.getRandom();
        Collections.shuffle(types, new java.util.Random(rnd.nextLong()));
        EntityType<?> chosen = null;
        BlockPos spot = null;
        List<BlockPos> dry = null, wet = null;
        for (EntityType<?> t : types) {
            boolean water = JuggernautPlan.needsWater(pathOf(t));
            List<BlockPos> spots;
            if (water) {
                if (wet == null) wet = WallBirth.findSpots(level, here, CANDIDATES, MIN_DIST, MAX_DIST, true);
                spots = wet;
            } else {
                if (dry == null) dry = WallBirth.findSpots(level, here, CANDIDATES, MIN_DIST, MAX_DIST, false);
                spots = dry;
            }
            BlockPos fit = firstFit(level, spots, t);
            if (fit != null) {
                chosen = t;
                spot = fit;
                break;
            }
        }
        if (chosen == null) {                                                // no water near a swimmer, or no room for any of them: a creature of the land comes instead
            if (dry == null) dry = WallBirth.findSpots(level, here, CANDIDATES, MIN_DIST, MAX_DIST, false);
            if (dry.isEmpty()) return false;
            for (String fallback : JuggernautPlan.defaults(FloorKind.CATACOMBS)) {
                Optional<EntityType<?>> t = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(fallback));
                if (t.isEmpty()) continue;
                BlockPos fit = firstFit(level, dry, t.get());
                if (fit != null) {
                    chosen = t.get();
                    spot = fit;
                    break;
                }
            }
            if (chosen == null) return false;
        }
        final EntityType<?> type = chosen;
        WallBirth.Factory factory = (lvl, at) -> FloorRoster.make(lvl, type, at, lvl.getRandom());
        Consumer<Mob> onBorn = m -> born(m, floor);
        if (WallBirth.birthAt(level, List.of(spot), DELAY, factory, onBorn) == 0) return false;
        PENDING_UNTIL.addLast(now + DELAY + WAIT_MARGIN);
        LOG.info("[nusmp] A Juggernaut ({}) is being born near {} on {} ({})", Wildlife.idOf(type), p.getGameProfile().getName(), floor.label(), reason);
        return true;
    }

    private static String pathOf(EntityType<?> t) { return BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath(); }

    /** The first of the places that has room for a creature of this type, or null. */
    private static BlockPos firstFit(ServerLevel level, List<BlockPos> spots, EntityType<?> type) {
        for (BlockPos s : spots) if (WallBirth.fits(level, s, type)) return s;
        return null;
    }

    /** The kinds a Juggernaut of the floor may be made of: the floor's tag, else the tier-3 roster, else the built-in list; only those that exist in this game. */
    private static List<EntityType<?>> typesFor(FloorKind floor) {
        List<EntityType<?>> out = new ArrayList<>();
        readTag(out, TagKey.create(Registries.ENTITY_TYPE, id(JuggernautPlan.tagPath(floor))));
        if (out.isEmpty()) readTag(out, Wildlife.TIER3);
        if (out.isEmpty()) {
            for (String s : JuggernautPlan.defaults(floor)) {
                ResourceLocation rl = ResourceLocation.tryParse(s);
                if (rl != null) BuiltInRegistries.ENTITY_TYPE.getOptional(rl).ifPresent(out::add);
            }
        }
        return out;
    }

    private static void readTag(List<EntityType<?>> out, TagKey<EntityType<?>> tag) {
        Optional<HolderSet.Named<EntityType<?>>> set = BuiltInRegistries.ENTITY_TYPE.getTag(tag);
        if (set.isEmpty()) return;
        for (Holder<EntityType<?>> h : set.get()) {
            EntityType<?> t = h.value();
            if (t.getCategory() != MobCategory.MISC) out.add(t);
        }
    }

    // ---------------------------------------------------------------- growing it
    /** The wall has given way: the creature is made a Juggernaut. */
    private static void born(Mob m, FloorKind floor) {
        PENDING_UNTIL.pollFirst();
        try {
            grow(m, floor);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] Could not grow a Juggernaut", failure);
        }
        adopt(m);
    }

    private static void grow(Mob m, FloorKind floor) {
        m.addTag(SentientTags.BORN);
        m.addTag(SentientTags.JUGGERNAUT);
        m.getPersistentData().putInt(SentientTags.FLOOR, floor.ordinal());
        m.setCustomName(Component.literal(SentientText.JUGGERNAUT).withStyle(ChatFormatting.DARK_RED));
        m.setCustomNameVisible(true);
        m.setGlowingTag(true);
        JuggernautPlan.Profile p = JuggernautPlan.profile(floor);
        AttributeInstance health = m.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            double extra = JuggernautPlan.healthExtra(health.getValue(), p.healthExtra());
            if (extra > 0.0) health.addOrReplacePermanentModifier(new AttributeModifier(id("juggernaut_health"), extra, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        modify(m, Attributes.ATTACK_DAMAGE, "juggernaut_damage", p.damageExtra(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modify(m, Attributes.MOVEMENT_SPEED, "juggernaut_speed", p.speedExtra(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modify(m, Attributes.ARMOR, "juggernaut_armor", p.armor(), AttributeModifier.Operation.ADD_VALUE);
        modify(m, Attributes.KNOCKBACK_RESISTANCE, "juggernaut_knockback", p.knockback(), AttributeModifier.Operation.ADD_VALUE);
        AttributeInstance follow = m.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null && follow.getValue() < p.followRange()) {
            follow.addOrReplacePermanentModifier(new AttributeModifier(id("juggernaut_follow"), p.followRange() - follow.getValue(), AttributeModifier.Operation.ADD_VALUE));
        }
        m.setHealth(m.getMaxHealth());
        m.setPersistenceRequired();
    }

    private static void modify(Mob m, Holder<Attribute> attribute, String name, double amount, AttributeModifier.Operation op) {
        AttributeInstance a = m.getAttribute(attribute);
        if (a != null && amount != 0.0) a.addOrReplacePermanentModifier(new AttributeModifier(id(name), amount, op));
    }

    // ---------------------------------------------------------------- bars
    /** Keeps a Juggernaut (new, or loaded from disk) in the list with its bar. */
    static void adopt(Mob m) {
        if (ALIVE.containsKey(m.getUUID())) return;
        ServerBossEvent bar = new ServerBossEvent(Component.literal(SentientText.JUGGERNAUT).withStyle(ChatFormatting.DARK_RED), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        ALIVE.put(m.getUUID(), new Juggernaut(m, bar));
    }

    /** Called every twenty ticks: the bars show the health to the players within 48 blocks, and a Juggernaut that fell or went takes its bar with it. */
    static void upkeep() {
        double r2 = (double) BAR_RANGE * BAR_RANGE;
        for (Iterator<Juggernaut> it = ALIVE.values().iterator(); it.hasNext(); ) {
            Juggernaut j = it.next();
            Mob m = j.mob;
            if (!m.isAlive() || m.isRemoved() || !(m.level() instanceof ServerLevel level)) {
                j.bar.removeAllPlayers();
                it.remove();
                continue;
            }
            j.bar.setProgress(Math.max(0f, Math.min(1f, m.getHealth() / m.getMaxHealth())));
            for (ServerPlayer p : level.players()) {
                if (!p.isSpectator() && p.distanceToSqr(m) <= r2) j.bar.addPlayer(p);
                else j.bar.removePlayer(p);
            }
            for (ServerPlayer gone : new ArrayList<>(j.bar.getPlayers())) {                  // a player who left the dimension is not in level.players(): off the bar too
                if (gone.level() != level) j.bar.removePlayer(gone);
            }
        }
    }

    /** A Juggernaut died: its bar goes. */
    static void forget(Entity e) {
        Juggernaut j = ALIVE.remove(e.getUUID());
        if (j != null) j.bar.removeAllPlayers();
    }

    /** A player left: they are taken off every bar. */
    static void dropPlayer(ServerPlayer p) {
        for (Juggernaut j : ALIVE.values()) j.bar.removePlayer(p);
    }

    static void clear() {
        for (Juggernaut j : ALIVE.values()) j.bar.removeAllPlayers();
        ALIVE.clear();
        PENDING_UNTIL.clear();
        sorry = false;
    }
}
