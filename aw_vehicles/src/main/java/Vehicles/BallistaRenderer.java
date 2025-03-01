package Vehicles;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class BallistaRenderer extends EntityRenderer<EntityBallista> {

    RenderType r;

    protected BallistaRenderer(EntityRendererProvider.Context context) {
        super(context);
        r = RenderType.create("x", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,1024, RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
                .setLightmapState(LIGHTMAP)
                .setTextureState(new TextureStateShard(getTextureLocation(null),false,true))
                .createCompositeState(false));
    }

    @Override
    public void render(EntityBallista p_entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(1,0,0,180));
        createBodyLayer().bakeRoot().render(poseStack,bufferSource.getBuffer(r),packedLight,0);

        super.render(p_entity,entityYaw,partialTick,poseStack,bufferSource,packedLight);
    }


        @Override
    public ResourceLocation getTextureLocation(EntityBallista entityBallista) {
        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/entity/ballista_stand_3.png");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // baseMain (texture size 256x256 is set when baking the LayerDefinition)
        PartDefinition baseMain = root.addOrReplaceChild("baseMain",
                CubeListBuilder.create()
                        .texOffs(0, 21)
                        .addBox(-7.0F, -2.0F, -7.0F, 14, 2, 14),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // basePillar is a child of baseMain
        PartDefinition basePillar = baseMain.addOrReplaceChild("basePillar",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 10, 4),
                PartPose.offset(0.0F, -12.0F, 0.0F)
        );

        // pivot is a child of basePillar
        basePillar.addOrReplaceChild("pivot",
                CubeListBuilder.create()
                        .texOffs(17, 0)
                        .addBox(-1.0F, 0.0F, -1.0F, 2, 2, 2),
                PartPose.offset(0.0F, -2.0F, 0.0F)
        );

        // armMain is a separate part attached to the root
        PartDefinition armMain = root.addOrReplaceChild("armMain",
                CubeListBuilder.create()
                        .texOffs(0, 128)
                        .addBox(-1.5F, -2.0F, -4.5F, 3, 2, 28),
                // setPieceRotation(armMain, -6.585082E-7f, -2.3593943E-6f, 0.0f)
                PartPose.offsetAndRotation(0.0F, -14.0F, 0.0F,
                        -6.585082E-7F, -2.3593943E-6F, 0.0F)
        );

        // armFront as child of armMain
        armMain.addOrReplaceChild("armFront",
                CubeListBuilder.create()
                        .texOffs(63, 128)
                        .addBox(0.0F, 0.0F, 0.0F, 3, 2, 4),
                PartPose.offset(-1.5F, -2.0F, -11.5F)
        );

        // turretHorizontalBrace2 as child of armMain
        armMain.addOrReplaceChild("turretHorizontalBrace2",
                CubeListBuilder.create()
                        .texOffs(63, 135)
                        .addBox(-13.0F, 0.0F, -3.0F, 13, 1, 3),
                PartPose.offsetAndRotation(11.0F, -1.5F, -4.5F,
                        0.0F, 0.5410521F, 0.0F)
        );

        // turretHorizontalBrace3 as child of armMain
        armMain.addOrReplaceChild("turretHorizontalBrace3",
                CubeListBuilder.create()
                        .texOffs(63, 140)
                        .addBox(0.0F, 0.0F, 0.0F, 22, 2, 3),
                PartPose.offset(-11.0F, -2.0F, -7.5F)
        );

        // armMidBrace as child of armMain
        armMain.addOrReplaceChild("armMidBrace",
                CubeListBuilder.create()
                        .texOffs(0, 159)
                        .addBox(0.0F, 0.0F, 0.0F, 3, 1, 35),
                PartPose.offset(-1.5F, -3.0F, -11.5F)
        );

        // armSlotLeft and armSlotRight as children of armMain
        armMain.addOrReplaceChild("armSlotLeft",
                CubeListBuilder.create()
                        .texOffs(77, 159)
                        .addBox(0.0F, 0.0F, 0.0F, 1, 1, 35),
                PartPose.offset(0.5F, -4.0F, -11.5F)
        );
        armMain.addOrReplaceChild("armSlotRight",
                CubeListBuilder.create()
                        .texOffs(77, 159)
                        .addBox(0.0F, 0.0F, 0.0F, 1, 1, 35),
                PartPose.offset(-1.5F, -4.0F, -11.5F)
        );

        // Vertical parts on the arm
        armMain.addOrReplaceChild("armleftVertical3",
                CubeListBuilder.create()
                        .texOffs(78, 128)
                        .addBox(0.0F, 0.0F, 0.0F, 1, 1, 1),
                PartPose.offset(10.0F, -3.0F, -6.0F)
        );
        armMain.addOrReplaceChild("armLeftVertical2",
                CubeListBuilder.create()
                        .texOffs(78, 128)
                        .addBox(0.0F, 0.0F, 0.0F, 1, 1, 1),
                PartPose.offset(10.0F, -7.0F, -6.0F)
        );
        armMain.addOrReplaceChild("armLeftVertical1",
                CubeListBuilder.create()
                        .texOffs(83, 128)
                        .addBox(0.0F, 0.0F, 0.0F, 1, 5, 1),
                PartPose.offset(10.0F, -7.0F, -7.0F)
        );

        // turretHorizontalBrace4
        armMain.addOrReplaceChild("turretHorizontalBrace4",
                CubeListBuilder.create()
                        .texOffs(63, 140)
                        .addBox(0.0F, 0.0F, 0.0F, 22, 2, 3),
                PartPose.offset(-11.0F, -9.0F, -7.5F)
        );

        // leftTensionerRope and its children
        PartDefinition leftTensionerRope = armMain.addOrReplaceChild("leftTensionerRope",
                CubeListBuilder.create()
                        .texOffs(114, 128)
                        .addBox(-0.5F, -3.0F, -0.5F, 1, 11, 1),
                PartPose.offset(5.5F, -7.0F, -6.0F)
        );
        leftTensionerRope.addOrReplaceChild("leftTensioner",
                CubeListBuilder.create()
                        .texOffs(88, 128)
                        .addBox(-1.0F, -0.5F, -0.5F, 2, 1, 1),
                PartPose.offset(0.0F, -3.0F, 0.0F)
        );
        leftTensionerRope.addOrReplaceChild("leftTensioner2",
                CubeListBuilder.create()
                        .texOffs(88, 131)
                        .addBox(-0.5F, -0.5F, -1.0F, 1, 1, 2),
                PartPose.offset(0.0F, -3.0F, 0.0F)
        );

        // rightTensionerRope and its children
        PartDefinition rightTensionerRope = armMain.addOrReplaceChild("rightTensionerRope",
                CubeListBuilder.create()
                        .texOffs(114, 128)
                        .addBox(-0.5F, -3.0F, -0.5F, 1, 11, 1),
                PartPose.offset(-5.5F, -7.0F, -6.0F)
        );
        rightTensionerRope.addOrReplaceChild("rightTensioner",
                CubeListBuilder.create()
                        .texOffs(88, 128)
                        .addBox(-1.0F, -0.5F, -0.5F, 2, 1, 1),
                PartPose.offset(0.0F, -3.0F, 0.0F)
        );
        rightTensionerRope.addOrReplaceChild("rightTensioner2",
                CubeListBuilder.create()
                        .texOffs(88, 131)
                        .addBox(-0.5F, -0.5F, -1.0F, 1, 1, 2),
                PartPose.offset(0.0F, -3.0F, 0.0F)
        );

        // turretHorizontalBrace1
        armMain.addOrReplaceChild("turretHorizontalBrace1",
                CubeListBuilder.create()
                        .texOffs(63, 135)
                        .addBox(0.0F, 0.0F, -3.0F, 13, 1, 3),
                PartPose.offsetAndRotation(-11.0F, -1.5F, -4.5F,
                        0.0F, -0.5410521F, 0.0F)
        );

        // armRight vertical parts
        armMain.addOrReplaceChild("armRightVertical3",
                CubeListBuilder.create()
                        .texOffs(78, 128)
                        .addBox(0.0F, 0.0F, 0.0F, 1, 1, 1),
                PartPose.offset(-11.0F, -3.0F, -6.0F)
        );
        armMain.addOrReplaceChild("armRightVertical2",
                CubeListBuilder.create()
                        .texOffs(78, 128)
                        .addBox(0.0F, 0.0F, 0.0F, 1, 1, 1),
                PartPose.offset(-11.0F, -7.0F, -6.0F)
        );
        armMain.addOrReplaceChild("armRightVertical1",
                CubeListBuilder.create()
                        .texOffs(83, 128)
                        .addBox(0.0F, 0.0F, 0.0F, 1, 5, 1),
                PartPose.offset(-11.0F, -7.0F, -7.0F)
        );

        // trigger1 and its child trigger2
        PartDefinition trigger1 = armMain.addOrReplaceChild("trigger1",
                CubeListBuilder.create()
                        .texOffs(63, 146)
                        .addBox(-0.5F, -1.0F, 0.0F, 1, 1, 5),
                PartPose.offsetAndRotation(0.0F, -1.0F, 17.5F,
                        -1.256629F, 0.0F, 0.0F)
        );
        trigger1.addOrReplaceChild("trigger2",
                CubeListBuilder.create()
                        .texOffs(76, 146)
                        .addBox(-0.5F, -4.0F, 0.0F, 1, 4, 1),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F,
                        1.2217292F, 1.934953E-7F, 3.1625038E-7F)
        );

        // crankAxle and its children
        PartDefinition crankAxle = armMain.addOrReplaceChild("crankAxle",
                CubeListBuilder.create()
                        .texOffs(63, 153)
                        .addBox(0.0F, -0.5F, -0.5F, 4, 1, 1),
                PartPose.offset(-2.0F, -2.0F, 21.0F)
        );
        crankAxle.addOrReplaceChild("crankHandle1",
                CubeListBuilder.create()
                        .texOffs(81, 146)
                        .addBox(-0.5F, -2.5F, -0.5F, 1, 5, 1),
                PartPose.offset(-0.5F, 0.0F, 0.0F)
        );
        crankAxle.addOrReplaceChild("crankHandle2",
                CubeListBuilder.create()
                        .texOffs(86, 146)
                        .addBox(-0.5F, -0.5F, -2.5F, 1, 1, 5),
                PartPose.offset(-0.5F, 0.0F, 0.0F)
        );

        // catch2 and catch1 as children of armMain
        armMain.addOrReplaceChild("catch2",
                CubeListBuilder.create()
                        .texOffs(99, 146)
                        .addBox(0.0F, 0.0F, 0.0F, 2, 1, 4),
                PartPose.offsetAndRotation(-1.0F, -6.0F, 20.5F,
                        -0.8552113F, 0.0F, 0.0F)
        );
        armMain.addOrReplaceChild("catch1",
                CubeListBuilder.create()
                        .texOffs(99, 152)
                        .addBox(0.0F, 0.0F, 0.0F, 2, 1, 3),
                PartPose.offset(-1.0F, -6.0F, 17.5F)
        );

        // armRightMain and its children
        PartDefinition armRightMain = armMain.addOrReplaceChild("armRightMain",
                CubeListBuilder.create()
                        .texOffs(0, 215)
                        .addBox(-6.5F, -1.0F, -1.0F, 8, 3, 1),
                PartPose.offsetAndRotation(-5.5F, -5.0F, -6.0F,
                        0.0F, 0.5235982F, 0.0F)
        );
        armRightMain.addOrReplaceChild("armRightMainInner",
                CubeListBuilder.create()
                        .texOffs(0, 203)
                        .addBox(-2.5F, -1.0F, 0.0F, 4, 3, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        armRightMain.addOrReplaceChild("armRightMainInner3",
                CubeListBuilder.create()
                        .texOffs(0, 196)
                        .addBox(-6.5F, 0.0F, 0.0F, 4, 1, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        armRightMain.addOrReplaceChild("armRightMainInner2",
                CubeListBuilder.create()
                        .texOffs(0, 199)
                        .addBox(-6.5F, -0.5F, -0.5F, 4, 2, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        armRightMain.addOrReplaceChild("armRightInner",
                CubeListBuilder.create()
                        .texOffs(0, 208)
                        .addBox(-13.25F, 0.0F, -0.5F, 7, 1, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        armRightMain.addOrReplaceChild("armRightOuter",
                CubeListBuilder.create()
                        .texOffs(0, 211)
                        .addBox(-13.5F, -0.5F, -1.0F, 7, 2, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        armRightMain.addOrReplaceChild("stringRight",
                CubeListBuilder.create()
                        .texOffs(0, 220)
                        .addBox(0.0F, -0.5F, 0.0F, 17, 1, 1),
                PartPose.offsetAndRotation(-13.0F, 0.5F, 0.0F, 0.0F, -0.5235985F, 0.0F)
        );

        // armLeftMain and its children
        PartDefinition armLeftMain = armMain.addOrReplaceChild("armLeftMain",
                CubeListBuilder.create()
                        .texOffs(0, 215)
                        .addBox(-1.5F, 0.0F, -1.0F, 8, 3, 1),
                PartPose.offsetAndRotation(5.5F, -6.0F, -6.0F,
                        0.0F, -0.5235988F, 0.0F)
        );
        armLeftMain.addOrReplaceChild("armLeftMainInner",
                CubeListBuilder.create()
                        .texOffs(0, 203)
                        .addBox(-1.5F, 0.0F, 0.0F, 4, 3, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        armLeftMain.addOrReplaceChild("armLeftOuter",
                CubeListBuilder.create()
                        .texOffs(0, 211)
                        .addBox(6.5F, 0.5F, -1.0F, 7, 2, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        armLeftMain.addOrReplaceChild("armLeftMainInner2",
                CubeListBuilder.create()
                        .texOffs(0, 199)
                        .addBox(2.5F, 0.5F, -0.5F, 4, 2, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        armLeftMain.addOrReplaceChild("armLeftMainInner3",
                CubeListBuilder.create()
                        .texOffs(0, 196)
                        .addBox(2.5F, 1.0F, -0.0F, 4, 1, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        armLeftMain.addOrReplaceChild("armLeftInner",
                CubeListBuilder.create()
                        .texOffs(0, 208)
                        .addBox(6.25F, 1.0F, -0.5F, 7, 1, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        armLeftMain.addOrReplaceChild("stringLeft",
                CubeListBuilder.create()
                        .texOffs(0, 220)
                        .addBox(-17.0F, -0.5F, 0.0F, 17, 1, 1),
                PartPose.offsetAndRotation(13.0F, 1.5F, 0.0F, 0.0F, 0.5235985F, 0.0F)
        );

        return LayerDefinition.create(mesh, 256, 256);
    }
}