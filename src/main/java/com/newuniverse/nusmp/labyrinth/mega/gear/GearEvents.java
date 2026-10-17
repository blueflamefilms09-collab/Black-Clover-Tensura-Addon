package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.quests.NusmpEvents;
import io.github.manasmods.tensura.data.TensuraTags;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: this is where Rare gear does the thing the owner named it for, "high-grade items providing magic resistance". When any creature is about to take
 * damage, the pieces it WEARS in the four armour positions and the weapon and shield it HOLDS are read for their {@code nusmp:gear_grade} component, their
 * percentages are summed (pure {@link ResistMath}), held to the configured ceiling ({@code maxResist}, 40 by default, never above 90) and, if the damage is
 * magic ({@link #isMagicDamage}), the amount is cut by that share. It is an EVENT and not an attribute for three reasons: vanilla armour does not reduce magic
 * damage at all, so there is no vanilla attribute to lean on; Tensura's own magic-resistance attribute has a scale and a formula that live in its code and
 * cannot be verified here, so a number put in it could be a hundred times too strong or do nothing; and a ceiling over the SUM of several pieces cannot be
 * written as per-item attribute modifiers, while a plain function over the stacks is exact, testable and the same for a player and for a monster that
 * picked the piece up. It listens at LOW priority so it runs after the multipliers of other systems and cuts the final spell damage. The scan allocates
 * nothing (a static array of the six positions) and stops at the first cheap test that fails: the config ceiling, then the equipment, and only a creature
 * that really wears gear pays for the damage-type test. The second duty of the class is the quest hook: the first time a player PICKS UP a piece of each
 * grade the quest key {@code mega.gear} fires with the grade id (counted once per player and grade by {@link NusmpEvents#fireOnce}, which remembers it in the
 * player's persistent data). Never throws out of an event: an exception is logged once and the damage goes through unchanged.
 */
public final class GearEvents {
    private GearEvents() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    /** The quest key of the first piece of each grade a player receives (argument: the grade id). */
    public static final String QUEST_KEY = "mega.gear";

    /** The six positions that count: four armour slots, then the two hands. A static array so the scan allocates nothing. */
    private static final EquipmentSlot[] POSITIONS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    /** For each position, the armour index {@link ResistMath#counts} wants (negative for a hand). */
    private static final int[] WORN = {0, 1, 2, 3, -1, -1};

    /** The built-in rule's answer per damage-type id: the set of damage types is small and fixed, and the rule splits strings, so it runs once per type. */
    private static final Map<ResourceLocation, Boolean> RULE = new ConcurrentHashMap<>();

    private static volatile String extraRaw = "";
    private static volatile List<String> extraParsed = List.of();

    // ---------------------------------------------------------------- the resistance
    /** The sum of the magic resistance of everything the creature wears and holds that counts, before the ceiling. Zero for a creature with no standard gear. */
    public static int rawResist(LivingEntity v) {
        DataComponentType<GearInfo> type = GearComponents.GEAR.get();
        int sum = 0;
        for (int i = 0; i < POSITIONS.length; i++) {
            ItemStack st = v.getItemBySlot(POSITIONS[i]);
            if (st.isEmpty()) continue;
            GearInfo info = st.get(type);
            if (info == null || info.magicResistPercent() <= 0) continue;
            if (ResistMath.counts(info.slot(), WORN[i])) sum += info.magicResistPercent();
        }
        return sum;
    }

    /** The creature's magic resistance in percent, held to the ceiling. */
    public static int resistPercent(LivingEntity v, int ceiling) {
        return ceiling <= 0 ? 0 : ResistMath.effective(rawResist(v), ceiling);
    }

    /**
     * The amount of this damage after the creature's gear: cut by its resistance when the damage is magic, otherwise returned as it came. The function the
     * event handler and the self-test both call.
     */
    public static float reduced(LivingEntity victim, DamageSource source, float amount, int ceiling) {
        if (amount <= 0f || ceiling <= 0) return amount;
        int percent = resistPercent(victim, ceiling);
        if (percent <= 0 || !isMagicDamage(source)) return amount;
        return ResistMath.reduce(amount, percent);
    }

    /**
     * Is this damage a spell? The pure rule over the damage-type id ({@link MagicDamage}: vanilla magic and indirect magic, Tensura's elemental and spell
     * types), the owner's extra list from the config, and Tensura's own tag of magic fire.
     */
    public static boolean isMagicDamage(DamageSource s) {
        var key = s.typeHolder().unwrapKey();
        if (key.isPresent()) {
            ResourceLocation id = key.get().location();
            if (RULE.computeIfAbsent(id, k -> MagicDamage.isMagicType(k.getNamespace(), k.getPath()))) return true;
            if (MagicDamage.matchesExtra(id.getNamespace(), id.getPath(), extra())) return true;
        }
        return s.is(TensuraTags.DamageTypes.IS_MAGIC_FIRE);
    }

    /** The owner's extra list, parsed once per distinct config string. */
    private static List<String> extra() {
        String raw = GearConfig.get(GearConfig.EXTRA_MAGIC_TYPES);
        if (raw == null || raw.isEmpty()) return List.of();
        if (!raw.equals(extraRaw)) {
            extraParsed = MagicDamage.parseExtra(raw);
            extraRaw = raw;
        }
        return extraParsed;
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        try {
            int ceiling = GearConfig.get(GearConfig.MAX_RESIST);
            if (ceiling <= 0) return;
            float in = e.getAmount();
            float out = reduced(e.getEntity(), e.getSource(), in, ceiling);
            if (out != in) e.setAmount(out);
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] Mega-Dungeon gear resistance failed once and is skipped for that hit", ex);
        }
    }

    // ---------------------------------------------------------------- the quest
    /** The first time a player picks up a piece of a grade: the quest key with the grade id. */
    public static void onPickup(ItemEntityPickupEvent.Post e) {
        try {
            if (!(e.getPlayer() instanceof ServerPlayer player)) return;
            GearInfo info = e.getOriginalStack().get(GearComponents.GEAR.get());
            if (info != null) NusmpEvents.fireOnce(player, QUEST_KEY, info.gradeId());
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] Mega-Dungeon gear quest hook failed once", ex);
        }
    }
}
