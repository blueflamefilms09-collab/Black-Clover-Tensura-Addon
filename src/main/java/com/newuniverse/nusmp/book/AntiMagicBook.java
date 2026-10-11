package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.ModeArmor;
import com.newuniverse.nusmp.item.MagicWeaponItem;
import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Anti-Magic (Liebe / Asta), 0.48: one skill. The Anti-Magic Spirit Lord used to be a second Unique skill that showed up as its own
 * ability; it is now a progression state of this grimoire (the owner's fix), and old Spirit Lords are folded in on their next login
 * (their Anti-Magic Power and awakening carry over, the separate skill is removed).
 * <ul>
 *   <li><b>From the start:</b> the Demon-Slayer Sword (Genesis-grade, see item.DemonSlayerSwordItem); Anti-Magic Power (AMP) gathers
 *       from magic that hits you, demon-sword hits and slowly over time (stored on this grimoire).</li>
 *   <li><b>Mastered - the Anti-Magic Lord awakens</b> (or an admin's {@code /multiverse grimoire awaken_anti}): physique (+15% attack,
 *       knockback resistance, 30% less magic damage, absorbed as AMP, half fall damage), Demon-Dweller, Demon-Destroyer and
 *       Demon-Slasher, and the Black Asta page becomes the sustained Black Form toggle (no magic reaches you, +25% damage, speed;
 *       drains 25 AMP a second).</li>
 * </ul>
 * Page ids and order are those of the old Anti-Magic book, so unlocks and mastery carry over. No magicule costs.
 */
public class AntiMagicBook extends ElementBook {
    static final String K_LORD = "Lord", K_FORM = "BlackForm";
    private static final ResourceLocation PHYS_DMG = ResourceLocation.fromNamespaceAndPath("nusmp", "anti_magic_physique");
    private static final ResourceLocation PHYS_KB = ResourceLocation.fromNamespaceAndPath("nusmp", "anti_magic_knockback");
    private static final ResourceLocation FORM_DMG = ResourceLocation.fromNamespaceAndPath("nusmp", "black_form");

    public AntiMagicBook() {
        super(MagicType.ANTI_MAGIC, 0xFF2A0A30, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                new BookPage("demon_slayer_slash", "Black Slash", "Demon-Dweller Sword: Black Slash", 0, 0, 80, CanonSpells::blackSlash)
                        .withWeapon(com.newuniverse.nusmp.item.WeaponMagicHelper.GREATSWORDS),
                new BookPage("black_divider", "Black Divider", "Demon-Slayer Sword: Black Divider", 0, 0, 160, CanonSpells::blackDivider)
                        .withWeapon(com.newuniverse.nusmp.item.WeaponMagicHelper.GREATSWORDS),
                new BookPage("black_meteorite", "Black Meteorite", "Black Meteorite", 0, 0, 300, CanonSpells::blackMeteorite),
                new BookPage("black_hurricane", "Black Hurricane", "Black Hurricane", 0, 0, 300, CanonSpells::blackHurricane),
                new BookPage("black_form", "Black Asta", "Black Asta", 0, 0, 0, AntiMagicBook::blackForm),
                new BookPage("bull_thrust", "Bull Thrust", "Bull Thrust", 0, 0, 160, CanonSpells::bullThrust)
                        .withWeapon(com.newuniverse.nusmp.item.WeaponMagicHelper.GREATSWORDS),
                new BookPage("infinite_slash", "Infinite Slash", "Demon-Slasher Katana: Infinite Slash", 0, 0, 240, CanonSpells::infiniteSlash)
                        .withWeapon(com.newuniverse.nusmp.item.WeaponMagicHelper.KATANAS),
                new BookPage("infinite_slash_equinox", "Infinite Slash Equinox", "Demon-Slasher Katana: Infinite Slash Equinox", 0, 0, 900, CanonSpells::infiniteSlashEquinox)
                        .withWeapon(com.newuniverse.nusmp.item.WeaponMagicHelper.KATANAS)));
    }

    public static boolean isLord(ManasSkillInstance i) { return i.getOrCreateTag().getBoolean(K_LORD); }
    public static boolean inBlackForm(ManasSkillInstance i) { return i.getOrCreateTag().getBoolean(K_FORM); }

    /** Awakens the Anti-Magic Lord on this grimoire (mastery, an admin, or an old Spirit Lord folded in). */
    public static void awaken(ServerPlayer p, ManasSkillInstance i, boolean announce) {
        if (isLord(i)) return;
        i.getOrCreateTag().putBoolean(K_LORD, true);
        i.markDirty();
        List<ItemStack> drawn = new java.util.ArrayList<>();
        for (Item sword : List.of(NUItems.DEMON_DWELLER.get(), NUItems.DEMON_DESTROYER.get(), NUItems.DEMON_SLASHER_KATANA.get()))
            if (!owns(p, sword)) drawn.add(MagicWeaponItem.bound(sword, p));
        if (!drawn.isEmpty()) com.newuniverse.nusmp.anim.SwordDraw.draw(p, com.newuniverse.nusmp.anim.SwordDraw.ANTI_MAGIC, drawn.toArray(new ItemStack[0]));   // 0.52
        if (announce) {
            p.sendSystemMessage(Component.literal("Your grimoire's devil stirs: you have become the Anti-Magic Lord.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD));
            p.sendSystemMessage(Component.literal("Demon-Dweller, Demon-Destroyer and Demon-Slasher answer you. Black Asta is now your Black Form.").withStyle(ChatFormatting.GRAY));
        }
    }

    static boolean owns(ServerPlayer p, Item item) {
        for (ItemStack s : p.getInventory().items) if (s.is(item) && MagicWeaponItem.usableBy(s, p)) return true;
        for (ItemStack s : p.getInventory().offhand) if (s.is(item) && MagicWeaponItem.usableBy(s, p)) return true;
        return false;
    }

    @Override
    public void onLearnSkill(ManasSkillInstance i, LivingEntity e) {
        super.onLearnSkill(i, e);
        if (e instanceof ServerPlayer p && !owns(p, NUItems.DEMON_SLAYER.get())) {
            com.newuniverse.nusmp.anim.SwordDraw.draw(p, com.newuniverse.nusmp.anim.SwordDraw.ANTI_MAGIC, MagicWeaponItem.bound(NUItems.DEMON_SLAYER.get(), p));   // 0.52
            p.sendSystemMessage(Component.literal("Your grimoire opens - a demon's sword falls into your hand.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD));
        }
    }

    @Override
    public void onSkillMastered(ManasSkillInstance i, LivingEntity e) {
        super.onSkillMastered(i, e);
        if (e instanceof ServerPlayer p) awaken(p, i, true);
    }

    @Override
    public void onForgetSkill(ManasSkillInstance i, LivingEntity e) {
        super.onForgetSkill(i, e);
        physique(e, false);
        modifier(e, Attributes.ATTACK_DAMAGE, FORM_DMG, 0, false);
        if (e instanceof ServerPlayer p && inBlackForm(i)) ModeArmor.stop(p, ModeArmor.Mode.DEMON);
    }

    // ---------------------------------------------------------------- Black Asta / Black Form
    /**
     * Black Asta. Before the Lord awakens: the timed form (30 s, three times a day). Once awakened: Black Form, a toggle held by
     * Anti-Magic Power (needs 100 AMP to start; recast to end).
     */
    static boolean blackForm(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!isLord(i)) {
            if (!CanonSpells.blackAsta(b, i, p, mode)) return false;
            i.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds((int) (1200 * com.newuniverse.nusmp.NUGameRules.spellCooldown(p.level()))), mode);
            return true;
        }
        if (inBlackForm(i)) { setForm(p, i, false); return true; }
        if (AntiMagic.amp(i) < 100 && !i.isMastered(p)) {
            fail(p, "Not enough Anti-Magic Power (" + AntiMagic.amp(i) + "/100).");
            return false;
        }
        setForm(p, i, true);
        return true;
    }

    public static void setForm(ServerPlayer p, ManasSkillInstance i, boolean on) {
        i.getOrCreateTag().putBoolean(K_FORM, on);
        i.markDirty();
        if (on) {
            p.displayClientMessage(Component.literal("Black Form.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD), true);
            ModeArmor.start(p, ModeArmor.Mode.DEMON, 45);
            VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SPIRIT_AURA, p, p.position().add(0, 1, 0), 0xFF1A1018, 60, 1.4f);
            VfxSpawn.send(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, p.getEyePosition(), p.getEyePosition().add(p.getViewVector(1f).scale(3)), 0xFF2A0A30, 18, 2.0f);
        } else {
            modifier(p, Attributes.ATTACK_DAMAGE, FORM_DMG, 0, false);
            ModeArmor.stop(p, ModeArmor.Mode.DEMON);
        }
    }

    // ---------------------------------------------------------------- the Lord's body
    private static void modifier(LivingEntity e, Holder<Attribute> a, ResourceLocation id, double v, boolean on) {
        var inst = e.getAttribute(a);
        if (inst == null) return;
        if (on && !inst.hasModifier(id)) inst.addTransientModifier(new AttributeModifier(id, v, a == Attributes.KNOCKBACK_RESISTANCE
                ? AttributeModifier.Operation.ADD_VALUE : AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if (!on && inst.hasModifier(id)) inst.removeModifier(id);
    }

    static void physique(LivingEntity e, boolean on) {
        modifier(e, Attributes.ATTACK_DAMAGE, PHYS_DMG, 0.15, on);
        modifier(e, Attributes.KNOCKBACK_RESISTANCE, PHYS_KB, 0.3, on);
    }

    @Override
    protected void tickBook(ManasSkillInstance i, ServerPlayer p) {
        super.tickBook(i, p);
        boolean lord = isLord(i);
        physique(p, lord);
        if (!inBlackForm(i) || !lord) {
            if (inBlackForm(i)) setForm(p, i, false);
            AntiMagic.setAmp(i, AntiMagic.amp(i) + 5);                       // AMP gathers slowly while the form rests
            return;
        }
        int amp = AntiMagic.amp(i) - 25;
        if (amp <= 0) {
            AntiMagic.setAmp(i, 0);
            setForm(p, i, false);
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            p.displayClientMessage(Component.literal("Your Anti-Magic runs dry. Black Form ends.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        AntiMagic.setAmp(i, amp);
        if (ModeArmor.active(p) == ModeArmor.Mode.DEMON || ModeArmor.active(p) == ModeArmor.Mode.NONE) ModeArmor.start(p, ModeArmor.Mode.DEMON, 45);
        modifier(p, Attributes.ATTACK_DAMAGE, FORM_DMG, 0.25, true);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
        if (p.tickCount % 60 < 20) VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SPIRIT_AURA, p, p.position().add(0, 1, 0), 0xFF1A1018, 62, 1.4f);
        p.displayClientMessage(Component.literal("Black Form - AMP " + amp + "/" + AntiMagic.MAX_AMP).withStyle(ChatFormatting.DARK_GRAY), true);
    }

    /** Black Form: magic does not reach you at all (it feeds your AMP). */
    @Override
    public boolean onBeingDamaged(ManasSkillInstance i, LivingEntity e, DamageSource s, float amount) {
        if (isLord(i) && inBlackForm(i) && AntiMagic.isMagic(s)) {
            AntiMagic.setAmp(i, AntiMagic.amp(i) + (int) (amount * 5));
            return false;
        }
        return super.onBeingDamaged(i, e, s, amount);
    }

    /** The Lord's physique: 30% less magic damage (absorbed as AMP), half fall damage. AMP gathers from magic hits either way. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource s, Changeable<Float> amount) {
        boolean r = super.onTakenDamage(i, owner, s, amount);
        if (AntiMagic.isMagic(s)) {
            AntiMagic.setAmp(i, AntiMagic.amp(i) + (int) (amount.get() * 5));
            if (isLord(i)) amount.set(amount.get() * 0.7f);
        }
        if (isLord(i) && s.is(DamageTypeTags.IS_FALL)) amount.set(amount.get() * 0.5f);
        return r;
    }
}
