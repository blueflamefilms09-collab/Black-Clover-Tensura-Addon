package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.saiyan.SaiyanPhaseConfig;
import com.newuniverse.nusmp.saiyan.SaiyanProdigyConfig;
import com.newuniverse.nusmp.saiyan.SaiyanProdigyHub;
import com.newuniverse.nusmp.saiyan.SaiyanProdigyModes;
import com.newuniverse.nusmp.saiyan.SaiyanStore;
import com.newuniverse.nusmp.skill.modes.ModeSpec;
import com.newuniverse.nusmp.skill.modes.MultiModeSkill;
import com.newuniverse.nusmp.skill.rules.DbRules;
import com.newuniverse.nusmp.skill.rules.DbRules.Mode;
import com.newuniverse.nusmp.skill.rules.DbRules.Spec;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * The Saiyan Prodigy (docs/saiyan_prodigy.md): ONE intrinsic skill, one slot, that holds every Saiyan ability as a mode, so a Saiyan needs one key and the mode key, not one key per ability. The mode table is
 * the one data list {@link SaiyanProdigyModes#BUNDLE}; the generic part (mode count, wheel walk with locked modes skipped, hold and release plumbing) is {@link MultiModeSkill}. A Saiyan mode sends the same op the
 * old key sent, through {@link SaiyanProdigyHub#act}, so every old rule (the awakening, the Goku lock, Ki costs, cooldowns, the rate limit) is unchanged. Two modes are the skill's own:
 * <ul>
 * <li><b>Art</b>: press performs the selected art, sneak + press selects the next. An art is one of the Dragon Ball rework techniques or a Dragon Ball character's active move (the old Dragon Ball skills'
 * modes, opened by Battle Power or by owning the old skill).</li>
 * <li><b>Fighting Style</b>: press switches the selected character's passive stance on and off, sneak + press selects the next character.</li>
 * </ul>
 * State is the player's own skill tag (the selected art and style, the game time each art is ready, mirrored flags for the client's mode key). Nothing is shared, nothing is static, nothing ticks except one
 * look a second. A press does at most one list build (at most 128 entries, 30 skill lookups) and one old-system op. Server setting {@code prodigy.prodigyHub}: off = the skill does nothing.
 */
public class SaiyanProdigySkill extends MultiModeSkill {
    static final SkillText TEXT = SkillText.of("saiyan_prodigy", "Saiyan Prodigy",
            "Intrinsic. A Saiyan's whole fighting art in one slot: Ki Blast, Barrage, Flight, Senses, Barrier, Deflection, Energy Transfer, Ki Wave, forms, Arts (the rework techniques and the Dragon Ball characters' moves), Dragon Fist, and for the Goku line Kamehameha, Rock-Scissors-Paper, Instant Transmission, Spirit Bomb, Meteor Combination and God Bind. Sneak + press changes the choice of a mode that has one.");

    /** Skill tag keys. */
    static final String SY = "Sy", AWK = "Awk", GK = "Gk", ART = "Art", STYLE = "Style", STYLE_LAST = "StyleLast", STYLE_ULT = "StyleUlt", ART_AT = "ArtAt", ART_READY = "ArtReady", GAP = "Gap", GROW = "GrowAt";
    private static final int GAP_TICKS = 4;

    public SaiyanProdigySkill() { super(SkillType.INTRINSIC, TEXT, SaiyanProdigyModes.BUNDLE); }

    @Override public boolean canBeToggled(ManasSkillInstance i, LivingEntity e) { return true; }
    @Override public boolean canTick(ManasSkillInstance i, LivingEntity e) { return true; }
    @Override public boolean canBeSlotted(ManasSkillInstance i, LivingEntity e, int slot) { return true; }

    // ---------------------------------------------------------------------------------------------------- which modes are open
    @Override
    protected boolean modeOpen(ManasSkillInstance instance, LivingEntity wearer, ModeSpec m) {
        if (m.action() == SaiyanProdigyModes.ACT_ART || m.action() == SaiyanProdigyModes.ACT_STYLE) return true;      // open to anyone who owns the skill: the list inside decides
        boolean sy, awk, gk;
        if (wearer instanceof ServerPlayer p) {
            sy = SaiyanStore.isSaiyan(p);
            awk = SaiyanProdigyHub.awakened(p);
            gk = SaiyanProdigyHub.goku(p);
        } else {
            CompoundTag t = instance.getTag();     // the client: the flags the server mirrored into the skill tag
            sy = t != null && t.getBoolean(SY);
            awk = t != null && t.getBoolean(AWK);
            gk = t != null && t.getBoolean(GK);
        }
        return sy && SaiyanProdigyModes.open(m, awk, gk);
    }

