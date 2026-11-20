package com.newuniverse.nusmp.skill;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.beastwill.BeastWillState;
import com.newuniverse.nusmp.beastwill.BeastWillStore;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.relics.djinn.DjinnState;
import com.newuniverse.nusmp.skill.rules.WeaponSkillRules;
import com.newuniverse.nusmp.skill.rules.WeaponSkillRules.Kind;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Weapon skills (docs/weapon_skills.md): registration of the 12 skills (ten Vassal weapons, the Djinn Equip, the Beast Will), the grant and the take-back, and the admin command. A skill is granted when its owner-ship begins (a
 * Vassal weapon bonds, a Djinn is bound or lent, a Beast Will is absorbed) and taken back when it ends (the bond is released, the Djinn is let go, the last will is cleared); the three systems call in from the one place each
 * of their changes passes ({@code HeroBonds} listener, {@code DjinnState.bind / lend / clearBond}, {@code BeastWillAcquire.grant} and the clear command), and one pass at login and respawn sets right whatever an older save or
 * a missed event left. Cost: nothing per tick. One skill-storage read per change and per login; one entry per online player (the last action tick), dropped at logout. Everything keyed by player UUID, server thread only.
 * Obtain by hand: {@code /multiverse weaponskill give|take <player> <id>} (permission 2).
 */
public final class WeaponSkills {
    private WeaponSkills() {}

    /** Registration order: the ten weapons, the Djinn, the Beast Will. */
    private static final Map<String, DeferredHolder<ManasSkill, WeaponSkill>> BY_ID = new LinkedHashMap<>();

    /** The tick of the last action of each online player (one action per player per tick). */
    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();

    /** Registers the 12 skills; called once from {@link NUSkills}. Returns how many. */
    static int registerAll(DeferredRegister<ManasSkill> reg) {
        for (HeroKind k : WeaponSkillRules.heroKinds()) BY_ID.put(WeaponSkillRules.heroId(k), reg.register(WeaponSkillRules.heroId(k), () -> new WeaponSkill(k)));
        BY_ID.put(WeaponSkillRules.DJINN_ID, reg.register(WeaponSkillRules.DJINN_ID, () -> new WeaponSkill(Kind.DJINN)));
        BY_ID.put(WeaponSkillRules.BEAST_ID, reg.register(WeaponSkillRules.BEAST_ID, () -> new WeaponSkill(Kind.BEAST)));
        return BY_ID.size();
    }

    /** The holder of a skill by its id ({@code ws_vassal_fan}), or null. */
    public static DeferredHolder<ManasSkill, WeaponSkill> holder(String id) { return BY_ID.get(id); }

    public static int count() { return BY_ID.size(); }

