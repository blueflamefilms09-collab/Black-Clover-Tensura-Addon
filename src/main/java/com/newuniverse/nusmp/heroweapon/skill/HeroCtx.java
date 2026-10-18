package com.newuniverse.nusmp.heroweapon.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.bond.HeroLevels;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: about 150 techniques are written by six builders, and each one needs the same dozen things: who the wielder is, the weapon's form and level, the foes
 * in a ring, a line or a cone, a hit that goes through the balance law, a heal, a cost in magicules, an effect, a sound, a line on the action bar. If every
 * builder wrote those again the numbers would drift (one weapon's damage ignoring the cap, another's heal bypassing the law) and the owner's balance rules
 * could not hold. This context is built by the dispatcher (or {@link #of}) for ONE call, carries the player, the bond, the ACTIVE form and the power of the
 * level, and wraps the mod's existing helpers ({@link BalanceLaw}, {@link GrimoireBook}, {@link VfxSpawn}) behind short names. It holds no state of its own. A skill
 * that needs more calls those APIs directly, as the older weapons do.
 */
public final class HeroCtx {
    public final ServerPlayer player;
    public final ServerLevel level;
    public final HeroKind kind;
    public final HeroBond bond;
    /** The active form (never null). */
    public final HeroForm form;
    /** The summoned weapon in the wielder's main hand; empty when it is stowed or not in that hand. */
    public final ItemStack stack;
    /** The strength factor of the bond level: 1.0 at level 0 to 2.2 at level 100 ({@link HeroLevels#power}). Multiply a skill's raw numbers by it. */
    public final double power;

    public HeroCtx(ServerPlayer player, HeroBond bond, HeroForm form, ItemStack stack) {
        this.player = player;
        this.level = player.serverLevel();
        this.bond = bond;
        this.kind = bond.kind();
        this.form = form;
        this.stack = stack;
        this.power = HeroLevels.power(bond.level());
    }

    /** The context for a player who holds a bond, or null. The stack is the main-hand item when it is the bonded weapon. */
    public static HeroCtx of(ServerPlayer p) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound()) return null;
        ItemStack held = p.getMainHandItem();
        ItemStack stack = HeroItems.kindOf(held) == b.kind() ? held : ItemStack.EMPTY;
        return new HeroCtx(p, b, HeroForms.active(b), stack);
    }

    /** True when the weapon is summoned and in the hand. */
    public boolean summoned() { return bond.summoned() && !stack.isEmpty(); }

    public int bondLevel() { return bond.level(); }

    /** The form's mastery as a share 0..1 (1 for a form with no mastery): the "mastery" argument of the balance law. */
    public double masteryShare() {
        if (form.mastery() <= 0) return 1.0;
        return Math.max(0.0, Math.min(1.0, bond.mastery(form.id()) / (double) form.mastery()));
    }

    // ------------------------------------------------------------------ geometry
    public Vec3 eye() { return player.getEyePosition(); }

    public Vec3 look() { return player.getViewVector(1f); }

    /** Foes (living, alive, not allied, not the wielder) within {@code r} of {@code c}. */
    public List<LivingEntity> around(Vec3 c, double r) { return GrimoireBook.around(player, c, r); }

    /** Foes within {@code width} of the segment a to b. */
    public List<LivingEntity> along(Vec3 a, Vec3 b, double width) { return GrimoireBook.along(player, a, b, width); }

    /** Foes within {@code range} of the wielder in front of them: within {@code halfAngleDeg} of the look direction. */
    public List<LivingEntity> cone(double range, double halfAngleDeg) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 eye = eye(), look = look();
        double min = Math.cos(Math.toRadians(halfAngleDeg));
        for (LivingEntity e : around(player.position(), range)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.lengthSqr() < 1e-6 || to.normalize().dot(look) >= min) out.add(e);
        }
        return out;
    }

    /** The point under the crosshair within {@code range} (a block hit, or the far end). */
    public Vec3 aim(double range) { return GrimoireBook.aim(player, range); }

    // ------------------------------------------------------------------ effects on others
    /**
     * A hit through the balance law: {@code raw} is the technique's base damage (it is multiplied by the level {@link #power}); the law scales it with the
     * wielder's existence, the form's mastery, the damage game rule and the caps (players: a share of their health; mobs: the config multiplier). False when
     * the target is the wielder, an ally, dead, or the hit was refused.
     */
    public boolean hit(LivingEntity target, float raw) {
        if (target == null || target == player || !target.isAlive() || target.isAlliedTo(player)) return false;
        float dmg = BalanceLaw.damage(player, target, (float) (raw * power), masteryShare());
        return target.hurt(player.damageSources().playerAttack(player), dmg);
    }

    /** A heal through the balance law (the law caps what a heal may do to bosses and players). */
    public void heal(LivingEntity target, float amount) {
        if (target != null && target.isAlive() && amount > 0f) BalanceLaw.heal(target, amount);
    }

    /** The shared hard-control gate: true once per the law's lock (12 s); call it before rooting, stunning or pulling anything. */
    public boolean control() { return BalanceLaw.beginControl(player); }

    /** Pushes a target away from {@code from} (horizontal), scaled down for heavy targets by vanilla's knockback resistance. */
    public void shove(LivingEntity target, Vec3 from, double strength) {
        Vec3 away = target.position().subtract(from).multiply(1, 0, 1);
        if (away.lengthSqr() < 1e-4) away = look().multiply(1, 0, 1);
        if (away.lengthSqr() < 1e-4) return;
        Vec3 n = away.normalize();
        target.knockback(strength, -n.x, -n.z);
    }

    // ------------------------------------------------------------------ cost
    /**
     * Spends {@code share} of the wielder's maximum magicules (then Aura for what the magicules cannot cover); false and nothing spent when they cannot pay.
     * Creative players and players without a Tensura record pay nothing. The dispatcher calls this for the skill's own cost; a skill calls it only for an extra
     * (a held channel).
     */
    public boolean pay(double share) {
        if (player.isCreative() || share <= 0) return true;
        var ex = TensuraStorages.getExistenceFrom(player);
        if (ex == null) return true;
        double max = EnergyHelper.getMaxMagicule(player), cost = max * share;
        double mp = ex.getMagicule(), ap = ex.getAura();
        double fromMp = Math.min(cost, Math.max(0.0, mp));
        double rest = cost - fromMp;
        if (rest > ap) return false;
        ex.setMagicule(mp - fromMp);
        if (rest > 0) ex.setAura(Math.max(0.0, ap - rest));
        ex.markDirty();
        if (ex.getMagicule() < max * 0.05) ManaLaw.exhaust(player);
        return true;
    }

    // ------------------------------------------------------------------ show
    /** One effect through the mod's effect system (never vanilla particle spam). {@code argb} is 0xAARRGGBB. */
    public void fx(VfxShape shape, Vec3 from, Vec3 to, int argb, int ticks, float size) { VfxSpawn.send(level, shape, from, to, argb, ticks, size); }

    public void sound(SoundEvent sound, float volume, float pitch) { level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch); }

    /** A line on the wielder's action bar. */
    public void say(String text) { player.displayClientMessage(Component.literal(text), true); }
}
