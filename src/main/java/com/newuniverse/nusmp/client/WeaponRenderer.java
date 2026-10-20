package com.newuniverse.nusmp.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.relics.item.HandWrap;
import com.newuniverse.nusmp.client.relics.item.ItemStateVariants;
import com.newuniverse.nusmp.client.relics.item.OversizeGuard;
import com.newuniverse.nusmp.client.relics.item.VariantRules;
import com.newuniverse.nusmp.client.relics.item.WeaponClasses;
import com.newuniverse.nusmp.client.relics.item.WeaponNormaliser;
import com.newuniverse.nusmp.client.relics.item.WeaponOrientation;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 0.50: every Black Clover weapon (and the relics) in 3D, to the Genesis Demon-Slayer's standard ({@link DemonSlayerRenderer}). The inventory
 * keeps each weapon's old icon (nusmp:item/&lt;id&gt;_icon, the old item model unchanged); in hand, on the ground, in frames and
 * on mobs the weapon is a pixel-built model:
 * <ul>
 *   <li><b>Mesh:</b> assets/nusmp/weapon_meshes/&lt;id&gt;.txt from tools/gen_weapon_models.py (beveled blades with a ridge,
 *       extruded pixel guards, boxed grips and pommels), placed on the old sprite's diagonal so the old display transforms fit.</li>
 *   <li><b>Skin:</b> textures/entity/weapon/&lt;id&gt;.png (cutout, with the enchantment glint).</li>
 *   <li><b>Glow:</b> textures/entity/weapon/&lt;id&gt;_glow.png drawn additive and full bright, its tint pulsing (runes, edges,
 *       gems).</li>
 *   <li><b>Void:</b> Asta's demon swords show the crimson dimensional void through their notches and broken patches
 *       ({@link NURenderTypes#demonVoid()}).</li>
 * </ul>
 * <p>
 * 1.29.0, size and orientation: every mesh follows one convention (mesh -x is forward, the tip is up-forward; docs/weapon_scale_orientation.md,
 * {@link WeaponOrientation}). tools/relics/weapon_fixes.json corrects the shipped meshes offline; {@link WeaponNormaliser} is the per-game
 * safety net: when a mesh loads it is checked against its class ({@link WeaponClasses}, assets/nusmp/weapon_classes.json, else a keyword guess of
 * the id) and, if it is far too small, tip-down or has its head toward the player, it is scaled up to the class minimum / turned over / mirrored, with
 * one log line per corrected mesh. 1.34.0 (the owner's screenshot 68: a sword held by its blade): a sword-like mesh with its hilt at the far end is turned over
 * along its length (the directive {@code grip <fraction>} states where the grip is; without it the thin-end geometry decides), and the size table is the
 * larger one of the size rule (greatswords 2.2 to 3 blocks, spears 2.8 to 3.6, items 0.5 to 0.9). A mesh with the directive {@code orient manual} (or a model json with {@code "nusmp_display_preset": "manual"}) is
 * never touched; {@code orient <rotZ degrees> <flipX 0|1>} applies an explicit turn / mirror at load.
 * <p>
 * Preview without the game: python tools/item_preview/preview_weapons.py (sizes and orientation: python tools/relics/audit_weapon_scale.py).
 */
public class WeaponRenderer extends BlockEntityWithoutLevelRenderer {
    /** The weapons and relics drawn this way (each needs a mesh, a skin, and an &lt;id&gt;_icon model). */
    static final List<Supplier<? extends Item>> BASE_WEAPONS = List.of(
            NUItems.DEMON_SLASHER_KATANA, NUItems.MIASMA_KATANA, NUItems.SPELL_FORGED_RAPIER, NUItems.SEVERING_GREATSWORD,
            NUItems.DEMON_DWELLER, NUItems.DEMON_DESTROYER, NUItems.LICHT_DWELLER, NUItems.LICHT_DESTROYER,
            NUItems.RIMEHEART_RUNEBLADE, NUItems.OTHERWORLD_TRIDENT, NUItems.MAGIC_TOOL_SWORD, NUItems.MAGIC_TOOL_SPEAR,
            NUItems.COMMUNICATION_DEVICE, NUItems.RUNE_STONE, NUItems.SPIRIT_CHARM, NUItems.BOND_THREAD, NUItems.FORTUNE_DIE,
            NUItems.GRIMOIRE_CHAIN, NUItems.ANTI_BIRD_CHARM, NUItems.RECOVERY_SALVE, NUItems.WRITTEN_CONSENT, NUItems.DEVIL_CONTRACT,
            NUItems.GAUCHE_MIRROR, NUItems.LAST_WORD, NUItems.ELSDOCIA, NUItems.SHROUD_OF_MARGINS, NUItems.CIRCLET_OF_THOUGHT,
            NUItems.QUICKLINGS_IVY, NUItems.MAGEBANE_LONGSWORD, NUItems.PRISM_BLADE, NUItems.THUNDERCLEAVE, NUItems.BLOODBOUND_KATANA,
            NUItems.PERMAFROST_SABRE, NUItems.CYCLONE_RAPIER, NUItems.BOLT_THROWER, NUItems.CROSSBOW_OF_SWIFT_DEATH,
            NUItems.CLUB_OF_ROT, NUItems.DOOM_HAMMER,
            NUItems.MAGEBANE_SHIELD, NUItems.WEAPON_OF_RESURGENCE, NUItems.BROOCH_PIN, NUItems.HAND_OF_PROVIDENCE, NUItems.BUTCHER_CLEAVER,
            com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.GLUTTON_CHALICE, com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.DEVIL_BOUND_SHEATH, com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.GAMBLERS_GRIMOIRE_PAGE,
            com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS,      // 1.22.0
            com.newuniverse.nusmp.arena.ArenaSystem.HOURGLASS,      // 1.46.0: the Hourglass of the Final Hour
            com.newuniverse.nusmp.labyrinth.mega.guard.GuardRegistry.MANA_CORE,      // 1.54.0: the Mana Core of a Mega-Dungeon sector's Guardian
            com.newuniverse.nusmp.arc.world.ArcWorldItems.CHRONICLE_SIGIL);      // 1.61.0: the Chronicle Sigil

    /** 1.24.0: the old list plus every 3D relic of Relics of Many Worlds. */
    static final List<Supplier<? extends Item>> WEAPONS = java.util.stream.Stream.of(BASE_WEAPONS.stream(), com.newuniverse.nusmp.relics.RelicCatalog.model3dItems().stream(),
            com.newuniverse.nusmp.heroweapon.HeroItems.all().stream(), com.newuniverse.nusmp.requip.RequipItems.weapons().stream(), com.newuniverse.nusmp.celestial.CelestialItems.renderables().stream()).flatMap(s -> s).toList();      // 1.62.0: + the 21 hero weapons; + the six weapons of Requip: The Knight

    static final int BODY = 0, VOID = 1, GLOW = 2;

    private static WeaponRenderer instance;
    private static final Map<String, Mesh> MESHES = new HashMap<>();
    /** The class table and the id to class map (assets/nusmp/weapon_classes.json), read once per resource reload. */
    private static WeaponClasses classes;

    public WeaponRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(WeaponRenderer::registerExtensions);
        modBus.addListener(WeaponRenderer::registerModels);
        modBus.addListener((ModelEvent.BakingCompleted e) -> { MESHES.clear(); classes = null; ItemStateVariants.clearCache(); });     // a resource reload re-reads the meshes
        // 1.15.0: the Magebane Shield is a real shield; this is vanilla's own "blocking" predicate, which swaps in the raised-shield model
        modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> ItemProperties.register(NUItems.MAGEBANE_SHIELD.get(),
                ResourceLocation.withDefaultNamespace("blocking"),
                (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F)));
    }

    static void registerExtensions(RegisterClientExtensionsEvent e) {
        IClientItemExtensions ext = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (instance == null) instance = new WeaponRenderer();
                return instance;
            }

            /** 1.25.0: a loaded relic crossbow is held at the ready like vanilla's (vanilla checks Items.CROSSBOW; this is the hook it leaves for other items). */
            @Override
            public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(net.minecraft.world.entity.LivingEntity holder, net.minecraft.world.InteractionHand hand, ItemStack stack) {
                if (stack.getItem() instanceof net.minecraft.world.item.CrossbowItem && !holder.swinging && net.minecraft.world.item.CrossbowItem.isCharged(stack))
                    return net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD;
                // 1.63.0: a drawn bow of the mod (class bow) is held in vanilla's bow pose whatever its use animation is (a relic bow answers NONE), so the arm is never left mis-posed
                if (holder.isUsingItem() && holder.getUseItem() == stack && "bow".equals(classes().className(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())))
                    return net.minecraft.client.model.HumanoidModel.ArmPose.BOW_AND_ARROW;
                return null;
            }
        };
        for (Supplier<? extends Item> item : WEAPONS) e.registerItem(ext, item.get());
    }

    static void registerModels(ModelEvent.RegisterAdditional e) {
        for (Supplier<? extends Item> item : WEAPONS) e.register(icon(BuiltInRegistries.ITEM.getKey(item.get()).getPath()));
    }

    static ModelResourceLocation icon(String id) { return ModelResourceLocation.standalone(DemonSlayerRenderer.rl("item/" + id + "_icon")); }

    /**
     * An item's inventory icon model with its overrides applied. This renderer draws the {@code _icon} model directly, which skips the override lookup,
     * so an item whose icon switches on a property (a state, an angle) must pass the icon through here first. (It lived in the Dungeon Delving Gear's
     * client class until 1.58.0; the lantern and the compass went with the Living Dungeon, the helper stayed.)
     */
    static BakedModel resolveIcon(BakedModel icon, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel resolved = icon.getOverrides().resolve(icon, stack, mc.level, mc.player, 0);
        return resolved == null ? icon : resolved;
    }

    /** The mesh id of a stack: Marid's Valor in its liquid state (1.24.0) draws the liquid blade; everything else is just {@code id}. */
    static String meshId(ItemStack stack, String id) {
        if (id.equals("marids_valor")) {
            net.minecraft.world.item.component.CustomData d = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (d != null && d.copyTag().getCompound("nusmp_w1").getLong("state") == 1L) return "marids_valor_liquid";
        }
        // 1.62.0: a hero weapon in a curse form (its stack says look=curse) is drawn from its <id>_curse mesh and skins; without that mesh it keeps the base look
        if (stack.getItem() instanceof com.newuniverse.nusmp.heroweapon.HeroWeaponItem && "curse".equals(com.newuniverse.nusmp.heroweapon.HeroComponents.look(stack)) && mesh(id + "_curse") != null)
            return id + "_curse";
        return id;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        // 1.25.0: the draw / load stage of a bow or crossbow and the Genesis / five-leaf variants pick the mesh (client/relics/item)
        ItemStateVariants.Result state = ctx == ItemDisplayContext.GUI ? null
                : ItemStateVariants.resolve(stack, meshId(stack, id), ctx, m -> mesh(m) != null);
        Mesh mesh = state == null ? null : mesh(state.meshId());
        if (mesh == null) {                                                        // the inventory (or a missing mesh): the old icon
            BakedModel icon = resolveIcon(mc.getModelManager().getModel(icon(id)), stack);
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0.5f);                                      // render() translates by -0.5 again
            mc.getItemRenderer().render(stack, ctx, false, pose, buffers, light, overlay, icon);
            pose.popPose();
            return;
        }
        float time = mc.level == null ? 0 : mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();
        HandWrap.Wrap wrap = ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ? HandWrap.of(id) : null;
        if (wrap != null) {                                                        // 1.71.0: worn on the hand, not held (client/relics/item/HandWrap)
            pose.translate(0.5f, 0.5f, 0.5f);                                      // the hand's own origin; the layer has already moved to the grip point
            pose.translate(0f, -HandWrap.ORIGIN_FORWARD / 16f, (HandWrap.ORIGIN_DOWN_ARM - wrap.mouthPx()) / 16f);
            if (ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) pose.mulPose(Axis.ZP.rotationDegrees(180f));          // item +x is the inside of the left arm: turn the same glove about the arm
            pose.mulPose(wrap.quaternion());
            pose.scale(wrap.sx(), wrap.sy(), wrap.sz());
        } else if (ctx.firstPerson()) {                                            // 1.25.0: an oversized weapon never goes through the camera (tools/relics/display_presets.py is the rule)
            float k = OversizeGuard.factor(mesh.length, pose.last().pose().getScale(new Vector3f()).x);
            if (k < 1f) {
                pose.translate(mesh.gripX, mesh.gripY, 0.5f);
                pose.scale(k, k, k);
                pose.translate(-mesh.gripX, -mesh.gripY, -0.5f);
            }
        }
        if (wrap == null) {
            pose.translate(mesh.ox, mesh.oy, 0.5f);
            pose.mulPose(Axis.ZP.rotationDegrees(mesh.angle - 90f));
            pose.scale(mesh.scale, mesh.scale, mesh.scale);
            pose.translate(0f, -mesh.ymin, 0f);
        }
        PoseStack.Pose p = pose.last();
        emit(p, ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutout(mesh.tex), false, stack.hasFoil()), mesh.layers[BODY],
                state.tint() ? VariantRules.BODY_TINT_5LEAF : 0xFFFFFFFF, light, overlay);
        if (!mesh.layers[VOID].isEmpty()) {
            VertexConsumer vc = buffers.getBuffer(NURenderTypes.demonVoid());
            Matrix4f m = p.pose();
            Vector3f q = SCRATCH_POS;
            for (float[] v : mesh.layers[VOID]) {
                m.transformPosition(v[0], v[1], v[2], q);
                vc.addVertex(q.x, q.y, q.z);
            }
        }
        if (mesh.glowTex != null && !mesh.layers[GLOW].isEmpty()) {
            float k = VariantRules.pulse(time, mesh.glowSpeed, mesh.glowMin, mesh.glowMax, state.intensify() ? VariantRules.GENESIS_BOOST : 1f);
            int rgb = state.tint() ? VariantRules.tintGlow(mesh.glowRgb) : mesh.glowRgb;
            int r = VariantRules.channel(rgb >> 16 & 255, k), g = VariantRules.channel(rgb >> 8 & 255, k), b = VariantRules.channel(rgb & 255, k);
            emit(p, buffers.getBuffer(RenderType.eyes(mesh.glowTex)), mesh.layers[GLOW], 0xFF000000 | r << 16 | g << 8 | b, 0xF000F0, OverlayTexture.NO_OVERLAY);
            if (state.intensify()) {                                               // 1.25.0: a Genesis glow adds a translucent emissive pass on top
                int a = VariantRules.emissiveAlpha(time);
                emit(p, buffers.getBuffer(RenderType.entityTranslucentEmissive(mesh.glowTex)), mesh.layers[GLOW], a << 24 | (rgb & 0xFFFFFF), 0xF000F0, OverlayTexture.NO_OVERLAY);
            }
        }
        if (mesh.glowTex != null && !mesh.layers[GLOW].isEmpty() && com.newuniverse.nusmp.item.ElsdociaRules.isLegacyException(stack)) {   // 1.97.0: Elsdocia's Genesis halo: a wider, fainter copy of the glow that breathes against the main pulse
            pose.pushPose();
            pose.scale(1.08f, 1f, 1.08f);
            int ha = Math.max(20, VariantRules.emissiveAlpha(time + 15f) / 2);
            emit(pose.last(), buffers.getBuffer(RenderType.entityTranslucentEmissive(mesh.glowTex)), mesh.layers[GLOW], ha << 24 | (mesh.glowRgb & 0xFFFFFF), 0xF000F0, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        if (state.socketRgb() != 0 && mesh.glowTex != null && !mesh.layers[GLOW].isEmpty())          // 1.30.0: a bound Spirit or Devil Core colours an emissive pass
            emit(p, buffers.getBuffer(RenderType.entityTranslucentEmissive(mesh.glowTex)), mesh.layers[GLOW], VariantRules.socketArgb(state.socketRgb(), time), 0xF000F0, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /** 1.99.0 (performance pass): render-thread scratch vectors for {@link #emit} and the void pass. */
    private static final Vector3f SCRATCH_POS = new Vector3f(), SCRATCH_NORMAL = new Vector3f();

    private static void emit(PoseStack.Pose p, VertexConsumer vc, List<float[]> verts, int argb, int light, int overlay) {
        Matrix4f m = p.pose();
        Matrix3f nm = p.normal();
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        Vector3f q = SCRATCH_POS, n = SCRATCH_NORMAL;                              // 1.99.0: two scratch vectors for the whole pass (two new vectors per vertex before); same maths, same order
        for (float[] v : verts) {
            m.transformPosition(v[0], v[1], v[2], q);
            nm.transform(v[5], v[6], v[7], n).normalize();
            vc.addVertex(q.x, q.y, q.z).setColor(r, g, b, a).setUv(v[3] / 128f, v[4] / 128f).setOverlay(overlay).setLight(light).setNormal(n.x, n.y, n.z);
        }
    }

    // ================================================================ mesh files
    /** One weapon's mesh: its placement on the old sprite's diagonal, its glow pulse, and its vertices per layer. */
    static final class Mesh {
        float ox, oy, angle, scale, ymin, glowSpeed, glowMin, glowMax;
        float ymax = -Float.MAX_VALUE, length, gripX, gripY;                     // 1.25.0: for the oversize guard (client/relics/item/OversizeGuard)
        boolean manual;                                                          // 1.29.0: `orient manual`: hand-tuned, never normalised
        float orientRot, orientFlip;                                             // 1.29.0: `orient <rotZ> <flipX>`
        float gripAt = Float.NaN;                                                // 1.34.0: `grip <fraction>`: where the grip's middle lies (share of the length from the low-y end); NaN = not stated
        int glowRgb;
        ResourceLocation tex, glowTex;
        @SuppressWarnings("unchecked")
        final List<float[]>[] layers = new List[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};
    }

    /** The parsed mesh for an item id, read once per resource reload; null if it is missing or broken (the icon is used). */
    static Mesh mesh(String id) {
        if (MESHES.containsKey(id)) return MESHES.get(id);
        Mesh mesh = null;
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(DemonSlayerRenderer.rl("weapon_meshes/" + id + ".txt"));
            if (res.isPresent()) {
                mesh = new Mesh();
                mesh.tex = skin(id, "");
                try (BufferedReader in = res.get().openAsReader()) {
                    String line;
                    while ((line = in.readLine()) != null) parse(mesh, id, line.trim().split("\\s+"));
                }
                float[] info = modelInfo(id);                                            // 1.29.0: the third-person display scale and the manual marker of the model json
                if (info[1] > 0f) mesh.manual = true;
                applyOrient(mesh);
                if (!mesh.manual && !HandWrap.isWrapped(id)) normalise(id, mesh, info[0]);                 // a wrapped weapon keeps its modelled axes (client/relics/item/HandWrap)
                mesh.length = Math.max(0f, (mesh.ymax - mesh.ymin) * mesh.scale);
                float along = OversizeGuard.GRIP_FRACTION * mesh.length;
                mesh.gripX = mesh.ox + along * (float) Math.cos(Math.toRadians(mesh.angle));
                mesh.gripY = mesh.oy + along * (float) Math.sin(Math.toRadians(mesh.angle));
            }
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] weapon mesh {} failed to load; its icon is used instead", id, ex);
            mesh = null;
        }
        MESHES.put(id, mesh);
        return mesh;
    }

    /** The class table (resource, else the built-in one), read once per reload. */
    private static WeaponClasses classes() {
        if (classes != null) return classes;
        Map<String, WeaponClasses.Cls> table = new HashMap<>();
        Map<String, String> weapons = new HashMap<>();
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(DemonSlayerRenderer.rl("weapon_classes.json"));
            if (res.isPresent()) {
                try (BufferedReader in = res.get().openAsReader()) {
                    JsonObject root = JsonParser.parseReader(in).getAsJsonObject();
                    if (root.has("classes")) {
                        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("classes").entrySet()) {
                            JsonObject c = e.getValue().getAsJsonObject();
                            table.put(e.getKey(), new WeaponClasses.Cls(e.getKey(), c.get("min").getAsFloat(), c.get("max").getAsFloat(),
                                    c.has("head_min") ? c.get("head_min").getAsFloat() : 0f, c.has("grip") ? c.get("grip").getAsFloat() : 0.25f));
                        }
                    }
                    if (root.has("weapons")) {
                        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("weapons").entrySet()) weapons.put(e.getKey(), e.getValue().getAsString());
                    }
                }
            }
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] weapon_classes.json failed to load; the built-in class table is used", ex);
            table.clear();
            weapons.clear();
        }
        classes = new WeaponClasses(table, weapons);
        return classes;
    }

    /** {third-person scale (0.85 when the json has none), 1 when the json is marked "nusmp_display_preset": "manual"} of an item's model json (a state mesh reads its base id's). */
    private static float[] modelInfo(String id) {
        float s3p = 0.85f;
        boolean manual = false;
        for (String cand : new String[]{id, WeaponClasses.baseId(id)}) {
            try {
                Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(DemonSlayerRenderer.rl("models/item/" + cand + ".json"));
                if (res.isEmpty()) continue;
                try (BufferedReader in = res.get().openAsReader()) {
                    JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
                    JsonElement mk = o.get("nusmp_display_preset");
                    if (mk != null && mk.isJsonPrimitive() && "manual".equals(mk.getAsString())) manual = true;
                    JsonElement disp = o.get("display");
                    if (disp != null && disp.isJsonObject()) {
                        JsonElement t = disp.getAsJsonObject().get("thirdperson_righthand");
                        if (t != null && t.isJsonObject()) {
                            JsonElement sc = t.getAsJsonObject().get("scale");
                            if (sc != null && sc.isJsonArray() && sc.getAsJsonArray().size() > 0) s3p = sc.getAsJsonArray().get(0).getAsFloat();
                        }
                    }
                }
                break;
            } catch (Exception ignored) {
                // an unreadable json: the defaults stand
            }
        }
        return new float[]{s3p, manual ? 1f : 0f};
    }

    /** The explicit {@code orient <rotZ> <flipX>} directive: mirror across x = 0, then turn about the middle of the mesh's length. */
    private static void applyOrient(Mesh mesh) {
        if (mesh.orientFlip == 0f && mesh.orientRot == 0f) return;
        if (mesh.orientFlip != 0f) for (List<float[]> layer : mesh.layers) WeaponOrientation.mirrorX(layer, 0.0);
        if (mesh.orientRot != 0f) {
            float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
            for (List<float[]> layer : mesh.layers) for (float[] v : layer) { lo = Math.min(lo, v[1]); hi = Math.max(hi, v[1]); }
            for (List<float[]> layer : mesh.layers) WeaponOrientation.rotateZ(layer, mesh.orientRot, (lo + hi) / 2.0);
        }
        mesh.ymax = maxY(mesh);
    }

    private static float maxY(Mesh mesh) {
        float hi = -Float.MAX_VALUE;
        for (List<float[]> layer : mesh.layers) for (float[] v : layer) hi = Math.max(hi, v[1]);
        return hi;
    }

    /** 1.29.0: the per-game safety net (client/relics/item/WeaponNormaliser): scale up / turn over / mirror a mesh that breaks its class; one log line per corrected mesh. */
    private static void normalise(String id, Mesh mesh, float thirdPersonScale) {
        try {
            WeaponClasses c = classes();
            WeaponClasses.Cls cls = c.classOf(id);
            float[] place = {mesh.ox, mesh.oy, mesh.angle, mesh.scale, mesh.ymin};
            WeaponNormaliser.Result r = WeaponNormaliser.normalise(mesh.layers, place, cls.name(), cls, thirdPersonScale, mesh.gripAt);
            if (!r.changed()) return;
            mesh.ox = place[0];
            mesh.oy = place[1];
            mesh.angle = place[2];
            mesh.scale = place[3];
            mesh.ymax = maxY(mesh);
            LogUtils.getLogger().info("[nusmp] weapon mesh {} ({}) normalised: {}", id, cls.name(), r.log());
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] weapon mesh {} could not be normalised; it is drawn as it is", id, ex);
        }
    }

    /**
     * The skin of a mesh: its own file when there is one, else the one of the rest mesh (a bow or crossbow state mesh, a Genesis or five-leaf variant
     * shares the skin and the UV layout of the weapon it belongs to).
     */
    private static net.minecraft.resources.ResourceLocation skin(String id, String suffix) {
        net.minecraft.resources.ResourceLocation own = DemonSlayerRenderer.rl("textures/entity/weapon/" + id + suffix + ".png");
        String base = id.replaceAll("_(pull[123]|charged|spent|genesis|5leaf)$", "");
        if (base.equals(id)) return own;
        try {
            if (Minecraft.getInstance().getResourceManager().getResource(own).isPresent()) return own;
        } catch (Exception ignored) { }
        return DemonSlayerRenderer.rl("textures/entity/weapon/" + base + suffix + ".png");
    }

    private static void parse(Mesh mesh, String id, String[] v) {
        switch (v[0]) {
            case "place" -> {
                mesh.ox = Float.parseFloat(v[1]);
                mesh.oy = Float.parseFloat(v[2]);
                mesh.angle = Float.parseFloat(v[3]);
                mesh.scale = Float.parseFloat(v[4]);
                mesh.ymin = Float.parseFloat(v[5]);
            }
            case "orient" -> {                                                       // 1.29.0: `orient manual` or `orient <rotZ degrees> <flipX 0|1>`
                if (v.length > 1 && v[1].equals("manual")) mesh.manual = true;
                else if (v.length > 2) {
                    mesh.orientRot = Float.parseFloat(v[1]);
                    mesh.orientFlip = Float.parseFloat(v[2]);
                }
            }
            case "grip" -> {                                                         // 1.34.0: `grip <fraction>`; above 0.5 the mesh has its hilt at the far end (WeaponNormaliser turns it over)
                if (v.length > 1) mesh.gripAt = Float.parseFloat(v[1]);
            }
            case "glow" -> {
                mesh.glowRgb = Integer.parseInt(v[1], 16);
                mesh.glowSpeed = Float.parseFloat(v[2]);
                mesh.glowMin = Float.parseFloat(v[3]);
                mesh.glowMax = Float.parseFloat(v[4]);
                mesh.glowTex = skin(id, "_glow");
            }
            case "q" -> {
                int layer = Integer.parseInt(v[1]);
                float nx = Float.parseFloat(v[22]), ny = Float.parseFloat(v[23]), nz = Float.parseFloat(v[24]);
                for (int k = 0; k < 4; k++) {
                    int o = 2 + k * 5;
                    mesh.layers[layer].add(new float[]{Float.parseFloat(v[o]), Float.parseFloat(v[o + 1]), Float.parseFloat(v[o + 2]),
                            Float.parseFloat(v[o + 3]), Float.parseFloat(v[o + 4]), nx, ny, nz});
                    mesh.ymax = Math.max(mesh.ymax, Float.parseFloat(v[o + 1]));
                }
            }
            case "qn" -> {      // 1.24.0: per-vertex smoothed normals (tools/relics/mesh_lib.py, normals="vertex")
                int layer = Integer.parseInt(v[1]);
                for (int k = 0; k < 4; k++) {
                    int o = 2 + k * 8;
                    mesh.layers[layer].add(new float[]{Float.parseFloat(v[o]), Float.parseFloat(v[o + 1]), Float.parseFloat(v[o + 2]),
                            Float.parseFloat(v[o + 3]), Float.parseFloat(v[o + 4]), Float.parseFloat(v[o + 5]), Float.parseFloat(v[o + 6]), Float.parseFloat(v[o + 7])});
                    mesh.ymax = Math.max(mesh.ymax, Float.parseFloat(v[o + 1]));
                }
            }
            default -> { }
        }
    }
}
