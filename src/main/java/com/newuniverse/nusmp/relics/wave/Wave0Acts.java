package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Wave 0: the bodies of the abilities the cards add to the sixteen remade items. Each one runs on the server after the core has settled the
 * cost, the cooldown and the attunement. What each does is written in {@code docs/doc_wave0}; the numbers are in {@link Wave0Rules}.
 */
final class Wave0Acts {
    private Wave0Acts() {}

    static final String K_IVY = "nusmp_w0_ivy_until", K_CHROMA = "nusmp_w0_chroma_until", K_CHROMA_CHARM = "nusmp_w0_chroma_charm";
    static final String T_CHARGES = "w0_tc", T_DAY = "w0_td", T_STORM = "w0_ts";

    // ---------------------------------------------------------------- Quickling's Ivy
    /** Primes the blade: for ten seconds the next hit sprouts a tangle of ivy that restrains the target (Strength save DC 16, again every second). */
    static void sproutIvy(ActCtx c) {
        ServerPlayer p = c.player();
        p.getPersistentData().putLong(K_IVY, p.level().getGameTime() + 200);
        Fx.lendSkill(p, "tensura:sticky_steel_thread", 200);                                         // the ivy is a living thread: Tensura's binding thread, lent for ten seconds
        Vec3 at = p.position();
        Wave0Util.send(p.serverLevel(), VfxShape.TREE_ROOTS, at, at, 0xFF6CE05A, 24, 0.8f);
        Wave0Util.note(p, "Ivy stirs along the blade: your next hit ensnares.", ChatFormatting.GREEN);
    }

