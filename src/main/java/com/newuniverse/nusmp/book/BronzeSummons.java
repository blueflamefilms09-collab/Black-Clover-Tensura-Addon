package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Bronze Magic: the summoned things of the book. "Bronze Guardian" casts a bronze statue (PropKind.BRONZE_1) that follows and defends the caster for
 * 25 s, with up to three poison lizards (PropKind.BRONZE_2) scuttling at its feet; "Bronze Armour" grows the plate armour over the caster
 * (Aura.BRONZE; style 1, the cast-bronze statue sheen, once the grimoire is well mastered or in a devil state) with Resistance II and thorns for
 * 20 s. The movement and the strikes of the props live in prop.BronzeProps; the softening of blows and the thorns in BronzeArts.guard.
 */
public final class BronzeSummons {
    private BronzeSummons() {}

    public static final int GUARDIAN_TICKS = 500, ARMOUR_TICKS = 400;

    /** Ticks of a summon, longer for a mighty caster (up to +50 %). */
    private static int span(int base, ServerPlayer p) { return (int) (base * Math.min(1.5, 1 + 0.04 * (BalanceLaw.epScale(p) - 1))); }

    /** Ends the caster's earlier summons of this kind (a new cast replaces them). */
    private static void dismiss(ServerPlayer p, PropKind kind) {
        for (MagicPropEntity e : p.serverLevel().getEntitiesOfClass(MagicPropEntity.class, p.getBoundingBox().inflate(96),
                x -> x.kind() == kind && p.getUUID().equals(x.ownerId())))
            e.expire();
    }

    /** Bronze Guardian: a cast-bronze statue rises at the caster's side (it follows, cuts the nearest foe, turns projectiles with its shield). */
    static boolean guardian(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        double ep = BalanceLaw.epScale(p);
        int ticks = span(GUARDIAN_TICKS, p);
        float s = 0.9f + 0.02f * (float) Math.min(8.0, ep - 1.0);
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 side = new Vec3(-look.z, 0, look.x);
        dismiss(p, PropKind.BRONZE_1);
        dismiss(p, PropKind.BRONZE_2);
        Vec3 at = p.position().add(look.scale(1.8)).add(side.scale(1.0));
        MagicPropEntity statue = MagicProps.spawn(level, PropKind.BRONZE_1, at, p.getYRot() + 180f, s, ticks, 0, p);
        if (statue == null) { GrimoireBook.fail(p, "The bronze would not take form."); return false; }
        int n = ep >= 7 ? 3 : ep >= 4 ? 2 : 1;
        for (int k = 0; k < n; k++) {
            Vec3 lat = p.position().add(look.scale(1.2)).add(side.scale(-0.9 - 0.7 * k));
            MagicProps.spawn(level, PropKind.BRONZE_2, lat, p.getYRot(), 0.8f + 0.02f * (float) Math.min(8.0, ep - 1.0), ticks, 0, p);
        }
        b.castCircle(p, 1.2f);
        VfxSpawn.send(level, VfxShape.BRONZE_FX2, at, at.add(0, 1, 0), BronzeBook.COLOR, 30, 2.2f);
        return true;
    }

    /** Bronze Armour: plates and rivets forge over the caster; Resistance II, and blows are softened and thrown back at the attacker as thorns. */
    static boolean armour(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = span(ARMOUR_TICKS, p);
        int style = b.masteryFrac(i) >= 0.75 || GrimoireBook.inDevilState(p) ? 1 : 0;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1));
        BronzeArts.guard(p, ticks, 0.25f, 2f + 0.3f * (float) Math.min(8.0, BalanceLaw.epScale(p) - 1.0));
        b.castCircle(p, 1.5f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BRONZE_FX3, p, p.position().add(0, 1, 0), BronzeBook.COLOR, 40, 1.4f);
        return true;
    }
}
