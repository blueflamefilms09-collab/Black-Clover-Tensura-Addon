# Model packing and the detail limit (3.32.0)

## What it does
The jar task (`build.gradle`, `packMeshes`) turns the plain text meshes of `weapon_meshes`, `weapon_meshes_vu`, `weapon_meshes_sz`, `weapon_meshes_vu_sz` and `djinn` into packed files with the **same names** and puts those into the jar instead of the text. The source tree, git and `build/resources` keep the plain text; the tests and dev runs read text.

## The packed form (`mesh/MeshPack.java`)
`NMSH`, version byte 1, then raw deflate of an opcode stream. Quad lines of both dialects (`qn layer (x y z u v nx ny nz)*4` and `q layer (x y z u v)*4 nx ny nz`) become one opcode each, numbers as zigzag varints of the value times 10^decimals (up to 4 decimals), and corners that repeat the corner of an earlier quad are stored as a back reference. Any other line (header, `place`, `glow`, `skin`, `g`, `orbit`, comments) is kept verbatim as a text op, and a quad line that cannot be held exactly (more than 4 decimals, exponent, wrong token count) also stays a text op, so unpack gives back the same lines (negative zero is written as 0). `MeshPackMain` unpacks every file it writes and compares it with the text; a difference fails the build.

## The loader (`mesh/MeshText.java`)
`MeshText.reader(...)` reads the bytes, looks for `NMSH` and returns a reader over the plain text either way. The four loaders and the self-tests that open meshes use it, so plain files (source tree, resource packs, older jars) and packed files both work.

## The detail limit (`mesh/MeshCap.java`)
Default 3000 quads (6000 triangles) per mesh, `-PmeshQuadCap=<n>` changes it, `0` turns it off. Over the limit, the quads with the smallest surface go first (ties: later lines first). Never dropped: for each layer the quads that reach the lowest and highest x, y and z (the build fails if any layer extent changes), every void (1) and halo-card (4) quad, and, when the mesh has `orbit` lines, the layers 3 and up. Header lines and the order of the rest are kept.

Measured on the shipped meshes: 52 of 3,198 meshes are over 3000 quads; 103,449 small quads are left out; the worst surface loss is 27.3% (Cardinal Halo Chakram, 7566 -> 3000). At 2000 quads it would be 184 meshes and 41% on the worst.

## Numbers
294 MB of text -> 15.8 MB packed -> 15.3 MB packed and cut. In the jar: about 33 MB of deflated text -> 15.3 MB. The textures (about 241 MB of PNG in a 368 MB jar) are the bulk and are not touched by this release.

## Looking at the result
`./gradlew packMeshes -PmeshCapDump=/some/dir` writes the cut meshes as text; then `python tools/mesh_cap/preview_cap.py /some/dir weapon_meshes_vu/blackrazor ...` draws full | cut from the front, the side and 45 degrees.

## Cost (worst case)
Build time only plus one decode of one mesh the first time a weapon draws. No packets, entities or per-tick work were added; cut meshes draw fewer quads.
