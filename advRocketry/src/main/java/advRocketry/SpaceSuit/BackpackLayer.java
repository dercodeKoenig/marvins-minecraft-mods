package advRocketry.SpaceSuit;

import ARLib.obj.ModelFormatException;
import ARLib.obj.Static;
import ARLib.obj.WavefrontObject;
import advRocketry.Main;
import advRocketry.Registry.Items;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

public class BackpackLayer<T extends LivingEntity, M extends PlayerModel<T>> extends RenderLayer<T, M> {

    static ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/models/armor/jetpack.png");
    static WavefrontObject model;

    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "models/armor/jetpack.obj"));
        } catch (ModelFormatException e) {
            throw new RuntimeException(e);
        }
    }

    public BackpackLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack chestStack = entity.getItemBySlot(EquipmentSlot.CHEST);

        // Check if the player is wearing your specific armor
        if (chestStack.getItem() instanceof ChestPlate chestPlate) {
            poseStack.pushPose();
            CompoundTag cachedData = chestPlate.getCachedData(chestStack);
            int pressureTanks = cachedData.contains("pressureTanks") ? cachedData.getInt("pressureTanks") : 0;
            boolean jetpack = cachedData.contains("jetpack") ? cachedData.getBoolean("jetpack") : false;

            VertexConsumer v = buffer.getBuffer(Static.ENTITY_SOLID_TRIANGLES.apply(texture));

            // 1. Attach to the body (this handles crouching/breathing animations)
            this.getParentModel().body.translateAndRotate(poseStack);

            // 2. Adjust position (backplates usually need a slight Z-offset to stay behind the armor)
            // armor renders upside down for some reason so rotate
            poseStack.rotateAround(new Quaternionf().fromAxisAngleDeg(0, 0, 1, 180), 0, 0, 0);
            poseStack.translate(0, -0.2, 0);

            // 3. Call your OBJ render here
            model.renderPart("center", poseStack, v, packedLight, OverlayTexture.NO_OVERLAY, 0xffffffff);
            if (pressureTanks == 1) {
                // render single tank in center
                model.renderPart("oxygenTank", poseStack, v, packedLight, OverlayTexture.NO_OVERLAY, 0xffffffff);
            }
            if (pressureTanks == 2) {
                // render 2 tanks with the connections
                model.renderPart("connections", poseStack, v, packedLight, OverlayTexture.NO_OVERLAY, 0xffffffff);
                poseStack.pushPose();
                poseStack.translate(0.11, 0, 0);
                model.renderPart("oxygenTank", poseStack, v, packedLight, OverlayTexture.NO_OVERLAY, 0xffffffff);
                poseStack.translate(-0.22, 0, 0);
                model.renderPart("oxygenTank", poseStack, v, packedLight, OverlayTexture.NO_OVERLAY, 0xffffffff);
                poseStack.popPose();
            }
            if (jetpack) {
                // render the 2 jetpack engines
                model.renderPart("connections", poseStack, v, packedLight, OverlayTexture.NO_OVERLAY, 0xffffffff);
                poseStack.pushPose();
                poseStack.translate(0.2, 0, 0.02);
                model.renderPart("engine", poseStack, v, packedLight, OverlayTexture.NO_OVERLAY, 0xffffffff);
                poseStack.translate(-0.4, 0, 0);
                model.renderPart("engine", poseStack, v, packedLight, OverlayTexture.NO_OVERLAY, 0xffffffff);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
    }
}
