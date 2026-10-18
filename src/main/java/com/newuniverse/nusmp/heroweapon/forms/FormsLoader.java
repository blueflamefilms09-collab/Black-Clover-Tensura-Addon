package com.newuniverse.nusmp.heroweapon.forms;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the forms of the 21 weapons are data ({@code data/nusmp/hero/forms/<itemId>.json}), so the people who design a weapon write JSON, not Java, and a data pack
 * can replace any of it. This listener reads every such file at each data reload, hands the text to {@link FormsSchema} (the same validator the unit test runs),
 * and swaps the result into {@link FormsStore} in one step, so a reader sees the old forms or the new ones, never a mixture. A broken form is logged and skipped,
 * a broken file leaves its weapon with the synthetic base form, a file that matches no weapon is reported; nothing here is ever fatal. Skills are checked against the
 * registered weapon logics (they are registered at mod construction, long before the first reload). Registered by {@code FormsSystem.init} through
 * {@code AddReloadListenerEvent}.
 */
public final class FormsLoader implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String NAMESPACE = "nusmp", ROOT = "hero/forms";

    /** The skill questions of the schema, answered from the weapon logics. */
    static final FormsSchema.SkillIndex SKILLS = new FormsSchema.SkillIndex() {
        @Override
        public boolean exists(String skillId) { return HeroLogics.skill(skillId) != null; }

        @Override
        public boolean ultimate(String skillId) {
            HeroSkill s = HeroLogics.skill(skillId);
            return s != null && s.ultimate();
        }
    };

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<HeroKind, List<HeroForm>> forms = new EnumMap<>(HeroKind.class);
        Map<HeroKind, FormsSchema.Parsed> found = new EnumMap<>(HeroKind.class);
        List<String> stray = new ArrayList<>();
        try {
            Map<ResourceLocation, Resource> files = manager.listResources(ROOT, rl -> rl.getNamespace().equals(NAMESPACE) && rl.getPath().endsWith(".json"));
            List<ResourceLocation> keys = new ArrayList<>(files.keySet());
            keys.sort(Comparator.comparing(ResourceLocation::toString));
            for (ResourceLocation rl : keys) {
                String id = idOf(rl.getPath());
                HeroKind kind = id == null ? null : HeroKind.byItemId(id);
                if (kind == null) {
                    stray.add(rl.getPath());
                    LOG.warn("[nusmp] hero forms file {} matches no weapon (the file name must be <itemId>.json); it is ignored", rl);
                    continue;
                }
                FormsSchema.Parsed p;
                try (InputStream in = files.get(rl).open()) {
                    p = FormsSchema.parseText(kind, new String(in.readAllBytes(), StandardCharsets.UTF_8), SKILLS);
                } catch (IOException | RuntimeException e) {
                    p = new FormsSchema.Parsed(kind, List.of(HeroForm.base(kind)), List.of("could not be read: " + e), List.of(), true);
                }
                found.put(kind, p);
                forms.put(kind, p.forms());
                if (!p.clean()) LOG.warn("[nusmp] hero forms file {} ({} forms used){}:\n{}", rl, p.forms().size(), p.fileRejected() ? ", REJECTED, the weapon keeps its base form" : "", p.report());
            }
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] hero forms could not scan their data folder", e);
        }
        FormsStore.install(forms, found, stray);
        LOG.info("[nusmp] hero forms: {} of {} weapons have a forms file", forms.size(), HeroKind.values().length);
    }

    /** {@code hero/forms/legendary_shield.json} gives {@code legendary_shield}; a file in a sub-folder is not a weapon's. */
    static String idOf(String path) {
        String p = path.startsWith(ROOT + "/") ? path.substring(ROOT.length() + 1) : path;
        if (!p.endsWith(".json")) return null;
        p = p.substring(0, p.length() - 5);
        return p.isEmpty() || p.contains("/") ? null : p;
    }
}