    // ---------------------------------------------------------------- the Magebane set
    /** The set bonus: Dispel Magic (3rd level) on the creature you aim at, or on everything around you; once until the next dawn. */
    static void dispelMagic(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        if (!RelicFlags.has(p, Wave0.F_MBS) || !RelicFlags.has(p, Wave0.F_MBSH)) {
            GrimoireBook.fail(p, "Dispel Magic needs the Magebane Longsword and the Magebane Shield, both attuned.");
            return;
        }
        if (!Wave0Util.takeDaily(p, "dispel", 1)) {
            GrimoireBook.fail(p, "The set has already dispelled once since dawn.");
            return;
        }
        LivingEntity aimed = GrimoireBook.target(p, 24);
        boolean onTarget = aimed != null && Wave0Util.targetable(p, aimed);
        Vec3 at = onTarget ? aimed.getBoundingBox().getCenter() : p.position().add(0, 1, 0);
        double r = onTarget ? 3.5 : 8.0;
        int stripped = 0;
        for (LivingEntity x : GrimoireBook.around(p, at, r)) {
            if (!Wave0Util.targetable(p, x)) continue;
            for (MobEffectInstance i : new ArrayList<>(x.getActiveEffects())) {
                if (!i.getEffect().value().isBeneficial()) continue;
                if (i.getAmplifier() <= 2 || p.getRandom().nextBoolean()) { x.removeEffect(i.getEffect()); stripped++; }   // a 3rd-level dispel is sure against the weak
            }
            Nullification.jamAll(x, 4, 0.3f);
        }
        Fx.lendSkills(p, 600, "tensura:magic_jamming", "tensura:chant_annulment");                    // Dispel Magic, merged from Tensura's jamming and chant annulment
        int bolts = SpellRuntime.dissolveBolts(sl, at, r, p);
        int wards = Nullification.shatterBarriers(sl, at, r, p);
        Wave0Util.send(sl, VfxShape.NIHILITY_ZONE, at, at, 0xFF2FB6AE, 60, (float) r);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2f, 0.7f);
        Wave0Util.note(p, "Dispel Magic: " + stripped + " effects, " + bolts + " spells and " + wards + " wards unravelled.", ChatFormatting.AQUA);
    }

    // ---------------------------------------------------------------- Prism Blade
    /** Blinding Flash, as the card has it: everything within 9 blocks saves against Constitution (DC 15) or is blinded, saving again every second. Once a day. */
    static void blindingRadiance(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        int ticks = BalanceLaw.beginControl(p) ? 600 : 100;
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, 9.0)) {
            Wave0Util.blind(t, ticks, 15);
            n++;
        }
        Vec3 at = p.position().add(0, 1.4, 0);
        Wave0Util.send(sl, VfxShape.LIGHT_FLARE, at, at, 0xFFFFFFFF, 30, 4.0f);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2f, 1.5f);
        Wave0Util.note(p, "Blinding Radiance: " + n + (n == 1 ? " foe" : " foes") + " dazzled.", ChatFormatting.WHITE);
    }

    /** Chromatic Strike: the next hit within ten seconds refracts a further 4d8 radiant damage and a Wisdom save (DC 15) against fear (sneaking: charm). */
    static void chromaticStrike(ActCtx c) {
        ServerPlayer p = c.player();
        p.getPersistentData().putLong(K_CHROMA, p.level().getGameTime() + 200);
        p.getPersistentData().putBoolean(K_CHROMA_CHARM, c.sneaking());
        Vec3 at = p.position().add(0, 1.2, 0);
        Wave0Util.send(p.serverLevel(), VfxShape.LIGHT_FLARE, at, at, 0xFFB890FF, 16, 1.4f);
        Wave0Util.note(p, c.sneaking() ? "Chromatic Strike ready: charm." : "Chromatic Strike ready: fear.", ChatFormatting.LIGHT_PURPLE);
    }

    // ---------------------------------------------------------------- the Boreal Blades
    static void borealFusion(ActCtx c) { Wave0Fusion.fuse(c.player()); }

    // ---------------------------------------------------------------- Bloodbound Katana
    static void bloodSip(ActCtx c) {
        if (c.isOn("blood_sip")) Fx.lendSkill(c.player(), "tensura:drain", 600);                      // the blade drinks: Tensura's Drain, lent while the sip is on
        Wave0Util.note(c.player(), c.isOn("blood_sip") ? "Blood Sip: the blade drinks from you too (1d4 a hit, one more charge)." : "Blood Sip: put away.", ChatFormatting.DARK_RED);
    }

    static void bloodflare(ActCtx c) {
        Wave0Util.note(c.player(), c.isOn("bloodflare") ? "Bloodflare: three charges a flare, +1d8 necrotic, up to twice a hit." : "Bloodflare: put away.", ChatFormatting.DARK_RED);
    }

    // ---------------------------------------------------------------- Thundercleave
    /** The charges the stack holds today (a new day refills both). */
    static int thunderCharges(ItemStack s, long today) {
        int c = Wave0Util.stackInt(s, T_CHARGES, Wave0Rules.THUNDER_MAX);
        long last = Wave0Util.stackLong(s, T_DAY, today);
        return Wave0Rules.thunderAtDawn(c, last, today);
    }

    /** Thunder Sweep: spend a charge, and the blade cuts a wide arc: up to three creatures within reach take 2d6 thunder, are pushed back and deafened. */
    static void thunderSweep(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        ItemStack s = Wave0Util.held(p, () -> NUItems.THUNDERCLEAVE.get());
        if (s.isEmpty()) { GrimoireBook.fail(p, "Hold Thundercleave to swing it."); return; }
        long today = Wave0Util.today(p);
        int charges = thunderCharges(s, today);
        if (charges <= 0) { GrimoireBook.fail(p, "Thundercleave has no charge left: dawn, or a thunderstorm, restores it."); return; }
        Wave0Util.edit(s, t -> { t.putInt(T_CHARGES, charges - 1); t.putLong(T_DAY, today); });
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        List<LivingEntity> foes = new ArrayList<>(GrimoireBook.around(p, p.position(), 3.4));
        foes.removeIf(t -> !Wave0Util.targetable(p, t) || t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < -0.1);
        foes.sort(Comparator.comparingDouble(t -> t.distanceToSqr(p)));
        for (int i = 0; i < foes.size() && i < 3; i++) {
            LivingEntity t = foes.get(i);
            Wave0Util.extra(p, t, Wave0Util.roll(p, 2, 6), "minecraft:lightning_bolt");
            Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4 && !BalanceLaw.isBoss(t)) t.knockback(0.9, -away.x, -away.z);
            t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
            Fx.addEffect(t, "tensura:silence", 40, 0);                                                // deafened: it cannot hear to chant
            Nullification.interfere(t, 1, 0.5f, 0.1f);
            Vec3 mid = t.getBoundingBox().getCenter();
            Wave0Util.send(sl, VfxShape.FX_LIGHTNING_ARC, eye.add(0, -0.4, 0), mid, 0xFF7FE6FF, 8, 0.9f);
        }
        Wave0Util.send(sl, VfxShape.WIND_SLASH, eye, eye.add(look.scale(4)), 0xFFB0F0FF, 12, 2.6f);
        sl.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2f, 1.3f);
        Wave0Util.note(p, "Thunder Sweep! " + (charges - 1) + " of " + Wave0Rules.THUNDER_MAX + " charges left.", ChatFormatting.AQUA);
    }

    // ---------------------------------------------------------------- Weapon of Resurgence
    /** Lay on Hands, from the blade's own pool: up to 10 points mend 2 hit points each; 5 more lift an affliction. The ally you look at, or yourself. */
    static void layOnHands(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        ItemStack s = Wave0Util.held(p, () -> NUItems.WEAPON_OF_RESURGENCE.get());
        if (s.isEmpty()) { GrimoireBook.fail(p, "Hold the Weapon of Resurgence to lay on hands."); return; }
        long now = sl.getGameTime();
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        Integer stored = cd != null && cd.copyTag().contains("LayPool") ? Integer.valueOf(cd.copyTag().getInt("LayPool")) : null;
        int pool = Wave0Rules.layPool(stored, Wave0Util.stackLong(s, "LayAt", 0), now);
        LivingEntity seen = GrimoireBook.target(p, 6);
        LivingEntity who = seen != null && seen != p && Wave0Util.healable(p, seen) ? seen : p;
        float hurt = who.getMaxHealth() - who.getHealth();
        boolean sick = Wave0Util.hasHarmful(who);
        if (pool <= 0) { GrimoireBook.fail(p, "The pool of Lay on Hands is empty: it refills as you fight."); return; }
        if (hurt < 0.5f && !sick) { GrimoireBook.fail(p, who == p ? "You are whole." : "They are whole."); return; }
        int heal = Math.min(Math.min(pool, Wave0Rules.LAY_PER_USE), (int) Math.ceil(hurt / 2f));
        int left = pool - heal;
        if (heal > 0) BalanceLaw.heal(who, Wave0Rules.layHeal(heal));
        if (sick && left >= Wave0Rules.LAY_CLEANSE && Wave0Util.cleanseOne(who)) left -= Wave0Rules.LAY_CLEANSE;
        final int saved = left;
        Wave0Util.edit(s, t -> { t.putInt("LayPool", saved); t.putLong("LayAt", now); });
        Vec3 at = who.position();
        Wave0Util.send(sl, VfxShape.MAGIC_CIRCLE, at.add(0, 0.12, 0), at.add(0, 1.12, 0), 0xFFFFD86B, 26, 1.8f);
        if (who != p) Wave0Util.send(sl, VfxShape.SEAL_WOUND, p.getEyePosition().add(0, -0.5, 0), who.getBoundingBox().getCenter(), 0xFFFFD86B, 20, 1.0f);
        sl.playSound(null, who.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4f, 1.4f);
        Wave0Util.note(p, "Lay on Hands: " + saved + " of " + Wave0Rules.LAY_MAX + " points left.", ChatFormatting.GOLD);
    }

    // ---------------------------------------------------------------- The Hand of Providence
    /** Mage Hand: a spectral hand fetches every dropped item and orb of experience within 12 blocks (up to 16) to you. */
    static void mageHand(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Vec3 to = p.getEyePosition().add(p.getViewVector(1f).scale(1.2)).add(0, -0.4, 0);
        int n = 0;
        for (ItemEntity i : sl.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(12), x -> x.isAlive())) {
            if (n >= 16) break;
            pull(sl, i, to);
            n++;
        }
        for (ExperienceOrb o : sl.getEntitiesOfClass(ExperienceOrb.class, p.getBoundingBox().inflate(12), x -> x.isAlive())) {
            if (n >= 16) break;
            pull(sl, o, to);
            n++;
        }
        if (n == 0) { GrimoireBook.fail(p, "The hand finds nothing to fetch."); return; }
        Fx.lendSkill(p, "tensura:gravity_manipulation", 60);                                         // the hand is Tensura's gravity: lent for three seconds (shorter than the four-second cooldown, so it is never held for good)
        Wave0Util.send(sl, VfxShape.MAGIC_CIRCLE, to, to.add(p.getViewVector(1f)), 0xFFFFD86B, 16, 0.9f);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.8f);
    }

    private static void pull(ServerLevel sl, net.minecraft.world.entity.Entity e, Vec3 to) {
        Vec3 d = to.subtract(e.position());
        e.setDeltaMovement(d.scale(0.22));
        e.hurtMarked = true;
        Wave0Util.send(sl, VfxShape.THREAD_LINE, to, e.position().add(0, 0.2, 0), 0xFFFFD86B, 6, 0.5f);
    }

    /** Laying On of Hands through the Mage Hand: the ally you aim at within 18 blocks is mended (30% of their health, at least 8), regenerates and is cleansed. */
    static void mageHandLay(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        LivingEntity seen = GrimoireBook.target(p, 18);
        LivingEntity who = seen != null && seen != p && Wave0Util.healable(p, seen) ? seen : p;
        boolean cleansed = Wave0Util.cleanseCurses(who);
        float hurt = who.getMaxHealth() - who.getHealth();
        if (hurt < 0.5f && !cleansed) { GrimoireBook.fail(p, who == p ? "You are whole." : "They are whole."); return; }
        BalanceLaw.heal(who, Math.max(8f, who.getMaxHealth() * 0.3f));
        who.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
        Fx.addEffect(who, "tensura:healthcare", 100, 0);
        Vec3 at = who.position(), chest = who.getBoundingBox().getCenter();
        if (who != p) Wave0Util.send(sl, VfxShape.SEAL_WOUND, p.getEyePosition().add(0, -0.5, 0), chest, 0xFFFFD86B, 26, 1.1f);
        Wave0Util.send(sl, VfxShape.MAGIC_CIRCLE, at.add(0, 0.12, 0), at.add(0, 1.12, 0), 0xFFFFD86B, 30, 2.0f);
        sl.playSound(null, who.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.6f);
    }

    // ---------------------------------------------------------------- the Infernal Contract
    static void sign(ActCtx c, Wave0Contract.Kind k) { Wave0Contract.sign(c.player(), k); }
}
