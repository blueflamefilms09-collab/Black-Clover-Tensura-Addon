package com.newuniverse.nusmp.arc.world;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcConfig;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.story.ArcStory;
import com.newuniverse.nusmp.arc.story.StoryData;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.quests.QuestTeams;
import com.newuniverse.nusmp.quests.QuestsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the story must move on whoever the boss was and wherever it fell, and a saga must pay its reward to whoever closes it, in exactly one place. The arcs'
 * own bosses are credited by the core ({@code ArcStory} listens to {@code ArcBoss.ON_DEFEATED}); the seven older bosses (Goliath, the Charybdis Spawn, the Devil
 * Avatar, Dante, Vanica, Zenon, Lucius) are not {@code ArcBoss}es and never call that list, so this class listens for THEIR deaths (after everybody else, and
 * only for a death nobody cancelled) and gives the same credit to the same people: the fighters the boss itself recorded, and with the share switches on the
 * nearby members of their FTB teams. Then, for both kinds, it settles the saga reward ({@link SagaRewards}): the first time a player's story shows a saga with
 * every boss defeated, the reward is marked paid FIRST (a crash between the mark and the gift loses a reward once, a crash the other way would pay twice) and then
 * given, with a commemorative book, and the quest key {@value #KEY_SAGA_CLEAR} is fired. The Charter Grimoire of the seven stays with {@code CharterEvents}, which
 * already hears their deaths. Server thread, never throws out of the event.
 */
public final class ArcWorldCredit {
    private ArcWorldCredit() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** Quest hook key (argument: the saga id): the player's story closed a saga. */
    public static final String KEY_SAGA_CLEAR = "arc.saga_clear";

    private static boolean registered;

    public static synchronized void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        bus.addListener(EventPriority.LOWEST, ArcWorldCredit::onDeath);
        ArcBoss.ON_DEFEATED.add(boss -> settleAll(ArcStory.credited(boss)));      // after the core's own listener (registered earlier in ArcStory.init), so the credit is in the story first
    }

    // ================================================================ the seven older bosses
    private static void onDeath(LivingDeathEvent e) {
        try {
            LivingEntity dead = e.getEntity();
            if (!(dead instanceof ZogratisBoss boss) || dead instanceof ArcBoss || !(dead.level() instanceof ServerLevel sl) || !ArcConfig.enabled()) return;
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
            if (key == null || !NUSMP.MODID.equals(key.getNamespace())) return;
            String id = ExistingBosses.idOfPath(key.getPath());
            if (id == null) return;
            MinecraftServer server = sl.getServer();
            if (server == null) return;
            List<ServerPlayer> credited = credited(server, boss);
            for (ServerPlayer p : credited) {
                try {
                    ArcCharacter c = ArcRoster.byId(id);
                    ArcStory.Credit cr = ArcStory.defeated(p, id);
                    if (cr.fresh() && c != null) tell(p, c, cr);
                } catch (RuntimeException ex) {
                    LOG.error("[nusmp] could not give {} the story credit for {}: {}", p.getGameProfile().getName(), id, ex.toString());
                }
            }
            settleAll(credited);
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] the story credit for an older boss failed: {}", ex.toString());
        }
    }

    /** The online fighters of the boss, and with the share switches on the nearby members of their FTB teams. */
    private static List<ServerPlayer> credited(MinecraftServer server, ZogratisBoss boss) {
        Map<UUID, ServerPlayer> out = new LinkedHashMap<>();
        for (UUID u : new ArrayList<>(boss.fighters())) {
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            if (p != null) out.put(u, p);
        }
        if (ArcConfig.shareCredit() && QuestsConfig.shareSquadCredit()) {
            for (ServerPlayer p : new ArrayList<>(out.values())) {
                try {
                    for (ServerPlayer m : QuestTeams.nearbyMembers(p, QuestsConfig.shareRadius())) out.putIfAbsent(m.getUUID(), m);
                } catch (RuntimeException noTeams) { /* no FTB Teams, or its API changed: the fighters alone */ }
            }
        }
        return new ArrayList<>(out.values());
    }

    /** The same three lines the core says for its own bosses (same message keys, so one translation serves both). */
    private static void tell(ServerPlayer p, ArcCharacter c, ArcStory.Credit cr) {
        p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.fallen", "[Story] %s has fallen.", c.fullName()).withStyle(ChatFormatting.GOLD));
        if (!cr.opened().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (ArcCharacter o : cr.opened()) names.add(o.fullName());
            p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.opened", "[Story] Now open: %s", String.join(", ", names)).withStyle(ChatFormatting.AQUA));
        }
        if (cr.sagaChanged()) {
            p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.saga", "[Story] A new saga begins: %s", cr.sagaNow().title()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // ================================================================ the saga reward
    /** Settles every saga that is now cleared and unpaid for each of these players. */
    public static void settleAll(List<ServerPlayer> players) {
        if (!ArcConfig.enabled() || !ArcWorldConfig.sagaRewards()) return;
        for (ServerPlayer p : players) {
            try {
                settle(p);
            } catch (RuntimeException ex) {
                LOG.error("[nusmp] could not settle the saga reward of {}: {}", p.getGameProfile().getName(), ex.toString());
            }
        }
    }

    /** Pays the reward of every saga this player's story has cleared and not yet been paid for. Returns how many were paid. */
    public static int settle(ServerPlayer p) {
        StoryData d = ArcStory.data(p);
        int paid = 0;
        for (ArcSaga saga : SagaRewards.owed(d, ArcRoster.all())) {
            if (!SagaRewards.markPaid(d, saga)) continue;
            give(p, saga);
            NusmpEvents.fire(p, KEY_SAGA_CLEAR, saga.id());
            paid++;
        }
        return paid;
    }

    private static void give(ServerPlayer p, ArcSaga saga) {
        p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.saga_clear", "[Story] %s is cleared. The Chronicle pays its reward.", saga.title())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (SagaRewards.Stack s : SagaRewards.reward(saga)) {
            ResourceLocation id = ResourceLocation.tryParse(s.item());
            Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item == null || item == Items.AIR) {
                LOG.warn("[nusmp] the reward of {} names the item {}, which does not exist: skipped", saga.id(), s.item());
                continue;
            }
            int left = s.count();
            while (left > 0) {
                ItemStack stack = new ItemStack(item, Math.min(left, item.getDefaultMaxStackSize()));
                left -= stack.getCount();
                hand(p, stack);
            }
        }
        List<String> names = new ArrayList<>();
        for (ArcCharacter b : SagaRewards.bossesOf(saga, ArcRoster.all())) names.add(b.name());
        List<Filterable<Component>> pages = new ArrayList<>();
        for (String page : SagaRewards.bookPages(saga, names)) pages.add(Filterable.passThrough(Component.literal(page)));
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(SagaRewards.bookTitle(saga)), SagaRewards.BOOK_AUTHOR, 0, pages, true));
        hand(p, book);
    }

    /** Into the pack; a full pack puts it at the player's feet where it cannot despawn or burn (the way the Charter's own gift does). */
    private static void hand(ServerPlayer p, ItemStack stack) {
        if (p.addItem(stack)) return;
        ItemEntity drop = new ItemEntity(p.serverLevel(), p.getX(), p.getY() + 0.5, p.getZ(), stack);
        drop.setPickUpDelay(0);
        drop.setUnlimitedLifetime();
        drop.setInvulnerable(true);
        p.serverLevel().addFreshEntity(drop);
    }
}
