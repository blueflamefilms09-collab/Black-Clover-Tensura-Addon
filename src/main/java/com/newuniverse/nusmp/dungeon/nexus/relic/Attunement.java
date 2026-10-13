package com.newuniverse.nusmp.dungeon.nexus.relic;

import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Attunement (docs/living_dungeon.md section 7): a relic is held in the main hand while its holder sneaks, ten seconds without a break and
 * standing still ({@link AttuneClock}: a blow, or a step of more than a quarter block, starts the ten seconds over), and then its stack
 * carries the player's id (the {@code attuned_to} component) and a bond id that is also written
 * in the player's own list ({@link Bonds}, saved in the player's persistent data). Both must agree, so another player's relic is
 * unattuned for you, and a relic whose bond was released is unattuned for its old owner too. Three at once; a fourth needs one released
 * (sneak-use it, drop it, or the release command). Death breaks every attunement (unless the inventory is kept).
 */
public final class Attunement {
    private Attunement() {}

    private static final String KEY = "nusmp_relics", BONDS = "Bonds";
    private static final Map<UUID, AttuneClock> CLOCKS = new HashMap<>();
    private static final Set<UUID> HURT = new HashSet<>();

    // ---------------------------------------------------------------- the player's list of bonds
    /** The relics' own corner of the player's persistent data (the bonds, the Wild Surge's expiry); null if there is none and {@code create} is false. */
    static CompoundTag data(Player p, boolean create) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            if (!create) return null;
            pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(KEY, Tag.TAG_COMPOUND)) {
            if (!create) return null;
            root.put(KEY, new CompoundTag());
        }
        return root.getCompound(KEY);
    }

    /** The bond ids this player holds, oldest first. */
    public static List<String> bonds(Player p) {
        CompoundTag d = data(p, false);
        if (d == null) return List.of();
        ListTag list = d.getList(BONDS, Tag.TAG_STRING);
        List<String> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    private static void setBonds(Player p, List<String> bonds) {
        ListTag list = new ListTag();
        for (String b : bonds) list.add(StringTag.valueOf(b));
        data(p, true).put(BONDS, list);
    }

    // ---------------------------------------------------------------- what a stack says
    /** Does the stack name this player as the one it is attuned to? (All a client can tell.) */
    public static boolean attunedToMe(ItemStack stack, Player p) {
        UUID to = stack.get(RelicComponents.ATTUNED_TO.get());
        return to != null && to.equals(p.getUUID());
    }

    /** Is this relic attuned to this player: it names them, and (on the server) the bond it carries is still in their list? */
    public static boolean isAttuned(ItemStack stack, Player p) {
        if (!(stack.getItem() instanceof RelicItem) || !attunedToMe(stack, p)) return false;
        if (p.level().isClientSide()) return true;
        UUID bond = stack.get(RelicComponents.BOND.get());
        return bond != null && Bonds.has(bonds(p), bond.toString());
    }

    /** The bonds of the relics this player is carrying (inventory, armour, off-hand and Curios slots) that are attuned to them. */
    static Set<String> carried(Player p) {
        Set<String> out = new HashSet<>();
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) addBond(out, inv.getItem(i), p);
        for (ItemStack s : CuriosLink.equipped(p)) addBond(out, s, p);
        return out;
    }

    private static void addBond(Set<String> out, ItemStack s, Player p) {
        if (!(s.getItem() instanceof RelicItem) || !attunedToMe(s, p)) return;
        UUID bond = s.get(RelicComponents.BOND.get());
        if (bond != null) out.add(bond.toString());
    }

    /** How many relics this player has attuned and still carries. */
    public static int count(Player p) {
        Set<String> carried = carried(p);
        int n = 0;
        for (String b : bonds(p)) if (carried.contains(b)) n++;
        return n;
    }

    // ---------------------------------------------------------------- attuning and releasing
    /**
     * Binds this relic to the player. FULL if three are already held (after letting go of bonds whose relic is no longer carried); ALREADY if
     * it was attuned to them; OK when it is done.
     */
    public static Bonds.Verdict attune(ServerPlayer p, ItemStack stack) {
        Relic relic = Relic.of(stack);
        if (relic == null) return Bonds.Verdict.FULL;
        if (isAttuned(stack, p)) return Bonds.Verdict.ALREADY;
        List<String> bonds = bonds(p);
        UUID old = stack.get(RelicComponents.BOND.get());
        if (old != null) bonds = Bonds.remove(bonds, old.toString());              // a lapsed bond of this very relic is replaced
        if (Bonds.free(bonds) == 0) bonds = Bonds.prune(bonds, carried(p));       // a relic lost on the way gives its slot back
        if (Bonds.free(bonds) == 0) {
            setBonds(p, bonds);
            return Bonds.Verdict.FULL;
        }
        UUID bond = UUID.randomUUID();
        stack.set(RelicComponents.ATTUNED_TO.get(), p.getUUID());
        stack.set(RelicComponents.ATTUNED_NAME.get(), p.getGameProfile().getName());
        stack.set(RelicComponents.BOND.get(), bond);
        if (relic == Relic.GLUTTONS_CHALICE) {                                     // the Chalice is fed the moment it is bound
            stack.set(RelicComponents.FED_AT.get(), p.level().getGameTime());
            stack.set(RelicComponents.HUNGER.get(), 0);
        }
        if (relic == Relic.DEVIL_BOUND_SHEATH) stack.set(RelicComponents.FULL_FOR.get(), 0);
        setBonds(p, Bonds.add(bonds, bond.toString()));
        Worn.forget(p.getUUID());
        return Bonds.Verdict.OK;
    }

    /** Lets go of this relic: its bond leaves the list and the stack is unattuned again. */
    public static void release(ServerPlayer p, ItemStack stack) {
        UUID bond = stack.get(RelicComponents.BOND.get());
        if (bond != null) setBonds(p, Bonds.remove(bonds(p), bond.toString()));
        stack.remove(RelicComponents.ATTUNED_TO.get());
        stack.remove(RelicComponents.ATTUNED_NAME.get());
        stack.remove(RelicComponents.BOND.get());
        Worn.forget(p.getUUID());
    }

    /** Lets go of the oldest bond (its relic, if the player still carries it, is unattuned by that). False if there was none. */
    public static boolean releaseOldest(ServerPlayer p) {
        List<String> bonds = bonds(p);
        String oldest = Bonds.oldest(bonds);
        if (oldest == null) return false;
        setBonds(p, Bonds.remove(bonds, oldest));
        Worn.forget(p.getUUID());
        return true;
    }

    /** Lets go of every attunement. Returns how many bonds were held. */
    public static int releaseAll(ServerPlayer p) {
        int n = bonds(p).size();
        setBonds(p, List.of());
        Worn.forget(p.getUUID());
        return n;
    }

    // ---------------------------------------------------------------- the ten seconds
    /** Called every tick for every player. */
    static void tick(ServerPlayer p) {
        UUID id = p.getUUID();
        AttuneClock clock = CLOCKS.get(id);
        ItemStack held = p.getMainHandItem();
        Relic r = Relic.of(held);
        boolean hurt = HURT.remove(id);
        if (clock == null) {
            if (r == null || !p.isShiftKeyDown()) return;                          // nothing to time: no clock is made for a player who never attunes
            clock = new AttuneClock();
            CLOCKS.put(id, clock);
        }
        int before = clock.progress();
        int ordinal = r != null && !p.isSpectator() && !isAttuned(held, p) ? r.ordinal() : -1;
        switch (clock.tick(p.isShiftKeyDown(), p.getInventory().selected, ordinal, hurt, p.getX(), p.getZ())) {
            case RUNNING -> {
                if (clock.progress() % 4 == 1) {
                    p.displayClientMessage(Component.literal("Attuning to " + r.label + "  " + AttuneClock.bar(clock.fraction(), 20) + "  " + clock.secondsLeft() + " s  -  stand still")
                            .withStyle(ChatFormatting.LIGHT_PURPLE), true);
                }
            }
            case DONE -> finish(p, held, r);
            case RESET -> {
                if (before >= 20) p.displayClientMessage(Component.literal(RelicText.reset(clock.why())).withStyle(ChatFormatting.GRAY), true);
            }
            default -> {
                if (before == 0) CLOCKS.remove(id);
            }
        }
    }

    private static void finish(ServerPlayer p, ItemStack held, Relic r) {
        ServerLevel level = p.serverLevel();
        switch (attune(p, held)) {
            case OK -> {
                int n = count(p);
                p.displayClientMessage(Component.literal("Attuned: " + r.label + " is bound to you (" + n + " of " + Bonds.CAP + " attunements).")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
                level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.6f, 0.8f);
                level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.9f, 1.4f);
                VfxSpawn.sendFollowing(level, VfxShape.MAGIC_CIRCLE, p, p.position().add(0, 0.08, 0), 0xFFB48CFF, 40, 1.6f);
            }
            case FULL -> p.displayClientMessage(Component.literal("You can keep only " + Bonds.CAP + " relics attuned. Release one first: sneak-use it, drop it, or use the release command.")
                    .withStyle(ChatFormatting.RED), true);
            case ALREADY -> { }
        }
    }

    // ---------------------------------------------------------------- events
    /** A blow lands on a player: an attunement in progress starts over. */
    static void onHurt(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getAmount() > 0 && CLOCKS.containsKey(p.getUUID())) HURT.add(p.getUUID());
    }

    /**
     * An attuned relic that appears in the world as a floating item has been dropped (or its owner died): it lets go of its owner, and the
     * owner, if they are on the server, gets the slot back.
     */
    static void onItemJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ItemEntity item)) return;
        ItemStack s = item.getItem();
        if (!(s.getItem() instanceof RelicItem)) return;
        UUID to = s.get(RelicComponents.ATTUNED_TO.get());
        if (to == null) return;
        UUID bond = s.get(RelicComponents.BOND.get());
        if (e.getLevel() instanceof ServerLevel level && level.getServer() != null) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(to);
            if (owner != null) {
                if (bond != null) setBonds(owner, Bonds.remove(bonds(owner), bond.toString()));
                Worn.forget(to);
                owner.displayClientMessage(Component.literal("You let go of " + s.getHoverName().getString() + ": it is unattuned.").withStyle(ChatFormatting.GRAY), true);
            }
        }
        s.remove(RelicComponents.ATTUNED_TO.get());
        s.remove(RelicComponents.ATTUNED_NAME.get());
        s.remove(RelicComponents.BOND.get());
    }

    /** Death breaks every attunement (the relics drop with their owner); with the inventory kept, they stay. */
    static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (!p.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) setBonds(p, List.of());
        forget(p.getUUID());
    }

    /** The bonds go to the new body of a player who came back from the End or kept their inventory; after a plain death they are empty. */
    static void onClone(PlayerEvent.Clone e) {
        Player from = e.getOriginal(), to = e.getEntity();
        boolean keep = !e.isWasDeath() || (to.level() instanceof ServerLevel sl && sl.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY));
        if (to instanceof ServerPlayer) setBonds(to, keep ? bonds(from) : List.of());
        forget(to.getUUID());
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { forget(e.getEntity().getUUID()); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { forget(e.getEntity().getUUID()); }

    private static void forget(UUID id) {
        CLOCKS.remove(id);
        HURT.remove(id);
        Worn.forget(id);
        RelicState.clear(id);
    }

    static void clearAll() {
        CLOCKS.clear();
        HURT.clear();
        Worn.clear();
        RelicState.clearAll();
    }
}
