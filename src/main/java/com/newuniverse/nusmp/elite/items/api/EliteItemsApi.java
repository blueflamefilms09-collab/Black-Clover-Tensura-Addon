package com.newuniverse.nusmp.elite.items.api;

import com.newuniverse.nusmp.elite.items.EliteComponents;
import com.newuniverse.nusmp.elite.items.crate.RewardSerializers;
import com.newuniverse.nusmp.elite.items.crate.RewardType;
import com.newuniverse.nusmp.elite.items.plush.PlushCollection;
import com.newuniverse.nusmp.elite.items.plush.PlushKinds;
import com.newuniverse.nusmp.elite.items.weapon.AbilityTables;
import com.newuniverse.nusmp.elite.items.weapon.WeaponAbilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiPredicate;

/**
 * Why: the one small door through which other elite builders reach this subsystem, without importing any of its other classes. Everything here is safe to call at any time:
 * <ul>
 *   <li>stack data: {@link #eliteRef} / {@link #chronicleId} (and the setters) for the Walpurgis Orb and Seal and the Chronicle Book;</li>
 *   <li>the plushie collection: {@link #plushCount}, {@link #plushTotal}, {@link #hasPlush}; the events {@link PlushCollectedEvent} and {@link CrateOpenedEvent} are posted on {@code NeoForge.EVENT_BUS};</li>
 *   <li>crate rewards: {@link #registerRewardType} adds a reward type that the crate tables can name;</li>
 *   <li>weapon abilities: {@link #abilitiesOf} reads the data-driven technique file of a weapon by its item path, and {@link #skillCheck} is the late-bound predicate the Void Edge uses to ask "does
 *       this player have the paired skill": the skills builder sets it once at init (default: nobody has it, the sword is simply not boosted).</li>
 * </ul>
 */
public final class EliteItemsApi {
    private EliteItemsApi() {}

    /** Set by the owner of the skills (elite/skills): does the player have the skill with this id ({@code nusmp:void_edge})? Default: no. */
    public static volatile BiPredicate<ServerPlayer, String> skillCheck = (p, skill) -> false;

    /** Does the player have the skill? Never throws; a failing predicate counts as "no". */
    public static boolean hasSkill(ServerPlayer p, String skillId) {
        if (p == null || skillId == null || skillId.isEmpty()) return false;
        try {
            return skillCheck.test(p, skillId);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public static String eliteRef(ItemStack stack) { return EliteComponents.eliteRef(stack); }

    public static void setEliteRef(ItemStack stack, String ref) { EliteComponents.setEliteRef(stack, ref); }

    public static String chronicleId(ItemStack stack) { return EliteComponents.chronicleId(stack); }

    public static void setChronicleId(ItemStack stack, String id) { EliteComponents.setChronicleId(stack, id); }

    /** How many different plushies the player has collected. */
    public static int plushCount(ServerPlayer p) { return PlushCollection.count(p); }

    /** How many plushies exist. */
    public static int plushTotal() { return PlushKinds.ALL.size(); }

    /** Has the player collected the plushie of this kind id ({@code warden})? */
    public static boolean hasPlush(ServerPlayer p, String kindId) { return PlushCollection.has(p, kindId); }

    /** Adds a crate reward type; a crate table can then name it in a {@code "type"} field. */
    public static void registerRewardType(RewardType type) { RewardSerializers.register(type); }

    /** The data-driven techniques of a weapon by its item path ({@code astral_edge}), or null. */
    public static WeaponAbilities abilitiesOf(String itemPath) { return AbilityTables.of(itemPath); }
}
