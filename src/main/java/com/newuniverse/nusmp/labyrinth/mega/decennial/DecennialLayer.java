package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialArenas.Live;
import com.newuniverse.nusmp.labyrinth.mega.guard.DndProfile;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Why: this is the Decennial boss as a LAYER on the Labyrinth Guardian, the same way the D&D layer is: the Guardian already is the colossal sector boss (scaling, theme trait, leash, damage budget,
 * altar, boss bar), so a Decennial boss adds no entity type. The layer holds the one profile the Guardian was given and drives it: it feeds the pure phase machine ({@link DecennialBossController})
 * the facts of the fight (health share, whether a party is in the hall, how many adds live) and does what its events say (a two-second invulnerable shift into the next form with the hall answering,
 * a lair action, a wave of adds), applies the phase's speed and damage through capped attribute modifiers, runs the signature mechanic ({@link Signatures}) and the lair engine ({@link LairEngine}),
 * keeps its hall honest (the seal closes when the fight starts, the hall resets and the boss heals when the party is wiped, every add is taken away when the boss falls) and pays the loot of its level
 * when it dies. It also hands the Guardian a D&D profile made from its own ({@link DecennialDnd}) so the Guardian's legendary actions and resistances serve it. A Guardian without a profile has an
 * inactive layer and behaves exactly as it did in 1.98.0.
 */
public final class DecennialLayer {
    /** The scoreboard tag and the persistent keys of a Decennial boss (the modules that look for it, and the renderer / audio builders, read these). */
    public static final String TAG = "nusmp_decennial", ID_KEY = "nusmp_dec_id", SOUND_KEY = "nusmp_sound_set";

    private static final ResourceLocation SPEED = ResourceLocation.fromNamespaceAndPath("nusmp", "decennial_speed"), DAMAGE = ResourceLocation.fromNamespaceAndPath("nusmp", "decennial_damage"),
            FREEZE = ResourceLocation.fromNamespaceAndPath("nusmp", "decennial_freeze");

    // ---- the profile the facade wants the NEXT Guardian to get (the altar creates the entity, so the wish travels in a static; server thread only)
    private static int wishLevel;
    private static String wishSite = "";

    /** The facade says: the next Guardian configured gets this level's profile and this hall. Always paired with {@link #unwish}. */
    public static void wish(int level, String siteKey) {
        wishLevel = level;
        wishSite = siteKey == null ? "" : siteKey;
    }

    public static void unwish() {
        wishLevel = 0;
        wishSite = "";
    }

    // ---- the state of one boss
    private DecennialProfile profile;
    private DecennialBossController ctl;
    private Signatures sig;
    private final LairEngine lairs = new LairEngine();
    private String siteKey = "";
    private boolean noLoot, dead, attached, fresh;
    private double appliedSpeed = Double.NaN, appliedDamage = Double.NaN;
    private int addsAlive, emptyTicks;
    private long fightTicks;

    public boolean active() { return profile != null; }

    public DecennialProfile profile() { return profile; }

    public String siteKey() { return siteKey; }

    public int phase() { return ctl == null ? 1 : ctl.phase(); }

    public DecennialBossController controller() { return ctl; }

    public int addsAlive() { return addsAlive; }

    /** An admin's kill: the boss pays nothing. */
    public void markNoLoot() { noLoot = true; }

    public void setSite(String key) { siteKey = key == null ? "" : key; }

    // ================================================================ choosing and binding
    /**
     * The Guardian is being configured as the boss of a sector whose last floor is level {@code levelOfFloor}: if the roster has a profile for it (or the facade wished for one) the layer binds to it and
     * returns the D&D profile the Guardian must run, else null and the Guardian is what it always was.
     */
    public DndProfile bind(LabyrinthGuardianEntity g, int levelFromSector, int sectorId) {
        int level = wishLevel != 0 ? wishLevel : levelFromSector;
        if (level == 0 || !DecennialConfig.get(DecennialConfig.ENABLED)) return clear();
        DecennialProfile p = DecennialRoster.forLevel(level);
        if (p == null) return clear();
        profile = p;
        ctl = new DecennialBossController(p);
        sig = new Signatures(DecennialVocab.signature(p.signature()));
        siteKey = !wishSite.isEmpty() ? wishSite : sectorId >= 0 ? DecennialData.keyOfSector(sectorId) : "";
        dead = false;
        attached = false;
        fresh = true;
        appliedSpeed = Double.NaN;
        appliedDamage = Double.NaN;
        stamp(g);
        return DecennialDnd.of(p, 1);
    }

