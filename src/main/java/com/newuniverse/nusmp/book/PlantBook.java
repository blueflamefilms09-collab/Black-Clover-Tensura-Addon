package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Plant Magic (1.93.0 rebuild; first release 0.3x). The first four pages keep their ids, order and mastery bits (guidepost, hundred_flowers, yggdrasil, briar_prison: the last two are the borrowed
 * captain spells and are untouched); guidepost and hundred_flowers are remade ({@link PlantArts}) with the magic's own effects, and six pages are appended:
 * seed_bloom, healing_bloom, flower_sprite, ivy_barrier, blossom_dress (the Blossom armour, Aura.PLANT) and petal_storm. The book also answers the generic casting circle and impact with the
 * magic's own shapes (PLANT_FX2 / PLANT_FX3) and runs the passive of the armour.
 */
public class PlantBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.mid("guidepost", "Magic Flower Guidepost", PlantArts::guidepost),
            BookPage.signature("hundred_flowers", "Dream World of a Hundred Flowers", PlantArts::dreamWorld),
            // 0.34: captains' character spells (William Vangeance, Charlotte Roselei)
            BookPage.signature("yggdrasil", "World Tree Magic: Yggdrasil", WikiSpells::yggdrasil).withCooldown(1200),
            BookPage.zone("briar_prison", "Briar Magic: Briar Prison", WikiSpells::briarPrison),
            // 1.93.0: appended
            BookPage.starter("seed_bloom", "Plant Magic: Seed Bloom", PlantArts::seedBloom),
            BookPage.mid("healing_bloom", "Plant Magic: Healing Bloom", PlantArts::healingBloom),
            BookPage.signature("flower_sprite", "Plant Magic: Flower Sprite", PlantArts::sprite).withCooldown(1000).withAnim("signature"),
            BookPage.mid("ivy_barrier", "Plant Magic: Ivy Barrier", PlantArts::ivy),
            BookPage.mid("blossom_dress", "Plant Magic: Blossom Dress", PlantArts::dress).withCooldown(300),
            BookPage.zone("petal_storm", "Plant Magic: Petal Storm", PlantArts::storm));

    public PlantBook() { super(MagicType.PLANT, 0xFF6CFF7A); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** The cast circle: a small version of the magic's own ground sigil under the caster. */
    @Override
    public void castCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.PLANT_FX2, p, p.position().add(0, 0.05, 0), color, 22, 1.1f * Math.max(0.6f, Math.min(1.6f, power)));
    }

    @Override
    public void impact(ServerPlayer p, Vec3 at, float power) {
        VfxSpawn.send(p.serverLevel(), VfxShape.PLANT_FX3, at, at.add(0, 1, 0), color, 22, Math.max(0.3f, Math.min(1.2f, power)));
    }

    /** The Blossom armour: while it runs the blows are softened and each one mends the wearer a little. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        TreeKit.armourPassive(MagicType.PLANT, i, owner, source, amount);
        return true;
    }
}