    @Override
    protected Component lockedMessage(ModeSpec m) {
        return switch (m.need()) {
            case SaiyanProdigyModes.NEED_AWAKENED -> SkillMsg.t("prodigy.locked_awake", "%s needs the Ki awakening.", modeName(m));
            case SaiyanProdigyModes.NEED_GOKU -> SkillMsg.t("prodigy.locked_goku", "%s belongs to the Goku line.", modeName(m));
            default -> SkillMsg.t("prodigy.locked_saiyan", "%s is a Saiyan's art.", modeName(m));
        };
    }

    // ---------------------------------------------------------------------------------------------------- the press
    @Override
    protected void press(ManasSkillInstance instance, ServerPlayer p, ModeSpec m, int modeNumber) {
        if (!SaiyanProdigyHub.enabled()) { SkillUtil.fail(p, SkillMsg.t("prodigy.off", "The Saiyan Prodigy is switched off on this server.")); return; }
        int op = p.isShiftKeyDown() && m.sneakAction() != 0 ? m.sneakAction() : m.action();
        CompoundTag tag = instance.getOrCreateTag();
        long now = p.level().getGameTime();
        if (op >= SaiyanProdigyModes.ACT_ART) {                       // the skill's own modes: a short gap so the selection keys cannot be spammed
            long gap = tag.getLong(GAP);
            if (now < gap && gap - now <= GAP_TICKS) return;
            tag.putLong(GAP, now + GAP_TICKS);
        }
        switch (op) {
            case SaiyanProdigyModes.ACT_ART -> art(instance, p, tag, now, modeNumber);
            case SaiyanProdigyModes.ACT_ART_NEXT -> nextArt(instance, p, tag);
            case SaiyanProdigyModes.ACT_STYLE -> style(instance, p, tag, false);
            case SaiyanProdigyModes.ACT_STYLE_NEXT -> style(instance, p, tag, true);
            default -> {
                SaiyanProdigyHub.act(p, op);
                growSlowly(instance, p, tag, now);
            }
        }
    }

    /** One mastery point at most every second, so spamming a free mode does not farm mastery. */
    private void growSlowly(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now) {
        if (now < tag.getLong(GROW)) return;
        tag.putLong(GROW, now + 20);
        grow(instance, p, 1);
    }

    @Override
    protected boolean hold(ManasSkillInstance instance, ServerPlayer p, ModeSpec m, int heldTicks) { return SaiyanProdigyHub.holding(p, m.releaseAction()); }

    @Override
    protected void release(ManasSkillInstance instance, ServerPlayer p, ModeSpec m, int heldTicks) { SaiyanProdigyHub.release(p, m.releaseAction()); }

    // ---------------------------------------------------------------------------------------------------- arts
    /** One art the player can use now: a Saiyan technique ({@code op > 0}) or a Dragon Ball character's active move. */
    record Art(String id, String name, int op, Spec spec, Mode mode, boolean ult) {}

    /** The arts open for this player: the Saiyan techniques (awakened Saiyan) and the Dragon Ball moves of every character whose Battle Power or old skill opens it. At most {@link SaiyanProdigyModes#MAX_ARTS}. */
    static List<Art> arts(ServerPlayer p, ManasSkillInstance instance) {
        List<Art> out = new ArrayList<>();
        if (SaiyanProdigyHub.awakened(p) && SaiyanPhaseConfig.enabled()) {
            for (SaiyanProdigyModes.Art a : SaiyanProdigyModes.SAIYAN_ARTS) out.add(new Art(a.id(), a.name(), a.op(), null, null, false));
        }
        if (SaiyanProdigyConfig.dbArts() && NUConfig.DB_ENABLED.get()) {
            for (Open o : openSpecs(p, instance)) {
                boolean fin = o.finisher;
                for (Mode m : o.spec.actives()) {
                    if (m.mastery() && !fin) continue;
                    if (out.size() >= SaiyanProdigyModes.MAX_ARTS) return out;
                    out.add(new Art(o.spec.id() + "/" + m.id(), o.spec.title() + ": " + m.name(), 0, o.spec, m, o.ult));
                }
            }
        }
        return out;
    }

    /** A Dragon Ball character open to this player, with the strength it is used at. */
    record Open(Spec spec, boolean ult, boolean finisher) {}

