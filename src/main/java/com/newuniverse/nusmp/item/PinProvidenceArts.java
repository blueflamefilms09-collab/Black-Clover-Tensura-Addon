package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.item.MagicWeaponItem.Kind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Brooch Pin (dagger) and the Hand of Providence (gauntlet), from the "Magic Item Compendium I" and the paladin sheets.
 * <ul>
 *   <li><b>Brooch Pin</b>: right-click folds it into a brooch or unfolds it into a dagger. Unfolded it hits harder (the sheet's +1 to
 *       attack and damage) and its hits find the gap in armour; folded it passes for jewellery. Sneak + right-click: folded, Hidden in
 *       Plain Sight (invisible for 10 s and everything hunting you loses you); unfolded, Pinprick Lunge (a four-block dash and stab,
 *       tripled in the back).</li>
 *   <li><b>Hand of Providence</b>: a gauntlet with a blue cross. Right-click: Mage Hand, a spectral golden hand reaches 12 blocks
 *       to lay on hands (mends an ally or yourself and cleanses an affliction) or, on the wicked, strikes them with holy damage and
 *       drags them in. Sneak + right-click: Lay on Hands on yourself, a bonus action, mending a third of your health.</li>
 * </ul>
 */
final class PinProvidenceArts {
    private PinProvidenceArts() {}

    private static final String K_OPEN = "Unfolded";
    private static final int RUBY = 0xFFE0243C, GOLD = 0xFFFFD86B, HOLY = 0xFFFFF2B0, STEEL = 0xFFD8E4F0;

    static boolean isMine(Kind k) { return k == Kind.BROOCH_PIN || k == Kind.HAND_OF_PROVIDENCE; }

    // ---------------------------------------------------------------- the pin's fold
    static boolean unfolded(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.copyTag().getBoolean(K_OPEN);
    }

    private static void setUnfolded(ItemStack s, boolean v) {
        CompoundTag t = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        t.putBoolean(K_OPEN, v);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    private static boolean friend(ServerPlayer p, LivingEntity x) { return x == p || x.isAlliedTo(p) || x instanceof ServerPlayer; }

    private static boolean evil(LivingEntity t) {
        return t.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD) || t instanceof net.minecraft.world.entity.monster.Monster;
    }

    private static void cleanse(LivingEntity a) {
        for (MobEffectInstance e : new ArrayList<>(a.getActiveEffects())) {
            if (!e.getEffect().value().isBeneficial()) { a.removeEffect(e.getEffect()); return; }
        }
    }

