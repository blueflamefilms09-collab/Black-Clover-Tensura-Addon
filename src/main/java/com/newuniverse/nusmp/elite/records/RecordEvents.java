package com.newuniverse.nusmp.elite.records;

import com.newuniverse.nusmp.elite.records.rule.RecordData;
import com.newuniverse.nusmp.elite.records.rule.Tracking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Why: THE public entry point other elite subsystems call, because the numbers behind several record conditions belong to them: the Walpurgis council knows who attended, the war system knows who
 * killed and who won, the forge knows what quality it just made, the calamities know who hurt and who killed them. Each method adds to the player's saved counters, asks for an early beat (so the
 * unlock toast comes within a tick, not two seconds later) and returns. They are NULL-SAFE and never throw: a null player, an offline UUID, a negative amount, the records system switched off in its
 * config, or any internal failure makes the call do nothing. Every method has a {@link ServerPlayer} and a {@link UUID} form (the UUID form acts on an ONLINE player only; an offline player cannot be
 * credited because their data is not loaded). They are cheap enough to call per kill or per hit. Server thread only.
 * <ul>
 *   <li>{@link #walpurgisAttended}: the player took part in a Walpurgis council (once per council);</li>
 *   <li>{@link #warKill} / {@link #warWin}: a kill made in a war / a war won on the player's side;</li>
 *   <li>{@link #forgeCrafted}: an item was forged, quality 1 (common) to 6 (mythical);</li>
 *   <li>{@link #calamityDamage} / {@link #calamityKill}: damage dealt to a calamity / a calamity killed (with or without its id);</li>
 *   <li>{@link #grantRecord}: unlock a record outright (quest rewards, scripted events);</li>
 *   <li>{@link #customStat}: the value of a title or synergy custom stat for an online player (for example {@code forge_quality_bonus}, {@code coin_bonus_percent}); 0 when none or offline.</li>
 * </ul>
 */
public final class RecordEvents {
    private RecordEvents() {}

    // ------------------------------------------------------------------ plumbing
    private static boolean on(ServerPlayer p) { return p != null && RecordsConfig.enabled(); }

    private static ServerPlayer online(UUID id) {
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        return s == null || id == null ? null : s.getPlayerList().getPlayer(id);
    }

    private static void edit(ServerPlayer p, Consumer<RecordData> change) {
        if (!on(p)) return;
        try {
            change.accept(RecordsAttachments.of(p));
            RecordsService.evaluateSoon(p.getUUID());
        } catch (RuntimeException | LinkageError ignored) {
            // the records system must never break the caller
        }
    }

    // ------------------------------------------------------------------ Walpurgis
    public static void walpurgisAttended(ServerPlayer p) { edit(p, Tracking::walpurgis); }

    public static void walpurgisAttended(UUID id) { walpurgisAttended(online(id)); }

    // ------------------------------------------------------------------ war
    public static void warKill(ServerPlayer p) { edit(p, Tracking::warKill); }

    public static void warKill(UUID id) { warKill(online(id)); }

    public static void warWin(ServerPlayer p) { edit(p, Tracking::warWin); }

    public static void warWin(UUID id) { warWin(online(id)); }

    // ------------------------------------------------------------------ forge
    /** An item was forged at {@code quality} 1 (common) .. 6 (mythical); anything below 1 is ignored, above 6 counts as 6. */
    public static void forgeCrafted(ServerPlayer p, int quality) { edit(p, d -> Tracking.forged(d, quality)); }

    public static void forgeCrafted(UUID id, int quality) { forgeCrafted(online(id), quality); }

    // ------------------------------------------------------------------ calamities
    /** Damage the player dealt to a calamity (a positive amount; others are ignored). */
    public static void calamityDamage(ServerPlayer p, double amount) { edit(p, d -> Tracking.calamityDamage(d, amount)); }

    public static void calamityDamage(UUID id, double amount) { calamityDamage(online(id), amount); }

    /** A calamity was killed by the player. */
    public static void calamityKill(ServerPlayer p) { edit(p, d -> Tracking.calamityKill(d, null)); }

    /** A calamity was killed by the player; {@code calamityId} (for example {@code nusmp:storm_giant}) feeds the per-calamity conditions. */
    public static void calamityKill(ServerPlayer p, String calamityId) { edit(p, d -> Tracking.calamityKill(d, calamityId)); }

    public static void calamityKill(UUID id) { calamityKill(online(id)); }

    public static void calamityKill(UUID id, String calamityId) { calamityKill(online(id), calamityId); }

    // ------------------------------------------------------------------ rewards
    /** Unlocks a record by id whatever its condition. Returns true when it was locked before and exists. */
    public static boolean grantRecord(ServerPlayer p, String recordId) {
        if (!on(p) || recordId == null) return false;
        try {
            return RecordsService.grantRecord(p, recordId);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ reading
    /** A custom stat of the active title and the synergies in effect (see {@code StatNames}); 0 when the player is offline or has none. */
    public static double customStat(UUID id, String name) {
        if (id == null || name == null || !RecordsConfig.enabled()) return 0.0;
        return TitleApplier.stat(id, name);
    }

    public static double customStat(ServerPlayer p, String name) { return p == null ? 0.0 : customStat(p.getUUID(), name); }
}
