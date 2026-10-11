package com.newuniverse.nusmp.entity.riven;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.antimagic.NihilityZone;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What Riven can read off one target, from server-known state only: health, armour, held items, effects, this mod's grimoire /
 * anti-magic / Nihility state, and Tensura's learned skills (read through ManasCore, fail-soft). Reduced to {@link #tags} the planner
 * scores skills against. Built in memory and never sent anywhere.
 */
public final class ThreatScan {
    public final Set<String> tags = new LinkedHashSet<>();
    public final List<String> notes = new ArrayList<>();
    public float health, maxHealth, armor, toughness, absorption, distance, heightGap;
    public boolean lineOfSight, mounted, flying, fast;
    public String magic = "";

    public boolean has(String tag) { return tags.contains(tag); }

    public static ThreatScan of(RivenBossEntity boss, LivingEntity t) {
        ThreatScan s = new ThreatScan();
        s.health = t.getHealth();
        s.maxHealth = t.getMaxHealth();
        s.absorption = t.getAbsorptionAmount();
        s.armor = (float) t.getAttributeValue(Attributes.ARMOR);
        s.toughness = (float) t.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        s.distance = (float) boss.distanceTo(t);
        s.heightGap = (float) (t.getY() - boss.getY());
        s.lineOfSight = boss.hasLineOfSight(t);
        s.mounted = t.getVehicle() != null;
        s.flying = t.isFallFlying() || (t instanceof Player p && p.getAbilities().flying) || (!t.onGround() && s.heightGap > 2.5f);
        s.fast = t.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.16 || t.hasEffect(MobEffects.MOVEMENT_SPEED);
        if (s.mounted) s.tags.add("mounted");
        if (s.flying) s.tags.add("flier");
        if (s.fast) s.tags.add("speedster");
        if (s.armor + s.toughness >= 16) s.tags.add("tank");
        if (s.armor + s.toughness <= 4) s.tags.add("low_armor");
        if (s.maxHealth > 0 && s.health / s.maxHealth < 0.35f) s.tags.add("low_health");
        if (t.hasEffect(MobEffects.REGENERATION) || t.hasEffect(MobEffects.ABSORPTION) || s.absorption > 4
                || t.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) s.tags.add("healer");
        if (t.isBlocking()) s.tags.add("barrier");
        if (t.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) s.tags.add("flier");
        scanHeld(s, t.getMainHandItem());
        if (t instanceof Player p) scanPlayer(s, p, boss);
        if (s.has("anti_magic") || s.has("barrier") || s.has("magic_null")) s.tags.add("disbelief");
        return s;
    }

    private static void scanHeld(ThreatScan s, ItemStack main) {
        if (main.isEmpty()) return;
        var item = main.getItem();
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem) s.tags.add("melee");
        if (item instanceof ProjectileWeaponItem) { s.tags.add("kiter"); s.tags.add("ranged"); }
        if (path.startsWith("demon_")) { s.tags.add("anti_magic"); s.notes.add("demon sword"); }
    }

    private static void scanPlayer(ThreatScan s, Player p, RivenBossEntity boss) {
        try {
            if (AntiMagic.isUser(p)) { s.tags.add("anti_magic"); s.notes.add("anti-magic user"); }
            if (p.level() instanceof ServerLevel sl && NihilityZone.inside(sl, p.position())) { s.tags.add("magic_null"); s.tags.add("anti_magic"); s.notes.add("inside Nihility"); }
            GrimoirePages.grimoireOf(p).ifPresent(g -> {
                s.magic = GrimoirePages.magicOf(g).name().toLowerCase(Locale.ROOT);
                if (!s.has("anti_magic")) s.tags.add("caster");
                s.notes.add("grimoire " + s.magic);
            });
        } catch (Throwable ignored) {}
        try {
            for (ManasSkillInstance i : new ArrayList<>(SkillAPI.getSkillsFrom(p).getLearnedSkills())) {
                String id = i.getSkill().getRegistryName() == null ? "" : i.getSkill().getRegistryName().getPath();
                if (id.contains("barrier")) { s.tags.add("barrier"); }
                if (id.contains("physical") && (id.contains("nullification") || id.contains("immun"))) { s.tags.add("physical_immune"); s.notes.add(id); }
                if (id.contains("magic") && (id.contains("nullification") || id.contains("immun"))) { s.tags.add("magic_null"); s.notes.add(id); }
                if (id.contains("spiritual")) s.tags.add("spiritual");
                if (id.contains("fire") && id.contains("resist")) s.tags.add("fire_resist");
                if (id.contains("magic") || id.contains("spell") || id.contains("bolt")) s.tags.add("caster");
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public String toString() { return "tags=" + tags + " notes=" + notes + " dist=" + String.format(Locale.ROOT, "%.1f", distance); }
}
