package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.entity.ZagredBossEntity;
import com.newuniverse.nusmp.item.OtherworldTridentItem;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 0.47 Kotodama Magic (Word Soul), Zagred's magic: words spoken by the caster become law. A God-class (Tensura ULTIMATE) skill.
 * <ul>
 *   <li><b>Speaking:</b> type a command word alone in chat ("Halt!", "Shatter.", "Heal") or cast its page. The word flashes in
 *       mid-air as demonic glyphs ({@link VfxShape#KOTO_WORDS}).</li>
 *   <li><b>Who can speak:</b>
 *     <ul>
 *       <li>{@link Source#GRIMOIRE}: the Kotodama grimoire is summoned, and the speaker is in creative or has beaten Zagred
 *           ({@link #earned}). Every word, at full power.</li>
 *       <li>{@link Source#DEVIL}: a grimoire with Zagred as its devil, in Devil Union (this replaced Zagred's old kneel aura).
 *           Only Halt, Shatter and Heal, at reduced power.</li>
 *       <li>{@link Source#BOSS}: the Zagred boss.</li>
 *     </ul></li>
 *   <li><b>Pools:</b> an astronomical magicule cost (a share of max magicules with a very high floor); what magicules can't
 *       cover is paid from Aura. Creative is free.</li>
 *   <li><b>EP scaling:</b> {@link EnergyBridge#power} drives radius, damage and counts.</li>
 *   <li><b>Effects:</b> Absolute Paralysis (Tensura paralysis + a velocity lock), Magic Jamming (Tensura silence), Spiritual
 *       Damage, and Soul Annihilation when a target's spiritual health is driven to its floor.</li>
 * </ul>
 */
public final class KotodamaWords {
    private KotodamaWords() {}

    public static final int VIOLET = 0xFF8A4CFF, ABYSS = 0xFF1E1640;
    static final String K_CD = "nusmp_koto_cd_", K_EARNED = "nusmp_kotodama_earned", K_SOUL = "nusmp_soul_annihilated";

    public enum Source { GRIMOIRE, DEVIL, BOSS }

    /** The command words. frac / floor: magicule cost (share of max, flat floor); cooldown in ticks. */
    public enum Word {
        HALT("Halt", 0.30, 40_000, 600, true, "halt", "bind", "kneel", "stop", "freeze"),
        SHATTER("Shatter", 0.20, 20_000, 300, true, "shatter", "return", "vanish", "break"),
        HEAL("Heal", 0.25, 30_000, 900, true, "heal", "mend", "restore"),
        SLUDGE("Devour", 0.35, 60_000, 1200, false, "underworld", "sludge", "devour", "drown"),
        TRIDENT("Trident", 0.25, 25_000, 1200, false, "trident", "spear", "pierce"),
        SWORDS("Swords", 0.40, 80_000, 900, false, "swords", "blades", "storm");

        public final String spoken;
        final double frac, floor;
        final int cooldown;
        final boolean devil;            // Zagred's devil can lend this word in Devil Union
        final String[] aliases;

        Word(String spoken, double frac, double floor, int cooldown, boolean devil, String... aliases) {
            this.spoken = spoken; this.frac = frac; this.floor = floor; this.cooldown = cooldown; this.devil = devil; this.aliases = aliases;
        }

        /** The word a chat line speaks: the whole message must be one command word (punctuation allowed), else null. */
        public static Word parse(String text) {
            if (text == null) return null;
            String s = text.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", "").trim();
            if (s.isEmpty() || s.contains(" ")) return null;
            for (Word w : values()) for (String a : w.aliases) if (a.equals(s)) return w;
            return null;
        }
    }

    // ---------------------------------------------------------------- access
    /** True if this player has beaten Zagred (kept through death). */
    public static boolean earned(Player p) {
        return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(K_EARNED);
    }

    static void setEarned(Player p) {
        CompoundTag root = p.getPersistentData();
        CompoundTag kept = root.getCompound(Player.PERSISTED_NBT_TAG);
        kept.putBoolean(K_EARNED, true);
        root.put(Player.PERSISTED_NBT_TAG, kept);
    }

    /** The player's grimoire whose devil is Zagred, or null. */
    static ManasSkillInstance zagredGrimoire(ServerPlayer p) {
        var skills = SkillAPI.getSkillsFrom(p);
        for (var id : NUSkills.allGrimoireSkillIds()) {
            var inst = skills.getSkill(id).orElse(null);
            if (inst != null && Devil.byName(inst.getOrCreateTag().getString("Devil")) == Devil.ZAGRED) return inst;
        }
        return null;
    }

    /** In Devil Union with Zagred (the book grimoires' Devil Union, or the old grimoire skill's union). */
    static boolean zagredUnion(ServerPlayer p) {
        ManasSkillInstance g = zagredGrimoire(p);
        if (g == null) return false;
        return GrimoireBook.inDevilState(p) || p.level().getGameTime() < g.getOrCreateTag().getLong("UnionUntil");
    }

    /** Which tier this player would speak with right now (null = the words are only words). */
    public static Source sourceOf(ServerPlayer p) {
        if (GrimoireSummon.isFloating(p, MagicType.KOTODAMA)) return Source.GRIMOIRE;
        if (zagredUnion(p)) return Source.DEVIL;
        return null;
    }

    // ---------------------------------------------------------------- chat
    /** A command word typed alone in chat is spoken (the line still shows in chat). */
    public static void onChat(ServerChatEvent e) {
        ServerPlayer p = e.getPlayer();
        Word w = Word.parse(e.getRawText());
        if (w == null) return;
        Source src = sourceOf(p);
        if (src == null) return;
        p.getServer().execute(() -> speak(p, w, src));
    }

    // ---------------------------------------------------------------- speaking
    /** Power for this speaker: God-class for the grimoire, a devil's loan in Union, fixed for the boss. */
    static float power(LivingEntity c, Source src) {
        return switch (src) {
            case GRIMOIRE -> EnergyBridge.power(c) * 1.5f;
            case DEVIL -> EnergyBridge.power(c) * 0.6f;
            case BOSS -> c instanceof ZagredBossEntity z ? 1.6f + 0.3f * z.phase() : 2f;
        };
    }

    /** Speaks a word. Returns true if reality obeyed (cost paid, cooldown started). */
    public static boolean speak(LivingEntity caster, Word w, Source src) {
        if (!(caster.level() instanceof ServerLevel level)) return false;
        ServerPlayer p = caster instanceof ServerPlayer sp ? sp : null;
        long now = level.getGameTime();
        if (p != null) {
            if (src == Source.GRIMOIRE && !p.isCreative() && !earned(p)) {
                GrimoireBook.fail(p, "The Word Soul does not answer. Kotodama is creative-only until Zagred is defeated.");
                return false;
            }
            if (src == Source.DEVIL && !w.devil) { GrimoireBook.fail(p, "Zagred lends you only Halt, Shatter and Heal."); return false; }
            long ready = p.getPersistentData().getLong(K_CD + w.name());
            if (now < ready) { GrimoireBook.fail(p, w.spoken + " still echoes (" + (ready - now + 19) / 20 + "s)."); return false; }
            double cost = src == Source.DEVIL ? BalanceLaw.cost(p, 10, 500) : BalanceLaw.cost(p, w.frac * 100, w.floor);
            if (!pay(p, cost)) { GrimoireBook.fail(p, "Your magicules and aura cannot carry the word \"" + w.spoken + "\"."); return false; }
        }
        float pw = power(caster, src);
        boolean done = switch (w) {
            case HALT -> halt(caster, pw, src);
            case SHATTER -> shatter(caster, pw, src);
            case HEAL -> heal(caster, pw, src);
            case SLUDGE -> sludge(caster, pw);
            case TRIDENT -> trident(caster, pw);
            case SWORDS -> swords(caster, pw);
        };
        if (!done) return false;
        glyphs(caster, w, src, pw);
        if (p != null) {
            int cd = (int) (w.cooldown * (src == Source.DEVIL ? 1.5 : 1) * (p.isCreative() ? 0.25 : 1));
            p.getPersistentData().putLong(K_CD + w.name(), now + cd);
            Component line = Component.literal("「" + w.spoken + "」").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
            for (ServerPlayer o : level.players()) if (o.distanceToSqr(p) < 48 * 48) o.displayClientMessage(line, true);
        }
        return true;
    }

    /** Magicules first, then Aura for what they can't cover (Aura bridging). Creative / no Tensura data: free. */
    static boolean pay(ServerPlayer p, double cost) {
        if (p.isCreative()) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return true;
        if (ex.getMagicule() + ex.getAura() < cost) return false;
        double fromMp = Math.min(cost, ex.getMagicule());
        ex.setMagicule(ex.getMagicule() - fromMp);
        if (cost > fromMp) ex.setAura(Math.max(0, ex.getAura() - (cost - fromMp)));
        ex.markDirty();
        return true;
    }

    /** The demonic glyphs of the word, flashing in mid-air in front of the speaker. */
    static void glyphs(LivingEntity c, Word w, Source src, float pw) {
        Vec3 eye = c.getEyePosition(), look = c.getViewVector(1f);
        long seed = w.ordinal() | ((long) src.ordinal() << 4) | ((long) c.getRandom().nextInt(1 << 20) << 8);
        VfxSpawn.send((ServerLevel) c.level(), new VfxPayload(VfxShape.KOTO_WORDS.ordinal(), eye.add(look.scale(2.2)), eye.add(look.scale(6)),
                VIOLET, 40, Math.min(2.4f, pw), -1, seed));
        c.level().playSound(null, c.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 1.2f, 0.6f);
    }

    // ---------------------------------------------------------------- targets & damage
    /** Everyone the speaker's words turn against within r of 'at'. The boss spares itself and creative players. */
    public static List<LivingEntity> foes(LivingEntity c, Vec3 at, double r) {
        return c.level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r), e -> e != c && e.isAlive() && !e.isSpectator()
                && !e.isAlliedTo(c) && e.distanceToSqr(at) <= r * r
                && !(c instanceof ZagredBossEntity && (e instanceof ZagredBossEntity || e instanceof Player pl && pl.isCreative())));
    }

    /** Darkness-element damage from the speaker: through the speaker's grimoire for players (balance law), plain magic for the boss. */
    public static void hurt(LivingEntity c, LivingEntity t, float raw) {
        if (t == c || t.isAlliedTo(c)) return;
        if (c instanceof ServerPlayer p) {
            ManasSkillInstance inst = SkillAPI.getSkillsFrom(p).getSkill(NUSkills.BOOK_KOTODAMA.getId()).orElse(null);
            if (inst == null) inst = zagredGrimoire(p);
            if (inst != null && inst.getSkill() instanceof GrimoireBook b) { b.hurtAs(inst, p, t, 0, raw, TensuraDamageTypes.DARKNESS_ELEMENTAL); return; }
            t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, t, raw, 0.5));
            return;
        }
        t.hurt(c.damageSources().indirectMagic(c, c), raw);
    }

    /**
     * Spiritual damage, and Soul Annihilation: once the target's spiritual health is at its floor, the soul itself is struck
     * (a heavy blow, every magicule torn out, terror). Bosses take a tenth; once per 5 s per target.
     */
    public static void spirit(LivingEntity c, LivingEntity t, double amount) {
        EnergyBridge.spirit(t, amount);
        var ex = TensuraStorages.getExistenceFrom(t);
        if (ex == null || ex.getSpiritualHealth() > 1) return;
        long now = t.level().getGameTime();
        if (now < t.getPersistentData().getLong(K_SOUL)) return;
        t.getPersistentData().putLong(K_SOUL, now + 100);
        hurt(c, t, t.getMaxHealth() * (BalanceLaw.isBoss(t) ? 0.1f : 0.6f));
        EnergyBridge.drain(t, c, 1.0);
        EnergyBridge.effect(t, "fear", 100, 1);
        VfxSpawn.send((ServerLevel) t.level(), VfxShape.KOTO_SHATTER, t.getBoundingBox().getCenter(), t.position(), ABYSS, 30, 1.6f);
        if (c instanceof ServerPlayer p) p.displayClientMessage(Component.literal("Soul Annihilation: " + t.getName().getString())
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), true);
    }

    // ---------------------------------------------------------------- the words
    /**
     * "Halt" / "Bind": Absolute Paralysis and Magic Jamming over a massive area. Everyone in range is paralysed (Tensura
     * paralysis, held in place by a velocity lock), their magic jammed (Tensura silence), and their spirit struck.
     */
    static boolean halt(LivingEntity c, float pw, Source src) {
        ServerLevel level = (ServerLevel) c.level();
        double r = src == Source.DEVIL ? 8 + 4 * pw : 12 + 8 * pw;
        List<LivingEntity> hit = foes(c, c.position(), r);
        for (LivingEntity t : hit) {
            int ticks = BalanceLaw.controlTicks(t, (int) (100 * pw));
            EnergyBridge.effect(t, "paralysis", ticks, 1);
            EnergyBridge.effect(t, "silence", ticks * 2, 1);
            spirit(c, t, src == Source.DEVIL ? 1 : 2 + pw);
            if (t instanceof Mob m) { m.getNavigation().stop(); m.setTarget(null); }
            Vec3 at = t.position();
            SpellRuntime.zone(level, ticks, 1, age -> {                       // the velocity lock
                if (!t.isAlive()) return;
                if (t.position().distanceToSqr(at) > 0.04) t.teleportTo(at.x, Math.min(t.getY(), at.y + 0.1), at.z);
                t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
                t.hurtMarked = true;
            });
        }
        VfxSpawn.sendFollowing(level, VfxShape.KOTO_AURA, c, c.position(), VIOLET, 30, (float) (r / 6));
        level.playSound(null, c.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.5f, 0.6f);
        return true;
    }

    /**
     * "Shatter" / "Return": every enemy projectile in range breaks apart and returns to magicules (the speaker gets 1% of max
     * magicules back for each). Spell bolts from other casters go the same way.
     */
    static boolean shatter(LivingEntity c, float pw, Source src) {
        ServerLevel level = (ServerLevel) c.level();
        double r = src == Source.DEVIL ? 10 + 4 * pw : 16 + 8 * pw;
        int n = 0;
        for (Projectile pr : level.getEntitiesOfClass(Projectile.class, c.getBoundingBox().inflate(r),
                x -> x.getOwner() != c && !(x.getOwner() != null && x.getOwner().isAlliedTo(c)))) {
            if (n < 16) VfxSpawn.send(level, VfxShape.KOTO_SHATTER, pr.position(), c.getBoundingBox().getCenter(), VIOLET, 24, 0.8f);
            pr.discard();
            n++;
        }
        n += SpellRuntime.dissolveBolts(level, c.position(), r, c);
        if (n > 0 && c instanceof ServerPlayer p) {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex != null) {
                ex.setMagicule(Math.min(EnergyHelper.getMaxMagicule(p), ex.getMagicule() + EnergyHelper.getMaxMagicule(p) * 0.01 * n));
                ex.markDirty();
            }
            p.displayClientMessage(Component.literal(n + " attack" + (n == 1 ? "" : "s") + " returned to magicules.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        VfxSpawn.sendFollowing(level, VfxShape.KOTO_SHATTER, c, c.getBoundingBox().getCenter(), VIOLET, 30, (float) (r / 8));
        level.playSound(null, c.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, 0.5f);
        return true;
    }

    /** "Heal": health and Aura to full, every harmful effect gone. With the grimoire, allies within 8 blocks too. */
    static boolean heal(LivingEntity c, float pw, Source src) {
        List<LivingEntity> who = new ArrayList<>();
        who.add(c);
        if (src == Source.GRIMOIRE) who.addAll(c.level().getEntitiesOfClass(LivingEntity.class, c.getBoundingBox().inflate(8),
                e -> e != c && e.isAlive() && e.isAlliedTo(c)));
        for (LivingEntity t : who) {
            double share = src == Source.DEVIL ? 0.5 : src == Source.BOSS ? 0.25 : 1.0;   // the boss heals a quarter, once
            t.setHealth((float) Math.min(t.getMaxHealth(), t.getHealth() + t.getMaxHealth() * share));
            var ex = TensuraStorages.getExistenceFrom(t);
            if (ex != null) {
                double maxAura = EnergyHelper.getMaxAura(t);
                ex.setAura(Math.min(maxAura, ex.getAura() + maxAura * (src == Source.GRIMOIRE ? 1.0 : 0.3)));
                ex.markDirty();
            }
            for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects()))
                if (!e.getEffect().value().isBeneficial()) t.removeEffect(e.getEffect());
            VfxSpawn.sendFollowing((ServerLevel) t.level(), VfxShape.KOTO_AURA, t, t.position(), 0xFFC8A0FF, 30, 0.8f);
        }
        c.level().playSound(null, c.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.2f, 0.6f);
        return true;
    }

    /** "Devour" (underworld sludge): purple-black matter floods out from where the speaker looks and devours the ground. */
    static boolean sludge(LivingEntity c, float pw) {
        Vec3 at = c instanceof ServerPlayer p ? GrimoireBook.aim(p, 24) : c.position();
        int radius = (int) (5 + 4 * pw);
        return UnderworldMatter.start((ServerLevel) c.level(), c, BlockPos.containing(at), radius, 300);
    }

    /** "Trident": an otherworldly trident forms in the speaker's hand (bound, gone after 60 s). */
    static boolean trident(LivingEntity c, float pw) {
        if (!(c instanceof ServerPlayer p)) return false;                   // the boss throws its own
        var stack = OtherworldTridentItem.bound(p);
        if (!p.getInventory().add(stack)) p.drop(stack, false);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.KOTO_TRIDENT, p, p.getEyePosition(), VIOLET, 30, 1f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.2f, 0.5f);
        return true;
    }

    /** "Swords": a storm of demon swords falls on the target area, each a physical blow that also cuts the spirit. */
    static boolean swords(LivingEntity c, float pw) {
        ServerLevel level = (ServerLevel) c.level();
        Vec3 at = c instanceof ServerPlayer p ? GrimoireBook.aim(p, 32)
                : c instanceof Mob m && m.getTarget() != null ? m.getTarget().position() : c.position();
        int n = 8 + 4 * MirrorWorks.tier(pw / 1.5f) + (int) pw * 2;
        double spread = 3 + 2 * pw;
        for (int k = 0; k < n; k++) {
            double a = c.getRandom().nextDouble() * Math.PI * 2, d = Math.sqrt(c.getRandom().nextDouble()) * spread;
            Vec3 ground = at.add(Math.cos(a) * d, 0, Math.sin(a) * d), sky = ground.add(c.getRandom().nextGaussian() * 1.5, 14, c.getRandom().nextGaussian() * 1.5);
            int delay = 4 + k * 2;
            SpellRuntime.later(level, delay, () -> VfxSpawn.send(level, VfxShape.KOTO_SWORDS, sky, ground, VIOLET, 14, 1f));
            SpellRuntime.later(level, delay + 8, () -> {
                for (LivingEntity t : foes(c, ground, 1.8)) { hurt(c, t, 6f * pw); spirit(c, t, 1); }
                level.playSound(null, BlockPos.containing(ground), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.HOSTILE, 0.8f, 0.6f);
            });
        }
        return true;
    }

    // ---------------------------------------------------------------- grimoire aura & the boss reward
    /** The corrupted purple-black aura and the glowing five-leaf emblem while the Kotodama grimoire is out. */
    public static void aura(ServerPlayer p) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.KOTO_AURA, p, p.position(), VIOLET, 70, 1f);
    }

    /** Beating Zagred (with the reward switched on): Kotodama is earned and the five-leaf grimoire comes to the player. */
    public static void reward(ServerPlayer p) {
        if (!com.newuniverse.nusmp.NUGameRules.kotodamaBossReward(p.level())) return;
        setEarned(p);
        var skills = SkillAPI.getSkillsFrom(p);
        var book = NUSkills.BOOK_KOTODAMA.get();
        if (skills.getSkill(book.getRegistryName()).isPresent()) return;
        ManasSkillInstance inst = book.createDefaultInstance();
        CompoundTag t = inst.getOrCreateTag();
        t.putString("Magic", MagicType.KOTODAMA.name());
        t.putString("Cover", GrimoireCover.FIVE_LEAF.name());
        t.putInt("Leaves", GrimoireCover.FIVE_LEAF.tier);
        t.putString("Devil", Devil.ZAGRED.name());
        // the grimoire itself arrives through GrimoireBook.onLearnSkill
        skills.learnSkill(inst, Component.literal("The Word Soul is yours. Kotodama Magic awakens.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
    }
}
