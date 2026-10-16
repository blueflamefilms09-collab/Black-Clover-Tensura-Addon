package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.magic.Element;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Spatial Magic (Finral / Langris). */
public class SpatialBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.starter("domination", "Sacred Mana Domination", SpatialBook::domination),
            BookPage.mid("fallen_angel_gate", "Fallen Angel Gate", SpatialBook::gate).withCooldown(0),
            BookPage.zone("space_cut", "Spatial Cut", TensuraShots.shot(TensuraShots.Shot.SPACE_CUT, 12, 2.2f, 0.5f, 0)),
            // 0.34: wiki spells, appended
            BookPage.zone("unopening_red_room", "Unopening Red Room", WikiSpells::redRoom),
            BookPage.mid("myriad_black", "Myriad Black", WikiSpells::myriadBlack),
            BookPage.signature("door_of_fate", "Door of Fate", WikiSpells::doorOfFate).withCooldown(2400));

    public SpatialBook() { super(MagicType.SPATIAL, 0xFFB088FF); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.SPACE_ELEMENTAL; }

    /** Erase the nearest enemy projectile within 8 blocks, or else pull the target 4 blocks. Never both. */
    static boolean domination(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Projectile best = null;
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(8))) {
            if (pr.getOwner() == p) continue;
            if (best == null || pr.distanceToSqr(p) < best.distanceToSqr(p)) best = pr;
        }
        b.castCircle(p, 0.6f);
        if (best != null) {
            b.vfx(p, VfxShape.SPATIAL_RIFT, best.position(), best.position().add(0, 1, 0), 14, 0.5f);
            best.discard();
            return true;
        }
        LivingEntity t = target(p, 12);
        if (t == null) { fail(p, "No projectile or target to grasp."); return false; }
        Vec3 dest = t.position().add(p.position().subtract(t.position()).normalize().scale(4));
        b.vfx(p, VfxShape.SPATIAL_RIFT, t.position(), t.position().add(0, 1, 0), 14, 0.6f);
        t.teleportTo(dest.x, dest.y, dest.z);
        b.vfx(p, VfxShape.SPATIAL_RIFT, dest, dest.add(0, 1, 0), 14, 0.6f);
        return true;
    }

    /**
     * First cast opens gate A, second opens gate B; both live 20 s. Then a 45 s cooldown. 1.43.0: each gate faces the caster as they
     * stood when casting (its facing is kept so a crossing keeps its momentum), and the two gates share one visual seed so the client
     * can draw the thread between them.
     */
    static boolean gate(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        var tag = i.getOrCreateTag();
        long now = p.level().getGameTime();
        Vec3 at = aim(p, 24).add(0, 0.1, 0);
        double face = PortalMomentum.facing(at.x, at.z, p.getX(), p.getZ());
        if (now > tag.getLong("GateAUntil")) {
            tag.putDouble("GAX", at.x); tag.putDouble("GAY", at.y); tag.putDouble("GAZ", at.z);
            tag.putDouble("GAFace", face);
            long pair = p.serverLevel().random.nextLong();
            tag.putLong("GatePair", pair);
            tag.putLong("GateAUntil", now + 400);
            gateVisual(b, p, at, pair);                                                 // 0.34: the gate itself
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("First gate open. Cast again to open the second."), true);
            return true;
        }
        Vec3 a = new Vec3(tag.getDouble("GAX"), tag.getDouble("GAY"), tag.getDouble("GAZ"));
        double faceA = tag.contains("GAFace") ? tag.getDouble("GAFace") : Double.NaN;      // old saves: no rotation
        long pair = tag.getLong("GatePair");
        Vec3 bb = at;
        tag.putLong("GateAUntil", 0);
        i.setCoolDown(45, mode);   // seconds
        gateVisual(b, p, bb, pair);
        ServerLevel level = p.serverLevel();
        SpellRuntime.zone(level, 400, 1, age -> {
            hop(level, a, faceA, bb, face);
            hop(level, bb, face, a, faceA);
        });
        return true;
    }

    /** One gate's visual: the same payload {@code b.vfx} sends, but with the pair's shared seed (the client links gates by it). */
    private static void gateVisual(GrimoireBook b, ServerPlayer p, Vec3 at, long pairSeed) {
        VfxSpawn.send(p.serverLevel(), new VfxPayload(VfxShape.SPACE_PORTAL.ordinal(), at, p.position(), b.color, 400, 1f, -1, pairSeed));
    }

    private static void hop(ServerLevel level, Vec3 from, Vec3 to) { hop(level, from, Double.NaN, to, Double.NaN); }

    /**
     * Sends everything standing in gate {@code from} out of gate {@code to}: living things, item entities and projectiles. When both facings
     * are known the velocity is turned by the difference of the gates (see {@link PortalMomentum}) and the entity comes out in front of the
     * exit; with NaN facings it is the old plain teleport. The 30 tick re-entry guard is unchanged.
     */
    private static void hop(ServerLevel level, Vec3 from, double faceFrom, Vec3 to, double faceTo) {
        long now = level.getGameTime();
        boolean turn = !Double.isNaN(faceFrom) && !Double.isNaN(faceTo);
        for (Entity e : level.getEntities((Entity) null, new AABB(from, from).inflate(0.7, 1, 0.7))) {
            if (!(e instanceof LivingEntity) && !(e instanceof net.minecraft.world.entity.item.ItemEntity) && !(e instanceof Projectile)) continue;
            if (e.getPersistentData().getLong("nusmp_gate_cd") > now) continue;
            e.getPersistentData().putLong("nusmp_gate_cd", now + 30);
            level.playSound(null, from.x, e.getY(), from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 0.8f);
            if (!turn) { e.teleportTo(to.x, to.y, to.z); }
            else {
                double t = PortalMomentum.turn(faceFrom, faceTo);
                int side = PortalMomentum.side(from.x, from.z, faceFrom, e.getX(), e.getZ());
                Vec3 v = e.getDeltaMovement();
                double[] nv = PortalMomentum.rotate(v.x, v.z, t);
                double[] at = PortalMomentum.exitPoint(from.x, from.z, faceFrom, to.x, to.z, faceTo, e.getX(), e.getZ(), side);
                double y = to.y + Math.max(0, e.getY() - from.y);
                float yaw = e.getYRot() + (float) Math.toDegrees(t);
                if (e instanceof ServerPlayer sp) sp.teleportTo(level, at[0], y, at[1], yaw, sp.getXRot());
                else { e.teleportTo(at[0], y, at[1]); e.setYRot(yaw); e.setYHeadRot(yaw); }
                e.setDeltaMovement(nv[0], v.y, nv[1]);
                e.hurtMarked = true;                               // tell the client its new velocity
                if (e instanceof Projectile) e.hasImpulse = true;
            }
            level.playSound(null, to.x, e.getY(), to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.2f);
        }
    }
}
