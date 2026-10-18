package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.elite.skills.rules.UltimateRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Hephaestus, Forgefather (Ultimate, id {@code hephaestus_ultimate}; designed for this mod; the original addon's source was not available). The mod ALREADY has Hephaestus as a Unique (the
 * DanMachi goddess of the forge, id {@code hephaestus}, which repairs the item in your hand): that skill is kept untouched and this Ultimate is its evolution target in
 * data/nusmp/elite/skills/evolutions.json (mastered Hephaestus + 50 forged items + an Epic forge + an offering of netherite). Mode 1, Reforge: every damageable item you carry (hotbar, inventory, armour,
 * off-hand) is restored to full. Mode 2, Temper: the weapon in your hand is tempered for ten minutes (twenty mastered): +20% (+30%) melee damage, applied by the damage handler. Mode 3, Molten
 * Anvil: an anvil of the forge falls where you look (24 blocks) and strikes the foes in 3.5 blocks for heavy damage and sets them alight. Numbers: {@link UltimateRules.Hephaestus}.
 */
public class HephaestusSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("hephaestus_ultimate", "Hephaestus, Forgefather",
            "Ultimate. Reforge every tool and piece of armour you carry, Temper the weapon in your hand for more damage, or drop a Molten Anvil on the foes where you look.");

    static final String TEMPER_UNTIL = "nusmp_tempered_until", TEMPER_BONUS = "nusmp_tempered_bonus";

    public HephaestusSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("hephaestus_ultimate.reforge", "Reforge", UltimateRules.Hephaestus.REFORGE_COST, UltimateRules.Hephaestus.REFORGE_COOLDOWN),
                EliteMode.of("hephaestus_ultimate.temper", "Temper", UltimateRules.Hephaestus.TEMPER_COST, UltimateRules.Hephaestus.TEMPER_COOLDOWN),
                EliteMode.of("hephaestus_ultimate.anvil", "Molten Anvil", UltimateRules.Hephaestus.ANVIL_COST, UltimateRules.Hephaestus.ANVIL_COOLDOWN));
    }

    @Override protected int color() { return 0xFFFF8A3A; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        boolean ownMastered = masteredBy(instance, player);
        ServerLevel level = player.serverLevel();
        switch (mode) {
            case 0 -> {
                List<ItemStack> all = new ArrayList<>(player.getInventory().items);
                all.addAll(player.getInventory().armor);
                all.addAll(player.getInventory().offhand);
                int points = 0, stacks = 0;
                for (ItemStack s : all) {
                    if (s.isEmpty() || !s.isDamageableItem() || s.getDamageValue() <= 0) continue;
                    points += UltimateRules.Hephaestus.reforgePoints(s.getDamageValue());
                    s.setDamageValue(0);
                    stacks++;
                }
                if (stacks == 0) {
                    EliteBridge.fail(player, SkillMsg.t("hephaestus_ultimate.nothing", "Nothing you carry needs reforging."));
                    return false;
                }
                EliteBridge.shape(level, VfxShape.ALCHEMY_CIRCLE, player.position().add(0, 0.05, 0), player.position().add(0, 1, 0), 0xFFFF8A3A, 36, 1.4f);
                EliteBridge.sound(level, player.position(), SoundEvents.ANVIL_USE, 1.0f, 0.8f);
                EliteBridge.actionbar(player, SkillMsg.t("hephaestus_ultimate.reforged", "Reforged %s items (%s durability).", stacks, points).withStyle(ChatFormatting.GOLD));
                return true;
            }
            case 1 -> {
                ItemStack weapon = player.getMainHandItem();
                if (weapon.isEmpty()) {
                    EliteBridge.fail(player, SkillMsg.t("hephaestus_ultimate.empty", "Hold the weapon to temper in your main hand."));
                    return false;
                }
                long until = level.getGameTime() + UltimateRules.Hephaestus.temperTicks(ownMastered);
                double bonus = UltimateRules.Hephaestus.temperBonus(ownMastered);
                EliteUtil.editItemTag(weapon, t -> {
                    t.putLong(TEMPER_UNTIL, until);
                    t.putDouble(TEMPER_BONUS, bonus);
                });
                EliteBridge.shape(level, VfxShape.FX_FIRE_ERUPTION, player.position(), player.position().add(0, 1, 0), 0xFFFF8A3A, 24, 0.8f);
                EliteBridge.sound(level, player.position(), SoundEvents.SMITHING_TABLE_USE, 1.0f, 0.7f);
                EliteBridge.actionbar(player, SkillMsg.t("hephaestus_ultimate.tempered", "Your %s is tempered.", weapon.getHoverName()).withStyle(ChatFormatting.GOLD));
                return true;
            }
            default -> {
                Vec3 at = EliteBridge.lookPoint(player, UltimateRules.Hephaestus.ANVIL_RANGE);
                double mastery = masteryFrac(instance);
                int hit = 0;
                for (LivingEntity t : EliteBridge.foes(player, at, UltimateRules.Hephaestus.ANVIL_RADIUS)) {
                    EliteBridge.hit(player, t, UltimateRules.Hephaestus.ANVIL_DAMAGE * EliteSkillsConfig.damageScale(), mastery, false);
                    t.setRemainingFireTicks(80);
                    hit++;
                }
                EliteBridge.shape(level, VfxShape.FX_FIRE_ERUPTION, at, at.add(0, 3, 0), 0xFFFF8A3A, 20, 1.2f);
                EliteBridge.shape(level, VfxShape.ALCHEMY_CIRCLE, at.add(0, 0.05, 0), at.add(0, 1, 0), 0xFFFF8A3A, 20, 1.0f);
                EliteBridge.sound(level, at, SoundEvents.ANVIL_LAND, 1.4f, 0.6f);
                EliteBridge.actionbar(player, SkillMsg.t("hephaestus_ultimate.anvil", "The Molten Anvil falls on %s foes.", hit).withStyle(ChatFormatting.GOLD));
                return true;
            }
        }
    }

    /** The damage multiplier of the weapon in this player's main hand (1.0 unless it is tempered right now). */
    static double temperMultiplier(LivingEntity attacker) {
        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.isEmpty()) return 1.0;
        var t = EliteUtil.itemTag(weapon);
        return UltimateRules.Hephaestus.tempered(attacker.level().getGameTime(), t.getLong(TEMPER_UNTIL)) ? 1.0 + t.getDouble(TEMPER_BONUS) : 1.0;
    }
}