    private DndProfile clear() {
        profile = null;
        ctl = null;
        sig = null;
        return null;
    }

    private void stamp(LabyrinthGuardianEntity g) {
        g.addTag(TAG);
        g.getPersistentData().putString(ID_KEY, profile.id());
        if (!profile.soundSet().isEmpty()) g.getPersistentData().putString(SOUND_KEY, profile.soundSet());
    }

    /** The look phase (1..3) the renderer shows for the boss's current phase: two phases show the first and the last, three show all, four repeat the middle. */
    public int lookPhase() {
        if (profile == null) return 1;
        int n = profile.phaseCount(), p = phase();
        if (n <= 2) return p <= 1 ? 1 : 3;
        if (n == 3) return p;
        return p <= 1 ? 1 : p >= 4 ? 3 : 2;
    }

    /** The name over the boss bar: the boss, the form it is in, and the floor it gates. */
    public Component displayName(String floorLabel) {
        DecennialProfile.Phase ph = profile.phase(phase());
        Component name = Component.translatableWithFallback("decennial.boss." + profile.id() + ".name", profile.name());
        Component form = Component.translatableWithFallback("decennial.boss." + profile.id() + ".phase." + phase(), ph.name());
        var out = Component.empty().append(name).append(Component.literal(" — ")).append(form);
        if (floorLabel != null) out.append(Component.literal(" (Floor " + floorLabel + ")"));
        return out;
    }

    /** After the Guardian's scaling: this boss's size, the room for the shield its signature feeds, the tint and the sound set the renderer and the audio read. */
    public void afterScaling(LabyrinthGuardianEntity g) {
        if (profile == null) return;
        AttributeInstance scale = g.getAttribute(Attributes.SCALE);
        if (scale != null) scale.setBaseValue(scale.getBaseValue() * profile.scale());
        if (List.of("shield_pulse", "add_tithe", "mana_leech").contains(sig.def().kind())) {
            AttributeInstance room = g.getAttribute(Attributes.MAX_ABSORPTION);
            if (room != null) room.setBaseValue(Math.max(room.getBaseValue(), g.getMaxHealth() * 0.25));
        }
        g.setDecennialTint(profile.tint());
        stamp(g);
    }

    // ================================================================ hurt
    /** Is the boss untouchable right now (the two seconds of a shift, or a hall that is not built yet)? */
    public boolean shielded(LabyrinthGuardianEntity g, ServerLevel sl) {
        if (profile == null) return false;
        if (ctl.invulnerable(sl.getGameTime())) return true;
        if (!siteKey.isEmpty()) {
            DecennialData.Site s = DecennialData.get(sl.getServer()).site(siteKey);
            if (s != null && !s.built) return true;
        }
        return false;
    }

    /** Damage the boss resists by category (a half), then the signature's say. */
    public float incoming(LabyrinthGuardianEntity g, ServerLevel sl, DamageSource source, float amount) {
        if (profile == null) return amount;
        float out = amount;
        String category = categoryOf(source);
        if (profile.resist().contains(category)) out *= 0.5f;
        return sig.incoming(g, sl, source, out, addsAlive);
    }

    /** The damage category a source belongs to, in the words of the profile's resist list. */
    static String categoryOf(DamageSource s) {
        if (s.is(DamageTypeTags.IS_FIRE)) return "fire";
        if (s.is(DamageTypeTags.IS_FREEZING)) return "freeze";
        if (s.is(DamageTypeTags.IS_EXPLOSION)) return "explosion";
        if (s.is(DamageTypeTags.IS_LIGHTNING)) return "lightning";
        if (s.is(DamageTypeTags.IS_PROJECTILE)) return "projectile";
        if (s.is(DamageTypes.MAGIC) && s.getEntity() == null) return "poison";
        if (s.is(DamageTypeTags.WITCH_RESISTANT_TO) || s.is(DamageTypes.INDIRECT_MAGIC) || s.is(DamageTypes.MAGIC)) return "magic";
        return "physical";
    }

