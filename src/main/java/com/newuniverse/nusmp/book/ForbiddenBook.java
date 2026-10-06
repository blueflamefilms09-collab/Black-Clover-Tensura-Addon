package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Forbidden Magic: a separate Unique skill written by a devil contract. Every cast has a price
 * (HP that stacks), announces itself in a dark subtitle and respects the balance law.
 */
public class ForbiddenBook extends GrimoireBook {
    static final int DARK = 0xFF8A0F2A;

    private final List<BookPage> pages = List.of(
            BookPage.signature("devil_dive", "Devil Dive", ForbiddenBook::devilDive),
            BookPage.signature("five_leaf_whisper", "Five-Leaf Whisper", ForbiddenBook::whisper),
            BookPage.mid("life_exchange", "Life Exchange", ForbiddenBook::lifeExchange),
            BookPage.signature("reversal_living", "Time Reversal on the Living", ForbiddenBook::rewindLiving),
            BookPage.zone("underworld_gate", "Underworld Gate", ForbiddenBook::underworld),
            BookPage.zone("mana_devour", "Mana Devour", ForbiddenBook::devour),
            BookPage.zone("curse_misfortune", "Curse of Misfortune", ForbiddenBook::curse),
            BookPage.signature("reincarnation", "Forbidden Reincarnation", ForbiddenBook::reincarnation));

    public ForbiddenBook() { super(MagicType.ANTI_MAGIC, DARK, ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/grimoire/forbidden.png")); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.CURSE; }

    /** Price + dark subtitle, shared by every forbidden page. */
    private static boolean price(ServerPlayer p, String page) {
        if (!ForbiddenMagic.payPrice(p)) return false;
        ForbiddenMagic.subtitle(p, Component.literal(p.getName().getString() + " casts forbidden magic: " + page));
        return true;
    }

    /** 8 s: +10% resistance pierce on all grimoire spells, HP drain, 2 s stun at the price floor. */
    static boolean devilDive(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!price(p, "Devil Dive")) return false;
        startDevilState(p, 160);
        p.getPersistentData().putString("nusmp_devil_src", "dive");
        i.getOrCreateTag().putBoolean("Diving", true);
        var devil = ForbiddenMagic.devilOf(p);
        b.vfx(p, VfxShape.DEVIL_CIRCLE, p.position(), p.position().add(0, 1, 0), 40, 2.0f);
        com.newuniverse.nusmp.vfx.VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SPIRIT_AURA, p, p.position().add(0, 1, 0), devil == null ? DARK : devil.color, 160, 1.2f);
        return true;
    }

    @Override
    protected void tickBook(ManasSkillInstance i, ServerPlayer p) {
        if (!i.getOrCreateTag().getBoolean("Diving")) return;
        if (!inDevilState(p)) { i.getOrCreateTag().putBoolean("Diving", false); return; }
        if (p.getHealth() <= 6f) {
            p.getPersistentData().putLong("nusmp_devil_until", p.level().getGameTime());
            i.getOrCreateTag().putBoolean("Diving", false);
            TimeStop.freeze(p, 40);
            fail(p, "The devil lets go. You are stunned.");
        } else {
            p.hurt(p.damageSources().magic(), (float) com.newuniverse.nusmp.item.MagicGear.selfDamageMult(p));
        }
    }

    /** Blank until a devil is bound; then that devil's own spell. */
    static boolean whisper(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        var devil = ForbiddenMagic.devilOf(p);
        if (devil == null) { fail(p, "The page is blank. No devil has written on it."); return false; }
        if (!price(p, devil.displayName + "'s Whisper")) return false;
        Vec3 c = p.position();
        switch (devil) {
            case IGNIVAR -> {   // Hellmouth: a burning nova
                for (LivingEntity t : around(p, c, 6)) { b.hurt(i, p, t, mode, 14f); t.igniteForSeconds(6); }
                b.vfx(p, VfxShape.FLAME_EXPLOSION, c, c.add(0, 2, 0), 26, 2.0f);
            }
            case VERBAEL -> {   // Edict: "Kneel." (shares the control lock)
                boolean ctl = BalanceLaw.beginControl(p);
                for (LivingEntity t : around(p, c, 8)) {
                    b.hurt(i, p, t, mode, 6f);
                    if (ctl) TimeStop.freeze(t, BalanceLaw.controlTicks(t, 40));
                }
                p.serverLevel().getServer().getPlayerList().broadcastSystemMessage(Component.literal("\"KNEEL.\"").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
                b.vfx(p, VfxShape.DEVIL_CIRCLE, c, c.add(0, 1, 0), 30, 1.6f);
            }
            case GRAVEMAW -> {  // Abyssal Weight
                for (LivingEntity t : around(p, c, 8)) {
                    b.hurt(i, p, t, mode, 10f);
                    t.setDeltaMovement(0, -2.5, 0);
                    t.hurtMarked = true;
                }
                b.vfx(p, VfxShape.MAGIC_CIRCLE_EXPLOSION, c, c.add(0, 1, 0), 26, 2.0f);
            }
            case KAIROVORE -> { // Devoured Hour: freeze one foe and eat half your grimoire's cooldowns
                LivingEntity t = target(p, 12);
                if (t != null && BalanceLaw.beginControl(p)) TimeStop.freeze(t, BalanceLaw.controlTicks(t, 60));
                GrimoirePages.grimoireOf(p).ifPresent(g -> { for (int m = 0; m < g.getModes(); m++) g.setCoolDown(g.getCoolDown(m) / 2, m); });
                b.vfx(p, VfxShape.WATER_RING, c, c.add(0, 1, 0), 30, 1.6f);
            }
        }
        return true;
    }

    /** Heal the ally you look at by taking their wounds at a worse ratio (1.5 : 1). */
    static boolean lifeExchange(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!(target(p, 10) instanceof ServerPlayer ally)) { fail(p, "Look at the ally whose wounds you will take."); return false; }
        float missing = ally.getMaxHealth() - ally.getHealth();
        if (missing <= 0.5f) { fail(p, "They have no wounds to take."); return false; }
        float healed = BalanceLaw.heal(ally, missing);
        float cost = healed * 1.5f * (float) com.newuniverse.nusmp.item.MagicGear.selfDamageMult(p);
        if (p.getHealth() <= cost + 1) { fail(p, "You cannot carry that much."); return false; }
        p.hurt(p.damageSources().magic(), cost);
        ForbiddenMagic.subtitle(p, Component.literal(p.getName().getString() + " takes another's wounds: Life Exchange"));
        b.vfx(p, VfxShape.THREAD_LINE, ally.getBoundingBox().getCenter(), p.getBoundingBox().getCenter(), 20, 1f);
        return true;
    }

