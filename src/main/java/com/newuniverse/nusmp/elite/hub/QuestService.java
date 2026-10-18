package com.newuniverse.nusmp.elite.hub;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Why: everything the Quest Giver does to a player on the SERVER is here, one method per thing: read the player's {@link PlayerQuestData} (and roll it over to today first, so a daily never
 * survives midnight), build the board the dialogue shows, accept, hand in and abandon a quest, and feed the state machine from the game's events. Every decision is re-derived from saved data and
 * the catalog (the client only ever sends a quest id), the reward is paid through the {@link Currency} seam (coins, or Magic Stones in the old mode), a delivery takes the items before it
 * pays and an item that is not there cancels the whole hand-in, and after any change the player's overlay and, when the dialogue is open, its board are sent again. The state machine itself
 * is pure ({@link PlayerQuestData}) and tested; this class is the thin layer that connects it to the inventory, the day clock and the network.
 */
public final class QuestService {
    private QuestService() {}

    public static PlayerQuestData data(ServerPlayer p) { return p.getData(HubAttachments.QUESTS); }

    public static Function<String, QuestDef> defs() {
        QuestCatalog c = HubCatalogs.quests();
        return c::get;
    }

    /** The world's day number (the overworld's clock; the Hub's own clock may be frozen). */
    public static long today(MinecraftServer server) { return QuestRules.dayOf(server.overworld().getDayTime()); }

    public static boolean on() { return HubConfig.enabled() && HubConfig.get(HubConfig.QUESTS_ENABLED); }

    /** The player's data, rolled over to today (a new day clears the done-today list and drops yesterday's dailies). */
    public static PlayerQuestData fresh(ServerPlayer p) {
        PlayerQuestData d = data(p);
        if (!d.rollover(today(p.server), defs()).isEmpty()) HubSync.request(p);
        return d;
    }

    /** Today's daily board for this player. */
    public static List<QuestDef> board(PlayerQuestData d, long day) {
        return QuestRules.dailyBoard(day, HubCatalogs.quests().dailies(), HubConfig.get(HubConfig.DAILY_COUNT), d.maxFloor());
    }

    // ================================================================ requests from the dialogue
    /** A request of the open dialogue: the sender must have a Quest Giver's menu open. */
    static void handle(ServerPlayer sp, int action, String questId) {
        if (!on() || !(sp.containerMenu instanceof HubMenus.QuestMenu menu) || !menu.stillValid(sp)) return;
        QuestDef def = HubCatalogs.quests().get(questId);
        if (def == null) {
            tell(sp, "message.nusmp.hub.quest.unknown", "That quest is no longer on offer.");
        } else if (action == HubNet.ACT_ACCEPT) {
            accept(sp, def);
        } else if (action == HubNet.ACT_TURN_IN) {
            turnIn(sp, def);
        } else if (action == HubNet.ACT_ABANDON) {
            if (fresh(sp).abandon(def.id())) tell(sp, "message.nusmp.hub.quest.abandoned", "Quest abandoned: %s", QuestText.title(def));
        }
        HubSync.request(sp);
        sendBoard(sp);
    }

    /** Accepts a quest; returns the verdict (the player is told). */
    public static PlayerQuestData.AcceptResult accept(ServerPlayer sp, QuestDef def) {
        PlayerQuestData d = fresh(sp);
        long day = today(sp.server);
        boolean onBoard = !def.daily() || board(d, day).stream().anyMatch(q -> q.id().equals(def.id()));
        PlayerQuestData.AcceptResult r = d.accept(def, day, HubConfig.get(HubConfig.MAX_ACTIVE_QUESTS), onBoard);
        switch (r) {
            case OK -> tell(sp, "message.nusmp.hub.quest.accepted", "Quest accepted: %s", QuestText.title(def));
            case ALREADY_ACTIVE -> tell(sp, "message.nusmp.hub.quest.already", "You already have that quest.");
            case ALREADY_DONE -> tell(sp, "message.nusmp.hub.quest.done", "You have already done that quest%s.", def.daily() ? " today" : "");
            case FULL -> tell(sp, "message.nusmp.hub.quest.full", "You cannot carry more quests (the limit is %s).", HubConfig.get(HubConfig.MAX_ACTIVE_QUESTS));
            case LOCKED -> tell(sp, "message.nusmp.hub.quest.locked", "That quest is not open to you yet.");
            case NOT_OFFERED -> tell(sp, "message.nusmp.hub.quest.not_offered", "That quest is not on today's board.");
        }
        return r;
    }

    /** Hands a quest in: a delivery takes its items, the reward is paid, the quest is done. Returns the reward paid (bronze), 0 when nothing happened. */
    public static long turnIn(ServerPlayer sp, QuestDef def) {
        PlayerQuestData d = fresh(sp);
        PlayerQuestData.Active a = d.find(def.id());
        if (a == null) {
            tell(sp, "message.nusmp.hub.quest.not_active", "You have not accepted that quest.");
            return 0;
        }
        long have = def.kind() == ObjectiveKind.DELIVER ? countItem(sp, def.item()) : 0;
        if (!d.canTurnIn(def, have)) {
            tell(sp, "message.nusmp.hub.quest.not_ready", "That quest is not finished yet.");
            return 0;
        }
        if (def.kind() == ObjectiveKind.DELIVER && !removeItem(sp, def.item(), a.target())) {
            tell(sp, "message.nusmp.hub.quest.not_ready", "That quest is not finished yet.");
            return 0;
        }
        if (!d.complete(def, have)) return 0;
        long reward = QuestRules.reward(def, HubConfig.get(HubConfig.REWARD_SCALE));
        if (reward > 0) Currency.give(sp, reward);
        tell(sp, "message.nusmp.hub.quest.complete", "Quest complete: %s. Reward: %s.", QuestText.title(def), CoinMath.describe(reward));
        return reward;
    }

