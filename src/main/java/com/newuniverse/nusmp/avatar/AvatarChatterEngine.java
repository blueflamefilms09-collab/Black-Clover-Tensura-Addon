package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.chatter.ChatterDictionary;
import com.newuniverse.nusmp.avatar.chatter.ChatterLimiter;
import com.newuniverse.nusmp.avatar.chatter.ChatterTemplate;
import com.newuniverse.nusmp.avatar.chatter.ChunibyoNames;
import com.newuniverse.nusmp.avatar.chatter.HybridMixer;
import com.newuniverse.nusmp.avatar.chatter.Rng;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Avatar's voice. {@code say} turns a moment ({@link ChatEvent}) into one line of the Avatar's own personality (or the mix of its two),
 * fills in names, and sends {@code [Name]: line} to the players around it, with a per-Avatar per-event cooldown and a global per-tick budget so a swarm
 * cannot spam. The lines are data ({@link ChatterDictionary}); the mixing is pure ({@link HybridMixer}); this class is only the glue to the game.
 * Server thread only, silently ignores a client call.
 */
public final class AvatarChatterEngine {
    private AvatarChatterEngine() {}

    private static final ChatterLimiter LIMITER = new ChatterLimiter();
    private static final String[] DUNGEONS = {"the Dungeon", "Orario's Dungeon", "the Labyrinth", "the Tower", "the Abyss"};

    public static void say(AvatarEntity avatar, ChatEvent event) { say(avatar, event, null); }

    /** {@code arg} is the event's subject: a skill name for SKILL_CAST, a target name for COMBAT_START. May be null. */
    public static void say(AvatarEntity avatar, ChatEvent event, String arg) {
        if (avatar == null || event == null || !(avatar.level() instanceof ServerLevel level) || !AvatarAiConfig.chatterEnabled()) return;
        try {
            long tick = level.getGameTime();
            AvatarPersonality primary = avatar.primary() == null ? AvatarPersonality.LOYAL : avatar.primary();
            AvatarPersonality secondary = avatar.secondary() == primary ? null : avatar.secondary();
            String template = choose(avatar.getRandom(), primary, secondary, event);
            if (template == null) return;
            if (!LIMITER.tryAcquire(avatar.getUUID(), event, tick, AvatarAiConfig.chatterCooldownTicks(event), AvatarAiConfig.chatterBudgetPerTick())) return;
            boolean chuni = primary == AvatarPersonality.CHUNIBYO || secondary == AvatarPersonality.CHUNIBYO;
            String line = ChatterTemplate.fill(template, values(avatar, event, arg, chuni));
            if (line.isEmpty()) return;
            Component msg = Component.literal("[" + avatar.avatarName() + "]: ").withStyle(ChatFormatting.AQUA).append(Component.literal(line).withStyle(ChatFormatting.WHITE));
            double r = AvatarAiConfig.chatterRadius(), r2 = r * r;
            for (ServerPlayer p : level.players()) if (p.distanceToSqr(avatar) <= r2) p.sendSystemMessage(msg);
        } catch (RuntimeException soft) {
            // a speech glitch must never break the Avatar's AI tick
        }
    }

    /** The raw template for the event: one personality, or the 50/30/20 mix of two. Public for tests of the glue; null when no line exists. */
    public static String choose(RandomSource random, AvatarPersonality primary, AvatarPersonality secondary, ChatEvent event) {
        Rng rng = new Rng() {
            @Override public int nextInt(int bound) { return random.nextInt(bound); }
            @Override public double nextDouble() { return random.nextDouble(); }
        };
        ChatterDictionary d = ChatterDictionary.current();
        List<String> a = d.lines(primary, event);
        if (secondary == null) return HybridMixer.pickOne(rng, a);
        return HybridMixer.pick(rng, primary, a, secondary, d.lines(secondary, event));
    }

    private static Map<String, String> values(AvatarEntity avatar, ChatEvent event, String arg, boolean chuni) {
        Map<String, String> v = new HashMap<>();
        ServerPlayer creator = avatar.creatorPlayer();
        String creatorName = creator != null ? creator.getGameProfile().getName() : "my creator";
        String subject = arg == null || arg.isBlank() ? null : arg;
        String skill = subject == null ? "magic" : subject;
        if (chuni && event == ChatEvent.SKILL_CAST) skill = ChunibyoNames.rename(skill);
        v.put("name", avatar.avatarName());
        v.put("creator", creatorName);
        v.put("target", subject == null ? "the enemy" : subject);
        v.put("skill", skill);
        v.put("grimoire", grimoire(creator));
        v.put("dungeon", DUNGEONS[avatar.getRandom().nextInt(DUNGEONS.length)]);
        v.put("relic", relic(creator));
        v.put("magicule", "magicules");
        return v;
    }

    private static String grimoire(ServerPlayer creator) {
        if (creator == null) return "grimoire";
        try {
            return GrimoirePages.grimoireOf(creator).map(i -> GrimoirePages.magicOf(i)).map(m -> m.displayName + " grimoire").orElse("grimoire");
        } catch (RuntimeException | LinkageError soft) {
            return "grimoire";
        }
    }

    private static String relic(ServerPlayer creator) {
        if (creator == null) return "that relic";
        ItemStack held = creator.getMainHandItem();
        return held.isEmpty() ? "that relic" : held.getHoverName().getString();
    }

    /** Forgets an Avatar's cooldowns (called when it leaves the world). */
    public static void forget(AvatarEntity avatar) { LIMITER.forget(avatar.getUUID()); }
}