    public void afterHurt(LabyrinthGuardianEntity g, ServerLevel sl, DamageSource source, float amount) {
        if (profile != null) sig.afterHurt(g, sl, source, amount);
    }

    public void afterMelee(LabyrinthGuardianEntity g, ServerLevel sl, Entity target) {
        if (profile != null) sig.afterMelee(g, sl, target, sl.getGameTime());
    }

    /** One of this boss's adds died. */
    public void addDied(LabyrinthGuardianEntity g) {
        if (profile == null) return;
        addsAlive = Math.max(0, addsAlive - 1);
        sig.addDied(g);
    }

    /** How many of {@code wanted} adds a broken element may bring now. */
    public int grantExtra(LabyrinthGuardianEntity g, ServerLevel sl, int wanted) {
        if (ctl == null) return 0;
        int granted = ctl.grantExtraAdds(wanted, addsAlive);
        addsAlive += granted;
        return granted;
    }

    // ================================================================ the tick
    public void tick(LabyrinthGuardianEntity g, ServerLevel sl) {
        if (profile == null || dead) return;
        long now = sl.getGameTime();
        DecennialData.Site site = siteKey.isEmpty() ? null : DecennialData.get(sl.getServer()).site(siteKey);
        boolean unbuilt = site != null && !site.built;
        Live hall = site != null && site.built ? DecennialArenas.liveByKey(sl.getServer(), siteKey) : null;
        if (!attached) {
            attached = true;
            if (site != null) DecennialArenas.attachBoss(sl.getServer(), siteKey, g.getUUID());
            if (hall != null) {
                g.setHallHalf(hall.plan.size / 2.0);
                if (fresh && !hall.site.defeated && (!hall.site.broken.isEmpty() || hall.site.sealed)) DecennialArenas.reset(sl, hall);     // a new boss of a hall a party left half broken: the hall starts clean
            }
            fresh = false;
        }
        if (hall != null && now % 20 == 0) hall.lastNear = now;
        List<Player> players = hall != null ? DecennialFx.inHall(sl, hall.frame, hall.plan.height) : unbuilt ? List.of() : DecennialFx.near(sl, g.getX(), g.getY(), g.getZ(), 30.0);
        boolean fighting = !players.isEmpty();
        if (now % 20 == 0) housekeeping(g, sl, hall);

        double hp = g.getMaxHealth() <= 0f ? 1.0 : g.getHealth() / g.getMaxHealth();
        List<DecennialBossController.Event> events = ctl.tick(now, hp, fighting, addsAlive);
        for (DecennialBossController.Event e : events) handle(g, sl, hall, players, e, now);
        boolean shifting = ctl.inCinematic(now);
        applyModifiers(g, shifting);
        if (shifting) {
            g.setTarget(null);
            g.getNavigation().stop();
            if (now % 4 == 0) sl.sendParticles(ParticleTypes.END_ROD, g.getX(), g.getY() + g.getBbHeight() * 0.5, g.getZ(), 8, 1.0 * g.getScale(), 1.2 * g.getScale(), 1.0 * g.getScale(), 0.1);
        }

        if (fighting) {
            emptyTicks = 0;
            fightTicks++;
            if (hall != null && now % 10 == 0) DecennialArenas.closeSeal(sl, hall);
            if (!shifting) sig.tick(g, sl, now, players, addsAlive, fightTicks);
        } else if (ctl.fightStart() >= 0 && !unbuilt) {
            emptyTicks++;
            if (emptyTicks >= DecennialConfig.get(DecennialConfig.WIPE_SECONDS) * 20) wipe(g, sl, hall);
        }
        lairs.tick(g, sl, hall, now, players);
    }

