package com.newuniverse.nusmp.blackclover;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/**
 * Grimoire Acceptance: once TR Nightmare has given a player a soul type, they get ONE roll.
 * Default odds: 1% five-leaf, 2% four-leaf, 10% three-leaf, otherwise no grimoire.
 */
public final class GrimoireAcceptance {
    private static final String KEY = "nusmp_grimoire";

    private GrimoireAcceptance() {}

    // ---- per-player record (survives death) ----
    private static CompoundTag persisted(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static boolean hasRolled(Player p) { return persisted(p).getBoolean(KEY + "_rolled"); }

    public static void markRolled(Player p, boolean rolled) {
        CompoundTag t = persisted(p);
        t.putBoolean(KEY + "_rolled", rolled);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, t);
    }

    // ---- automatic roll once a soul type exists ----
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 100 != 0) return;
        if (!NUConfig.GRIMOIRE_ENABLED.get()) return;
        if (hasRolled(player)) {
            if (player.tickCount % 400 == 0) GrimoirePages.migrateStarter(player);
            return;
        }
        // Grimoires now choose mages at the Acceptance Ceremony (multiverse.Ceremony); the old soul-type roll is kept behind a switch.
        if (!com.newuniverse.nusmp.multiverse.MultiverseConfig.get(com.newuniverse.nusmp.multiverse.MultiverseConfig.LEGACY_SOUL_AUTO_ROLL)) return;
        String soul = NightmareSouls.soulTypeOf(player);
        if (soul == null) return; // Nightmare hasn't assigned a soul type yet
        roll(player, soul);
    }

    /** Rolls leaves for the player and, on success, grants the grimoire. Returns leaves (0 = none). */
    public static int roll(ServerPlayer player, String soul) {
        markRolled(player, true);
        RandomSource r = player.getRandom();
        double x = r.nextDouble() * 100.0;
        double five = NUConfig.FIVE_LEAF_CHANCE.get(), four = NUConfig.FOUR_LEAF_CHANCE.get(), three = NUConfig.THREE_LEAF_CHANCE.get();
        int leaves = x < five ? 5 : x < five + four ? 4 : x < five + four + three ? 3 : 0;
        // Balance law: a devil cover is never a starting roll (only despair or a devil contract).
        if (leaves == 5 && !NUConfig.ALLOW_FORBIDDEN_START.get()) leaves = 4;

        if (leaves == 0) {
            player.displayClientMessage(Component.literal("You climbed the Grimoire Tower... but no grimoire answered your call.")
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        grant(player, leaves, soul);
        return leaves;
    }

    /** Gives a grimoire with the chosen leaf count, picking magic from the soul type. */
    public static void grant(ServerPlayer player, int leaves, String soul) {
        RandomSource r = player.getRandom();
        Kingdom kingdom = rollKingdom(r);
        // Devil covers only exist for Clover and Spade; a forbidden roll elsewhere is accepted by Clover.
        if (leaves >= 5 && (kingdom == Kingdom.HEART || kingdom == Kingdom.DIAMOND)) kingdom = Kingdom.CLOVER;
        GrimoireCover cover = kingdom.coverFor(leaves);
        MagicType magic;
        Devil devil = null;
        if (leaves >= 5 && "EMPTY".equals(soul)) {
            magic = MagicType.ANTI_MAGIC;
            devil = Devil.LIEBE;
        } else {
            List<MagicType> pool = starterPool(soul);
            magic = pool.get(r.nextInt(pool.size()));
            if (leaves >= 5) devil = randomDevil(r);
        }

        grantExact(player, cover, magic, devil);
    }

    /**
     * The attributes a grimoire can choose (config starterMagics, default Fire / Water / Wind / Earth), narrowed to the player's soul
     * family when that leaves any (a Water soul gets Water). Anti-Magic is never in here: it comes with the Black Magic cover.
     */
    public static List<MagicType> starterPool(String soul) {
        List<MagicType> starters = new java.util.ArrayList<>();
        for (String s : com.newuniverse.nusmp.multiverse.MultiverseConfig.get(com.newuniverse.nusmp.multiverse.MultiverseConfig.STARTER_MAGICS)) {
            try { MagicType t = MagicType.valueOf(s.trim().toUpperCase()); if (t != MagicType.ANTI_MAGIC && !starters.contains(t)) starters.add(t); }
            catch (IllegalArgumentException ignored) {}
        }
        if (starters.isEmpty()) starters.addAll(List.of(MagicType.FLAME, MagicType.WATER, MagicType.WIND, MagicType.EARTH));
        List<MagicType> soulPool = MagicType.forSoul(soul == null ? NightmareSouls.NO_NIGHTMARE : soul);
        List<MagicType> both = new java.util.ArrayList<>(starters);
        both.retainAll(soulPool);
        return both.isEmpty() || soulPool.size() == MagicType.values().length - 1 ? starters : both;
    }

    /** Weighted kingdom roll (config). */
    public static Kingdom rollKingdom(RandomSource r) {
        int[] w = {NUConfig.WEIGHT_CLOVER.get(), NUConfig.WEIGHT_SPADE.get(), NUConfig.WEIGHT_HEART.get(), NUConfig.WEIGHT_DIAMOND.get()};
        int total = Math.max(1, w[0] + w[1] + w[2] + w[3]), x = r.nextInt(total);
        for (int i = 0; i < 4; i++) { if (x < w[i]) return Kingdom.values()[i]; x -= w[i]; }
        return Kingdom.CLOVER;
    }

    /** Old signature (leaf count -> Clover). */
    public static void grantExact(ServerPlayer player, int leaves, MagicType magic, Devil devil) {
        grantExact(player, Kingdom.CLOVER.coverFor(leaves), magic, devil);
    }

    /** Gives exactly this grimoire (used by rolls, admin give, and creative binding). */
    public static void grantExact(ServerPlayer player, GrimoireCover cover, MagicType magic, Devil devil) {
        if (!cover.isForbidden()) devil = null;
        int leaves = cover.tier;
        Skills skills = SkillAPI.getSkillsFrom(player);
        for (var id : NUSkills.allGrimoireSkillIds()) skills.forgetSkill(id);
        ManasSkillInstance inst = NUSkills.grimoireSkillFor(magic).createDefaultInstance();
        CompoundTag tag = inst.getOrCreateTag();
        tag.putInt("Leaves", leaves);
        tag.putString("Cover", cover.name());
        tag.putString("Magic", magic.name());
        if (devil != null) tag.putString("Devil", devil.name());
        skills.learnSkill(inst, Component.literal("A grimoire has chosen you.").withStyle(ChatFormatting.GOLD));

        // Books hand over their own grimoire when learned; only give one here if none matches yet (no duplicates).
        boolean has = false;
        var inv = player.getInventory();
        if (GrimoireSlot.holdsOwn(player, magic)) {                                   // already in the Grimoire Slot: rebuild it there
            String canon = GrimoireItem.data(GrimoireSlot.get(player)).getString("Canon");
            ItemStack fresh = GrimoireItem.create(player, cover, magic, devil);
            if (!canon.isEmpty()) GrimoireItem.setCanon(fresh, canon);
            GrimoireSlot.set(player, fresh);
            has = true;
        }
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            var st = inv.getItem(slot);
            if (!GrimoireItem.isOwnedBy(st, player.getUUID())) continue;
            if (magic.name().equals(GrimoireItem.data(st).getString("Magic"))) {
                String canon = GrimoireItem.data(st).getString("Canon");               // a canon book keeps its look through a re-grant
                ItemStack fresh = GrimoireItem.create(player, cover, magic, devil);
                if (!canon.isEmpty()) GrimoireItem.setCanon(fresh, canon);
                inv.setItem(slot, fresh);
                has = true;
                break;
            }
        }
        if (!has) inv.placeItemBackInInventory(GrimoireItem.create(player, cover, magic, devil));
        markRolled(player, true);
        GrimoirePages.grantStarter(player, magic);

        player.serverLevel().playSound(null, player.blockPosition(),
                leaves >= 5 ? SoundEvents.WITHER_SPAWN : SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
        Component msg = Component.literal("[Grimoire Tower] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(player.getName().getString() + " received a ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(cover.displayName())
                        .withStyle(leaves >= 5 ? ChatFormatting.DARK_RED : leaves == 4 ? ChatFormatting.GOLD : ChatFormatting.GREEN))
                .append(Component.literal(" from the " + cover.kingdom.displayName).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" of " + magic.displayName + (devil != null ? ", inhabited by the devil " + devil.displayName : "") + "!")
                        .withStyle(ChatFormatting.WHITE));
        if (leaves >= 4 || NUConfig.ANNOUNCE_THREE_LEAF.get()) {
            player.getServer().getPlayerList().broadcastSystemMessage(msg, false);
        } else {
            player.displayClientMessage(msg, false);
        }
    }

    public static Devil randomDevil(RandomSource r) {
        Devil[] options = Devil.values();
        Devil d;
        do { d = options[r.nextInt(options.length)]; } while (d == Devil.LIEBE); // Liebe only bonds with Anti-Magic
        return d;
    }
}
