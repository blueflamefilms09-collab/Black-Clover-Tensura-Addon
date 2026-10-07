package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Body Magic: compressed flesh, reinforced bone, bullet-fast strikes and healing that knits tissue back together. */
public class BodyBook extends GrimoireBook {
    static final int COLOR = 0xFFFF8A4A;

    private final List<BookPage> pages = List.of(
            BookPage.starter("body_strike", "Titan Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.BODY_FX1, null, ElementBook.NONE)),
            // 0.54: the real book, appended after the first page
            BookPage.starter("bullet_fist", "Bullet Fist", BodyArts::bulletFist).withAnim("slam"),
            BookPage.starter("rapid_barrage", "Rapid Barrage", BodyArts::rapidBarrage),
            BookPage.mid("body_compression", "Body Compression", BodyArts::compression),
            BookPage.mid("tendon_sever", "Tendon Sever", BodyArts::tendonSever),
            BookPage.mid("hardened_frame", "Hardened Frame", BodyArts::hardenedFrame),
            BookPage.mid("tissue_mend", "Tissue Mend", BodyArts::tissueMend),
            BookPage.mid("leg_spring", "Leg Spring", BodyArts::legSpring).withAnim("up"),
            BookPage.zone("quake_stomp", "Quake Stomp", BodyArts::quakeStomp).withAnim("slam"),
            BookPage.signature("titan_form", "Titan Form", BodyArts::titanForm).withAnim("signature"),
            BookPage.daily("flesh_regrowth", "Flesh Regrowth", BodyArts::regrowth).withCooldown(24000));

    /** Set while Titan Form shakes the foes beside a target, so that shake cannot start another one. */
    private static boolean shaking;

    public BodyBook() { super(MagicType.BODY, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Titan Form: melee blows hit harder and the foes beside the target are shaken. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (shaking || source.getDirectEntity() != owner || !(owner instanceof ServerPlayer p)
                || owner.level().getGameTime() >= i.getOrCreateTag().getLong(BodyArts.TITAN)) return true;
        amount.set(amount.get() + BalanceLaw.damage(target, 6f, masteryFrac(i)));
        shaking = true;
        try {
            int mode = i.getOrCreateTag().getInt("LastMode");
            for (LivingEntity o : around(p, target.getBoundingBox().getCenter(), 2.5)) {
                if (o == target) continue;
                hurt(i, p, o, mode, 3f);
                BodyArts.shove(target.position(), o, 0.6);
            }
        } finally { shaking = false; }
        return true;
    }

    /** Hardened Frame: physical blows are cut by 30%, magic by 15%. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        if (owner.level().getGameTime() >= i.getOrCreateTag().getLong(BodyArts.FRAME)) return true;
        amount.set(amount.get() * (AntiMagic.isMagic(source) ? 0.85f : 0.7f));
        return true;
    }
}