    private void handle(LabyrinthGuardianEntity g, ServerLevel sl, Live hall, List<Player> players, DecennialBossController.Event e, long now) {
        switch (e.kind()) {
            case PHASE_START -> {
                lairs.clear();
                g.noteCast(2);
                DecennialProfile.Phase next = profile.phase(e.phase());
                Component line = Component.translatableWithFallback("message.nusmp.mega.decennial.phase", "%1$s shifts into its next form: %2$s!", g.getName(),
                        Component.translatableWithFallback("decennial.boss." + profile.id() + ".phase." + e.phase(), next.name())).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
                for (Player p : players) p.displayClientMessage(line, true);
                if (!next.shout().isEmpty()) {
                    Component shout = Component.translatableWithFallback("decennial.boss." + profile.id() + ".shout." + e.phase(), next.shout()).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC);
                    for (Player p : players) p.sendSystemMessage(shout);
                }
                sl.playSound(null, g.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.4f, 0.6f);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, g.getX(), g.getY() + g.getBbHeight() * 0.5, g.getZ(), 1, 0, 0, 0, 0);
                if (hall != null) hall.lastNear = now;
            }
            case PHASE_END -> {
                g.dndSwap(DecennialDnd.of(profile, e.phase()), false);
                g.decennialRefresh();
                sl.playSound(null, g.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.8f, 0.8f);
            }
            case LAIR -> {
                DecennialVocab.Lair lair = DecennialVocab.lair(e.id());
                if (lair != null && !players.isEmpty()) lairs.start(g, sl, hall, lair, e.power(), now, players);
            }
            case ADDS -> spawnWave(g, sl, hall, players, e);
        }
    }

    private void spawnWave(LabyrinthGuardianEntity g, ServerLevel sl, Live hall, List<Player> players, DecennialBossController.Event e) {
        List<String> kinds = e.id().equals("phase") ? profile.phase(e.phase()).addKinds() : profile.adds().pool();
        if (kinds.isEmpty()) kinds = profile.adds().pool();
        if (kinds.isEmpty()) return;
        for (int i = 0; i < e.count(); i++) {
            String kind = kinds.get(sl.random.nextInt(kinds.size()));
            double a = sl.random.nextDouble() * Math.PI * 2, d = 3.5 + sl.random.nextDouble() * 3.0;
            double x = g.getX() + Math.cos(a) * d, z = g.getZ() + Math.sin(a) * d;
            if (hall != null && !hall.frame.containsColumn((int) Math.floor(x), (int) Math.floor(z))) {
                x = g.getX();
                z = g.getZ();
            }
            Player target = players.isEmpty() ? null : players.get(sl.random.nextInt(players.size()));
            Mob m = AddKinds.spawn(sl, kind, x, g.getY(), z, profile.depth(), g.getUUID(), target);
            if (m != null) {
                addsAlive++;
                sl.sendParticles(ParticleTypes.SOUL, m.getX(), m.getY() + 0.6, m.getZ(), 8, 0.3, 0.5, 0.3, 0.03);
            }
        }
        Component line = Component.translatableWithFallback("message.nusmp.mega.decennial.adds", "Something stirs in the ground!").withStyle(ChatFormatting.DARK_PURPLE);
        for (Player p : players) p.displayClientMessage(line, true);
    }

    /** Once a second: count the adds, put back or discard the ones that left the hall. */
    private void housekeeping(LabyrinthGuardianEntity g, ServerLevel sl, Live hall) {
        double h = hall != null ? hall.plan.size / 2.0 + 3.0 : 40.0;
        double cx = hall != null ? hall.frame.cx() + 0.5 : g.getX(), cz = hall != null ? hall.frame.cz() + 0.5 : g.getZ(), y0 = hall != null ? hall.frame.y0() : g.getY();
        AABB box = new AABB(cx - h - 6, y0 - 8, cz - h - 6, cx + h + 6, y0 + 50, cz + h + 6);
        int alive = 0;
        for (Mob m : AddKinds.of(sl, box, g.getUUID())) {
            if (Math.abs(m.getX() - cx) > h || Math.abs(m.getZ() - cz) > h) {
                m.discard();                                                             // an add that left the hall (a vex through a wall) is taken back
                continue;
            }
            alive++;
        }
        addsAlive = alive;
    }

    // ================================================================ modifiers
    private void applyModifiers(LabyrinthGuardianEntity g, boolean shifting) {
        double speed = DecennialRules.clamp(ctl.speedMult(), DecennialRules.MIN_SPEED_MULT, DecennialRules.MAX_SPEED_MULT);
        double damage = DecennialRules.clamp(ctl.damageMult() * sig.damageBonus(), 0.5, DecennialRules.MAX_DAMAGE_MULT);
        if (speed != appliedSpeed) {
            setMod(g, Attributes.MOVEMENT_SPEED, SPEED, speed - 1.0);
            appliedSpeed = speed;
        }
        if (damage != appliedDamage) {
            setMod(g, Attributes.ATTACK_DAMAGE, DAMAGE, damage - 1.0);
            appliedDamage = damage;
        }
        setMod(g, Attributes.MOVEMENT_SPEED, FREEZE, shifting ? -1.0 : 0.0);
    }

    private static void setMod(LabyrinthGuardianEntity g, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance inst = g.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (cur != null && Math.abs(cur.amount() - amount) < 1e-6) return;
        if (cur != null) inst.removeModifier(id);
        if (Math.abs(amount) > 1e-6) inst.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** True while the boss is stopped from striking (the shift). */
    public boolean cannotStrike(ServerLevel sl) { return profile != null && ctl.inCinematic(sl.getGameTime()); }

    // ================================================================ wipe, death
    /** The party is gone: heal, forget the fight, take the adds away, put the hall back, go home. */
    private void wipe(LabyrinthGuardianEntity g, ServerLevel sl, Live hall) {
        ctl.reset();
        sig.reset();
        lairs.clear();
        emptyTicks = 0;
        fightTicks = 0;
        appliedSpeed = Double.NaN;
        appliedDamage = Double.NaN;
        for (Mob m : AddKinds.of(sl, g.getBoundingBox().inflate(80.0), g.getUUID())) m.discard();
        addsAlive = 0;
        g.setTarget(null);
        g.removeAllEffects();
        g.setAbsorptionAmount(0f);
        g.restoreHealth(g.getMaxHealth());
        g.dndSwap(DecennialDnd.of(profile, 1), true);
        g.decennialRefresh();
        if (g.anchor() != null) g.teleportTo(g.anchor().getX() + 0.5, g.anchor().getY() + 1.0, g.anchor().getZ() + 0.5);
        if (hall != null) DecennialArenas.reset(sl, hall);
    }

    /** The boss died (the Guardian has confirmed it): the hall is told, the adds are gone, the loot is paid. */
    public void onDeath(LabyrinthGuardianEntity g, ServerLevel sl, DamageSource source) {
        if (profile == null || dead) return;
        dead = true;
        lairs.clear();
        Live hall = siteKey.isEmpty() ? null : DecennialArenas.liveByKey(sl.getServer(), siteKey);
        if (hall != null) DecennialArenas.defeated(sl, hall, g);
        else for (Mob m : AddKinds.of(sl, g.getBoundingBox().inflate(80.0), g.getUUID())) m.discard();
        boolean standalone = false;
        if (!siteKey.isEmpty()) {
            DecennialData.Site s = DecennialData.get(sl.getServer()).site(siteKey);
            standalone = s != null && s.standalone;
        }
        if (!noLoot && !standalone) DecennialRewards.award(g, sl, source, profile, siteKey);
    }

    // ================================================================ save
    public void save(CompoundTag tag) {
        if (profile == null) return;
        tag.putString("DecId", profile.id());
        tag.putString("DecSite", siteKey);
        tag.putIntArray("DecCtl", ctl.save());
        tag.putBoolean("DecNoLoot", noLoot);
    }

    /** Reads the saved binding; returns the D&D profile the Guardian must run, or null when this Guardian is not a Decennial boss. */
    public DndProfile load(LabyrinthGuardianEntity g, CompoundTag tag) {
        if (!tag.contains("DecId") || !DecennialConfig.get(DecennialConfig.ENABLED)) return clear();
        DecennialProfile p = DecennialRoster.byId(tag.getString("DecId"));
        if (p == null) return clear();
        profile = p;
        ctl = new DecennialBossController(p);
        ctl.restore(tag.getIntArray("DecCtl"));
        sig = new Signatures(DecennialVocab.signature(p.signature()));
        siteKey = tag.getString("DecSite");
        noLoot = tag.getBoolean("DecNoLoot");
        dead = false;
        attached = false;
        fresh = false;
        appliedSpeed = Double.NaN;
        appliedDamage = Double.NaN;
        stamp(g);
        return DecennialDnd.of(p, ctl.phase());
    }
}