    /** Rewinds one consenting ally (holding Written Consent) — or yourself — 4 s of position and 30% health. */
    static boolean rewindLiving(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerPlayer t = target(p, 10) instanceof ServerPlayer o ? o : p;
        if (t != p && !ForbiddenMagic.consents(t)) { fail(p, "They have not given written consent."); return false; }
        CompoundTag td = ForbiddenMagic.data(t);
        long now = p.level().getGameTime();
        if (now - td.getLong("nusmp_rewound_at") < 168000 && td.getLong("nusmp_rewound_at") != 0) { fail(p, "Their time was already rewound this week."); return false; }
        if (!price(p, "Time Reversal on the Living")) return false;
        td.putLong("nusmp_rewound_at", now);
        t.getPersistentData().put(Player.PERSISTED_NBT_TAG, td);
        Vec3 back = ForbiddenMagic.fourSecondsAgo(t);
        t.teleportTo(back.x, back.y, back.z);
        BalanceLaw.heal(t, t.getMaxHealth() * 0.3f);
        ForbiddenMagic.payRewindSliver(p);
        b.vfx(p, VfxShape.WATER_RING, back, back.add(0, 1, 0), 30, 1.2f);
        return true;
    }

    /** A 6-block rift for 5 s: undead already nearby are dragged in, hostiles inside take a darkness DoT. */
    static boolean underworld(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!price(p, "Underworld Gate")) return false;
        Vec3 c = aim(p, 16);
        b.vfx(p, VfxShape.SPATIAL_RIFT, c, c.add(0, 1, 0), 100, 2.0f);
        b.vfx(p, VfxShape.DEVIL_CIRCLE, c, c.add(0, 1, 0), 100, 1.2f);
        SpellRuntime.zone(p.serverLevel(), 100, 5, age -> {
            for (LivingEntity t : around(p, c, 6)) {
                if (t.getType().is(EntityTypeTags.UNDEAD)) { t.setDeltaMovement(c.subtract(t.position()).normalize().scale(0.4)); t.hurtMarked = true; }
                if (age % 20 == 0) { b.hurt(i, p, t, mode, 3f); t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0)); }
            }
        });
        crackDiamond(p, 0.25);
        return true;
    }

    /** A 6-block zone for 5 s: everyone inside (you too) loses a buff and a little magicule each second. */
    static boolean devour(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!price(p, "Mana Devour")) return false;
        Vec3 c = p.position();
        b.vfx(p, VfxShape.ANTI_MAGIC_SLASH, c.add(0, 1, 0), c.add(p.getViewVector(1f).scale(3)).add(0, 1, 0), 20, 1.2f);
        b.vfx(p, VfxShape.DEVIL_CIRCLE, c, c.add(0, 1, 0), 100, 6 / 1.5f);
        SpellRuntime.zone(p.serverLevel(), 100, 20, age -> {
            List<LivingEntity> inside = new ArrayList<>(around(p, c, 6));
            if (p.distanceToSqr(c) < 36) inside.add(p);
            for (LivingEntity t : inside) {
                for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) {
                    if (e.getEffect().value().isBeneficial()) { t.removeEffect(e.getEffect()); break; }
                }
                var ex = TensuraStorages.getExistenceFrom(t);
                if (ex != null) { ex.setMagicule(Math.max(0, ex.getMagicule() - EnergyHelper.getMaxMagicule(t) * 0.02)); ex.markDirty(); }
            }
        });
        return true;
    }

    /** Forces the cracked-diamond state on a Diamond grimoire (a fortune ward blocks it); others get a misfortune mark. */
    static boolean curse(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 16);
        if (t == null) { fail(p, "Curse whom?"); return false; }
        if (!price(p, "Curse of Misfortune")) return false;
        b.vfx(p, VfxShape.DEVIL_CIRCLE, t.position(), p.getEyePosition(), 30, 0.8f);
        if (t instanceof ServerPlayer tp) {
            var g = GrimoirePages.grimoireOf(tp);
            if (g.isPresent() && GrimoirePages.coverOf(g.get()).kingdom == Kingdom.DIAMOND) {
                if (GrimoirePages.coverOf(g.get()) == GrimoireCover.FIVE_SIDED && p.level().getGameTime() >= tp.getPersistentData().getLong("nusmp_fortune_ready")) {
                    tp.getPersistentData().putLong("nusmp_fortune_ready", p.level().getGameTime() + 24000);
                    fail(p, "Their fortune ward turns the curse aside.");
                    return true;
                }
                setCover(tp, g.get(), GrimoireCover.CRACKED_DIAMOND);
                return true;
            }
        }
        t.getPersistentData().putLong("nusmp_misfortune_until", p.level().getGameTime() + 1200);
        return true;
    }

    /** A ritual: writes a soul mark here. Die within 3 in-game days and you return here — missing a page and some mana. */
    static boolean reincarnation(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        CompoundTag d = ForbiddenMagic.data(p);
        if (d.getLong("nusmp_soul_until") > p.level().getGameTime()) { fail(p, "A soul mark is already written."); return false; }
        if (!price(p, "Forbidden Reincarnation")) return false;
        d.putLong("nusmp_soul_until", p.level().getGameTime() + 72000);
        d.putDouble("nusmp_soul_x", p.getX()); d.putDouble("nusmp_soul_y", p.getY()); d.putDouble("nusmp_soul_z", p.getZ());
        d.putString("nusmp_soul_dim", p.level().dimension().location().toString());
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, d);
        b.vfx(p, VfxShape.DEVIL_CIRCLE, p.position(), p.position().add(0, 1, 0), 80, 1.6f);
        return true;
    }

    @Override
    public boolean onDeath(ManasSkillInstance i, LivingEntity owner, DamageSource source) {
        if (!(owner instanceof ServerPlayer p)) return true;
        CompoundTag d = ForbiddenMagic.data(p);
        if (d.getLong("nusmp_soul_until") <= p.level().getGameTime()) return true;
        if (!p.level().dimension().location().toString().equals(d.getString("nusmp_soul_dim"))) return true;
        d.putLong("nusmp_soul_until", 0);
        d.putDouble("nusmp_mana_tax", d.getDouble("nusmp_mana_tax") + 0.1);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, d);
        p.setHealth(p.getMaxHealth());
        p.teleportTo(d.getDouble("nusmp_soul_x"), d.getDouble("nusmp_soul_y"), d.getDouble("nusmp_soul_z"));
        GrimoirePages.grimoireOf(p).ifPresent(g -> {
            if (g.getSkill() instanceof GrimoireBook gb) for (int m = gb.familyCount() - 1; m >= 1; m--) {
                if (isUnlocked(g, m)) { g.getOrCreateTag().putInt("Unlocked", g.getOrCreateTag().getInt("Unlocked") & ~(1 << m)); g.markDirty(); break; }
            }
        });
        ForbiddenMagic.subtitle(p, Component.literal(p.getName().getString() + " returns from death. Something was left behind."));
        return false;
    }

    static void setCover(ServerPlayer p, ManasSkillInstance g, GrimoireCover cover) {
        g.getOrCreateTag().putString("Cover", cover.name());
        g.getOrCreateTag().putInt("Leaves", cover.tier);
        g.markDirty();
        var inv = p.getInventory();
        com.newuniverse.nusmp.blackclover.GrimoireSlot.replaceOwned(p, old -> GrimoireItem.create(p, cover, GrimoirePages.magicOf(g), null));
        for (int s = 0; s < inv.getContainerSize(); s++) {
            if (GrimoireItem.isOwnedBy(inv.getItem(s), p.getUUID())) inv.setItem(s, GrimoireItem.create(p, cover, GrimoirePages.magicOf(g), null));
        }
        p.displayClientMessage(Component.literal("Your grimoire's cover changes: " + cover.displayName()).withStyle(ChatFormatting.GRAY), false);
    }

    static void crackDiamond(ServerPlayer p, double chance) {
        GrimoirePages.grimoireOf(p).ifPresent(g -> {
            GrimoireCover c = GrimoirePages.coverOf(g);
            if ((c == GrimoireCover.DIAMOND || c == GrimoireCover.FIVE_SIDED) && p.getRandom().nextDouble() < chance) setCover(p, g, GrimoireCover.CRACKED_DIAMOND);
        });
    }
}
