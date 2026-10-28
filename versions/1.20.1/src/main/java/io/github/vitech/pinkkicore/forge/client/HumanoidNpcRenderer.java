package io.github.vitech.pinkkicore.forge.client;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.entity.HumanoidNpcEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class HumanoidNpcRenderer<T extends HumanoidNpcEntity> extends MobRenderer<T, PlayerModel<T>> {
    private final ResourceLocation texture;
    public HumanoidNpcRenderer(EntityRendererProvider.Context context, String textureName) {
        super(context, new Model<T>(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.5F);
        texture = new ResourceLocation(ModInfo.ID, "textures/entity/" + textureName + ".png");
        addLayer(new HumanoidArmorLayer<>(this,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)),
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR)), context.getModelManager()));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        addLayer(new ElytraLayer<>(this, context.getModelSet()));
    }
    @Override public ResourceLocation getTextureLocation(T entity) { return texture; }

    private static final class Model<T extends HumanoidNpcEntity> extends PlayerModel<T> {
        Model(ModelPart root) { super(root, true); }
        @Override public void setupAnim(T entity, float limbSwing, float limbAmount,
                                        float age, float yaw, float pitch) {
            ArmPose main = entity.getMainHandItem().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
            ArmPose off = entity.getOffhandItem().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
            boolean right = entity.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
            rightArmPose = right ? main : off;
            leftArmPose = right ? off : main;
            super.setupAnim(entity, limbSwing, limbAmount, age, yaw, pitch);
        }
    }
}
