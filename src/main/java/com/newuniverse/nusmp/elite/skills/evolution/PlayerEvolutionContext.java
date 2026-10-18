package com.newuniverse.nusmp.elite.skills.evolution;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.elite.skills.EliteUtil;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Why: the in-game {@link EvolutionContext}: it answers the triggers' questions from a real player, the player's persistent data (kills, awakenings) and the late-bound {@code EliteHooks}
 * of the forge, records and titles subsystems (safe defaults while those are not built or are switched off). It is created per check and holds nothing.
 */
public final class PlayerEvolutionContext implements EvolutionContext {
    private final ServerPlayer player;

    public PlayerEvolutionContext(ServerPlayer player) { this.player = player; }

    @Override public UUID id() { return player.getUUID(); }

    private Optional<ManasSkillInstance> skill(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id);
        return rl == null ? Optional.empty() : SkillAPI.getSkillsFrom(player).getSkill(rl);
    }

    @Override public boolean owns(String skillId) { return skill(skillId).isPresent(); }

    @Override public boolean ownsMastered(String skillId) { return skill(skillId).map(i -> i.isMastered(player)).orElse(false); }

    @Override public double ep() { return EliteUtil.ep(player); }

    @Override public int killsWith(String skillId, String filter) { return KillTracker.get(player, KillTracker.withKey(skillId, filter)); }

    @Override public int killsTotal(String filter) { return KillTracker.get(player, KillTracker.totalKey(filter)); }

    // ---------------------------------------------------------------- items
    private static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return null;
        return BuiltInRegistries.ITEM.get(rl);
    }

    private List<ItemStack> carried() {
        List<ItemStack> all = new ArrayList<>(player.getInventory().items);
        all.addAll(player.getInventory().offhand);
        return all;
    }

    @Override
    public int itemCount(String itemId) {
        Item it = item(itemId);
        if (it == null) return 0;
        int n = 0;
        for (ItemStack s : carried()) if (!s.isEmpty() && s.is(it)) n += s.getCount();
        return n;
    }

    @Override
    public boolean consumeItems(String itemId, int count) {
        Item it = item(itemId);
        if (it == null || count <= 0 || itemCount(itemId) < count) return false;
        int left = count;
        for (ItemStack s : carried()) {
            if (left <= 0) break;
            if (s.isEmpty() || !s.is(it)) continue;
            int take = Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
        }
        player.getInventory().setChanged();
        return left <= 0;
    }

    // ---------------------------------------------------------------- awakening
    @Override
    public boolean awakened(String kind) {
        CompoundTag data = player.getPersistentData();
        if (kind == null || kind.isEmpty() || "any".equals(kind)) {
            for (String k : new String[]{"any", "demon_lord", "hero"}) if (data.getBoolean(AwakeningEvolution.KEY_PREFIX + k)) return true;
            return false;
        }
        return data.getBoolean(AwakeningEvolution.KEY_PREFIX + kind);
    }

    /** Marks the player as awakened of this kind (called by the awakening system when it exists, or by an admin / test). */
    public static void setAwakened(ServerPlayer player, String kind, boolean value) {
        player.getPersistentData().putBoolean(AwakeningEvolution.KEY_PREFIX + kind, value);
    }

    // ---------------------------------------------------------------- late-bound hooks
    @Override public int forgeCrafted() { return EliteHooks.forgeCraftedCount.applyAsInt(player.getUUID()); }

    @Override public int forgeQuality() { return EliteHooks.forgeBestQuality.applyAsInt(player.getUUID()); }

    @Override public boolean record(String record) { return EliteHooks.recordUnlocked.test(player.getUUID(), record); }

    @Override public boolean hasTitle(String title) { return EliteHooks.hasTitle.test(player.getUUID(), title); }

    @Override public String activeTitle() { String t = EliteHooks.activeTitle.apply(player.getUUID()); return t == null ? "" : t; }

    @Override public int heldTitles() { return EliteHooks.heldTitleCount.applyAsInt(player.getUUID()); }

    @Override public int resets() { return EliteHooks.resetCount.applyAsInt(player.getUUID()); }
}
