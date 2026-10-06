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

/** Earth Magic. Gnome spirit. */
public class EarthBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.starter("spike_line", "Earth Spikes", EarthBook::spikes),
            BookPage.mid("wall", "Earth Wall", EarthBook::wall),
            BookPage.zone("mother_earth_split", "Mother Earth Split", EarthBook::split),
            BookPage.mid("boulder_shot", "Boulder Shot", TensuraShots.shot(TensuraShots.Shot.BOULDER_SHOT, 12, 1.4f, 1.4f, 0)));

    public EarthBook() { super(MagicType.EARTH, 0xFFB08850); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.EARTH_ELEMENTAL; }
    @Override public Element spiritElement() { return Element.EARTH; }
    @Override public String spiritName() { return "Gnome"; }

    static Vec3 flatDir(ServerPlayer p) { return p.getViewVector(1f).multiply(1, 0, 1).normalize(); }

    /** Spikes burst from the ground one after another along a line. */
    static boolean spikes(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 a = p.position(), dir = flatDir(p);
        int n = (int) (8 * size(i, p));
        b.vfx(p, VfxShape.FX_EARTH_SPIKES, a, a.add(dir.scale(n)), 0, 1f);
        for (int k = 1; k <= n; k++) {
            Vec3 pt = a.add(dir.scale(k));
            SpellRuntime.later(p.serverLevel(), k * 2, () -> {
                for (LivingEntity t : around(p, pt, 1.2)) {
                    b.hurt(i, p, t, mode, 9f);
                    t.setDeltaMovement(t.getDeltaMovement().add(0, 0.6, 0));
                    t.hurtMarked = true;
                }
            });
        }
        return true;
    }

    /** A 4-wide, 3-high stone wall in front of you for 8 s. */
    static boolean wall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = flatDir(p), side = new Vec3(-dir.z, 0, dir.x);
        Vec3 base = p.position().add(dir.scale(2.5));
        int placed = 0;
        for (int w = -2; w < 2; w++) for (int h = 0; h < 3; h++) {
            BlockPos pos = BlockPos.containing(base.add(side.scale(w + 0.5)).add(0, h, 0));
            if (SpellRuntime.tempBlock(p.serverLevel(), pos, Blocks.PACKED_MUD.defaultBlockState(), 160)) placed++;
        }
        if (placed == 0) { fail(p, "There's no room to raise a wall."); return false; }
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.EARTH_SPIKES, base.add(side.scale(-2)), base.add(side.scale(2)), 16, 0.8f);
        return true;
    }

    /** A 12-block fissure: damages and roots (shares the control lock; damage still lands if it's locked). */
    static boolean split(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 a = p.position(), end = a.add(flatDir(p).scale(12 * size(i, p)));
        boolean root = BalanceLaw.beginControl(p);
        for (LivingEntity t : along(p, a, end, 1.4)) {
            b.hurt(i, p, t, mode, 12f);
            if (root) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, 60), 6));
        }
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.EARTH_SPIKES, a, end, 26, 1.5f);
        return true;
    }
}
