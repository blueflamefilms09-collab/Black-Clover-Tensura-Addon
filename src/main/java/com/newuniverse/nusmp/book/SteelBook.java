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

/** Steel Magic. */
public class SteelBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.starter("steel_shot", "Blazing Steel Shot", SteelBook::shot),
            BookPage.signature("returning_lance", "Returning Lance", SteelBook::lance),
            BookPage.mid("obsidian_shot", "Steel Cannon", TensuraShots.shot(TensuraShots.Shot.OBSIDIAN_SHOT, 12, 1.8f, 1.0f, 0)));

    public SteelBook() { super(MagicType.STEEL, 0xFFB8C2CC); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Three steel bolts in a tight spread. */
    static boolean shot(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f), side = new Vec3(-dir.z, 0, dir.x).normalize();
        b.castCircle(p, 0.6f);
        for (int k = -1; k <= 1; k++) {
            Vec3 d = dir.add(side.scale(k * 0.06)).normalize();
            b.vfx(p, VfxShape.THREAD_LINE, start, start.add(d.scale(24)), 8, 0.6f);
            SpellRuntime.bolt(p, start, d.scale(2.0), 0.4, 12, false, null, (bolt, t) -> b.hurt(i, p, t, mode, 4f),
                    (bolt, at) -> b.impact(p, at, 0.3f));
        }
        return true;
    }

    /** A lance that pierces 16 blocks out, then flies back through everything again. */
    static boolean lance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.THREAD_LINE, start, start.add(dir.scale(16)), 10, 1.4f);
        SpellRuntime.bolt(p, start, dir.scale(2.0), 0.8, 8, true, null, (bolt, t) -> b.hurt(i, p, t, mode, 14f), (bolt, at) -> {
            b.vfx(p, VfxShape.THREAD_LINE, at, p.getEyePosition(), 10, 1.4f);
            SpellRuntime.bolt(p, at, p.getEyePosition().subtract(at).normalize().scale(2.0), 0.8,
                    (int) (at.distanceTo(p.getEyePosition()) / 2) + 1, true, null, (bolt2, t) -> b.hurt(i, p, t, mode, 14f), null);
        });
        return true;
    }
}
