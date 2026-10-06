package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.vfx.VfxShape;
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

    /** First cast opens gate A, second opens gate B; both live 20 s. Then a 45 s cooldown. */
    static boolean gate(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        var tag = i.getOrCreateTag();
        long now = p.level().getGameTime();
        Vec3 at = aim(p, 24).add(0, 0.1, 0);
        if (now > tag.getLong("GateAUntil")) {
            tag.putDouble("GAX", at.x); tag.putDouble("GAY", at.y); tag.putDouble("GAZ", at.z);
            tag.putLong("GateAUntil", now + 400);
            b.vfx(p, VfxShape.SPACE_PORTAL, at, p.position(), 400, 1f);           // 0.34: the gate itself
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("First gate open. Cast again to open the second."), true);
            return true;
        }
        Vec3 a = new Vec3(tag.getDouble("GAX"), tag.getDouble("GAY"), tag.getDouble("GAZ"));
        Vec3 bb = at;
        tag.putLong("GateAUntil", 0);
        i.setCoolDown(45, mode);   // seconds
        b.vfx(p, VfxShape.SPACE_PORTAL, bb, p.position(), 400, 1f);
        ServerLevel level = p.serverLevel();
        SpellRuntime.zone(level, 400, 1, age -> {
            hop(level, a, bb);
            hop(level, bb, a);
        });
        return true;
    }

    private static void hop(ServerLevel level, Vec3 from, Vec3 to) {
        long now = level.getGameTime();
        for (Entity e : level.getEntities((Entity) null, new AABB(from, from).inflate(0.7, 1, 0.7))) {
            if (!(e instanceof LivingEntity) && !(e instanceof net.minecraft.world.entity.item.ItemEntity)) continue;
            if (e.getPersistentData().getLong("nusmp_gate_cd") > now) continue;
            e.getPersistentData().putLong("nusmp_gate_cd", now + 30);
            e.teleportTo(to.x, to.y, to.z);
        }
    }
}
