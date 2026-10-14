package com.newuniverse.nusmp.client.merge;

import com.newuniverse.nusmp.core.magic.merge.MergedBlend;
import com.newuniverse.nusmp.core.magic.merge.MergedDef;
import com.newuniverse.nusmp.core.magic.merge.MergedRegistry;

import java.util.HashMap;
import java.util.Map;

/** The baked meshes of the merged magics, one per merged id, rebuilt only when the definition changes or the models reload. Render thread only. */
public final class MergedMeshCache {
    private MergedMeshCache() {}

    private static final Map<String, MergedMesh> MESHES = new HashMap<>();

    /** The mesh of a merged magic (baked on first use). Never null. */
    public static MergedMesh get(MergedDef def) {
        MergedMesh m = MESHES.get(def.id());
        if (m != null && m.def.equals(def)) return m;
        MergedBlend blend = MergedRegistry.blend(def);
        m = MergedMesh.bake(def, blend);
        MESHES.put(def.id(), m);
        return m;
    }

    public static void clear() { MESHES.clear(); }
}
