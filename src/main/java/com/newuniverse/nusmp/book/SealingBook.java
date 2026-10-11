package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
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

/** Sealing Magic. */
public class SealingBook extends GrimoireBook {
    private final List<BookPage> pages = com.newuniverse.nusmp.book.ext.Ext.join(List.of(
            BookPage.zone("seal", "Seal", SealingBook::seal).withAnim("seal_crush"),
            BookPage.signature("grand_seal", "Grand Seal", SealingBook::grandSeal).withAnim("seal_crush"),
            // 0.34: wiki spells, appended
            BookPage.mid("sealing_chains", "Seal Magic: Sealing Chains", WikiSpells::sealingChains).withAnim("seal_crush"),
            BookPage.signature("trinity_seal", "Trinity Seal Magic", WikiSpells::trinitySeal).withAnim("seal_crush"),
            BookPage.zone("seal_barrier", "Seal Magic: Barrier", WikiSpells::sealBarrier)), com.newuniverse.nusmp.book.ext.SealExt.pages());

    public SealingBook() { super(MagicType.SEALING, 0xFFE8C26A); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Seals a mage's grimoire for 5 s (bosses: 1 s); on monsters it seals the wound shut with a sealing cut. */
    static void apply(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, LivingEntity t, int mode, int ticks) {
        int dur = BalanceLaw.isBoss(t) ? 20 : ticks;
        if (t instanceof Player) t.getPersistentData().putLong("nusmp_sealed_until", t.level().getGameTime() + dur);
        b.hurt(i, p, t, mode, 6f);
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 1));
        b.vfx(p, VfxShape.SEAL_CHAINS, t.position().add(0, t.getBbHeight() / 2, 0), t.position(), dur, Math.max(0.8f, t.getBbHeight() / 1.8f));   // 0.34
    }

    static boolean seal(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 16);
        if (t == null) { fail(p, "Nothing to seal."); return false; }
        b.castCircle(p, 0.7f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SEAL_ORBIT, p, p.position().add(0, 1.35, 0), 0xFFFFD580, 18, 0.8f);
        apply(b, i, p, t, mode, 100);
        return true;
    }

    static boolean grandSeal(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        List<LivingEntity> ts = around(p, p.position(), 6);
        if (ts.isEmpty()) { fail(p, "Nothing to seal."); return false; }
        b.castCircle(p, 1.4f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SEAL_ORBIT, p, p.position().add(0, 1.35, 0), 0xFFFFD580, 18, 1.0f);
        for (LivingEntity t : ts) apply(b, i, p, t, mode, 80);
        return true;
    }
}
