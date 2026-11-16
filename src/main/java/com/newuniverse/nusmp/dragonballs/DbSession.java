package com.newuniverse.nusmp.dragonballs;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * One running ritual: Shenron (or Super Shenron) is in the sky above the summoner and waits for the wishes. At most one per set, so at most two on the whole server. The summoner types a wish (a keyword of
 * {@link WishRules}) or clicks one in the menu ({@code /dragonwish <id>}); the server validates, applies, logs it. The ritual ends when the wishes are used up, after {@code sessionSeconds} (a farewell), or
 * at once when the summoner dies, logs out, changes dimension or the server stops. It then scatters the balls ({@link DbManager#finish}); a ritual that granted at least one wish also locks the set (stone).
 * Server tick cost: {@link #tickAll} runs once a second and looks at two sessions at most.
 */
public final class DbSession {
    private static final DbSession[] SESSIONS = new DbSession[BallSet.values().length];

    final BallSet set;
    final UUID summoner;
    final String summonerName;
    final String dim;
    final Vec3 at;
    final long startTick;
    int wishesLeft, wishesGranted;
    UUID dragon;
    private long lastSkyTick;
    private boolean ended;

    private DbSession(ServerPlayer p, BallSet set, Vec3 at) {
        this.set = set;
        this.summoner = p.getUUID();
        this.summonerName = p.getGameProfile().getName();
        this.dim = p.level().dimension().location().toString();
        this.at = at;
        this.startTick = p.getServer().getTickCount();
        this.wishesLeft = DbConfig.wishes(set);
    }

    // ================================================================ lookup
    public static DbSession of(BallSet set) { return SESSIONS[set.ordinal()]; }

    public static DbSession byPlayer(UUID id) {
        for (DbSession s : SESSIONS) if (s != null && s.summoner.equals(id)) return s;
        return null;
    }

    // ================================================================ start
    /** Puts the dragon in the sky, darkens it for the players near, thunders, and asks for the wish. Called by {@link DbManager#trySummon} after the balls were taken. */
    static void start(ServerPlayer p, BallSet set) {
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        double lx = look.x, lz = look.z, len = Math.sqrt(lx * lx + lz * lz);
        if (len < 1e-3) { lx = 0; lz = 1; len = 1; }
        Vec3 pos = new Vec3(p.getX() + lx / len * 16.0, p.getY() + 8.0, p.getZ() + lz / len * 16.0);
        DbSession s = new DbSession(p, set, pos);
        SESSIONS[set.ordinal()] = s;
        ShenronEntity e = DbBoot.SHENRON.get().create(level);
        if (e != null) {
            e.setSet(set);
            e.moveTo(pos.x, pos.y, pos.z, (float) (Math.toDegrees(Math.atan2(-lx, lz)) + 180.0), 0f);       // the front looks back at the summoner
            level.addFreshEntity(e);
            s.dragon = e.getUUID();
        }
        for (int i = 0; i < 2; i++) {                                                          // two visual-only bolts (no fire, no damage) and the thunder
            LightningBolt b = EntityType.LIGHTNING_BOLT.create(level);
            if (b != null) {
                b.setVisualOnly(true);
                b.moveTo(pos.x + (i == 0 ? -6 : 6), p.getY(), pos.z);
                level.addFreshEntity(b);
            }
        }
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 8.0f, 0.7f);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 6, pos.z, 40, 3.0, 5.0, 3.0, 0.05);
        s.lastSkyTick = p.getServer().getTickCount();
        DbNet.sendSky(level, pos, set, 260);
        for (ServerPlayer q : level.players()) if (q.distanceToSqr(pos.x, pos.y, pos.z) < DbNet.RANGE * DbNet.RANGE) q.sendSystemMessage(DbMsg.say(set, "arrive", s.summonerName));
        DbManager.LOG.info("[dragonballs] {} summoned {} (uuid {}) at {} {} {}", s.summonerName, set.dragonName(), s.summoner, (int) pos.x, (int) pos.y, (int) pos.z);
        p.sendSystemMessage(DbMsg.say(set, "ask", s.wishesLeft));
        menu(p, s);
    }

    // ================================================================ the menu (clickable wishes)
    static void menu(ServerPlayer p, DbSession s) {
        MutableComponent line = Component.empty();
        for (WishRules.Wish w : WishRules.forSet(s.set)) {
            line.append(Component.literal("[" + Character.toUpperCase(w.id().charAt(0)) + w.id().substring(1) + "] ")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dragonwish " + w.id()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(DbMsg.P + "wish." + w.id() + "." + s.set.id())))));
        }
        p.sendSystemMessage(line);
    }

    static void skillMenu(ServerPlayer p, DbSession s) {
        MutableComponent line = Component.empty();
        for (String id : WishRules.skills(s.set)) {
            String name = WishRules.skillName(id);
            line.append(Component.literal("[" + name + "] ")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dragonwish skill " + name))));
        }
        p.sendSystemMessage(line);
    }

    // ================================================================ the wish
    /** What the summoner said or clicked. Returns after answering; never throws into the chat. */
    public void wish(ServerPlayer p, String text) {
        if (ended || !p.getUUID().equals(summoner)) return;
        String t = DbRules.norm(text);
        if (DbRules.contains(t, "cancel") || DbRules.contains(t, "never mind") || DbRules.contains(t, "nevermind") || DbRules.contains(t, "farewell") || DbRules.contains(t, "goodbye")) {
            end(p.getServer(), "cancelled by the summoner", true);
            return;
        }
        if (WishRules.forbidden(t)) {
            p.sendSystemMessage(DbMsg.say(set, "refuse"));
            return;
        }
        WishRules.Wish w = WishRules.match(set, t);
        if (w == null) {
            p.sendSystemMessage(DbMsg.say(set, "unknown"));
            menu(p, this);
            return;
        }
        DbWishes.Outcome o = DbWishes.grant(p, set, w, t, this);
        p.sendSystemMessage(o.msg());
        if (!o.ok()) return;
        wishesGranted++;
        wishesLeft--;
        DbManager.LOG.info("[dragonballs] wish granted: player={} uuid={} dragon={} wish={} text='{}' ({} left)", summonerName, summoner, set.dragonName(), w.id(), t.length() > 80 ? t.substring(0, 80) : t, wishesLeft);
        if (wishesLeft <= 0) end(p.getServer(), "wishes granted", true);
        else p.sendSystemMessage(DbMsg.say(set, "another", wishesLeft));
    }

    // ================================================================ the clock (once a second)
    static void tickAll(MinecraftServer s, long tick) {
        for (int i = 0; i < SESSIONS.length; i++) {
            DbSession x = SESSIONS[i];
            if (x != null) x.check(s, tick);
        }
    }

    private void check(MinecraftServer s, long tick) {
        ServerPlayer p = s.getPlayerList().getPlayer(summoner);
        if (p == null) { end(s, "the summoner left the server", false); return; }
        if (!p.isAlive()) { end(s, "the summoner died", false); return; }
        if (!p.level().dimension().location().toString().equals(dim)) { end(s, "the summoner changed dimension", false); return; }
        if ((tick - startTick) / 20 >= DbConfig.sessionSeconds()) { end(s, "time ran out", true); return; }
        if (tick - lastSkyTick >= 200) {                                                       // players who walked up late hear of the dark sky; the client expires it by itself
            lastSkyTick = tick;
            ServerLevel l = p.serverLevel();
            DbNet.sendSky(l, at, set, 260);
        }
    }

    // ================================================================ end
    /** Ends this ritual: the dragon leaves (with a farewell when {@code farewell}), the sky clears, the balls scatter, the set is locked when a wish was granted. */
    void end(MinecraftServer s, String why, boolean farewell) {
        if (ended) return;
        ended = true;
        SESSIONS[set.ordinal()] = null;
        ServerLevel level = DbManager.levelOf(s, dim);
        if (level != null) {
            if (dragon != null && level.getEntity(dragon) != null) level.getEntity(dragon).discard();
            DbNet.sendSky(level, at, set, 0);
            if (farewell) for (ServerPlayer q : level.players()) if (q.distanceToSqr(at.x, at.y, at.z) < DbNet.RANGE * DbNet.RANGE) q.sendSystemMessage(DbMsg.say(set, wishesGranted > 0 ? "farewell" : "farewell_none"));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 6.0f, 1.2f);
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 6, at.z, 30, 3.0, 5.0, 3.0, 0.05);
        }
        DbManager.finish(s, set, wishesGranted > 0);
        DbManager.LOG.info("[dragonballs] {} ritual of {} (uuid {}) ended: {} ({} wish(es) granted, locked={})", set.id(), summonerName, summoner, why, wishesGranted, wishesGranted > 0 && DbConfig.lockMinutes() > 0);
    }

    static void endAll(MinecraftServer s, String why) {
        for (int i = 0; i < SESSIONS.length; i++) if (SESSIONS[i] != null) SESSIONS[i].end(s, why, false);
    }
}
