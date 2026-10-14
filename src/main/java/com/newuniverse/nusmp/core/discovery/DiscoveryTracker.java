package com.newuniverse.nusmp.core.discovery;

import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.GrimoireSlot;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.entity.ElementalSpiritLordEntity;
import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.multiverse.SpiritLordSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The server half of the discovery system: it fills each player's {@link DiscoveryData} and tells the client.
 * <ul>
 *   <li><b>Items and relics:</b> an item of this mod is found the moment it is picked up, crafted, smelted or received; a once-a-second slot scan
 *       (reference comparison, see {@link DiscoveryRules#diffSlots}) catches everything else (a /give, a chest click, creative).</li>
 *   <li><b>Skills, pages, magic:</b> every two seconds the learned skills are read: a skill of the mod, a grimoire's open pages (a bitmask diff
 *       per book) and its magic.</li>
 *   <li><b>Creatures:</b> a creature of the mod is found when the player hurts it, is hurt by it or kills it; an elemental Spirit Lord also
 *       finds its element; a bonded player finds their own spirit.</li>
 *   <li><b>Grandfathering:</b> the first login after the update runs {@link #migrate}: everything the player owns or has unlocked counts as found.</li>
 * </ul>
 * Register once on the game bus with {@link #register}. All state is per player UUID and dropped on logout.
 */
public final class DiscoveryTracker {
    private DiscoveryTracker() {}

    /** The server thread's scratch array for one scan (every slot is overwritten each time). */
    private static Item[] SCRATCH = new Item[0];
    private static final Map<UUID, Item[]> SEEN = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> BOOK_MASKS = new HashMap<>();
    private static final Map<UUID, List<String>> PENDING = new HashMap<>();
    private static final Map<UUID, Boolean> SHOWN_ALL = new HashMap<>();

    public static void register(IEventBus bus) {
        bus.addListener(DiscoveryTracker::onLogin);
        bus.addListener(DiscoveryTracker::onLogout);
        bus.addListener(DiscoveryTracker::onClone);
        bus.addListener(DiscoveryTracker::onRespawn);
        bus.addListener(DiscoveryTracker::onCrafted);
        bus.addListener(DiscoveryTracker::onSmelted);
        bus.addListener(DiscoveryTracker::onPickup);
        bus.addListener(DiscoveryTracker::onTick);
        bus.addListener(DiscoveryTracker::onDamaged);
        bus.addListener(DiscoveryTracker::onDeath);
    }

    // ---------------------------------------------------------------- the questions
    public static DiscoveryData data(Player p) { return p.getData(DiscoveryAttachments.DATA); }

    /** Does this player see everything (the config switch, or creative mode)? */
    public static boolean showAll(Player p) { return DiscoveryConfig.showAll() || p.isCreative(); }

    public static boolean has(Player p, String key) { return key != null && data(p).has(key); }

    /** Has this player found this thing, or do they see everything? */
    public static boolean visibleTo(Player p, String key) { return showAll(p) || has(p, key); }

    /**
     * For a command: the console and operators (permission 2, who can /give anything anyway) see everything, a player sees what they found.
     * A key of null (something that is not tracked) is always visible.
     */
    public static boolean canSee(CommandSourceStack src, String key) {
        if (key == null || src.hasPermission(2) || DiscoveryConfig.showAll()) return true;
        return src.getEntity() instanceof ServerPlayer p ? visibleTo(p, key) : true;
    }

    // ---------------------------------------------------------------- finding
    /** Marks a key as found. True if it was new. The client hears about it on the next tick. */
    public static boolean discover(ServerPlayer p, String key) {
        if (key == null || !data(p).add(key)) return false;
        PENDING.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(key);
        return true;
    }

    public static boolean discoverItem(ServerPlayer p, ItemStack stack) { return discover(p, DiscoveryKeys.item(stack)); }

    public static boolean discoverItem(ServerPlayer p, Item item) { return discover(p, DiscoveryKeys.item(item)); }

    // ---------------------------------------------------------------- events
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        DiscoveryData d = data(p);
        if (DiscoveryRules.needsMigration(d.version())) migrate(p, d);
        PENDING.remove(p.getUUID());
        sendFull(p);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        SEEN.remove(id);
        BOOK_MASKS.remove(id);
        PENDING.remove(id);
        SHOWN_ALL.remove(id);
    }

    /** A new body (death, the End): the found set goes with the player. The attachment copies itself on death; this unions it on every clone. */
    private static void onClone(PlayerEvent.Clone e) {
        if (!(e.getEntity() instanceof ServerPlayer to)) return;
        DiscoveryData into = data(to);
        into.merge(data(e.getOriginal()));
        if (data(e.getOriginal()).version() > into.version()) into.setVersion(data(e.getOriginal()).version());
        SEEN.remove(to.getUUID());
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PENDING.remove(p.getUUID());
        sendFull(p);
    }

    private static void onCrafted(PlayerEvent.ItemCraftedEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) discoverItem(p, e.getCrafting());
    }

    private static void onSmelted(PlayerEvent.ItemSmeltedEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) discoverItem(p, e.getSmelting());
    }

    private static void onPickup(ItemEntityPickupEvent.Post e) {
        if (e.getPlayer() instanceof ServerPlayer p) discoverItem(p, e.getOriginalStack());
    }

    private static void onDamaged(LivingDamageEvent.Post e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide()) return;
        Entity attacker = e.getSource().getEntity();
        if (attacker instanceof ServerPlayer p && !(victim instanceof Player)) meet(p, victim);
        else if (victim instanceof ServerPlayer p && attacker != null && !(attacker instanceof Player)) meet(p, attacker);
    }

    private static void onDeath(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide()) return;
        if (e.getSource().getEntity() instanceof ServerPlayer p && !(e.getEntity() instanceof Player)) meet(p, e.getEntity());
    }

    /** The player has fought this creature: it is found (and, for an elemental Spirit Lord, so is its element). */
    private static void meet(ServerPlayer p, Entity other) {
        discover(p, DiscoveryKeys.mob(other.getType()));
        if (other instanceof ElementalSpiritLordEntity el) discover(p, DiscoveryRules.spiritLordKey(el.kind().name()));
    }

    // ---------------------------------------------------------------- the tick
    private static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        List<String> pending = PENDING.get(p.getUUID());
        if (pending != null && !pending.isEmpty()) {
            DiscoveryNet.sendDelta(p, pending, showAll(p));
            SHOWN_ALL.put(p.getUUID(), showAll(p));
            pending.clear();
        }
        int phase = Math.floorMod(p.getId(), 40);                               // each player scans on their own tick, so the server never does them all at once
        if (p.tickCount % 20 == phase % 20) scanInventory(p);
        if (p.tickCount % 40 == (phase + 11) % 40) {
            scanSkills(p, false);
            for (ItemStack worn : CuriosLink.equipped(p)) discoverItem(p, worn);       // a relic put straight into a Curios slot never passed the inventory
        }
    }

    /** The slot scan: the whole inventory (main, armour, off-hand) and the Grimoire Slot, by reference; only a changed slot costs more. */
    private static void scanInventory(ServerPlayer p) {
        Inventory inv = p.getInventory();
        int n = inv.getContainerSize();
        Item[] now = SCRATCH.length == n + 1 ? SCRATCH : (SCRATCH = new Item[n + 1]);
        for (int i = 0; i < n; i++) {
            ItemStack s = inv.getItem(i);
            now[i] = s.isEmpty() ? null : s.getItem();
        }
        ItemStack slot = GrimoireSlot.get(p);
        now[n] = slot.isEmpty() ? null : slot.getItem();
        Item[] prev = SEEN.get(p.getUUID());
        if (prev == null || prev.length != now.length) {
            prev = new Item[now.length];
            SEEN.put(p.getUUID(), prev);
        }
        DiscoveryRules.diffSlots(prev, now, it -> discoverItem(p, it));
        boolean all = showAll(p);
        Boolean shown = SHOWN_ALL.get(p.getUUID());
        if (shown == null || shown != all) {
            SHOWN_ALL.put(p.getUUID(), all);
            DiscoveryNet.sendState(p, all);
        }
    }

    /** Learned skills of the mod, the pages of every grimoire in them, the magic, and the bonded spirit. */
    private static void scanSkills(ServerPlayer p, boolean full) {
        var skills = SkillAPI.getSkillsFrom(p);
        if (skills != null) {
            for (ManasSkillInstance i : new ArrayList<>(skills.getLearnedSkills())) {
                discover(p, DiscoveryKeys.skill(i.getSkill().getRegistryName()));
                if (i.getSkill() instanceof GrimoireBook book) scanBook(p, i, book, full);
            }
        }
        String spirit = SpiritLordSkill.bondedType(p);
        if (spirit != null && !spirit.isEmpty() && !"-".equals(spirit)) discover(p, DiscoveryRules.spiritLordKey(spirit));
    }

    /** A book's pages: open ones by the unlock bitmask (diffed per book), the always-open base pages by their own rule. */
    private static void scanBook(ServerPlayer p, ManasSkillInstance inst, GrimoireBook book, boolean full) {
        discover(p, DiscoveryRules.magicKey(GrimoirePages.magicOf(inst).name()));
        String path = book.getRegistryName().getPath();
        int mask = inst.getOrCreateTag().getInt("Unlocked");
        Map<String, Integer> masks = BOOK_MASKS.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        Integer last = masks.get(path);
        if (!full && last != null && last == mask && p.tickCount % 200 >= 40) return;      // unchanged: the base pages are only re-read every ten seconds
        masks.put(path, mask);
        int family = book.familyCount();
        for (int m : DiscoveryRules.unlockedModes(mask, family)) pageFound(p, book, path, m);
        int total = book.pages().size();
        for (int m = family; m < total; m++) if (book.usable(inst, p, m)) pageFound(p, book, path, m);
    }

    private static void pageFound(ServerPlayer p, GrimoireBook book, String path, int mode) {
        var page = book.page(mode);
        if (page != null) discover(p, DiscoveryRules.pageKey(path, page.id()));
    }

    // ---------------------------------------------------------------- sync
    private static void sendFull(ServerPlayer p) {
        boolean all = showAll(p);
        SHOWN_ALL.put(p.getUUID(), all);
        DiscoveryNet.sendFull(p, data(p), all);
    }

    // ---------------------------------------------------------------- grandfathering
    /**
     * The migration pass: everything the player already owns (inventory, Grimoire Slot, ender chest, worn Curios) and has unlocked (skills, every
     * grimoire's open pages, its magic, the bonded spirit) counts as found, so an existing world loses nothing. Runs once (the version is stamped).
     */
    static void migrate(ServerPlayer p, DiscoveryData d) {
        SHOWN_ALL.put(p.getUUID(), showAll(p));                                  // the full sync that follows carries the state: no lone state packet before it
        scanInventory(p);
        for (ItemStack s : p.getEnderChestInventory().getItems()) discoverItem(p, s);
        try {
            if (CuriosLink.available()) for (ItemStack s : CuriosLink.equipped(p)) discoverItem(p, s);
        } catch (RuntimeException | LinkageError curiosTrouble) {
            // no Curios, or a different version: the worn items were in the inventory when they were put on
        }
        scanSkills(p, true);
        d.setVersion(DiscoveryRules.VERSION);
    }
}
