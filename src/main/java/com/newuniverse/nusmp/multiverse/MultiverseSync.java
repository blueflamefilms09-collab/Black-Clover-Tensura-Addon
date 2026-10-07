package com.newuniverse.nusmp.multiverse;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpiritBond;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server -> client status summary for the Multiverse status panel. The server builds one read-only compound with everything
 * the panel shows (rank, stars, squad, class, race, grimoire, spirit, anti-magic, ceremony) and sends it only when it changed.
 * The client never decides anything from it.
 */
public final class MultiverseSync {
    private static final Map<UUID, CompoundTag> LAST = new HashMap<>();
    private static final Set<UUID> DIRTY = new HashSet<>();

    private MultiverseSync() {}

    public static void markDirty(ServerPlayer p) { DIRTY.add(p.getUUID()); }

    /** Checks every 2 s (or the next tick when something marked it dirty). */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!DIRTY.remove(p.getUUID()) && p.tickCount % 40 != 0) return;
        CompoundTag now = summary(p);
        if (now.equals(LAST.get(p.getUUID()))) return;
        LAST.put(p.getUUID(), now);
        PacketDistributor.sendToPlayer(p, new StatusPayload(now));
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { LAST.remove(e.getEntity().getUUID()); DIRTY.remove(e.getEntity().getUUID()); }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) markDirty(p); }

    /** Everything the status panel shows, as plain display values. */
    public static CompoundTag summary(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        // rank & stars
        t.putString("Rank", MultiverseProfile.describeRank(p));
        t.putInt("Gold", MultiverseProfile.goldStars(p));
        t.putInt("Black", MultiverseProfile.blackStars(p));
        t.putInt("Net", MultiverseProfile.netStars(p));
        // squad
        t.put("Squad", Squads.summary(p));
        // social class & race
        t.putString("Social", MultiverseProfile.socialClass(p).displayName);
        t.putString("Race", RaceBuffs.displayName(p));
        // grimoire
        CompoundTag g = new CompoundTag();
        var inst = GrimoirePages.grimoireOf(p);
        if (inst.isPresent()) {
            var cover = GrimoirePages.coverOf(inst.get());
            g.putString("Cover", cover.displayName());
            g.putString("Magic", GrimoirePages.magicOf(inst.get()).displayName);
            if (inst.get().getSkill() instanceof GrimoireBook book) {
                int unlocked = 0;
                for (int m = 0; m < book.familyCount(); m++) if (GrimoireBook.isUnlocked(inst.get(), m)) unlocked++;
                g.putInt("Unlocked", unlocked);
                g.putInt("Pages", book.familyCount());
                g.putInt("Slots", MasteryPages.slots(p, inst.get(), book));
                g.putInt("Mastery", (int) Math.round(book.masteryFrac(inst.get()) * 100));
                // 0.46: the unlocked pages themselves, for the Four Kingdoms menu's grimoire (name, lang key, cost, cooldown)
                net.minecraft.nbt.ListTag pages = new net.minecraft.nbt.ListTag();
                for (int m = 0; m < book.familyCount(); m++) {
                    if (!GrimoireBook.isUnlocked(inst.get(), m)) continue;
                    var page = book.page(m);
                    if (page == null) continue;
                    CompoundTag e = new CompoundTag();
                    e.putString("Id", page.id());
                    e.putString("Name", page.name());
                    e.putString("Key", "nusmp.page." + book.getRegistryName().getPath() + "." + page.id());
                    e.putInt("Cost", (int) Math.round(page.costPercent()));
                    e.putInt("Cooldown", page.cooldown() / 20);
                    e.putInt("Mode", m);
                    pages.add(e);
                }
                g.put("PageList", pages);
                g.putString("MagicId", GrimoirePages.magicOf(inst.get()).name());
            }
        }
        t.put("Grimoire", g);
        // spirit lord
        CompoundTag s = new CompoundTag();
        if (SpiritBond.bonded(p) || SpiritLordSkill.instance(p).isPresent()) {
            s.putString("Type", SpiritLordSkill.bondedType(p));
            s.putInt("Trust", SpiritBond.trust(p));
            s.putInt("Energy", SpiritLordSkill.energy(p));
            s.putBoolean("Incarnated", SpiritBond.incarnate(p));
            s.putString("Name", SpiritBond.trueName(p));
        }
        t.put("Spirit", s);
        // anti-magic
        CompoundTag a = new CompoundTag();
        AntiMagic.lord(p).ifPresent(i -> {
            a.putString("Mode", MultiverseProfile.ANTI_MODES[MultiverseProfile.antiMode(p)]);
            a.putInt("AMP", AntiMagic.amp(i));
        });
        t.put("Anti", a);
        // ceremony
        CompoundTag c = new CompoundTag();
        c.putBoolean("Accepted", MultiverseProfile.accepted(p) || inst.isPresent());
        c.putBoolean("Eligible", Ceremony.isEligible(p));
        c.putBoolean("Active", Ceremony.isActive(p.getServer()));
        c.putString("Where", MultiverseProfile.acceptedAt(p));
        t.put("Ceremony", c);
        // 0.39: the Convergence (what this player may know, their origin, secret quests)
        CompoundTag v = new CompoundTag();
        if (Convergence.enabled()) {
            v.putBoolean("On", true);
            v.putBoolean("Anomaly", Convergence.isAnomaly(p));
            v.putBoolean("Revealed", Convergence.revealed(p));
            v.putBoolean("Detected", Convergence.detected(p));
            v.putString("Stage", Convergence.stage(p.getServer()).name());
            v.putString("StageTitle", Convergence.stage(p.getServer()).title);
            var k = Convergence.kingdomOf(p);
            if (k != null) v.putString("Kingdom", k.displayName);
            v.put("Quests", SecretQuests.summary(p));
        }
        t.put("Convergence", v);
        return t;
    }

    // ---------------------------------------------------------------- network
    public record StatusPayload(CompoundTag status) implements CustomPacketPayload {
        public static final Type<StatusPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "multiverse_status"));
        public static final StreamCodec<FriendlyByteBuf, StatusPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, StatusPayload::status, StatusPayload::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Mod-bus listener. The handler body only runs on clients. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(StatusPayload.TYPE, StatusPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.multiverse.MultiverseStatusClient.receive(payload.status())));
    }
}
