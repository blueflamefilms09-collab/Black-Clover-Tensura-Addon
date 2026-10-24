package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiPredicate;

/**
 * True Lightning: a real vanilla {@code LightningBolt} entity at the impact. The rules are {@link TrueWorldRules}. Bounded: at most ONE bolt per cast (the caller strikes once), at most
 * {@value TrueWorldRules#LIGHTNING_PER_SECOND} per second on the whole server, never in the Labyrinth or a world the Dungeon Guard holds, only into a loaded chunk, only where the caster may
 * interact; no chunk is loaded. The bolt entity lives a few ticks and removes itself.
 *
 * <p>Safety for the caster and everyone else: by default the bolt is the visual-only kind (the flash, the thunder and the vanilla bolt entity itself, but no vanilla damage, no fire and no
 * mob changes: the rune's own attack does the damage and credits the caster). The full vanilla bolt is used only with {@code trueMagic.lightningRealBolt}, when the game rule
 * {@code doFireTick} and {@code trueMagic.placeFire} allow fire, and when nobody who is not a foe of the caster (the caster, allies, pets, villagers) stands within
 * {@value TrueWorldRules#SAFE_RADIUS} blocks.
 */
public final class TrueLightning {
    private TrueLightning() {}

    /** {second, bolts used in it}: the server-wide allowance. */
    private static final long[] WINDOW = {Long.MIN_VALUE, 0};

    /** Strikes the impact; true when a bolt was summoned. */
    public static boolean strike(ServerLevel level, ServerPlayer caster, Vec3 at, BiPredicate<ServerLevel, BlockPos> may, long now) {
        if (!ManaMethodConfig.lightningBolt() || !TrueBlocks.levelAllows(level)) return false;
        BlockPos pos = BlockPos.containing(at);
        if (!level.isLoaded(pos) || !may.test(level, pos)) return false;
        if (!TrueWorldRules.takeBolt(level.getGameTime(), WINDOW)) return false;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return false;
        bolt.moveTo(at.x, at.y, at.z);
        boolean real = ManaMethodConfig.lightningRealBolt()
                && TrueWorldRules.realBoltAllowed(true, level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK), ManaMethodConfig.trueFire(), bystanders(level, caster, at));
        bolt.setVisualOnly(!real);
        if (real) bolt.setCause(caster);
        level.addFreshEntity(bolt);
        return true;
    }

    /** How many living things that are not the caster's foes (the caster included) stand within the safe radius: ONE small query, only asked when the full bolt is switched on. */
    private static int bystanders(ServerLevel level, ServerPlayer caster, Vec3 at) {
        double r = TrueWorldRules.SAFE_RADIUS;
        int n = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r), e -> e.isAlive() && e.distanceToSqr(at) <= r * r))
            if (TrueWorldRules.bystander(Foes.isFoe(caster, e))) n++;
        return n;
    }

    /** Test hook: forgets the server-wide allowance. */
    static void resetForTest() {
        WINDOW[0] = Long.MIN_VALUE;
        WINDOW[1] = 0;
    }
}
