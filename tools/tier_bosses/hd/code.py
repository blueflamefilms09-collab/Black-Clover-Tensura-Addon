import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import hdlib as H

def paint(spec, cid, masks):
    img64, glow64, hair, hrgb, slim = H.blueprint(cid, spec, masks)
    cv = H.Canvas(256, slim)
    H.from64(cv, img64, glow64, {}, skip=lambda part, name, ov: False, soft=0.75)
    skin, glow = cv.finish()
    return skin, glow, hair, hrgb, slim
