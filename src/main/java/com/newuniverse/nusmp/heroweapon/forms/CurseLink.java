package com.newuniverse.nusmp.heroweapon.forms;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Why: the forms system and the curse system share one fact and one action, and agreed on them in words rather than in code, so that two builders could work at
 * once. The fact: which Curse Series the wielder has unlocked, kept by the curse system in {@code bond.extra("curse")} under the key {@code series} (a list of sin
 * ids); a curse form can only be chosen or learned when its sin is in it. The action: the Embrace button of the Weapon Book, which must run the curse system's
 * own accept routine. Reading is defensive (the key is optional, it may be a list of texts, a list of compounds with a {@code sin} or one comma-separated text)
 * and never creates the compound; the embrace goes to {@code CurseSystem.embrace(ServerPlayer)} when it exists and otherwise runs the command
 * {@code /multiverse hero curse embrace} as the player. Keeping all of that in one small class means the day the curse system is merged, this is the only file to
 * look at.
 */
final class CurseLink {
    private CurseLink() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final String SYSTEM = "curse", CURSE_SYSTEM_CLASS = "com.newuniverse.nusmp.heroweapon.curse.CurseSystem";

    /** The sin ids of the unlocked Curse Series (lower case); empty when the curse system has written nothing. */
    static Set<String> series(HeroBond bond) {
        if (bond == null || !bond.hasExtra(SYSTEM)) return Set.of();
        CompoundTag c = bond.extra(SYSTEM);
        Set<String> out = new LinkedHashSet<>();
        if (c.contains("series", Tag.TAG_LIST)) {
            ListTag texts = c.getList("series", Tag.TAG_STRING);
            for (int i = 0; i < texts.size(); i++) add(out, texts.getString(i));
            if (texts.isEmpty()) {
                ListTag compounds = c.getList("series", Tag.TAG_COMPOUND);
                for (int i = 0; i < compounds.size(); i++) {
                    CompoundTag e = compounds.getCompound(i);
                    add(out, e.contains("sin", Tag.TAG_STRING) ? e.getString("sin") : e.getString("id"));
                }
            }
        } else if (c.contains("series", Tag.TAG_STRING)) {
            for (String s : c.getString("series").split("[,;\\s]+")) add(out, s);
        }
        return Collections.unmodifiableSet(out);
    }

    /** The sin the weapon is currently offering to the wielder ("whispering"), or an empty string. */
    static String offered(HeroBond bond) {
        if (bond == null || !bond.hasExtra(SYSTEM)) return "";
        CompoundTag c = bond.extra(SYSTEM);
        if (c.contains("offered", Tag.TAG_STRING)) return c.getString("offered").toLowerCase(Locale.ROOT);
        if (c.contains("offered", Tag.TAG_COMPOUND)) {
            CompoundTag o = c.getCompound("offered");
            if (o.contains("sin", Tag.TAG_STRING)) return o.getString("sin").toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private static void add(Set<String> into, String sin) {
        String s = sin == null ? "" : sin.strip().toLowerCase(Locale.ROOT);
        if (!s.isEmpty()) into.add(s);
    }

    /** Asks the curse system to accept the offered series for this player; true when the request was passed on. */
    static boolean embrace(ServerPlayer player) {
        try {
            Method m = Class.forName(CURSE_SYSTEM_CLASS).getMethod("embrace", ServerPlayer.class);
            Object result = m.invoke(null, player);
            return !(result instanceof Boolean ok) || ok;
        } catch (NoSuchMethodException | ClassNotFoundException absent) {
            // the curse system has no such method (yet): use the command, which the design promises
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("[nusmp] hero forms: CurseSystem.embrace failed", e);
            return false;
        }
        try {
            player.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(), "multiverse hero curse embrace");
            return true;
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] hero forms: the embrace command failed", e);
            return false;
        }
    }
}
