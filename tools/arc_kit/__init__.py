"""Why: the art kit of the characters of the arcs (tools/gen_arc_kit.py is its front door; docs/arc/art_kit.md is its manual). One Python spec file per
character becomes a GeoLite model, its clips and its textures. The parts live in one module each: color, core (painters, packer), rig (body proportions and
the skeleton), face, hair, outfit, accessories, props, anim (clips, inverse kinematics, clearance checks), spec (defaults and validation), build (the pipeline),
scenes (the preview harness scenes and the manifest)."""
