package com.newuniverse.nusmp.devil.phase2;

import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.devil.DevilConfig;
import com.newuniverse.nusmp.devil.DevilContract;
import com.newuniverse.nusmp.devil.DevilContracts;
import com.newuniverse.nusmp.devil.DevilRitualRun;
import com.newuniverse.nusmp.devil.DevilStore;
import com.newuniverse.nusmp.devil.WorldDevil;
import com.newuniverse.nusmp.devil.dual.CorruptionRules;
import com.newuniverse.nusmp.devil.dual.DevilHolders;
import com.newuniverse.nusmp.devil.dual.DevilOwnership;
import com.newuniverse.nusmp.devil.dual.HolderRules;
import com.newuniverse.nusmp.devil.phase2.Phase2Rules.Kind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Why: the server side of the contract rules of phase 2 (docs/devil_contract_phase2.md): who may call a devil, whether it answers, who it turns on, what the player may sign and how it is shown, how many contracts
 * a shared devil has and how its power is split, who is severed by a kill and what sabotage costs. The decisions are the pure {@link Phase2Rules} and the data of {@link Phase2Table}; the memory is
 * {@link DevilLedger}; the shared files only call the few public methods here (begin, answer, confirm, choose, joined, onDissolve, power, corruption). Everything is decided on the server, per player UUID, at an
 * event (a ritual begins or ends, a contract is signed, a player dies or logs out, a blow lands between two players): there is no tick work and no scan, and no state is kept in a static collection.
 *
 * <p>Cost (rule 11): a gate is one ledger lookup, one inventory pass (at most 41 stacks, for the relic bonus, once per ritual end) and one EP read; the power of a holder is one ledger lookup; a death between holders is
 * one pass over at most 16 x 16 ids; the betrayal hurts at most {@link #MAX_STRUCK} players once; a confirmation is at most 14 chat lines to the one caller; the friendly-fire check runs only for a blow between two
 * players, with no allocation when they hold nothing in common. No packet of its own: chat lines and the existing contract sync.
 */
public final class Phase2 {
    private Phase2() {}

    /** The most players one betrayal strikes, and the share of their maximum health it takes (never the last heart). */
    public static final int MAX_STRUCK = 8;
    public static final double BETRAY_SHARE = 0.4;
    /** Ticks between two swaps of the active devil of a player who holds several. */
    public static final long SWAP_COOLDOWN = 1200L;

    public static void register(IEventBus bus) {
        bus.addListener(Phase2::onLogin);
        bus.addListener(Phase2::onLogout);
        bus.addListener(Phase2::onDimension);
        bus.addListener(Phase2::onDeath);
        bus.addListener(Phase2::onDamage);
    }

    // ================================================================ facts
    public static boolean on() { return Phase2Config.enabled() && DevilConfig.enabled(); }

    /** May a player who holds a contract take another? (the setting; the caps are in {@link Phase2Rules#stack}). */
    public static boolean stacking() { return on() && Phase2Config.stack(); }

    /** Is this devil one-of-one (one holder in the world)? Supreme always (while sharing is on); every devil when sharing is off; an id outside the roster too. */
    public static boolean oneOfOne(String devilId) {
        WorldDevil d = WorldDevil.byId(devilId);
        return d == null || Phase2Rules.oneOfOne(on() && Phase2Config.shared(), d.tier());
    }

    private static DevilLedger ledger(MinecraftServer s) { return DevilLedger.get(s); }

    private static long now(MinecraftServer s) { return s.overworld().getGameTime(); }

    /** The player's Tensura EP (the maximum, so a fight does not lower it); creative players have all; no Tensura data reads as 0. */
    public static double ep(ServerPlayer p) {
        if (p.isCreative()) return Phase2Rules.MAX_EP;
        try {
            return Math.max(0.0, EnergyHelper.getMaxEP(p));
        } catch (RuntimeException | LinkageError noTensuraData) {
            return 0.0;
        }
    }

    /** The single EP requirement of a devil (before stacking), 0 when the rule is off. */
    public static double need(WorldDevil d) { return Phase2Config.ep() ? Phase2Table.get().requiredEp(d.id(), d.tier(), Phase2Config.epScale()) : 0.0; }

    private static final String RELICS = "devil_relics/";

    /** Does the player carry an item of this devil (the item tag anime_multiverse:devil_relics/&lt;id&gt;, main inventory and off hand)? */
    public static boolean hasRelic(ServerPlayer p, WorldDevil d) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("anime_multiverse", RELICS + d.id()));
        for (ItemStack s : p.getInventory().items) if (!s.isEmpty() && s.is(tag)) return true;
        for (ItemStack s : p.getInventory().offhand) if (!s.isEmpty() && s.is(tag)) return true;
        return false;
    }

    private static String fmt(double v) {
        if (v >= 1.0E9) return String.format(Locale.ROOT, "%.2f billion", v / 1.0E9);
        if (v >= 1.0E6) return String.format(Locale.ROOT, "%.2f million", v / 1.0E6);
        return String.format(Locale.ROOT, "%,.0f", v);
    }

    private static String name(WorldDevil d) { return d.name().isEmpty() ? d.name() : Character.toUpperCase(d.name().charAt(0)) + d.name().substring(1); }

    // ================================================================ the gate: may this player take this devil at all?
    public record Gate(boolean ok, String why, double needEp) {}

    private record Held(int weak, int strong) {
        int total() { return weak + strong; }
    }

    private static Held held(DevilLedger l, UUID id) {
        int weak = 0, strong = 0;
        for (DevilLedger.Pact pa : l.pacts(id)) {
            WorldDevil w = WorldDevil.byId(pa.devil());
            if (w != null && Phase2Rules.strong(w.tier())) strong++; else weak++;
        }
        return new Held(weak, strong);
    }

    public static Gate gate(ServerPlayer p, DevilContract c, WorldDevil d) {
        if (!on()) return new Gate(true, "", 0);
        DevilLedger l = ledger(p.getServer());
        UUID id = p.getUUID();
        if (l.banned(id, d.id())) return new Gate(false, "banned", 0);
        if (l.pact(id, d.id()) != null || (c != null && d.id().equals(c.devil))) return new Gate(false, "already", 0);
        if (!oneOfOne(d.id()) && !Phase2Rules.canJoin(l.count(d.id()), Phase2Config.maxContracts())) return new Gate(false, "full", 0);
        Held h = held(l, id);
        boolean holdsAny = h.total() > 0 || (c != null && c.hasContract());
        double base = need(d), ep = Phase2Config.ep() ? ep(p) : Phase2Rules.MAX_EP;
        Phase2Rules.Stack s = Phase2Rules.stack(Phase2Config.stack(), holdsAny, h.weak(), h.strong(), Phase2Rules.strong(d.tier()), base, ep, Phase2Config.weakMax(), Phase2Config.strongMax(),
                Phase2Config.strongFirst(), Phase2Config.strongMaxFactor());
        return new Gate(s.ok(), s.ok() ? "" : s.why(), s.needEp());
    }

    private static void deny(ServerPlayer p, Gate g) {
        String ep = fmt(ep(p)), need = fmt(g.needEp());
        MutableComponent m = switch (g.why()) {
            case "banned" -> Phase2Text.t("deny.banned", "You were severed from this devil. It will never sign with you again.");
            case "already" -> Phase2Text.t("deny.already", "You already hold a contract with this devil.");
            case "full" -> Phase2Text.t("deny.full", "This devil already has all the contracts it will give (%s). Nobody can join until one ends.", Phase2Config.maxContracts());
            case "one_only" -> Phase2Text.t("deny.one_only", "You already hold a devil contract. A mage holds one on this server.");
            case "weak_cap" -> Phase2Text.t("deny.weak_cap", "You hold as many devils as one body can carry (%s).", Phase2Config.weakMax());
            case "strong_cap" -> Phase2Text.t("deny.strong_cap", "You hold the most Supreme devils one body can carry (%s).", Phase2Config.strongMax());
            case "ep_stack" -> Phase2Text.t("deny.ep_stack", "A second Supreme devil asks far more of you. Your existence points: %s. It asks: %s.", ep, need);
            default -> Phase2Text.t("deny.ep", "The devil will not answer one so small. Your existence points: %s. It asks: %s.", ep, need);
        };
        Phase2Text.say(p, ChatFormatting.RED, m);
    }

    /** The ritual is asked for: true (with the reason said) when this player may not call this devil. Server decides; the same gate runs again when the devil answers and when the contract is signed. */
    public static boolean denyBegin(ServerPlayer p, DevilContract c, WorldDevil d) {
        Gate g = gate(p, c, d);
        if (g.ok()) return false;
        deny(p, g);
        return true;
    }

    // ================================================================ the answer of the ritual
    public enum Answer { OFFER, TRIAL, FAIL }

    /** The ritual ended: does the devil come, does it turn on the circle, does it fight first? OFFER = go on as before. */
    public static Answer answer(ServerPlayer p, DevilContract c, WorldDevil d, DevilRitualRun r) {
        if (!on()) return Answer.OFFER;
        Gate g = gate(p, c, d);
        if (!g.ok()) {
            deny(p, g);
            return Answer.FAIL;
        }
        if (Phase2Config.summon() && !p.isCreative()) {
            boolean relic = hasRelic(p, d);
            double chance = Phase2Table.get().summonChance(d.tier(), relic);
            if (!Phase2Rules.roll(chance, p.getRandom().nextDouble())) {
                Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("summon.fail", "%s does not answer. The circle goes cold. (It answers about one call in %s. Carrying something of it raises the odds.)", name(d), Math.max(1, Math.round(1.0 / Math.max(0.01, chance)))));
                return Answer.FAIL;
            }
            Phase2Text.say(p, ChatFormatting.DARK_PURPLE, relic
                    ? Phase2Text.t("summon.relic", "%s answers. What you carry of it drew it in. (%s percent)", name(d), Phase2Rules.percent(chance))
                    : Phase2Text.t("summon.ok", "%s answers. (%s percent)", name(d), Phase2Rules.percent(chance)));
        }
        betray(p, d, r);
        if (Phase2Config.trial() && Phase2Table.get().devil(d.id()).trial()) {
            if (Trials.start(p, c, d, r)) {
                Phase2Text.say(p, ChatFormatting.DARK_RED, Phase2Text.t("trial.start", "%s will not sign with the weak. It steps out of the circle to try you. Impress it, or beat it.", name(d)));
                return Answer.TRIAL;
            }
            Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("trial.no_room", "%s cannot take shape here (no room, or too many trials are running). The ritual is spent.", name(d)));
            return Answer.FAIL;
        }
        return Answer.OFFER;
    }

    /** The devil turns on everyone in the circle but the caller (when the data says it may): one strike, at most {@link #MAX_STRUCK} players, never the last heart. */
    private static void betray(ServerPlayer p, WorldDevil d, DevilRitualRun r) {
        double chance = Phase2Table.get().devil(d.id()).betray();
        if (!Phase2Config.betrayal() || chance <= 0 || !Phase2Rules.roll(chance, p.getRandom().nextDouble())) return;
        double rad = DevilConfig.darkRadius(), rad2 = rad * rad;
        int struck = 0;
        for (ServerPlayer q : p.serverLevel().players()) {
            if (struck >= MAX_STRUCK) break;
            if (q == p || !q.isAlive() || q.isSpectator() || q.isCreative() || q.distanceToSqr(r.x, r.y, r.z) > rad2) continue;
            float dmg = Math.min((float) (q.getMaxHealth() * BETRAY_SHARE), Math.max(0f, q.getHealth() - Phase2Config.betrayalLeaves()));
            if (dmg > 0f) q.hurt(q.damageSources().magic(), dmg);
            q.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            q.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            Vec3 at = q.position().add(0, 1, 0);
            VfxSpawn.send(q.serverLevel(), VfxShape.DARKFX_BURST, at, at.add(0, 1, 0), 0xFF000000 | (d.color() & 0xFFFFFF), 24, 1.5f);
            Phase2Text.say(q, ChatFormatting.DARK_RED, Phase2Text.t("betray.victim", "%s turns on the circle. It strikes you down: it was never yours to call.", name(d)));
            struck++;
        }
        if (struck > 0) Phase2Text.say(p, ChatFormatting.DARK_RED, Phase2Text.t("betray.caller", "%s turns on everyone in the circle but you. (%s struck)", name(d), struck));
    }

    // ================================================================ the terms: what the screen shows and what a signature needs
    private record Terms(Gate gate, double ep, double need, double dominanceNeed, int count, int cap, boolean one, boolean fights, int trial, boolean domOk) {}

    private static Terms terms(ServerPlayer p, DevilContract c, WorldDevil d) {
        Gate g = gate(p, c, d);
        boolean one = oneOfOne(d.id());
        DevilLedger l = ledger(p.getServer());
        double ep = Phase2Config.ep() ? ep(p) : Phase2Rules.MAX_EP, base = Math.max(need(d), g.needEp()), dom = Phase2Rules.dominanceEp(base, Phase2Config.dominanceFactor());
        int trial = c != null && d.id().equals(c.trialDevil) ? c.trialResult : Phase2Rules.TRIAL_NONE;
        return new Terms(g, ep, base, dom, one ? 0 : l.count(d.id()), Phase2Config.maxContracts(), one, Phase2Table.get().devil(d.id()).trial(), trial, !Phase2Config.ep() || Phase2Rules.epOk(ep, dom));
    }

    private static boolean allowed(Terms t, Kind k) {
        if (!t.gate().ok()) return false;
        if (!Phase2Config.kinds()) return k == Kind.DOMINANCE;
        return Phase2Rules.kindAllowed(k, true, t.domOk(), t.fights(), Phase2Config.trial(), t.trial());
    }

    private static int holdersAfter(Terms t) { return t.one() || !Phase2Config.shared() ? 1 : t.count() + 1; }

    private static int sharePercent(Terms t, Kind k) { return Phase2Rules.percent(Phase2Rules.power(Phase2Config.kinds() ? k : Kind.DOMINANCE, holdersAfter(t), 1.0)); }

    /** The confirmation panel before signing: the explanation, how many contracts the devil has, your share after joining, which kinds you may take (clickable) and the risks. At most 14 lines, to the caller only. */
    public static void confirm(ServerPlayer p, DevilContract c, WorldDevil d) {
        Terms t = terms(p, c, d);
        Phase2Text.say(p, ChatFormatting.DARK_RED, Phase2Text.t("confirm.title", "%s offers a contract. Read the terms before you sign:", name(d)).withStyle(ChatFormatting.BOLD));
        if (!t.gate().ok()) {
            deny(p, t.gate());
            return;
        }
        if (t.one() || !Phase2Config.shared()) Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("confirm.one", "%s is one of a kind: it has no other contract. You would be its only holder.", name(d)));
        else Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("confirm.count", "%s already has %s of %s contracts. After you join it has %s, and its power is split evenly between them.", name(d), t.count(), t.cap(), t.count() + 1));
        if (Phase2Config.ep()) Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("confirm.ep", "Your existence points: %s. This contract asks: %s (a dominance contract: %s).", fmt(t.ep()), fmt(t.need()), fmt(t.dominanceNeed())));
        for (Kind k : Kind.values()) {
            if (!Phase2Config.kinds() && k != Kind.DOMINANCE) continue;
            boolean ok = allowed(t, k);
            String word = k.key();
            int share = sharePercent(t, k);
            MutableComponent line = switch (k) {
                case SUBMISSION -> Phase2Text.t("kind.submission", "Submission: you serve it. You get %s percent of its power; the creeping corruption of its price grows half as fast.", share);
                case EVEN -> Phase2Text.t("kind.even", "Even: a pact of equals, 50-50. You get %s percent of its power.", share);
                case DOMINANCE -> Phase2Text.t("kind.dominance", "Dominance: you overpower it and take what it has. You get %s percent of its power.", share);
            };
            if (ok) {
                MutableComponent btn = switch (k) {
                    case SUBMISSION -> Phase2Text.t("kind.sign.submission", "[Submit]");
                    case EVEN -> Phase2Text.t("kind.sign.even", "[Even 50-50]");
                    case DOMINANCE -> Phase2Text.t("kind.sign.dominance", "[Dominate]");
                };
                btn.withStyle(s -> s.withColor(ChatFormatting.DARK_RED).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/devil sign " + word))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Phase2Text.t("kind.hover", "The price is taken at once and stays through death."))));
                p.sendSystemMessage(Component.literal("  ").append(btn).append(Component.literal(" ")).append(line.withStyle(ChatFormatting.GRAY)));
            } else {
                String why = Phase2Config.kinds() ? Phase2Rules.kindWhy(k, true, t.domOk(), t.fights(), Phase2Config.trial(), t.trial()) : "";
                MutableComponent reason = switch (why) {
                    case "not_won" -> Phase2Text.t("why.not_won", "beat the devil in the trial first");
                    case "ep_dominance" -> Phase2Text.t("why.ep_dominance", "your existence points are too low for it");
                    case "no_trial" -> Phase2Text.t("why.no_trial", "this devil does not fight, so it cannot be overpowered");
                    case "no_trial_yet" -> Phase2Text.t("why.no_trial_yet", "this devil tests you first");
                    case "ep" -> Phase2Text.t("why.ep", "your existence points are too low");
                    default -> Phase2Text.t("why.other", "not possible now");
                };
                p.sendSystemMessage(Component.literal("  ").append(Component.literal("[" + word + "] ").withStyle(ChatFormatting.DARK_GRAY)).append(Component.literal("(").withStyle(ChatFormatting.DARK_GRAY)).append(reason.withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY)));
            }
        }
        if (!t.one() && Phase2Config.shared() && Phase2Config.sever()) Phase2Text.say(p, ChatFormatting.YELLOW, Phase2Text.t("confirm.warn", "If another holder of this devil kills you, your contract is severed for good. If you kill a fellow holder you are cursed for %s seconds: your devil's power drops to %s percent.", Phase2Config.debuffSeconds(), Phase2Rules.percent(Phase2Config.debuffPower())));
        p.sendSystemMessage(Component.literal("  ").append(Phase2Text.t("confirm.refuse", "[Refuse]").withStyle(s -> s.withColor(ChatFormatting.GRAY).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/devil refuse"))))
                .append(Component.literal("   ")).append(Phase2Text.t("confirm.more", "[Rules]").withStyle(s -> s.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/devil terms")))));
    }

    /** The rules in plain words (the explanation of the signing screen); /devil terms. Seven short lines. */
    public static void explain(ServerPlayer p) {
        Phase2Text.say(p, ChatFormatting.DARK_RED, Phase2Text.t("explain.title", "How a devil contract works").withStyle(ChatFormatting.BOLD));
        Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("explain.1", "A devil answers a ritual only sometimes, more often when you carry something of it. It may turn on the others in the circle. It asks for existence points (EP): the higher its rank, the more."));
        Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("explain.2", "Some devils fight you first. Impress them (bring them to half health) to sign an even or a submission contract. Beat them to try to dominate them."));
        Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("explain.3", "Submission: a quarter of its power, safer. Even: half. Dominance: all of it, but only if you beat it and your EP is far above its own."));
        Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("explain.4", "Supreme devils are one of a kind: one holder in the world. Every lesser devil is shared: the more contracts it has, the less each holder gets (four contracts: a quarter each)."));
        Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("explain.5", "You may hold many lesser devils, but only one is active at a time (/devil pact use). A second Supreme devil asks five times the EP, a third more, and two is the most."));
        Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("explain.6", "A holder who kills another holder of the same shared devil cuts that contract for good, and is cursed for a few minutes. Hitting fellow holders over and over costs the same curse."));
    }

    // ================================================================ choosing a kind and signing
    /** The kind to sign with, or null (with the reason said) when this cannot be signed now. {@code word} null = the default (even, else submission; dominance when kinds are off). Decided again here: the screen only asked. */
    public static Kind choose(ServerPlayer p, DevilContract c, WorldDevil d, String word) {
        Terms t = terms(p, c, d);
        if (!t.gate().ok()) {
            deny(p, t.gate());
            return null;
        }
        Kind k = word == null ? null : Kind.byName(word);
        if (word == null) {
            if (!Phase2Config.kinds()) k = Kind.DOMINANCE;
            else k = allowed(t, Kind.EVEN) ? Kind.EVEN : allowed(t, Kind.SUBMISSION) ? Kind.SUBMISSION : null;
        } else if (k == null) {
            Phase2Text.say(p, ChatFormatting.RED, Phase2Text.t("choose.unknown", "Choose submit, even or dominate."));
            return null;
        }
        if (k == null || !allowed(t, k)) {
            Phase2Text.say(p, ChatFormatting.RED, Phase2Text.t("choose.no", "That contract is not possible for you now. Read the terms again (/devil sign)."));
            return null;
        }
        return k;
    }

    /** The contract was signed (the shared files did their part): it goes into the ledger, the other holders' shares are recomputed, the trial is over. */
    public static void joined(ServerPlayer p, DevilContract c, WorldDevil d, Kind kind) {
        MinecraftServer server = p.getServer();
        DevilLedger l = ledger(server);
        long now = now(server);
        l.join(p.getUUID(), d.id(), kind, now);
        com.newuniverse.nusmp.devil.mastery.Mastery.joined(c, d, d.id().equals(c.trialDevil) ? c.trialResult : Phase2Rules.TRIAL_NONE);   // a devil that was beaten or impressed starts the mastery a little higher
        Trials.end(server, c);
        c.trialResult = Phase2Rules.TRIAL_NONE;
        c.trialDevil = "";
        int count = l.count(d.id());
        if (!oneOfOne(d.id())) {
            for (UUID id : l.holders(d.id())) {
                if (id.equals(p.getUUID())) continue;
                ServerPlayer q = server.getPlayerList().getPlayer(id);
                DevilContract qc = q == null ? null : DevilStore.peek(q);
                if (q == null || qc == null) continue;
                if (d.id().equals(qc.devil)) DevilContracts.refresh(q, qc);
                Phase2Text.say(q, ChatFormatting.GRAY, Phase2Text.t("joined.other", "Another player signed with %s. It has %s contracts now: your share of its power is %s percent.", name(d), count, Phase2Rules.percent(power(q, qc))));
            }
        }
        if (Phase2Config.sameDayHook()) NeoForge.EVENT_BUS.post(new DevilPactJoinedEvent(p, d.id(), count, now / 24000L));
        DevilContracts.refresh(p, c);
        boolean active = d.id().equals(c.devil);
        Phase2Text.say(p, ChatFormatting.DARK_RED, Phase2Text.t("joined.you", "You signed a %s contract. You hold %s percent of the power of %s.", kind.key(), active ? Phase2Rules.percent(power(p, c)) : Phase2Rules.percent(Phase2Rules.power(kind, oneOfOne(d.id()) ? 1 : Math.max(1, count), 1.0)), name(d)));
        if (!active) Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("joined.stacked", "%s is held but not active: your active devil stays. /devil pact use %s swaps (once a minute).", name(d), d.id()));
    }

    // ================================================================ power
    /** The part of its devil's power this player has now: 1 for a contract with no devil of the roster or with phase 2 off; else the kind's share, split between the holders, times the sabotage debuff. */
    public static double power(ServerPlayer p, DevilContract c) {
        if (!on() || c == null || c.devil.isEmpty() || p.getServer() == null) return 1.0;
        MinecraftServer s = p.getServer();
        DevilLedger l = ledger(s);
        DevilLedger.Pact pa = l.pact(p.getUUID(), c.devil);
        Kind kind = pa == null || !Phase2Config.kinds() ? Kind.DOMINANCE : pa.kind();
        int holders = oneOfOne(c.devil) || !Phase2Config.shared() ? 1 : Math.max(1, l.count(c.devil));
        boolean debuff = Phase2Config.sabotage() && Phase2Rules.debuffActive(l.debuffUntil(p.getUUID()), now(s));
        return Phase2Rules.power(kind, holders, Phase2Rules.sabotageFactor(debuff, Phase2Config.debuffPower()));
    }

    /** The corruption numbers of this player: a submission contract grows the corruption of its price more slowly. */
    public static CorruptionRules.Params corruption(ServerPlayer p, DevilContract c, CorruptionRules.Params base) {
        if (!on() || !Phase2Config.kinds() || c == null || c.devil.isEmpty() || p.getServer() == null) return base;
        DevilLedger.Pact pa = ledger(p.getServer()).pact(p.getUUID(), c.devil);
        double f = pa == null ? 1.0 : pa.kind().corruption;
        if (f == 1.0) return base;
        return new CorruptionRules.Params(base.enabled(), (float) (base.idlePerMin() * f), (float) (base.assimPerMin() * f), (float) (base.unionPerMin() * f), base.killRelief(), base.deathKeep(), base.cap(), base.quietSeconds());
    }

    // ================================================================ a contract ends
    /** The player's contract with this devil ended by one of the older paths (a lost one-of-one, an operator): the ledger forgets it, the others' shares grow, and if the player holds more devils the next one becomes active. */
    public static void onDissolve(ServerPlayer p, String devil) {
        MinecraftServer server = p.getServer();
        if (server == null || devil == null || devil.isEmpty()) return;
        DevilLedger l = ledger(server);
        l.leave(p.getUUID(), devil);
        refreshHolders(server, devil);
        DevilContract c = DevilStore.peek(p);
        if (c != null && !c.hasContract()) promote(p, c);
    }

    /** An operator cleared the player: every contract in the ledger ends. */
    public static void clearAll(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        for (String devil : ledger(server).leaveAll(p.getUUID())) {
            if (oneOfOne(devil)) DevilOwnership.release(p, devil);                                  // a one-of-one devil is free again
            refreshHolders(server, devil);
        }
    }

    /** Recomputes the stats of the online holders of a shared devil (its count changed). At most the devil's cap (16) players. */
    private static void refreshHolders(MinecraftServer server, String devil) {
        for (UUID id : ledger(server).holders(devil)) {
            ServerPlayer q = server.getPlayerList().getPlayer(id);
            DevilContract qc = q == null ? null : DevilStore.peek(q);
            if (q != null && qc != null && devil.equals(qc.devil)) DevilContracts.refresh(q, qc);
        }
    }

    /** The first devil the player still holds becomes the active one. */
    private static void promote(ServerPlayer p, DevilContract c) {
        for (DevilLedger.Pact pa : ledger(p.getServer()).pacts(p.getUUID())) {
            WorldDevil w = WorldDevil.byId(pa.devil());
            if (w == null) continue;
            DevilContracts.activate(p, c, w);
            Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("promote", "%s is your active devil now.", name(w)));
            return;
        }
    }

    /** A contract with this devil is taken from this player (a lost one-of-one, severed), online or not. {@code ban} = never again. */
    public static void lose(MinecraftServer server, UUID id, String devil, boolean ban) {
        DevilLedger l = ledger(server);
        l.leave(id, devil);
        if (ban) l.ban(id, devil);
        ServerPlayer q = server.getPlayerList().getPlayer(id);
        if (q != null) {
            DevilContract qc = DevilStore.peek(q);
            if (qc != null && devil.equals(qc.devil)) DevilContracts.dissolve(q, qc);         // calls onDissolve: the next devil, if any, becomes active
        }
        refreshHolders(server, devil);
    }

    // ================================================================ the active devil
    /** {@code /devil pact use <devil>}: a player who holds several makes one of them the active one (the one whose power they use). */
    public static boolean use(ServerPlayer p, String devilId) {
        DevilContract c = DevilStore.peek(p);
        WorldDevil d = WorldDevil.byId(devilId);
        String why = null;
        if (!on()) why = "off";
        else if (d == null || c == null || ledger(p.getServer()).pact(p.getUUID(), d.id()) == null) why = "not_held";
        else if (d.id().equals(c.devil)) why = "same";
        else if (c.inUnion() || c.assimilating) why = "busy";
        else if (now(p.getServer()) - c.lastSwap < SWAP_COOLDOWN) why = "cooldown";
        if (why != null) {
            Phase2Text.say(p, ChatFormatting.RED, switch (why) {
                case "off" -> Phase2Text.t("use.off", "Devil contracts of phase 2 are off on this server.");
                case "not_held" -> Phase2Text.t("use.not_held", "You hold no contract with that devil (/devil pact list).");
                case "same" -> Phase2Text.t("use.same", "That devil is already your active one.");
                case "busy" -> Phase2Text.t("use.busy", "Push the devil back first: you cannot swap while it is let in or in the union.");
                default -> Phase2Text.t("use.cooldown", "You swapped a moment ago. Wait a minute.");
            });
            return false;
        }
        c.lastSwap = now(p.getServer());
        DevilContracts.activate(p, c, d);
        Phase2Text.say(p, ChatFormatting.DARK_PURPLE, Phase2Text.t("use.ok", "%s is your active devil now.", name(d)));
        return true;
    }

    /** {@code /devil pact list}: your contracts, their kind and your share, and the devils you were severed from. */
    public static void list(ServerPlayer p) {
        DevilLedger l = ledger(p.getServer());
        DevilContract c = DevilStore.peek(p);
        List<DevilLedger.Pact> pacts = l.pacts(p.getUUID());
        if (pacts.isEmpty()) Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("list.none", "You hold no contract with a devil of the roster."));
        for (DevilLedger.Pact pa : pacts) {
            WorldDevil w = WorldDevil.byId(pa.devil());
            if (w == null) continue;
            boolean active = c != null && pa.devil().equals(c.devil);
            int holders = oneOfOne(pa.devil()) ? 1 : Math.max(1, l.count(pa.devil()));
            Phase2Text.say(p, active ? ChatFormatting.GOLD : ChatFormatting.GRAY, Phase2Text.t("list.line", "%s: %s, %s contract(s) held, your share %s percent%s", name(w), pa.kind().key(), holders, Phase2Rules.percent(Phase2Rules.power(pa.kind(), holders, 1.0)), active ? " (active)" : ""));
        }
        for (String b : l.bans(p.getUUID())) Phase2Text.say(p, ChatFormatting.DARK_RED, Phase2Text.t("list.banned", "Severed for good: %s", b));
        if (Phase2Rules.debuffActive(l.debuffUntil(p.getUUID()), now(p.getServer()))) Phase2Text.say(p, ChatFormatting.DARK_RED, Phase2Text.t("list.cursed", "You are cursed: your devil's power is cut for %s more seconds.", Math.max(0, (l.debuffUntil(p.getUUID()) - now(p.getServer())) / 20L)));
    }

    /** An operator lifts a ban. */
    public static boolean unban(MinecraftServer server, UUID id, String devil) { return ledger(server).unban(id, devil); }

    // ================================================================ events
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !on() || p.getServer() == null) return;
        DevilContract c = DevilStore.peek(p);
        if (c == null || !c.hasContract() || c.devil.isEmpty()) return;
        DevilLedger l = ledger(p.getServer());
        if (l.pact(p.getUUID(), c.devil) == null && !l.banned(p.getUUID(), c.devil)) l.join(p.getUUID(), c.devil, Kind.DOMINANCE, now(p.getServer()));      // a contract older than phase 2 keeps its whole power
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) cleanup(p);
    }

    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) cleanup(p);
    }

    /** Logout, death and a change of dimension: the trial ends (and its open offer with it), the friendly-fire count and the trial result start again. The debuff and the bans stay: they are the world's memory. */
    private static void cleanup(ServerPlayer p) {
        DevilContract c = DevilStore.peek(p);
        if (c == null || p.getServer() == null) return;
        if (c.trial != null) {
            Trials.end(p.getServer(), c);
            c.offer = "";
        }
        c.trialResult = Phase2Rules.TRIAL_NONE;
        c.trialDevil = "";
        c.ffHits = 0;
        c.ffStart = 0;
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer victim) || victim.getServer() == null) return;
        cleanup(victim);
        if (!on() || !Phase2Config.sever() || !NUGameRules.devilSeverOnKill(victim.level())) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer killer) || killer == victim) return;
        MinecraftServer server = victim.getServer();
        DevilLedger l = ledger(server);
        List<String> hit = Phase2Rules.severed(true, false, names(l.pacts(killer.getUUID())), names(l.pacts(victim.getUUID())), d -> !oneOfOne(d));
        if (hit.isEmpty()) return;
        for (String devil : hit) {
            lose(server, victim.getUUID(), devil, true);
            WorldDevil w = WorldDevil.byId(devil);
            Phase2Text.say(victim, ChatFormatting.DARK_RED, Phase2Text.t("sever.victim", "%s killed you. Your contract with %s is severed for good: it will never sign with you again.", killer.getName().getString(), w == null ? devil : name(w)));
        }
        Phase2Text.say(killer, ChatFormatting.GOLD, Phase2Text.t("sever.killer", "You cut %s from %s. Your share of it grows.", victim.getName().getString(), hit.size() == 1 && WorldDevil.byId(hit.get(0)) != null ? name(WorldDevil.byId(hit.get(0))) : hit.size() + " devils"));
        sabotage(killer, "sever");
    }

    private static List<String> names(List<DevilLedger.Pact> pacts) {
        List<String> out = new ArrayList<>(pacts.size());
        for (DevilLedger.Pact p : pacts) out.add(p.devil());
        return out;
    }

    /** A blow between two players: repeated blows on a holder of the same shared devil trip the sabotage debuff. Only a blow between two players is looked at; nothing is allocated when they hold nothing in common. */
    private static void onDamage(LivingIncomingDamageEvent e) {
        if (e.getAmount() <= 0f || !(e.getEntity() instanceof ServerPlayer victim) || !(e.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == victim) return;
        if (!on() || !Phase2Config.sabotage() || Phase2Config.friendlyFireHits() <= 0 || victim.getServer() == null) return;
        MinecraftServer server = victim.getServer();
        if (!ledger(server).commonShared(attacker.getUUID(), victim.getUUID(), d -> !oneOfOne(d))) return;
        DevilContract ac = DevilStore.peek(attacker);
        if (ac == null) return;
        Phase2Rules.Fire f = Phase2Rules.friendlyFire(ac.ffHits, ac.ffStart, now(server), Phase2Config.friendlyFireWindow(), Phase2Config.friendlyFireHits());
        ac.ffHits = f.hits();
        ac.ffStart = f.start();
        if (f.trip()) sabotage(attacker, "friendly");
    }

    /** The saboteur pays: their devil's power is cut for a few minutes and they carry the curse mark (Weakness). Bounded: one write, one effect, one message. */
    public static void sabotage(ServerPlayer k, String why) {
        if (!on() || !Phase2Config.sabotage() || k.getServer() == null) return;
        MinecraftServer server = k.getServer();
        DevilLedger l = ledger(server);
        long until = Math.max(l.debuffUntil(k.getUUID()), Phase2Rules.debuffUntil(now(server), Phase2Config.debuffSeconds()));
        l.setDebuff(k.getUUID(), until);
        if (Phase2Config.curseMark()) k.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.min(Phase2Config.debuffSeconds(), 600) * 20, 0, false, true));
        DevilContract c = DevilStore.peek(k);
        if (c != null && c.hasContract()) DevilContracts.refresh(k, c);
        Phase2Text.say(k, ChatFormatting.DARK_RED, "friendly".equals(why)
                ? Phase2Text.t("sabotage.friendly", "You keep striking your fellow holders. The devil punishes it: your power is cut to %s percent for %s seconds.", Phase2Rules.percent(Phase2Config.debuffPower()), Phase2Config.debuffSeconds())
                : Phase2Text.t("sabotage.sever", "Cutting a bond has a price. The devil curses you: your power is cut to %s percent for %s seconds.", Phase2Rules.percent(Phase2Config.debuffPower()), Phase2Config.debuffSeconds()));
    }

    // ================================================================ the four grimoire pact devils
    /** Is this grimoire pact devil (IGNIVAR ...) held by somebody else, with {@code devilPhase2.shared.pactDevilsOneOfOne} on? Said to the player. False when the setting is off (they stay open to everybody). */
    public static boolean pactTaken(ServerPlayer p, String pactName) {
        if (!Phase2Config.pactOneOfOne() || p.getServer() == null || pactName == null) return false;
        HolderRules.Verdict v = DevilOwnership.verdict(p.getServer(), "pact_" + pactName.toLowerCase(Locale.ROOT), p.getUUID());
        if (v.mayTake) return false;
        Phase2Text.say(p, ChatFormatting.RED, Phase2Text.t("pact.taken", "That pact devil already has a holder on this server."));
        return true;
    }

    /** The pact was bound: with the setting on, this player is the holder of that pact devil. */
    public static void pactClaim(ServerPlayer p, String pactName) {
        if (!Phase2Config.pactOneOfOne() || p.getServer() == null || pactName == null) return;
        DevilHolders.get(p.getServer()).claimKeep("pact_" + pactName.toLowerCase(Locale.ROOT), p.getUUID(), now(p.getServer()));
    }
}
