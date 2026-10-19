package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.93.0 nature-magic rebuild: the grimoire of Tree, Vine, Poison Plant, Mud and Mucus Magic (these five are wiki attributes: {@link WikiBooks#attribute} hands out this class through
 * {@link TreeRebuild}; Plant Magic has its own {@link PlantBook}). It is an {@link ElementBook} (its pages, the shared bolt / line / dash shapes, the Empower passives are the family's), with:
 * <ul>
 *   <li>its own effects: the three generic wiki shapes of the family (WIKI_MAGIC_CAST / FIELD / BURST) are answered by the magic's own layer ({@code TREE_FX1..3}, {@code VINE_FX1..3} ...), and
 *       the cast circle of every page is a small version of the magic's own ground sigil;</li>
 *   <li>the passive of its temporary armour: while the armour of a cast runs, {@link TreeKit#armourPassive} softens the blows and answers the attacker in the magic's way.</li>
 * </ul>
 */
public class TreeElementBook extends ElementBook {
    private final VfxShape cast, field, burst;

    public TreeElementBook(MagicType magic, int color, ResourceKey<DamageType> damage, List<BookPage> pages) {
        super(magic, color, damage, pages);
        this.cast = shape(magic, 1);
        this.field = shape(magic, 2);
        this.burst = shape(magic, 3);
    }

    /** The magic's own shape number n (1 cast, 2 zone, 3 impact), or the generic wiki one for a magic that has none. */
    public static VfxShape shape(MagicType magic, int n) {
        String name = magic.name() + "_FX" + n;
        try {
            return VfxShape.valueOf(name);
        } catch (IllegalArgumentException e) {
            return n == 1 ? VfxShape.WIKI_MAGIC_CAST : n == 2 ? VfxShape.WIKI_MAGIC_FIELD : VfxShape.WIKI_MAGIC_BURST;
        }
    }

    @Override
    public void vfx(ServerPlayer player, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxShape own = shape == VfxShape.WIKI_MAGIC_CAST ? cast : shape == VfxShape.WIKI_MAGIC_FIELD ? field : shape == VfxShape.WIKI_MAGIC_BURST ? burst : shape;
        super.vfx(player, own, from, to, ticks, power);
    }

    /** The cast circle: a small version of the magic's own ground sigil under the caster. */
    @Override
    public void castCircle(ServerPlayer player, float power) {
        VfxSpawn.sendFollowing(player.serverLevel(), field, player, player.position().add(0, 0.05, 0), color, 22, 1.1f * Math.max(0.6f, Math.min(1.6f, power)));
    }

    @Override
    public void impact(ServerPlayer player, Vec3 at, float power) {
        VfxSpawn.send(player.serverLevel(), burst, at, at.add(0, 1, 0), color, 22, Math.max(0.3f, Math.min(1.2f, power)));
    }

    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        TreeKit.armourPassive(magic, i, owner, source, amount);
        return true;
    }
}
