package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.item.MagicWeaponItem.Kind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.3.0: the Hand of Providence (the golden gauntlet with the blue cross on the back of the hand, from the paladin sheet). Reached from
 * {@link ReferenceWeapons} and {@link MagicWeaponItem}. Damage and healing go through the balance law; holy damage is Tensura's own holy
 * damage type against undead, demons and devil-tainted mages.
 * <ul>
 *   <li><b>Healing fists</b> (the passive): every punch mends the wielder a little (2% of their health, at least half a heart and at most
 *       two; half as much again against the wicked) and gives the foe a small shove. Undead and demons take a little holy fire.</li>
 *   <li><b>Laying On of Hands</b> (right-click): lays the gauntlet on the ally you are looking at within 6 blocks (any player, a pet, a
 *       villager, a golem, a team mate), or on yourself: a big mend (40% of their health, at least 10), Regeneration, and every curse and
 *       poison is cleansed.</li>
 *   <li><b>Providence Strike</b> (sneak + right-click): a radiant punch down a 6 block line. Foes are hurled back and burned by holy light;
 *       undead and demons are blinded and weakened, the rest are lit up. Bosses are only shoved a little and blinded for a moment.</li>
 * </ul>
 */
final class HandOfProvidenceArts {
    private HandOfProvidenceArts() {}

    static final int GOLD = 0xFFFFD86B, HOLY = 0xFFFFF2B0, BLUE = 0xFF6CC8FF;
    private static final String[] CURSES = {"poison", "curse", "wither", "hex", "infection", "corrosion", "plague", "disease", "unluck", "rot", "misfortune", "doom"};

    static final WeaponArts.Alt ALT = new WeaponArts.Alt("Providence Strike",
            "A radiant punch down a 6 block line: foes are hurled back and burned by holy light (undead and demons far worse). Undead and demons are also blinded and weakened, everyone else is lit up. Bosses are only shoved a little.", 700);

    static boolean isMine(Kind k) { return k == Kind.HAND_OF_PROVIDENCE; }

    // ---------------------------------------------------------------- who is who
    /** Undead, demons and devil-tainted mages feel the gauntlet as holy fire. */
    static boolean evil(LivingEntity t) {
        if (t.getType().is(EntityTypeTags.UNDEAD) || GrimoireBook.inDevilState(t)) return true;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(t.getType());
        return id != null && (id.getPath().contains("demon") || id.getPath().contains("devil"));
    }

    /** Never struck: you, your allies and your pets. */
    private static boolean ally(ServerPlayer p, LivingEntity x) {
        return x == p || x.isAlliedTo(p) || (x instanceof OwnableEntity o && o.getOwner() == p);
    }

    /** May be laid hands on: you, your allies, any player, anyone's pet, villagers and golems made by a player. */
    private static boolean healable(ServerPlayer p, LivingEntity x) {
        if (ally(p, x) || x instanceof Player || x instanceof AbstractVillager) return true;
        if (x instanceof IronGolem g && g.isPlayerCreated()) return true;
        return x instanceof OwnableEntity o && o.getOwner() instanceof Player;
    }

    /** A holy blow: Tensura's holy damage on the wicked (x1.6, and they burn), light damage on the rest. */
    private static void smite(ServerPlayer p, LivingEntity t, float raw) {
        if (ally(p, t) || !t.isAlive()) return;
        boolean evil = evil(t);
        DamageSource src;
        try {
            src = TensuraDamageTypes.getEntityDamageSource(p.serverLevel(), evil ? TensuraDamageTypes.HOLY_DAMAGE : TensuraDamageTypes.LIGHT_ELEMENTAL, p);
        } catch (RuntimeException noType) {
            src = p.damageSources().playerAttack(p);
        }
        t.hurt(src, BalanceLaw.damage(p, t, evil ? raw * 1.6f : raw, 0.6));
        if (evil) t.igniteForSeconds(3);
    }

