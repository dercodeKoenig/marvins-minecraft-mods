package NPCs.Npc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BowItem;

public class NPCRenderer extends MobRenderer<NPCBase, HumanoidModel<NPCBase>> {
    public NPCRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));

    }

    @Override
    public ResourceLocation getTextureLocation(NPCBase entity) {
        String texture = entity.getEntityData().get(NPCBase.DATA_TEXTURE);
        if(texture.isEmpty())texture="worker.png";
        ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("aw_npc", "textures/entity/" + texture);
        return tex;
    }

    @Override
    public void render(NPCBase entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        model.attackTime = entity.getAttackAnim(partialTicks);

        if (entity.getMainHandItem().isEmpty())
            model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        else
            model.rightArmPose = HumanoidModel.ArmPose.ITEM;

        if (entity.getOffhandItem().isEmpty())
            model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        else
            model.leftArmPose = HumanoidModel.ArmPose.ITEM;


        if(entity.isUsingItem() && entity.getMainHandItem().getItem() instanceof BowItem){
            model.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}