    static List<Open> openSpecs(ServerPlayer p, ManasSkillInstance instance) {
        List<Open> out = new ArrayList<>();
        Skills skills = SkillAPI.getSkillsFrom(p);
        double bp = SaiyanProdigyHub.battlePower(p), scale = SaiyanProdigyConfig.dbArtsBpScale();
        boolean mastered = instance.isMastered(p);
        for (Spec s : DbRules.SPECS) {
            boolean ownsUnique = false, ownsUlt = false;
            if (skills != null) {
                try {
                    ownsUnique = skills.getSkill(DbSkills.UNIQUE.get(s.id()).getId()).isPresent();
                    ownsUlt = skills.getSkill(DbSkills.ULTIMATE.get(s.id()).getId()).isPresent();
                } catch (RuntimeException ignored) { /* a skill storage that cannot be read now: only the Battle Power opens it */ }
            }
            double unlock = SaiyanProdigyModes.dbUnlockBp(s.scale(), scale);
            if (!SaiyanProdigyModes.dbOpen(bp, unlock, ownsUnique || ownsUlt)) continue;
            out.add(new Open(s, SaiyanProdigyModes.dbUltimateStrength(bp, unlock, mastered, ownsUlt), SaiyanProdigyModes.dbFinisher(bp, unlock, mastered, ownsUlt)));
        }
        return out;
    }

    private void nextArt(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag) {
        List<Art> list = arts(p, instance);
        if (list.isEmpty()) { SkillUtil.fail(p, SkillMsg.t("prodigy.no_arts", "No art is open to you yet.")); return; }
        String cur = tag.getString(ART);
        int at = -1;
        for (int i = 0; i < list.size(); i++) if (list.get(i).id().equals(cur)) at = i;
        Art next = list.get((at + 1) % list.size());
        tag.putString(ART, next.id());
        instance.markDirty();
        p.displayClientMessage(SkillMsg.t("prodigy.art_selected", "Art: %s", artName(next.id(), next.name())), true);
    }

    private void art(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now, int modeNumber) {
        if (now < tag.getLong(ART_AT)) return;
        List<Art> list = arts(p, instance);
        if (list.isEmpty()) { SkillUtil.fail(p, SkillMsg.t("prodigy.no_arts", "No art is open to you yet.")); return; }
        String cur = tag.getString(ART);
        Art pick = list.get(0);
        for (Art a : list) if (a.id().equals(cur)) pick = a;
        if (!pick.id().equals(cur)) { tag.putString(ART, pick.id()); instance.markDirty(); }
        boolean done = pick.op() > 0 ? saiyanArt(p, pick) : dragonBallArt(instance, p, tag, pick, now, modeNumber);
        if (done) {
            tag.putLong(ART_AT, now + SaiyanProdigyConfig.artCooldownSeconds() * 20L);
            growSlowly(instance, p, tag, now);
        }
    }

    private boolean saiyanArt(ServerPlayer p, Art a) {
        SaiyanProdigyHub.act(p, a.op());
        return true;
    }

    private boolean dragonBallArt(ManasSkillInstance inst, ServerPlayer p, CompoundTag tag, Art a, long now, int modeNumber) {
        Spec spec = a.spec();
        Mode m = a.mode();
        CompoundTag ready = tag.getCompound(ART_READY);
        long until = ready.getLong(a.id());
        if (now < until) { SkillUtil.fail(p, SkillMsg.t("prodigy.recharging", "%s is recharging (%ss).", a.name(), (until - now + 19) / 20)); return false; }
        boolean mastered = inst.isMastered(p);
        double cost = p.isCreative() ? 0 : price(inst, p, DMUtil.cost(DbRules.cost(m, mastered, 1.0) * (a.ult() ? 1.0 : DbRules.UNIQUE_FACTOR + 0.5)));
        if (!charge(inst, p, modeNumber, cost)) return false;
        if (!DbMoves.cast(spec, a.ult(), m, p, mastered ? 1.0 : 0.4)) { SkillUtil.refund(p, cost); return false; }
        int seconds = DbPassives.cooldownSeconds(DbRules.cooldown(m, mastered), p, spec);
        for (String k : new ArrayList<>(ready.getAllKeys())) if (ready.getLong(k) <= now) ready.remove(k);      // only the arts still recharging are kept (the tag stays small)
        if (seconds > 0) ready.putLong(a.id(), now + seconds * 20L);
        tag.put(ART_READY, ready);
        inst.markDirty();
        SkillUtil.castVfx(p, m.move().rgb() == 0xFFFFFFFF ? 0xFFE0F0FF : m.move().rgb());
        return true;
    }