    /** Takes off every curse and poison; true if anything was lifted. */
    private static boolean cleanse(LivingEntity a) {
        boolean any = false;
        for (MobEffectInstance e : new ArrayList<>(a.getActiveEffects())) {
            if (e.getEffect().value().isBeneficial()) continue;
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect().value());
            String path = id == null ? "" : id.getPath();
            for (String word : CURSES) {
                if (path.contains(word)) { a.removeEffect(e.getEffect()); any = true; break; }
            }
        }
        long now = a.level().getGameTime();
        CompoundTag pd = a.getPersistentData();
        for (String flag : new String[]{"nusmp_misfortune_until", "nusmp_sealed_until"}) {              // the mod's own curses: misfortune and sealing
            if (pd.getLong(flag) > now) { pd.putLong(flag, 0); any = true; }
        }
        return any;
    }

    // ---------------------------------------------------------------- the passive: healing fists
    static void onHit(ServerPlayer p, ItemStack stack, LivingEntity t) {
        ServerLevel sl = p.serverLevel();
        boolean evil = evil(t);
        Vec3 mid = t.getBoundingBox().getCenter();
        if (p.getHealth() < p.getMaxHealth()) {
            p.heal(Mth.clamp(p.getMaxHealth() * 0.02f, 0.5f, 4f) * (evil ? 1.5f : 1f));
            sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.6, p.getZ(), 1, 0.25, 0.2, 0.25, 0.0);
        }
        if (!BalanceLaw.isBoss(t)) t.knockback(0.25, p.getX() - t.getX(), p.getZ() - t.getZ());       // a punch
        if (evil) {
            smite(p, t, 2f);
            VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, mid, mid, GOLD, 8, 0.6f);
        }
        sl.sendParticles(ParticleTypes.ENCHANTED_HIT, mid.x, mid.y, mid.z, 5, 0.25, 0.3, 0.25, 0.3);
    }

    /** Full swing: Radiant Knuckle, a holy blow with a flash of light that mends the wielder a little more. */
    static void fullSwing(ServerPlayer p, LivingEntity t) {
        ServerLevel sl = p.serverLevel();
        Vec3 mid = t.getBoundingBox().getCenter();
        smite(p, t, 4f);
        if (!BalanceLaw.isBoss(t)) t.knockback(0.7, p.getX() - t.getX(), p.getZ() - t.getZ());
        if (evil(t)) t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0));
        p.heal(1.5f);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, mid, mid, HOLY, 10, 1.1f);
        VfxSpawn.send(sl, VfxShape.WIND_RING, mid.subtract(0, 0.6, 0), mid, GOLD, 10, 0.9f);
        sl.playSound(null, t.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.3f, 1.5f);
    }

    // ---------------------------------------------------------------- right-click: Laying On of Hands
    static boolean technique(ServerPlayer p, ItemStack stack) {
        ServerLevel sl = p.serverLevel();
        LivingEntity seen = GrimoireBook.target(p, 6);
        LivingEntity who = seen != null && seen != p && healable(p, seen) ? seen : p;
        boolean cleansed = cleanse(who);
        float hurt = who.getMaxHealth() - who.getHealth();
        if (hurt < 0.5f && !cleansed) {
            GrimoireBook.fail(p, who == p ? "You are whole." : "They are whole.");
            return false;
        }
        BalanceLaw.heal(who, Math.max(10f, who.getMaxHealth() * 0.4f));
        who.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        Vec3 at = who.position(), head = who.position().add(0, who.getBbHeight() + 0.3, 0), chest = who.getBoundingBox().getCenter();
        if (who != p) VfxSpawn.send(sl, VfxShape.SEAL_WOUND, p.getEyePosition().add(0, -0.5, 0).add(p.getViewVector(1f).scale(0.5)), chest, GOLD, 26, 1.1f);
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, at.add(0, 0.12, 0), at.add(0, 1.12, 0), GOLD, 34, 2.4f);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, head, head, HOLY, 20, 2.0f);
        VfxSpawn.send(sl, VfxShape.WATER_RING, at.add(0, 0.1, 0), at.add(0, 1, 0), BLUE, 22, 1.5f);
        sl.sendParticles(ParticleTypes.END_ROD, chest.x, chest.y, chest.z, 22, 0.4, 0.7, 0.4, 0.06);
        sl.sendParticles(ParticleTypes.HEART, chest.x, chest.y + 0.4, chest.z, 6, 0.4, 0.5, 0.4, 0.0);
        sl.playSound(null, who.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.6f, 1.5f);
        sl.playSound(null, who.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.8f, 1.2f);
        sl.playSound(null, who.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.2f, 1.4f);
        return true;
    }

    // ---------------------------------------------------------------- sneak + right-click: Providence Strike
    static boolean cast(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        Vec3 fist = eye.add(look.scale(1.0)).add(0, -0.45, 0), end = eye.add(look.scale(6.0));
        MagicWeaponItem.eraseProjectiles(p, new AABB(eye, end).inflate(1.5));
        int struck = 0;
        for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.5)) {
            boolean evil = evil(t), boss = BalanceLaw.isBoss(t);
            Vec3 mid = t.getBoundingBox().getCenter();
            smite(p, t, 9f);
            t.knockback(boss ? 0.5 : 1.9, p.getX() - t.getX(), p.getZ() - t.getZ());
            if (!boss) {
                t.setDeltaMovement(t.getDeltaMovement().add(0, 0.3, 0));
                t.hurtMarked = true;
            }
            if (evil) {
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.controlTicks(t, 140), 0));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            } else {
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
            }
            VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, mid, mid, evil ? HOLY : GOLD, 14, evil ? 1.8f : 1.2f);
            sl.sendParticles(ParticleTypes.END_ROD, mid.x, mid.y, mid.z, 12, 0.3, 0.4, 0.3, 0.12);
            struck++;
        }
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, fist, fist.add(look), GOLD, 16, 2.2f);                  // the sigil the fist breaks through
        VfxSpawn.send(sl, VfxShape.LIGHT_BLADE, eye.add(0, -0.45, 0), end, HOLY, 12, 1.4f);
        VfxSpawn.send(sl, VfxShape.WIND_RING, fist, fist.add(look), GOLD, 16, 1.8f);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, end, end, HOLY, 14, 1.6f + Math.min(1.2f, struck * 0.3f));
        for (int k = 1; k <= 6; k++) {
            Vec3 pt = eye.add(look.scale(k));
            sl.sendParticles(ParticleTypes.END_ROD, pt.x, pt.y - 0.3, pt.z, 3, 0.15, 0.15, 0.15, 0.02);
        }
        sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.6f, 0.8f);
        sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4f, 1.9f);
        sl.playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.6f, 0.9f);
        return true;
    }

    // ---------------------------------------------------------------- tooltip
    static void tooltip(ItemStack stack, List<Component> tip) {
        tip.add(Component.literal("Healing fists (passive)").withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("  Every punch mends you a little (half as much again against undead and demons, which also take holy fire) and shoves the foe back.").withStyle(ChatFormatting.YELLOW));
        tip.add(Component.literal("A gauntlet with a blue cross on the back of the hand. The hand takes a form befitting the god you follow.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