    // ================================================================ entry points from ReferenceWeapons
    static boolean technique(ServerPlayer p, Kind k, ItemStack stack) {
        ServerLevel sl = p.serverLevel();
        if (k == Kind.BROOCH_PIN) {
            boolean open = !unfolded(stack);
            setUnfolded(stack, open);
            Vec3 c = p.getBoundingBox().getCenter().add(p.getViewVector(1f).scale(0.6));
            VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, c, c, open ? STEEL : RUBY, 8, 0.5f);
            sl.playSound(null, p.blockPosition(), open ? SoundEvents.CHAIN_PLACE : SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.PLAYERS, 1f, open ? 1.6f : 1.2f);
            p.displayClientMessage(Component.literal(open ? "The brooch unfolds into a dagger." : "The dagger folds into a brooch.").withStyle(ChatFormatting.GOLD), true);
            return true;
        }
        // Mage Hand
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), end = eye.add(look.scale(12));
        LivingEntity pick = null;
        for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.4)) {
            if (t != p && t.isAlive() && (pick == null || t.distanceToSqr(p) < pick.distanceToSqr(p))) pick = t;
        }
        Vec3 to = pick != null ? pick.getBoundingBox().getCenter() : end;
        VfxSpawn.send(sl, VfxShape.LIGHT_BLADE, eye.add(0, -0.3, 0), to, GOLD, 10, 1.1f);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, to, to, HOLY, 12, 0.9f);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.6f, 1.1f);
        if (pick == null || friend(p, pick)) {
            LivingEntity who = pick == null ? p : pick;
            BalanceLaw.heal(who, Math.max(4f, who.getMaxHealth() * 0.2f));
            cleanse(who);
            VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, who.position().add(0, 1.3, 0), who.position().add(0, 1.3, 0), HOLY, 14, 0.8f);
        } else {
            boolean bad = evil(pick);
            pick.hurt(p.damageSources().playerAttack(p), BalanceLaw.damage(p, pick, bad ? 9f : 5f, 0.6));
            if (bad) pick.igniteForSeconds(3);
            if (!BalanceLaw.isBoss(pick)) {
                Vec3 pull = p.position().subtract(pick.position()).normalize().scale(0.9);
                pick.setDeltaMovement(pick.getDeltaMovement().add(pull.x, 0.2, pull.z));
                pick.hurtMarked = true;
            }
        }
        return true;
    }

    static WeaponArts.Alt alt(Kind k) {
        return k == Kind.BROOCH_PIN
                ? new WeaponArts.Alt("Hidden in Plain Sight / Pinprick Lunge", "Folded: you are invisible for 10 s and everything hunting you loses you. Unfolded: a four-block dash and stab, tripled when it lands in the back.", 800)
                : new WeaponArts.Alt("Lay on Hands", "A bonus action: your gauntlet mends a third of your health and cleanses an affliction.", 1200);
    }

    static boolean cast(ServerPlayer p, Kind k, ItemStack stack) {
        ServerLevel sl = p.serverLevel();
        if (k == Kind.HAND_OF_PROVIDENCE) {
            BalanceLaw.heal(p, p.getMaxHealth() / 3f);
            cleanse(p);
            VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, p.position().add(0, 1.3, 0), p.position().add(0, 1.3, 0), HOLY, 16, 1.1f);
            sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.6f);
            return true;
        }
        if (!unfolded(stack)) {                                                    // Hidden in Plain Sight
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, false));
            for (Mob m : sl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24), x -> x.getTarget() == p)) m.setTarget(null);
            VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, p.position().add(0, 1, 0), p.position().add(0, 1, 0), RUBY, 10, 0.8f);
            return true;
        }
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);                 // Pinprick Lunge
        List<LivingEntity> hit = GrimoireBook.along(p, eye, eye.add(look.scale(4)), 1.2);
        LivingEntity first = null;
        for (LivingEntity t : hit) if (t != p && t.isAlive() && (first == null || t.distanceToSqr(p) < first.distanceToSqr(p))) first = t;
        Vec3 dest = first != null ? first.position().subtract(look.scale(1.0)) : p.position().add(look.scale(4));
        p.teleportTo(dest.x, p.getY(), dest.z);
        if (first != null) {
            boolean back = first.getViewVector(1f).dot(look) > 0.3;
            first.hurt(p.damageSources().playerAttack(p), BalanceLaw.damage(p, first, back ? 18f : 6f, 0.6));
            sl.playSound(null, first.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 1.4f);
            VfxSpawn.send(sl, VfxShape.WIND_SLASH, first.getBoundingBox().getCenter().subtract(look), first.getBoundingBox().getCenter().add(look), STEEL, 8, 0.8f);
        }
        return true;
    }

    /** The pin's passive: unfolded, every blow is +1 and finds the gap. */
    static void onHit(ServerPlayer p, ItemStack stack, LivingEntity t, Kind k) {
        if (k == Kind.BROOCH_PIN && unfolded(stack) && t.isAlive()) {
            t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, t, 1.5f, 0.6));
        } else if (k == Kind.HAND_OF_PROVIDENCE && t.isAlive() && evil(t)) {
            t.igniteForSeconds(2);
        }
    }

    static void fullSwing(ServerPlayer p, LivingEntity t, Kind k) {
        ServerLevel sl = p.serverLevel();
        if (k == Kind.BROOCH_PIN) {
            t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, t, unfolded(p.getMainHandItem()) ? 4f : 1f, 0.6));
            VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), RUBY, 8, 0.6f);
        } else {
            t.hurt(p.damageSources().playerAttack(p), BalanceLaw.damage(p, t, evil(t) ? 6f : 3f, 0.6));
            VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), HOLY, 10, 0.8f);
            sl.playSound(null, t.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.3f);
        }
    }

    static void tooltip(ItemStack stack, Kind k, List<Component> tip) {
        if (k == Kind.BROOCH_PIN) {
            tip.add(Component.literal("  " + (unfolded(stack) ? "Unfolded: a dagger, +1 to hits that find the gap in armour" : "Folded: it passes for a piece of jewellery")).withStyle(ChatFormatting.GOLD));
            tip.add(Component.literal("A ruby brooch in gold filigree. Folded it is jewellery; a click and it is a dagger.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            tip.add(Component.literal("A gauntlet with a blue cross on the back of the palm: a spectral hand in the shape of the god you follow.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