    // ================================================================ events
    /** A creature fell to the player. */
    public static void onKill(ServerPlayer sp, KillFact fact) {
        if (!on()) return;
        feed(sp, fresh(sp).onKill(fact, defs()));
    }

    public static void onFloorEntered(ServerPlayer sp, int floor) {
        if (!on()) return;
        feed(sp, fresh(sp).onFloorEntered(floor, defs()));
        HubSync.request(sp);
    }

    public static void onFloorCleared(ServerPlayer sp, int floor) {
        if (!on()) return;
        feed(sp, fresh(sp).onFloorCleared(floor, defs()));
    }

    private static void feed(ServerPlayer sp, List<PlayerQuestData.Update> updates) {
        if (updates.isEmpty()) return;
        for (PlayerQuestData.Update u : updates) {
            if (u.change() != PlayerQuestData.Change.READY) continue;
            QuestDef def = HubCatalogs.quests().get(u.id());
            if (def != null) tell(sp, "message.nusmp.hub.quest.ready", "Quest ready to hand in: %s", QuestText.title(def));
        }
        HubSync.request(sp);
    }

    // ================================================================ what the client is shown
    /** The overlay's lines: every accepted quest. */
    public static List<HubNet.HudEntry> hud(ServerPlayer sp) {
        List<HubNet.HudEntry> out = new ArrayList<>();
        if (!on()) return out;
        PlayerQuestData d = data(sp);
        for (PlayerQuestData.Active a : d.active()) {
            if (out.size() >= HubNet.MAX_HUD) break;
            QuestDef def = HubCatalogs.quests().get(a.id());
            int progress = a.progress();
            boolean ready = a.ready();
            if (def != null && def.kind() == ObjectiveKind.DELIVER) {
                progress = (int) Math.min(a.target(), countItem(sp, def.item()));
                ready = progress >= a.target();
            }
            out.add(new HubNet.HudEntry(def == null ? a.id() : QuestText.title(def), def == null ? a.id() : QuestText.label(def), progress, a.target(), ready ? 1 : 0,
                    def == null ? 0 : QuestRules.reward(def, HubConfig.get(HubConfig.REWARD_SCALE))));
        }
        return out;
    }

    /** The dialogue's board: the accepted quests, today's dailies, the next steps of the chain, and what is done today. */
    public static HubNet.BoardSync boardFor(ServerPlayer sp) {
        List<HubNet.BoardEntry> out = new ArrayList<>();
        PlayerQuestData d = fresh(sp);
        long day = today(sp.server);
        double scale = HubConfig.get(HubConfig.REWARD_SCALE);
        for (PlayerQuestData.Active a : d.active()) {
            QuestDef def = HubCatalogs.quests().get(a.id());
            if (def == null) continue;
            int progress = a.progress();
            boolean ready = a.ready();
            if (def.kind() == ObjectiveKind.DELIVER) {
                progress = (int) Math.min(a.target(), countItem(sp, def.item()));
                ready = progress >= a.target();
            }
            out.add(entry(def, ready ? HubNet.READY : HubNet.ACTIVE, progress, a.target(), scale));
        }
        for (QuestDef q : board(d, day)) {
            if (d.find(q.id()) != null) continue;
            out.add(entry(q, d.dailyDoneToday().contains(q.id()) ? HubNet.DONE : HubNet.OFFER, 0, q.target(), scale));
        }
        for (QuestDef q : QuestRules.progressionOffers(HubCatalogs.quests().progression(), d.completed(), d.activeIds(), d.maxFloor(), 8)) out.add(entry(q, HubNet.OFFER, 0, q.target(), scale));
        return new HubNet.BoardSync(Currency.balance(sp), out.size() > HubNet.MAX_BOARD ? out.subList(0, HubNet.MAX_BOARD) : out);
    }

    private static HubNet.BoardEntry entry(QuestDef q, int state, int progress, int target, double scale) {
        return new HubNet.BoardEntry(q.id(), q.daily() ? 0 : 1, state, QuestText.title(q), QuestText.fill(q.desc(), q), QuestText.label(q), progress, target, QuestRules.reward(q, scale));
    }

    /** Sends the dialogue's board to the player. */
    public static void sendBoard(ServerPlayer sp) {
        if (!HubSync.hasConnection(sp)) return;
        PacketDistributor.sendToPlayer(sp, boardFor(sp));
    }

    // ================================================================ inventory helpers
    /** How many of an item the player carries. */
    public static long countItem(ServerPlayer sp, String itemId) {
        Item item = item(itemId);
        if (item == null) return 0;
        long n = 0;
        Inventory inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(item)) n += s.getCount();
        }
        return n;
    }

    /** Takes {@code n} of an item; false (and nothing is taken) when the player has fewer. */
    public static boolean removeItem(ServerPlayer sp, String itemId, int n) {
        Item item = item(itemId);
        if (item == null || countItem(sp, itemId) < n) return false;
        Inventory inv = sp.getInventory();
        int left = n;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !s.is(item)) continue;
            int take = Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
        }
        inv.setChanged();
        return true;
    }

    static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id);
        return rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
    }

    // ================================================================ messages
    static void tell(ServerPlayer sp, String key, String english, Object... args) {
        try {
            sp.sendSystemMessage(Component.translatableWithFallback(key, english, args));
        } catch (RuntimeException ignored) {
            // a stand-in player of a self-test has no connection: the message is only news
        }
    }
}
