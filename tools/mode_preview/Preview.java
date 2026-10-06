import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.blackclover.ModeArmor;
import com.newuniverse.nusmp.client.mode.ModeArmorClient;
import com.newuniverse.nusmp.client.mode.ModeArmorLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Vector4f;

import java.io.FileWriter;
import java.util.Locale;

/** Runs the real ModeArmorLayer for each mode with the player renderer's transform; dumps world-space quads as JSON. */
public final class Preview {
    public static void main(String[] a) throws Exception {
        StringBuilder sb = new StringBuilder("{");
        PlayerModel<AbstractClientPlayer> model = new PlayerModel<>();
        ModeArmorLayer layer = new ModeArmorLayer(() -> model);
        boolean firstMode = true;
        for (ModeArmor.Mode m : ModeArmor.Mode.values()) {
            if (m == ModeArmor.Mode.NONE) continue;
            ModeArmorClient.MODE = m;
            Minecraft.getInstance().level.time = Long.parseLong(a.length > 1 ? a[1] : "40");
            StringBuilder q = new StringBuilder();
            int[] n = {0};
            PoseStack pose = new PoseStack();
            pose.mulPose(Axis.YP.rotationDegrees(180f));          // LivingEntityRenderer at body yaw 0
            pose.scale(-1f, -1f, 1f);
            pose.translate(0f, -1.501f, 0f);
            layer.render(pose, type -> new VertexConsumer() {
                PoseStack.Pose p;
                float x, y, z, u, v; int al;
                public VertexConsumer addVertex(PoseStack.Pose pp, float xx, float yy, float zz) {
                    Vector4f w = pp.pose.transform(new Vector4f(xx, yy, zz, 1f));
                    x = w.x; y = w.y; z = w.z; return this;
                }
                public VertexConsumer setColor(int r, int g, int b, int aa) { al = aa; return this; }
                public VertexConsumer setUv(float uu, float vv) { u = uu; v = vv; return this; }
                public VertexConsumer setOverlay(int o) { return this; }
                public VertexConsumer setLight(int l) { return this; }
                public VertexConsumer setNormal(PoseStack.Pose pp, float nx, float ny, float nz) {
                    if (n[0] % 4 == 0) q.append(n[0] > 0 ? "," : "").append("{\"t\":\"").append(type.texture().getPath()).append("\",\"k\":\"").append(type.kind()).append("\",\"a\":").append(al).append(",\"v\":[");
                    else q.append(',');
                    q.append(String.format(Locale.ROOT, "[%.4f,%.4f,%.4f,%.3f,%.3f]", x, y, z, u, v));
                    if (n[0] % 4 == 3) q.append("]}");
                    n[0]++;
                    return this;
                }
            }, 0xF000F0, new AbstractClientPlayer(), 0f, 0f, 0f, 40f, 0f, 0f);
            sb.append(firstMode ? "" : ",").append('"').append(m.name()).append("\":[").append(q).append(']');
            firstMode = false;
        }
        sb.append('}');
        try (FileWriter w = new FileWriter(a[0])) { w.write(sb.toString()); }
    }
}