    // ---------------------------------------------------------------------------------------------------- the fighting style
    private void style(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, boolean next) {
        if (!NUConfig.DB_ENABLED.get() || !SaiyanProdigyConfig.dbArts()) { SkillUtil.fail(p, SkillMsg.t("prodigy.style_off", "Fighting styles are switched off on this server.")); return; }
        String cur = tag.getString(STYLE);
        if (!next && !cur.isEmpty()) { stopStyle(instance, p, tag); p.displayClientMessage(SkillMsg.t("prodigy.style_ended", "Your fighting style fades."), true); return; }
        List<Open> open = openSpecs(p, instance);
        if (open.isEmpty()) { SkillUtil.fail(p, SkillMsg.t("prodigy.no_style", "No fighting style is open to you yet.")); return; }
        String want = next ? cur : (tag.getString(STYLE_LAST).isEmpty() ? cur : tag.getString(STYLE_LAST));
        int at = -1;
        for (int i = 0; i < open.size(); i++) if (open.get(i).spec().id().equals(want)) at = i;
        Open pick = open.get(next ? (at + 1) % open.size() : Math.max(0, at));
        tag.putString(STYLE, pick.spec().id());
        tag.putString(STYLE_LAST, pick.spec().id());
        tag.putBoolean(STYLE_ULT, pick.ult());
        if (!instance.isToggled()) instance.setToggled(true);      // the passives run only for a toggled skill (the old rule); the player's own on/off of the skill is the master switch
        instance.markDirty();
        DbPassives.clear(p);
        p.displayClientMessage(SkillMsg.t("prodigy.style_set", "Fighting style: %s", pick.spec().title()), true);
    }

    private void stopStyle(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag) {
        tag.putString(STYLE, "");
        instance.markDirty();
        DbPassives.clear(p);
    }

    // ---------------------------------------------------------------------------------------------------- once a second
    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity e) {
        if (!(e instanceof ServerPlayer p) || p.tickCount % DbRules.PASSIVE_PERIOD_TICKS != 0) return;
        CompoundTag tag = instance.getOrCreateTag();
        boolean sy = SaiyanStore.isSaiyan(p), awk = SaiyanProdigyHub.awakened(p), gk = SaiyanProdigyHub.goku(p), changed = false;
        if (tag.getBoolean(SY) != sy) { tag.putBoolean(SY, sy); changed = true; }
        if (tag.getBoolean(AWK) != awk) { tag.putBoolean(AWK, awk); changed = true; }
        if (tag.getBoolean(GK) != gk) { tag.putBoolean(GK, gk); changed = true; }
        String style = tag.getString(STYLE);
        if (!style.isEmpty()) {
            Spec spec = DbRules.spec(style);
            if (spec == null || !SaiyanProdigyHub.enabled() || !NUConfig.DB_ENABLED.get() || !SaiyanProdigyConfig.dbArts()) { tag.putString(STYLE, ""); changed = true; DbPassives.clear(p); }
            else DbPassives.second(instance, p, spec, tag.getBoolean(STYLE_ULT), instance.isMastered(p), true);
        }
        if (changed) instance.markDirty();
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity e) {
        super.onToggleOff(instance, e);
        if (e instanceof ServerPlayer p && !instance.getOrCreateTag().getString(STYLE).isEmpty()) stopStyle(instance, p, instance.getOrCreateTag());
    }

    @Override
    public void onForgetSkill(ManasSkillInstance instance, LivingEntity e) {
        super.onForgetSkill(instance, e);
        if (e instanceof ServerPlayer p) DbPassives.clear(p);
    }

    // ---------------------------------------------------------------------------------------------------- names
    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        Component base = super.getModeName(i, mode);
        ModeSpec m = bundle().at(mode);
        CompoundTag t = i.getTag();
        if (t == null) return base;
        if (m.action() == SaiyanProdigyModes.ACT_ART && !t.getString(ART).isEmpty()) return base.copy().append(Component.literal(": ")).append(artName(t.getString(ART), ""));
        if (m.action() == SaiyanProdigyModes.ACT_STYLE && !t.getString(STYLE).isEmpty()) {
            Spec s = DbRules.spec(t.getString(STYLE));
            if (s != null) return base.copy().append(Component.literal(": " + s.title()));
        }
        return base;
    }

    /** The name of an art id: a Saiyan art by its lang key, a Dragon Ball art by the character and move names. {@code fallback} is used for an id that is neither (a removed art). */
    static Component artName(String id, String fallback) {
        SaiyanProdigyModes.Art a = SaiyanProdigyModes.saiyanArt(id);
        if (a != null) return Component.translatableWithFallback("anime_multiverse.skill.art." + a.id(), a.name());
        int slash = id.indexOf('/');
        if (slash > 0) {
            Spec s = DbRules.spec(id.substring(0, slash));
            if (s != null) for (Mode m : s.modes()) if (m.id().equals(id.substring(slash + 1))) return Component.literal(s.title() + ": " + m.name());
        }
        return Component.literal(fallback.isEmpty() ? id : fallback);
    }
}
