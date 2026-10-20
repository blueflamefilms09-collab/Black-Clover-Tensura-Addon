package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryData;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryDef;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryMobEntity;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryRegistry;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryScaling;
import com.newuniverse.nusmp.labyrinth.mega.rift.PocketRealm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Why: the exclusive mobs of a tomb. They are not new entity types (the brief forbids those): each is an existing body, a Mob Foundry mob (a look and a set of abilities the Foundry already animates and
 * balances) or a vanilla or Tensura type when the Foundry roster does not hold the id, wearing the tomb's name, size, aura and a permanent set of attribute modifiers that stand it one tier above the floor of
 * the rift that opened the tomb ({@link PocketRules#tierFactor}) and then apply the profile's own multipliers. A room's monsters are made when the first delver walks into it, not before: a tomb of twenty
 * rooms holds its monsters asleep as data, not as sixty loaded entities. The rooms with a gravity rule get only the profile's fliers (a walker would stand on the wrong side of the room), the others only the
 * walkers. The keeper of the key room carries the Sigil; the mega-boss's court is called by the boss at half health. All of it carries scoreboard tags so a sweep can find it, and all of it is persistent
 * (a tomb's monster does not despawn behind a delver's back).
 */
public final class PocketMobs {
    private PocketMobs() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String TAG = "nusmp_pocket_mob", TAG_KEEPER = "nusmp_tomb_keeper", TAG_ADD = "nusmp_tomb_add";
    private static final ResourceLocation HP = id("pocket_hp"), DMG = id("pocket_damage"), ARMOR = id("pocket_armor"), SPEED = id("pocket_speed"), SCALE = id("pocket_scale"), KB = id("pocket_knockback"),
            KEEPER = id("pocket_keeper");

    private static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath("nusmp", p); }

    /** The tag a mob of a profile carries (so the aura and the sweeps know which profile it is). */
    public static String tagOf(PocketDef.Mob m) { return TAG + "_" + m.id(); }

    // ---------------------------------------------------------------- one mob
    /** Makes the mob of a profile for a floor (not yet in the level), or null when no body could be made. */
    public static Mob create(ServerLevel level, PocketDef def, PocketDef.Mob m, int floor) {
        Mob mob = null;
        int baseTier = 1;
        if (m.foundryBase()) {
            FoundryDef d = FoundryData.roster().byId(m.foundryId());
            if (d != null) {
                FoundryMobEntity f = FoundryRegistry.MOB.get().create(level);
                if (f != null) {
                    f.setFoundry(d, floor);
                    baseTier = d.tier();
                    mob = f;
                }
            }
        } else {
            mob = typed(level, m.base());
        }
        if (mob == null) mob = typed(level, m.fallback());
        if (mob == null) mob = typed(level, m.air() ? "minecraft:vex" : "minecraft:zombie");
        if (mob == null) return null;
        double hp = PocketRules.tierFactor(FoundryScaling.HEALTH, baseTier, floor) * m.hp(), dmg = PocketRules.tierFactor(FoundryScaling.DAMAGE, baseTier, floor) * m.damage();
        modify(mob, Attributes.MAX_HEALTH, HP, hp - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modify(mob, Attributes.ATTACK_DAMAGE, DMG, dmg - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modify(mob, Attributes.ARMOR, ARMOR, m.armor(), AttributeModifier.Operation.ADD_VALUE);
        modify(mob, Attributes.MOVEMENT_SPEED, SPEED, m.speed() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modify(mob, Attributes.SCALE, SCALE, m.scale() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modify(mob, Attributes.KNOCKBACK_RESISTANCE, KB, m.knockback(), AttributeModifier.Operation.ADD_VALUE);
        mob.setHealth(mob.getMaxHealth());
        mob.setCustomName(Component.translatableWithFallback("pocket.nusmp." + def.id() + ".mob." + m.id(), m.name()));
        mob.setCustomNameVisible(false);
        mob.setPersistenceRequired();
        mob.addTag(TAG);
        mob.addTag(tagOf(m));
        if (m.elite()) mob.setGlowingTag(true);
        return mob;
    }

    private static Mob typed(ServerLevel level, String typeId) {
        ResourceLocation rl = typeId == null ? null : ResourceLocation.tryParse(typeId);
        if (rl == null) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
        if (type == null) return null;
        try {
            return type.create(level) instanceof Mob mob ? mob : null;
        } catch (RuntimeException ex) {
            LOG.debug("[nusmp] pocket mob type {} could not be created: {}", typeId, ex.toString());
            return null;
        }
    }

    private static void modify(Mob mob, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = mob.getAttribute(attribute);
        if (inst == null || Math.abs(amount) < 1e-6) return;
        inst.removeModifier(id);
        inst.addPermanentModifier(new AttributeModifier(id, amount, op));
    }

    /** Puts a created mob in the level at a place (and finalises a body that is not a Foundry one). */
    public static boolean place(ServerLevel level, Mob mob, double x, double y, double z, Random rnd) {
        mob.moveTo(x, y, z, rnd.nextFloat() * 360f, 0f);
        if (!(mob instanceof FoundryMobEntity)) {
            try {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.TRIGGERED, null);
            } catch (RuntimeException ex) {
                LOG.debug("[nusmp] finalizeSpawn failed for a pocket mob: {}", ex.toString());
            }
            mob.setPersistenceRequired();
        }
        return level.addFreshEntity(mob);
    }

    // ---------------------------------------------------------------- a room's monsters
    /** Wakes the monsters of a room for {@code players} delvers; returns how many were put there. */
    public static int fillRoom(ServerLevel level, PocketRuntime rt, PocketRealm.Pocket p, int roomId, int players) {
        PocketLayout.Room room = rt.layout.room(roomId);
        PocketPlan.RoomBox box = rt.plan.box(roomId);
        int want = PocketRules.monsterCount(room.kind(), room.half(), players);
        if (want <= 0 || box.spots().isEmpty()) return 0;
        boolean air = room.kind().gravity();
        Random rnd = new Random(rt.seed ^ (roomId * 0x9E3779B97F4A7C15L) ^ 0x6D0B5L);
        List<int[]> spots = new ArrayList<>(box.spots());
        Collections.shuffle(spots, rnd);
        int floor = PocketRules.effectiveFloor(p.originFloor, rt.def.floorMin()), made = 0, spot = 0, guard = 0;
        while (made < want && guard++ < 24) {
            int idx = PocketRules.pickMob(rt.def.mobs(), air, rnd);
            if (idx < 0) break;
            PocketDef.Mob m = rt.def.mobs().get(idx);
            int group = Math.min(want - made, m.groupMin() + (m.groupMax() > m.groupMin() ? rnd.nextInt(m.groupMax() - m.groupMin() + 1) : 0));
            for (int g = 0; g < group; g++) {
                int[] s = spots.get(spot++ % spots.size());
                double y = air ? airHeight(room, rnd) : PocketPlan.Y0;
                BlockPos at = new BlockPos(PocketCell.centreX(rt.slot) + s[0], (int) Math.floor(y), PocketCell.centreZ() + s[1]);
                if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) continue;
                Mob mob = create(level, rt.def, m, floor);
                if (mob == null) continue;
                if (place(level, mob, at.getX() + 0.5, y, at.getZ() + 0.5, rnd)) made++;
            }
        }
        return made;
    }

    private static double airHeight(PocketLayout.Room room, Random rnd) {
        int h = room.height();
        return room.kind() == PocketLayout.Kind.INVERTED ? PocketPlan.Y0 + h - 5 + rnd.nextInt(2) : PocketPlan.Y0 + 3 + rnd.nextInt(Math.max(1, h - 7));
    }

    /** The keeper of the Sigil in the key room: the tomb's elite (or its heaviest walker), twice as hardy, glowing, named. Returns it or null. */
    public static Mob keeper(ServerLevel level, PocketRuntime rt, PocketRealm.Pocket p) {
        if (rt.keySpawn == null) return null;
        PocketDef.Mob pick = null;
        for (PocketDef.Mob m : rt.def.mobs()) if (!m.air() && m.elite()) pick = m;
        if (pick == null) for (PocketDef.Mob m : rt.def.mobs()) if (!m.air() && (pick == null || m.weight() > pick.weight())) pick = m;
        if (pick == null) return null;
        Mob mob = create(level, rt.def, pick, PocketRules.effectiveFloor(p.originFloor, rt.def.floorMin()));
        if (mob == null) return null;
        modify(mob, Attributes.MAX_HEALTH, KEEPER, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mob.setHealth(mob.getMaxHealth());
        mob.addTag(TAG_KEEPER);
        mob.setGlowingTag(true);
        mob.setCustomName(Component.translatableWithFallback("message.nusmp.mega.pocket.keeper", "%s, Keeper of the Sigil",
                Component.translatableWithFallback("pocket.nusmp." + rt.def.id() + ".mob." + pick.id(), pick.name())));
        mob.setCustomNameVisible(true);
        return place(level, mob, rt.keySpawn.getX() + 0.5, rt.keySpawn.getY(), rt.keySpawn.getZ() + 0.5, new Random(rt.seed ^ 0x4B3EL)) ? mob : null;
    }

    /** The court the mega-boss calls at half health: {@code count} of the mob {@code mobId} round {@code centre}. */
    public static int adds(ServerLevel level, PocketRuntime rt, PocketRealm.Pocket p, String mobId, int count, BlockPos centre) {
        PocketDef.Mob m = null;
        for (PocketDef.Mob x : rt.def.mobs()) if (x.id().equals(mobId)) m = x;
        if (m == null || count <= 0) return 0;
        Random rnd = new Random(rt.seed ^ 0xADD5L ^ level.getGameTime());
        int made = 0, floor = PocketRules.effectiveFloor(p.originFloor, rt.def.floorMin());
        for (int i = 0; i < Math.min(count, PocketRules.ADDS_CAP); i++) {
            double a = Math.PI * 2 * i / Math.max(1, count) + rnd.nextDouble(), r = 4 + rnd.nextInt(4);
            BlockPos at = BlockPos.containing(centre.getX() + 0.5 + Math.cos(a) * r, m.air() ? centre.getY() + 4 : centre.getY(), centre.getZ() + 0.5 + Math.sin(a) * r);
            if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) continue;
            Mob mob = create(level, rt.def, m, floor);
            if (mob == null) continue;
            mob.addTag(TAG_ADD);
            if (place(level, mob, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, rnd)) made++;
        }
        return made;
    }

    // ---------------------------------------------------------------- the aura
    /** The particle of a profile's aura as an option (a dust in the colour when the id is dust, else the simple particle of that id), or null. */
    public static ParticleOptions auraOf(PocketDef.Mob m) {
        if (m.aura().equals("minecraft:dust")) return new DustParticleOptions(new Vector3f(((m.auraRgb() >> 16) & 255) / 255f, ((m.auraRgb() >> 8) & 255) / 255f, (m.auraRgb() & 255) / 255f), 1.0f);
        ResourceLocation rl = ResourceLocation.tryParse(m.aura());
        if (rl == null) return null;
        return BuiltInRegistries.PARTICLE_TYPE.getOptional(rl).filter(t -> t instanceof SimpleParticleType).map(t -> (ParticleOptions) t).orElse(null);
    }

    /** The profile a living tomb mob wears, from its tags, or null. */
    public static PocketDef.Mob profileOf(PocketDef def, Mob mob) {
        for (PocketDef.Mob m : def.mobs()) if (mob.getTags().contains(tagOf(m))) return m;
        return null;
    }

    /** Draws the aura of every tomb mob near a point (the caller runs it every few ticks for the tombs that have delvers). */
    public static void aura(ServerLevel level, PocketRuntime rt, double x, double y, double z) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(x - 40, y - 24, z - 40, x + 40, y + 24, z + 40), e -> e.isAlive() && e.getTags().contains(TAG))) {
            PocketDef.Mob m = profileOf(rt.def, mob);
            if (m == null) continue;
            ParticleOptions opt = auraOf(m);
            if (opt != null) level.sendParticles(opt, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 2, mob.getBbWidth() * 0.4, mob.getBbHeight() * 0.35, mob.getBbWidth() * 0.4, 0.01);
        }
    }

    /** How many tomb mobs live in a cell. */
    public static int alive(ServerLevel level, int slot) { return level.getEntitiesOfClass(Mob.class, PocketCell.bounds(slot), e -> e.isAlive() && e.getTags().contains(TAG)).size(); }
}
