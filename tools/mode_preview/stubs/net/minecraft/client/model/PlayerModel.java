package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;

public class PlayerModel<T> {
    public final ModelPart head = new ModelPart(0, 0, 0), body = new ModelPart(0, 0, 0),
            rightArm = new ModelPart(-5, 2, 0), leftArm = new ModelPart(5, 2, 0),
            rightLeg = new ModelPart(-1.9f, 12, 0), leftLeg = new ModelPart(1.9f, 12, 0);
}
