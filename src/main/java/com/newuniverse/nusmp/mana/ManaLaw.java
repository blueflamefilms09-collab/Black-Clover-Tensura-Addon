package com.newuniverse.nusmp.mana;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.multiverse.MultiverseConfig;
import com.newuniverse.nusmp.multiverse.MultiverseProfile;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 1.5.0: the mana system of Black Clover as "The Weeb Commander" lays it out (see docs/mana_system.md), built so that no magic is a
 * bad draw:
 * <ul>
 *   <li><b>Mana is life force.</b> Spend it to nothing and you collapse (and, if the server allows it, can die).</li>
 *   <li><b>Mage Stages 9 to 0, Arcane and Saint.</b> Power comes from <i>training</i> (rank, mastery), never from which magic you drew.</li>
 *   <li><b>Affinity</b> (loved / ordinary / hated by mana) is a balanced trade you may change at will, not a luck tier.</li>
 *   <li><b>Mana Zone:</b> 1.99.0: a LATE-GAME technique (gate, rules and mechanics in mana.zone.*, docs/mana/mana_zone_redesign.md): command the natural mana of the surroundings; the denser the place the stronger it is.
 *       The entry points below (toggleZone, forceZone, zoneActive) are kept and now delegate to {@link com.newuniverse.nusmp.mana.zone.ManaZoneManager}; the 1.5.0 data ({@code ZoneUntil}) stays on old saves, untouched and unused.</li>
 *   <li><b>Negative Mana (Forbidden Magic):</b> a Magic Stone amplifies everything and corrupts you; only an Arcane mage survives it.</li>
 * </ul>
 * Everything is stored on the player (persisted data) and is server-side; the client only shows a summary.
 */
public final class ManaLaw {
    private ManaLaw() {}

    private static final String KEY = "nusmp_mana";
    public static final String SEALED = "nusmp_sealed_until";

