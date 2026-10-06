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

/** Plant Magic. */
public class PlantBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.mid("guidepost", "Magic Flower Guidepost", PlantBook::guidepost),
            BookPage.signature("hundred_flowers", "Dream World of a Hundred Flowers", PlantBook::dreamWorld),
            // 0.34: captains' character spells (William Vangeance, Charlotte Roselei)
            BookPage.signature("yggdrasil", "World Tree Magic: Yggdrasil", WikiSpells::yggdrasil).withCooldown(1200),
            BookPage.zone("briar_prison", "Briar Magic: Briar Prison", WikiSpells::briarPrison));

    public PlantBook() { super(MagicType.PLANT, 0xFF6CFF7A); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** A heal beam on the player you look at (or yourself): 20% max health, healing decay applies. */
    static boolean guidepost(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 16);
        LivingEntity heal = t instanceof Player ? t : p;   // look at a player to heal them, otherwise yourself
        BalanceLaw.heal(heal, heal.getMaxHealth() * 0.2f);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.THREAD_LINE, p.getEyePosition(), heal.getBoundingBox().getCenter(), 14, 0.8f);
        b.vfx(p, VfxShape.ELF_CIRCLE, heal.position(), heal.position().add(0, 1, 0), 24, 0.6f);
        return true;
    }

    /** A 6-block field for 8 s: you and nearby players heal, enemies are slowed and cut by thorns. */
    static boolean dreamWorld(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = p.position();
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.ELF_CIRCLE, c, c.add(0, 1, 0), 160, 6 / 1.6f);
        SpellRuntime.zone(p.serverLevel(), 160, 20, age -> {
            for (Player ally : p.serverLevel().getEntitiesOfClass(Player.class, new AABB(c, c).inflate(6))) {
                if (ally == p || ally.isAlliedTo(p)) BalanceLaw.heal(ally, 2f);
            }
            for (LivingEntity t : around(p, c, 6)) {
                if (t instanceof Player pl && pl.isAlliedTo(p)) continue;
                b.hurt(i, p, t, mode, 2f);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            }
        });
        return true;
    }
}
