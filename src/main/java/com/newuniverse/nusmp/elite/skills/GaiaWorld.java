package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Minecraft-facing half of Gaia. The pure parts ({@link GaiaZone}, {@link GaiaCost}, {@link GaiaWarCheck}, {@link GaiaBloomLedger}, {@link VeinBloom}, {@link GaiaPillars}) decide; this class
 * reads the world and the players, asks them, and applies the answer: raise or dismiss a domain, the once-a-second upkeep (the price, the blessing of allies, the burden on enemies, the points the
 * ledger earns), the pillar volley (damage and a knock-up, no blocks placed) and the bloom (replace a few natural stone blocks with a small vein of ore, within the daily cap). The war check reads
 * nations through the late-bound {@code EliteHooks} (neutral while nations do not exist). Everything is server side and nothing here throws on a missing piece.
 */
final class GaiaWorld {
    private GaiaWorld() {}

    static final String LEDGER = "Ledger";
    /** Volleys still rising per owner (they add a little to the upkeep while they last). */
    private static final Map<UUID, Integer> IN_FLIGHT = new HashMap<>();

    static String dim(Level level) { return level.dimension().location().toString(); }

    static GaiaWarCheck.Relation relation(UUID owner, UUID other) { return GaiaWarCheck.relation(owner, other, EliteHooks.nationOf, EliteHooks.atWar); }

    // ================================================================== the domain
    static GaiaZone raise(ServerPlayer owner, boolean mastered) {
        ServerLevel level = owner.serverLevel();
        double radius = GaiaCost.radius(mastered);
        GaiaZone zone = new GaiaZone(owner.getUUID(), dim(level), owner.getX(), owner.getY(), owner.getZ(), radius, level.getGameTime() + GaiaCost.durationTicks(mastered));
        GaiaZone.put(zone);
        EliteBridge.ring(level, owner.position().add(0, 0.1, 0), Math.min(radius, 20.0), 0x5E9A3A);
        EliteBridge.shape(level, VfxShape.EARTH_RISE, owner.position(), owner.position().add(0, 1, 0), 0xFF6AAA3A, 40, (float) (radius / 8.0));
        EliteBridge.burst(level, owner.position().add(0, 1, 0), false, 18, radius * 0.15);
        EliteBridge.sound(level, owner.position(), SoundEvents.BEACON_ACTIVATE, 1.2f, 0.6f);
        EliteBridge.actionbar(owner, SkillMsg.t("gaia.raised", "Gaia's domain rises (%s blocks).", (int) radius).withStyle(ChatFormatting.GREEN));
        return zone;
    }

    static boolean dismiss(ServerPlayer owner, boolean quiet) {
        GaiaZone removed = GaiaZone.remove(owner.getUUID());
        if (removed != null && !quiet) EliteBridge.actionbar(owner, SkillMsg.t("gaia.dismissed", "Gaia's domain fades.").withStyle(ChatFormatting.GRAY));
        return removed != null;
    }