    // ---------------------------------------------------------------- storage
    private static CompoundTag data(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(KEY, 10)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    // ---------------------------------------------------------------- affinity: a balanced trade, never a luck tier
    public enum Affinity {
        LOVED("Loved by Mana", "A deep well and a slow stream: spells cost 10% less mana and recharge 10% slower.", 0.90, 1.10),
        NORMAL("Ordinary", "No lean either way.", 1.0, 1.0),
        HATED("Hated by Mana", "A shallow well and a quick stream: spells cost 10% more mana and recharge 10% faster.", 1.10, 0.90);

        public final String label, blurb;
        public final double cost, cooldown;

        Affinity(String label, String blurb, double cost, double cooldown) { this.label = label; this.blurb = blurb; this.cost = cost; this.cooldown = cooldown; }

        public static Affinity byName(String n) {
            for (Affinity a : values()) if (a.name().equalsIgnoreCase(n)) return a;
            return null;
        }
    }

    public static Affinity affinity(Player p) {
        Affinity a = Affinity.byName(data(p).getString("Affinity"));
        return a == null ? Affinity.NORMAL : a;
    }

    public static void setAffinity(Player p, Affinity a) { data(p).putString("Affinity", a.name()); }

    // ---------------------------------------------------------------- stages
    /** 9 (lowest) to 0 (highest) from rank and class, then the Arcane and Saint stages above them. */
    public record Stage(int number, boolean arcane, boolean saint) {
        public String label() { return saint ? "Saint Stage" : arcane ? "Arcane Stage" : "Stage " + number; }
        /** Spells cost less the higher you stand. */
        public double cost() { return saint ? 0.70 : arcane ? 0.75 : 1.15 - 0.039 * (9 - number); }
        /** ...and hit harder. */
        public double power() { return saint ? 1.35 : arcane ? 1.30 : 0.90 + 0.04 * (9 - number); }
    }

    public static Stage stage(Player p) {
        int step = MultiverseProfile.step(p);                                      // 0 (5th class junior) .. 16 (Wizard King)
        int number = 9 - Mth.clamp(Math.round(step * 9f / 16f), 0, 9);
        CompoundTag d = data(p);
        boolean arcane = d.getBoolean("Arcane") || d.getBoolean("Pact") || AntiMagic.isUser(p);   // magic that defies what magic can do
        boolean king = com.newuniverse.nusmp.multiverse.WizardKing.isKing(p);       // the crown is a Saint Stage awakening
        return new Stage(number, arcane || king, d.getBoolean("Saint") || king);
    }

    public static void setArcane(Player p, boolean v) { data(p).putBoolean("Arcane", v); }

    public static void setSaint(Player p, boolean v) { data(p).putBoolean("Saint", v); }

    // ---------------------------------------------------------------- the numbers every spell passes through
    /** Multiplies a grimoire spell's mana cost. */
    public static double costMult(ServerPlayer p) {
        double c = stage(p).cost() * affinity(p).cost * Heritage.costMult(p) * Signature.cost(p) * com.newuniverse.nusmp.dungeon.MealBuffs.cost(p) * (com.newuniverse.nusmp.multiverse.WizardKing.isKing(p) ? 0.8 : 1.0);
        c *= com.newuniverse.nusmp.mana.zone.ManaZoneManager.costMult(p);                    // 1.99.0: cheaper inside an open Mana Zone, dearer while exhausted after one
        if (negativeActive(p)) c *= 0.7;
        return c;
    }

    /** Multiplies a spell's cooldown. */
    public static double cooldownMult(ServerPlayer p) { return affinity(p).cooldown * Signature.cooldown(p); }

    /** Multiplies the damage of a spell. */
    public static double powerMult(LivingEntity e) {
        if (!(e instanceof ServerPlayer p)) return 1.0;
        double m = stage(p).power() * Heritage.powerMult(p) * Signature.power(p) * com.newuniverse.nusmp.dungeon.MealBuffs.power(p) * (com.newuniverse.nusmp.multiverse.WizardKing.isKing(p) ? 1.25 : 1.0);
        if (zoneActive(p)) m *= com.newuniverse.nusmp.mana.zone.ManaZoneManager.powerMult(p, density(p.level(), p.blockPosition()));      // 1.99.0: the zone's bonus ramps in with its growth
        double boon = com.newuniverse.nusmp.mana.zone.GrandZones.boonOf(p);                                                  // 1.99.0: a strong player is fed by a Grand Magic Zone
        if (boon > 0) m *= com.newuniverse.nusmp.mana.zone.GrandZoneMath.powerMult(boon);
        if (negativeActive(p)) m *= 1.6;
        m *= com.newuniverse.nusmp.scalecast.ScaleCastKit.magicMult(p);                      // Scale casting: a foe inside a Scale user's aura casts up to 30 percent weaker (docs/magic/scale_casting.md)
        return m;
    }

    // ---------------------------------------------------------------- mana density, Grand Magic Zones
    /** How thick the mana is here: 1.0 is ordinary; the deep sea, volcanic ground, mountain tops, the deep dark and the End are Grand Magic Zones. */
    public static double density(Level level, BlockPos pos) {
        var biome = level.getBiome(pos);
        double d = 1.0;
        if (biome.is(BiomeTags.IS_DEEP_OCEAN)) d += 1.0;                            // the Seabed Temple
        if (biome.is(BiomeTags.IS_NETHER)) d += 0.8;                                // the Ultime Volcano Trail
        if (biome.is(BiomeTags.IS_MOUNTAIN)) d += 0.4;
        if (biome.is(Biomes.DEEP_DARK)) d += 1.2;
        if (biome.is(BiomeTags.IS_END)) d += 1.0;
        if (pos.getY() > 190) d += 0.3;
        d = Math.max(d, com.newuniverse.nusmp.mana.zone.GrandZones.densityAt(level, pos));        // 1.99.0: a Grand Magic Zone region (data/<ns>/grand_magic_zones) is as dense as it says
        return Math.min(3.0, d);
    }

    public static boolean isGrandZone(Level level, BlockPos pos) { return density(level, pos) >= 1.8; }     // kept: "dense mana" (1.8 and over); the named Grand Magic Zones are the regions of mana.zone.GrandZones

    // ---------------------------------------------------------------- Mana Zone
    /** 1.99.0: true while the player's Mana Zone is open (mana.zone.ManaZoneManager; one tracked entry per caster). The 1.5.0 timer in the player data is no longer read. */
    public static boolean zoneActive(Player p) { return com.newuniverse.nusmp.mana.zone.ManaZoneManager.active(p); }

    /** A Mana Zone that costs nothing (the Wizard King's Crown of Mana): no gate, no upkeep, for {@code ticks}. */
    public static void forceZone(ServerPlayer p, int ticks) {
        com.newuniverse.nusmp.mana.zone.ManaZoneManager.startForced(p, ticks);
        p.displayClientMessage(Component.literal("Crown of Mana: the mana of the whole land answers you.").withStyle(ChatFormatting.GOLD), true);
    }

    /**
     * The Mana Zone page: toggles. 1.99.0: it is a LATE-GAME technique, so for anyone who has not met the gate (EP, rank, mana skin, bosses, depth and the
     * attunement ritual; see mana.zone.ManaZoneGate) this refuses with the list of what is missing and opens nothing. Returns true when a zone was opened or closed.
     */
    public static boolean toggleZone(ServerPlayer p) { return com.newuniverse.nusmp.mana.zone.ManaZoneManager.toggle(p); }

    // ---------------------------------------------------------------- Negative Mana (Forbidden Magic)
    public static boolean negativeActive(Player p) { return p.level().getGameTime() < data(p).getLong("NegUntil"); }

    public static float corruption(Player p) { return data(p).getFloat("Corruption"); }

    /** Attune to a Magic Stone. Only a mage whose magic already defies the ordinary can carry it; anyone else is torn by it. */
    public static boolean attuneNegative(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        if (negativeActive(p)) { p.displayClientMessage(Component.literal("Negative mana already runs through you.").withStyle(ChatFormatting.DARK_PURPLE), true); return false; }
        if (!stage(p).arcane()) {
            addCorruption(p, 60f);
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
            p.displayClientMessage(Component.literal("The stone tears at you: only an Arcane Stage mage can hold negative mana.").withStyle(ChatFormatting.DARK_RED), true);
            return true;                                                          // the stone is spent
        }
        if (!p.isCreative() && EnergyHelper.isOutOfEnergy(p, 0, EnergyHelper.getMaxMagicule(p) * 0.2)) {       // Tensura: true = cannot afford (nothing deducted)
            p.displayClientMessage(Component.literal("You do not have the mana to open yourself to negative mana (20% of your magicule).").withStyle(ChatFormatting.RED), true);
            return false;
        }
        data(p).putLong("NegUntil", sl.getGameTime() + 1200);
        data(p).putBoolean("Pact", true);
        VfxSpawn.sendFollowing(sl, VfxShape.MAGIC_CIRCLE, p, p.position().add(0, 0.08, 0), 0xFF3A0A5A, 100, 2.8f);
        sl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2f, 0.6f);
        p.displayClientMessage(Component.literal("Negative mana floods through you. Your magic is mighty, and it costs you a little of yourself with every cast.")
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        return true;
    }

    public static void addCorruption(ServerPlayer p, float amount) {
        CompoundTag d = data(p);
        d.putFloat("Corruption", Mth.clamp(d.getFloat("Corruption") + amount, 0f, 150f));
    }

    // ---------------------------------------------------------------- the choice at the ceremony (no magic is a bad draw)
    public static void setOffer(Player p, String cover, String where) {
        CompoundTag d = data(p);
        d.putString("OfferCover", cover);
        d.putString("OfferWhere", where);
        d.putLong("OfferSentAt", p.level().getGameTime());
    }

    public static String offerCover(Player p) { return data(p).getString("OfferCover"); }

    public static String offerWhere(Player p) { return data(p).getString("OfferWhere"); }

    public static void clearOffer(Player p) { data(p).remove("OfferCover"); data(p).remove("OfferWhere"); }

    /** True when the offer was last shown more than 30 s ago (and marks it shown now). */
    public static boolean offerDue(Player p) {
        long now = p.level().getGameTime();
        if (now - data(p).getLong("OfferSentAt") < 600) return false;
        data(p).putLong("OfferSentAt", now);
        return true;
    }

    /** The panel's Mana block. */
    public static CompoundTag summary(Player p) {
        CompoundTag t = new CompoundTag();
        Stage s = stage(p);
        t.putString("Stage", s.label());
        t.putString("Affinity", affinity(p).label);
        t.putInt("Cost", (int) Math.round(s.cost() * affinity(p).cost * 100));
        t.putInt("Power", (int) Math.round(s.power() * 100));
        t.putInt("Corruption", (int) corruption(p));
        t.putBoolean("Zone", zoneActive(p));
        t.putBoolean("Negative", negativeActive(p));
        t.putString("Density", String.format("%.1f", density(p.level(), p.blockPosition())));
        t.putBoolean("Grand", isGrandZone(p.level(), p.blockPosition()));
        t.putString("Offer", offerCover(p));
        t.putString("Signature", Signature.describe(p));
        var sc = com.newuniverse.nusmp.multiverse.MultiverseProfile.socialClass(p);
        var st = Heritage.socialTrait(sc);
        t.putString("SocialName", sc.displayName);
        t.putString("SocialBuff", st.buff());
        t.putString("SocialDebuff", st.debuff());
        var k = Heritage.kingdom(p);
        var b = Heritage.blood(p);
        if (k != null) { var tr = Heritage.kingdomTrait(k); t.putString("Kingdom", k.displayName); t.putString("KingdomBuff", tr.buff()); t.putString("KingdomDebuff", tr.debuff()); }
        if (b != null) { var tr = Heritage.bloodTrait(b); t.putString("Blood", b.displayName); t.putString("BloodBuff", tr.buff()); t.putString("BloodDebuff", tr.debuff()); }
        return t;
    }

    // ---------------------------------------------------------------- hooks from the grimoire
    /** After a successful grimoire cast. */
    public static void onCast(ServerPlayer p) {
        if (negativeActive(p)) addCorruption(p, 4f);
    }

    /** Spending your mana to nothing: the shock, then the collapse. Mana is life force. */
    public static void exhaust(ServerPlayer p) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null || p.isCreative() || com.newuniverse.nusmp.multiverse.WizardKing.isKing(p)) return;        // the Wizard King never collapses
        double frac = ex.getMagicule() / Math.max(1.0, EnergyHelper.getMaxMagicule(p));
        if (frac < 0.05) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1));
            p.displayClientMessage(Component.literal("Your mana is spent. Exhaustion takes you.").withStyle(ChatFormatting.GRAY), true);
        }
        if (frac < 0.005) collapse(p);
    }

    private static void collapse(ServerPlayer p) {
        long now = p.level().getGameTime();
        CompoundTag d = data(p);
        if (now - d.getLong("LastCollapse") < 200) return;                          // one faint at a time
        d.putLong("LastCollapse", now);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 4));
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
        float shock = p.getMaxHealth() * 0.3f;
        boolean lethal = MultiverseConfig.get(MultiverseConfig.LETHAL_EXHAUSTION);
        float dmg = lethal ? shock : Math.min(shock, Math.max(0f, p.getHealth() - 1f));    // by default exhaustion alone never kills
        if (dmg > 0) p.hurt(p.damageSources().magic(), dmg);
        p.displayClientMessage(Component.literal("You black out. Mana is your life force, and you have burned it all.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
    }

    // ---------------------------------------------------------------- once a second
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_MANA_LAW = com.newuniverse.nusmp.perf.PerfProbe.section("s.mana.law");   // 1.99.0: performance probe (off by default: one flag read)

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onPlayerTick0(event);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_MANA_LAW, t0);
        }
    }

    private static void onPlayerTick0(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        ServerLevel sl = p.serverLevel();
        CompoundTag d = data(p);
        long now = sl.getGameTime();
        if (com.newuniverse.nusmp.multiverse.WizardKing.isKing(p)) {                      // the awakening: mana and life return by themselves
            EnergyHelper.gainMagicule(p, EnergyHelper.getMaxMagicule(p) * 0.015, EnergyHelper.GainType.NORMAL);      // Tensura's own gain path (respects its gain multipliers)
            if (p.getHealth() < p.getMaxHealth()) p.heal(0.5f);
        }
        // 1.99.0: the Mana Zone's own upkeep runs in mana.zone.ManaZoneManager (once a second per open zone); here the gate, the attunement and the client's gate state
        com.newuniverse.nusmp.mana.zone.ManaZoneManager.onSecond(p);
        // entering a Grand Magic Zone
        boolean grand = isGrandZone(sl, p.blockPosition());
        if (grand != d.getBoolean("InGrand")) {
            d.putBoolean("InGrand", grand);
            if (grand) p.displayClientMessage(Component.literal("The mana here is thick.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        // corruption: it eats, and it slowly lets go
        float c = d.getFloat("Corruption");
        if (c > 0) {
            if (!negativeActive(p)) c = Math.max(0, c - 0.5f);
            d.putFloat("Corruption", c);
            if (c >= 100) {
                p.getPersistentData().putLong(SEALED, now + 1200);                  // the grimoire shuts itself against you
                p.hurt(p.damageSources().magic(), 6f);
                d.putFloat("Corruption", 60f);
                p.displayClientMessage(Component.literal("Negative mana corrupts you. Your grimoire seals itself.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            } else if (c >= 70) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, false, true));
            } else if (c >= 40) {
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
            }
        }
    }

    // ---------------------------------------------------------------- the universal combat page
    /**
     * Mana Strike: every grimoire has it, whatever its magic. A bolt of raw mana 24 blocks long, tinted with the book's colour. Power
     * comes from the caster's stage, not from the book, so a healer's or a sealer's grimoire is never a dud.
     */
    public static boolean manaStrike(GrimoireBook book, ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        double reach = com.newuniverse.nusmp.mana.zone.ManaZoneManager.strikeRange(p, 24);                   // 1.99.0: longer and wider inside an open Mana Zone
        int maxHits = com.newuniverse.nusmp.mana.zone.ManaZoneManager.strikeTargets(p, 3);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), end = eye.add(look.scale(reach));
        int hits = 0;
        double width = zoneActive(p) ? 1.8 : 1.2;                                                           // 1.99.0: and wider (the zone's "size")
        for (LivingEntity t : GrimoireBook.along(p, eye, end, width)) {
            if (t == p || t.isAlliedTo(p)) continue;
            t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, t, 7f, 0.6));
            hits++;
            if (hits >= maxHits) { end = t.getBoundingBox().getCenter(); break; }
        }
        VfxSpawn.send(sl, VfxShape.LIGHT_BLADE, eye.add(0, -0.3, 0), end, book.color, 8, (float) (width / 1.2));
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, end, end, book.color, 10, 0.5f);
        sl.playSound(null, p.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.2f, 1.3f);
        return true;
    }
}