    /** Called once from the mod constructor: the bond listener of the Vassal weapons and the login, respawn and logout hooks. */
    public static void init() {
        HeroBonds.addListener(new HeroBonds.Listener() {
            @Override
            public void onBound(ServerPlayer player, HeroBond bond) { if (WeaponSkillRules.coversHero(bond.kind())) set(player, WeaponSkillRules.heroId(bond.kind()), true); }

            @Override
            public void onReleased(ServerPlayer player, HeroKind was) { if (WeaponSkillRules.coversHero(was)) set(player, WeaponSkillRules.heroId(was), false); }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) reconcile(p); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) reconcile(p); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> LAST.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> LAST.clear());
        WeaponSkillSelfTests.register();
    }

    /** True when this player may act on this tick (and notes it): at most one action per player per tick. */
    static boolean turn(ServerPlayer p) {
        long now = p.level().getGameTime();
        Long last = LAST.put(p.getUUID(), now);
        return last == null || WeaponSkillRules.due(now, last);
    }

    /** Forgets the player's last action tick (logout; the self-test, which presses twice in one tick). */
    static void forgetTurn(ServerPlayer p) { LAST.remove(p.getUUID()); }

    // ---------------------------------------------------------------- grant and take back
    private static Skills skillsOf(ServerPlayer p) {
        try {
            return SkillAPI.getSkillsFrom(p);
        } catch (RuntimeException noSkills) {
            return null;
        }
    }

    public static boolean has(ServerPlayer p, String id) {
        DeferredHolder<ManasSkill, WeaponSkill> h = BY_ID.get(id);
        Skills s = h == null ? null : skillsOf(p);
        return s != null && s.getSkill(h.getId()).isPresent();
    }

    /** Makes the player hold the skill ({@code want}) or not. A new skill is only given while the setting {@code weaponSkills} is on; taking one back always works. Never throws. */
    static void set(ServerPlayer p, String id, boolean want) {
        DeferredHolder<ManasSkill, WeaponSkill> h = BY_ID.get(id);
        Skills skills = h == null || p == null ? null : skillsOf(p);
        if (skills == null) return;
        try {
            boolean held = skills.getSkill(h.getId()).isPresent();
            if (want && !held && NUConfig.WEAPON_SKILLS.get()) {
                ManasSkill skill = h.get();
                skills.learnSkill(skill.createDefaultInstance(), Component.translatableWithFallback("anime_multiverse.skill.msg.weapon.granted", "%s is now on your skill list.", skill.getName()).withStyle(ChatFormatting.GOLD));
            } else if (!want && held) {
                skills.forgetSkill(h.getId(), Component.translatableWithFallback("anime_multiverse.skill.msg.weapon.revoked", "%s leaves your skill list.", h.get().getName()).withStyle(ChatFormatting.GRAY));
            }
        } catch (RuntimeException skillStorageNotReady) {
            // the next login sets it right
        }
    }

    /** The Djinn Equip skill follows the bond (an own Djinn or a lent one): called by {@link DjinnState} after every change of it. */
    public static void syncDjinn(ServerPlayer p) { set(p, WeaponSkillRules.DJINN_ID, DjinnState.of(p).active() != null); }

    /** The Beast Will skill follows the wills: called when a will is bound and when the wills are cleared. */
    public static void syncBeast(ServerPlayer p) {
        BeastWillState s = BeastWillStore.peek(p);
        set(p, WeaponSkillRules.BEAST_ID, s != null && !s.active.isEmpty());
    }

    /** Login and respawn, once: every skill is set to what the player owns now (a weapon bonded, a Djinn held or a will carried before this release, an event that was missed). Twelve storage reads. */
    static void reconcile(ServerPlayer p) {
        HeroKind bound = HeroBonds.kindOf(p);
        for (HeroKind k : WeaponSkillRules.heroKinds()) set(p, WeaponSkillRules.heroId(k), k == bound);
        syncDjinn(p);
        syncBeast(p);
    }

    // ---------------------------------------------------------------- the command
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("weaponskill").requires(c -> c.hasPermission(2))
                .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(BY_ID.keySet(), b))
                                .executes(c -> change(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "id"), true)))))
                .then(Commands.literal("take").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(BY_ID.keySet(), b))
                                .executes(c -> change(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "id"), false)))))
                .then(Commands.literal("sync").then(Commands.argument("player", EntityArgument.player()).executes(c -> {
                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                    reconcile(p);
                    c.getSource().sendSuccess(() -> Component.literal("Set the weapon skills of " + p.getName().getString() + " to what they own now."), true);
                    return 1;
                })))
                .then(Commands.literal("list").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.literal(String.join(", ", BY_ID.keySet())), false);
                    return BY_ID.size();
                }));
    }

    /** The admin door: gives or takes a skill whether or not the player owns what it speaks for (a tester's tool; the skill still refuses to act without ownership). */
    private static int change(CommandSourceStack src, ServerPlayer p, String id, boolean give) {
        DeferredHolder<ManasSkill, WeaponSkill> h = BY_ID.get(id);
        if (h == null) { src.sendFailure(Component.literal("Unknown weapon skill '" + id + "'. Use /multiverse weaponskill list.")); return 0; }
        Skills skills = skillsOf(p);
        if (skills == null) { src.sendFailure(Component.literal("That player has no skill storage.")); return 0; }
        boolean held = skills.getSkill(h.getId()).isPresent();
        if (give == held) { src.sendFailure(Component.literal(p.getName().getString() + (give ? " already has it." : " does not have it."))); return 0; }
        if (give) skills.learnSkill(h.get().createDefaultInstance(), Component.literal("You are granted " + h.get().getName().getString() + ".").withStyle(ChatFormatting.GOLD));
        else skills.forgetSkill(h.getId(), Component.literal(h.get().getName().getString() + " leaves your skill list.").withStyle(ChatFormatting.GRAY));
        src.sendSuccess(() -> Component.literal((give ? "Gave " : "Took ") + h.get().getName().getString() + (give ? " to " : " from ") + p.getName().getString() + "."), true);
        return 1;
    }
}