    /** Once a second, for the owner of a Gaia skill. */
    static void upkeep(ManasSkillInstance instance, ServerPlayer owner) {
        GaiaZone z = GaiaZone.of(owner.getUUID());
        if (z == null) return;
        ServerLevel level = owner.serverLevel();
        long now = level.getGameTime();
        if (!z.active(now)) {
            dismiss(owner, false);
            return;
        }
        double dx = owner.getX() - z.cx(), dz = owner.getZ() - z.cz();
        if (!dim(level).equals(z.dim()) || dx * dx + dz * dz > 64.0 * 64.0) {
            dismiss(owner, false);                                           // the domain needs its keeper near it
            return;
        }
        List<GaiaWarCheck.Relation> inside = new ArrayList<>();
        List<ServerPlayer> visitors = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || !z.contains(z.dim(), p.getX(), p.getY(), p.getZ())) continue;
            GaiaWarCheck.Relation rel = relation(owner.getUUID(), p.getUUID());
            inside.add(rel);
            visitors.add(p);
            if (GaiaWarCheck.blesses(rel)) {
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false));
            } else if (GaiaWarCheck.burdens(rel)) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false));
            }
        }
        boolean war = GaiaWarCheck.warFooting(inside);
        int flight = IN_FLIGHT.getOrDefault(owner.getUUID(), 0);
        double price = EliteSkillsConfig.cost(GaiaCost.sane(GaiaCost.upkeepPerSecond(z.radius(), flight, false, war)));
        if (!EliteUtil.tryDrain(owner, price)) {
            EliteBridge.actionbar(owner, SkillMsg.t("gaia.starved", "Your magicules cannot hold the domain: it fades.").withStyle(ChatFormatting.RED));
            dismiss(owner, true);
            return;
        }
        if (z.contains(z.dim(), owner.getX(), owner.getY(), owner.getZ())) {
            GaiaBloomLedger ledger = ledger(instance, level);
            ledger.earn(1);
            save(instance, ledger);
        }
        if (owner.tickCount % 100 < 20) {
            EliteBridge.ring(level, new Vec3(z.cx(), z.cy() + 0.1, z.cz()), Math.min(z.radius(), 20.0), 0x5E9A3A);
            EliteBridge.burst(level, new Vec3(z.cx(), z.cy() + 1.0, z.cz()), false, 6, z.radius() * 0.3);
        }
        instance.markDirty();
    }

    static GaiaBloomLedger ledger(ManasSkillInstance instance, Level level) {
        return GaiaBloomLedger.fromInts(instance.getOrCreateTag().getIntArray(LEDGER), EliteUtil.day(level));
    }

    static void save(ManasSkillInstance instance, GaiaBloomLedger ledger) { instance.getOrCreateTag().putIntArray(LEDGER, ledger.toInts()); }

    // ================================================================== the pillars
    static boolean pillars(ServerPlayer owner, boolean mastered, double mastery) {
        ServerLevel level = owner.serverLevel();
        Vec3 look = owner.getViewVector(1.0F);
        int count = GaiaPillars.count(mastered);
        double[][] centres = GaiaPillars.centres(owner.getX(), owner.getZ(), look.x, look.z, count);
        boolean launch = BalanceLaw.beginControl(owner);
        UUID id = owner.getUUID();
        IN_FLIGHT.merge(id, count, Integer::sum);
        EliteBridge.startZone(owner, GaiaPillars.STAGGER_TICKS, count, (o, lvl, i) -> {
            IN_FLIGHT.computeIfPresent(id, (k, v) -> v <= 1 ? null : v - 1);
            double[] c = centres[i];
            double y = groundY(lvl, c[0], owner.getY() + 1.0, c[1]);
            Vec3 base = new Vec3(c[0], y, c[1]);
            for (LivingEntity t : EliteBridge.foes(o, base.add(0, 1.0, 0), GaiaPillars.RADIUS + 0.5)) {
                EliteBridge.hit(o, t, GaiaPillars.damage(i, count, mastered) * EliteSkillsConfig.damageScale(), mastery, false);
                if (launch) {
                    t.setDeltaMovement(t.getDeltaMovement().multiply(0.3, 0.0, 0.3).add(0, GaiaPillars.LAUNCH, 0));
                    t.hurtMarked = true;
                }
            }
            EliteBridge.shape(lvl, VfxShape.EARTH_RISE, base, base.add(0, 4, 0), 0xFF8A6A3A, 16, 0.9f);
            EliteBridge.burst(lvl, base.add(0, 1, 0), false, 6, 0.6);
            EliteBridge.sound(lvl, base, SoundEvents.DRIPSTONE_BLOCK_BREAK, 1.0f, 0.6f);
        });
        EliteBridge.actionbar(owner, SkillMsg.t("gaia.pillars", "The earth rises in %s pillars.", count).withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** The surface at (x, z) nearest below {@code fromY}: walks down through air for at most eight blocks. */
    static double groundY(ServerLevel level, double x, double fromY, double z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(Math.floor(x), Math.floor(fromY), Math.floor(z));
        for (int i = 0; i < 8; i++) {
            if (!level.getBlockState(p.below()).isAir()) return p.getY();
            p.move(0, -1, 0);
        }
        return p.getY();
    }

    // ================================================================== the bloom
    /** Grows a vein of ore in natural stone near the owner, inside the domain. */
    static boolean bloom(ServerPlayer owner, ManasSkillInstance instance, boolean mastered) {
        GaiaZone z = GaiaZone.of(owner.getUUID());
        ServerLevel level = owner.serverLevel();
        long now = level.getGameTime();
        if (z == null || !z.active(now) || !z.contains(dim(level), owner.getX(), owner.getY(), owner.getZ())) {
            EliteBridge.fail(owner, SkillMsg.t("gaia.no_domain", "Raise your domain and stand in it to bloom the earth."));
            return false;
        }
        if (!EliteSkillsConfig.get(EliteSkillsConfig.GAIA_BLOOM)) {
            EliteBridge.fail(owner, SkillMsg.t("gaia.bloom_off", "Blooming is switched off on this server."));
            return false;
        }
        int cap = EliteSkillsConfig.get(EliteSkillsConfig.GAIA_DAILY_VEINS);
        GaiaBloomLedger ledger = ledger(instance, level);
        long today = EliteUtil.day(level);
        if (!ledger.canBloom(today, cap)) {
            if (ledger.remainingToday(today, cap) <= 0) EliteBridge.fail(owner, SkillMsg.t("gaia.cap", "The earth has given all it will today (%s veins).", cap));
            else EliteBridge.fail(owner, SkillMsg.t("gaia.points", "The domain has not gathered enough life yet (%s of %s).", ledger.points(), GaiaBloomLedger.VEIN_COST));
            save(instance, ledger);
            return false;
        }
        RandomSource rnd = owner.getRandom();
        int level0 = mastered ? 1 : 0;
        int reach = (int) Math.min(z.radius(), 14.0);
        BlockPos base = null;
        for (int tries = 0; tries < 48 && base == null; tries++) {
            int bx = (int) Math.floor(owner.getX()) + rnd.nextInt(reach * 2 + 1) - reach, bz = (int) Math.floor(owner.getZ()) + rnd.nextInt(reach * 2 + 1) - reach;
            for (int by = (int) owner.getY() + 3; by >= (int) owner.getY() - 18 && by > level.getMinBuildHeight(); by--) {
                BlockPos p = new BlockPos(bx, by, bz);
                if (z.contains(z.dim(), bx + 0.5, by + 0.5, bz + 0.5) && isStone(level.getBlockState(p)) && level.mayInteract(owner, p)) { base = p; break; }
            }
        }
        if (base == null) {
            EliteBridge.fail(owner, SkillMsg.t("gaia.no_stone", "There is no natural stone in reach to bloom."));
            return false;
        }
        VeinBloom.Ore ore = VeinBloom.pick(rnd.nextDouble(), level0);
        int size = VeinBloom.size(rnd.nextDouble(), level0);
        int placed = 0;
        for (int[] off : VeinBloom.shape(rnd.nextLong(), size)) {
            BlockPos p = base.offset(off[0], off[1], off[2]);
            if (!z.contains(z.dim(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) || !isStone(level.getBlockState(p)) || !level.mayInteract(owner, p)) continue;
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(ore.blockFor(VeinBloom.deep(p.getY()))));
            if (block.defaultBlockState().isAir()) continue;
            level.setBlock(p, block.defaultBlockState(), 3);
            placed++;
        }
        if (placed <= 0) {
            EliteBridge.fail(owner, SkillMsg.t("gaia.no_stone", "There is no natural stone in reach to bloom."));
            return false;
        }
        ledger.bloom(today, cap);
        save(instance, ledger);
        Vec3 at = Vec3.atCenterOf(base);
        EliteBridge.shape(level, VfxShape.TREE_ROOTS, at, at.add(0, 1, 0), 0xFF6AAA3A, 30, 1.0f);
        EliteBridge.burst(level, at, false, 10, 0.8);
        EliteBridge.sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
        EliteBridge.actionbar(owner, SkillMsg.t("gaia.bloomed", "A vein blooms in the earth: %s blocks of %s (%s veins left today).", placed,
                ore.name().toLowerCase(java.util.Locale.ROOT), ledger.remainingToday(today, cap)).withStyle(ChatFormatting.GREEN));
        instance.markDirty();
        return true;
    }

    static boolean isStone(BlockState s) { return s.is(BlockTags.STONE_ORE_REPLACEABLES) || s.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES); }

    /** A kill inside the owner's own domain feeds the ledger. */
    static void onKillInDomain(ManasSkillInstance instance, ServerPlayer owner, LivingEntity victim) {
        GaiaZone z = GaiaZone.of(owner.getUUID());
        if (z == null || !z.active(owner.level().getGameTime()) || !z.contains(dim(owner.level()), victim.getX(), victim.getY(), victim.getZ())) return;
        GaiaBloomLedger ledger = ledger(instance, owner.level());
        ledger.earn(5);
        save(instance, ledger);
        instance.markDirty();
    }

    static void forget(UUID owner) { IN_FLIGHT.remove(owner); }

    static void clearAll() { IN_FLIGHT.clear(); }
}
