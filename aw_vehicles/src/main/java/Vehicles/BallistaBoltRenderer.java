package Vehicles;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class BallistaBoltRenderer extends EntityRenderer<BallistaBolt> {

    RenderType r;
    ModelPart main;

    protected BallistaBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        r = RenderType.create("x", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1024, RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
                .setLightmapState(LIGHTMAP)
                .setTextureState(new TextureStateShard(getTextureLocation(null), false, true))
                .createCompositeState(false));

        main = createBodyLayer().bakeRoot();
    }

    @Override
    public void render(BallistaBolt entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        //main = createBodyLayer().bakeRoot();
        poseStack.pushPose();

        float scale = 0.2f;
        poseStack.translate(0,0.125f,0);
        poseStack.scale(scale,scale,scale);

        double dx = entity.dx * partialTick;
        double dy = entity.dy * partialTick;
        double dz = entity.dz * partialTick;
        poseStack.translate(dx,dy,dz);

        main.setRotation((float) (entity.getXRot() / 180 * Math.PI), (float) (entity.getYRot() / 180 * Math.PI), 0);

        poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(1, 0, 0, 180));

        main.render(poseStack, bufferSource.getBuffer(r), packedLight, 0);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);


    }


    @Override
    public ResourceLocation getTextureLocation(BallistaBolt bolt) {
        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/entity/arrow_wood.png");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // headPoint with a 90° rotation about Y (converted from Trig.toRadians(90.f))
        PartDefinition headPoint = root.addOrReplaceChild("headPoint",
                CubeListBuilder.create()
                        .texOffs(0, 7)
                        .addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, (float) Math.toRadians(90.0F), 0.0F)
        );

        // Shaft core parts
        PartDefinition shaftCoreTop = headPoint.addOrReplaceChild("shaftCoreTop",
                CubeListBuilder.create()
                        .texOffs(0, 9)
                        .addBox(0.5F, -2.5F, -1.0F, 80, 1, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition shaftCoreBottom = headPoint.addOrReplaceChild("shaftCoreBottom",
                CubeListBuilder.create()
                        .texOffs(0, 9)
                        .addBox(0.5F, 1.5F, -1.0F, 80, 1, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition shaftCoreLeft = headPoint.addOrReplaceChild("shaftCoreLeft",
                CubeListBuilder.create()
                        .texOffs(0, 13)
                        .addBox(0.5F, -1.0F, -2.5F, 80, 2, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition shaftCoreRight = headPoint.addOrReplaceChild("shaftCoreRight",
                CubeListBuilder.create()
                        .texOffs(0, 13)
                        .addBox(0.5F, -1.0F, 1.5F, 80, 2, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition shaftCoreRightT1 = headPoint.addOrReplaceChild("shaftCoreRightT1",
                CubeListBuilder.create()
                        .texOffs(0, 13)
                        .addBox(0.5F, -1.5F, 1.0F, 80, 1, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition shaftCoreRightB1 = headPoint.addOrReplaceChild("shaftCoreRightB1",
                CubeListBuilder.create()
                        .texOffs(0, 13)
                        .addBox(0.5F, 0.5F, 1.0F, 80, 1, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition shaftCoreLeftB1 = headPoint.addOrReplaceChild("shaftCoreLeftB1",
                CubeListBuilder.create()
                        .texOffs(0, 13)
                        .addBox(0.5F, 0.5F, -2.0F, 80, 1, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition shaftCoreLeftT1 = headPoint.addOrReplaceChild("shaftCoreLeftT1",
                CubeListBuilder.create()
                        .texOffs(0, 13)
                        .addBox(0.5F, -1.5F, -2.0F, 80, 1, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition shaftCoreTopR = headPoint.addOrReplaceChild("shaftCoreTopR",
                CubeListBuilder.create()
                        .texOffs(0, 13)
                        .addBox(0.5F, -2.0F, 0.5F, 80, 1, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition shaftCoreTopL = headPoint.addOrReplaceChild("shaftCoreTopL",
                CubeListBuilder.create()
                        .texOffs(0, 13)
                        .addBox(0.5F, -2.0F, -1.5F, 80, 1, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition shaftCoreBottomL = headPoint.addOrReplaceChild("shaftCoreBottomL",
                CubeListBuilder.create()
                        .texOffs(0, 13)
                        .addBox(0.5F, 1.0F, -1.5F, 80, 1, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition shaftCoreBottomR = headPoint.addOrReplaceChild("shaftCoreBottomR",
                CubeListBuilder.create()
                        .texOffs(0, 13)
                        .addBox(0.5F, 1.0F, 0.5F, 80, 1, 1),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // Veins
        PartDefinition veinTop = headPoint.addOrReplaceChild("veinTop",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -0.5F, -0.5F, 15, 5, 1),
                PartPose.offsetAndRotation(66.0F, -2.0F, 0.0F, 1.0402973E-9F, 0.0F, -0.29670483F)
        );
        PartDefinition veinBottom = headPoint.addOrReplaceChild("veinBottom",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -4.5F, -0.5F, 15, 5, 1),
                PartPose.offsetAndRotation(66.0F, 2.0F, 0.0F, 1.0402973E-9F, 0.0F, 0.29670596F)
        );
        PartDefinition veinRight = headPoint.addOrReplaceChild("veinRight",
                CubeListBuilder.create()
                        .texOffs(33, 0)
                        .addBox(-0.5F, -0.5F, -4.5F, 15, 1, 5),
                PartPose.offsetAndRotation(66.0F, 0.0F, 2.0F, -3.2249215E-8F, -0.2967058F, 0.0F)
        );
        PartDefinition veinLeft = headPoint.addOrReplaceChild("veinLeft",
                CubeListBuilder.create()
                        .texOffs(33, 0)
                        .addBox(-0.5F, -0.5F, -0.5F, 15, 1, 5),
                PartPose.offsetAndRotation(66.0F, 0.0F, -2.0F, -3.2249215E-8F, 0.29670596F, 0.0F)
        );

        // Tips
        PartDefinition tipRight = headPoint.addOrReplaceChild("tipRight",
                CubeListBuilder.create()
                        .texOffs(0, 17)
                        .addBox(-0.5F, -0.5F, -3.5F, 15, 1, 4),
                PartPose.offsetAndRotation(-7.0F, 0.0F, 2.0F, -3.2249215E-8F, -0.26179862F, 0.0F)
        );
        PartDefinition tipLeft = headPoint.addOrReplaceChild("tipLeft",
                CubeListBuilder.create()
                        .texOffs(0, 17)
                        .addBox(-0.5F, -0.5F, -0.5F, 15, 1, 4),
                PartPose.offsetAndRotation(-7.0F, 0.0F, -2.0F, -3.2249215E-8F, 0.2617993F, 0.0F)
        );
        PartDefinition tipTop = headPoint.addOrReplaceChild("tipTop",
                CubeListBuilder.create()
                        .texOffs(39, 17)
                        .addBox(-0.5F, -0.5F, -0.5F, 15, 4, 1),
                PartPose.offsetAndRotation(-7.0F, -2.0F, 0.0F, 1.0402973E-9F, 0.0F, -0.261798F)
        );
        PartDefinition tipBottom = headPoint.addOrReplaceChild("tipBottom",
                CubeListBuilder.create()
                        .texOffs(39, 17)
                        .addBox(-0.5F, -3.5F, -0.5F, 15, 4, 1),
                PartPose.offsetAndRotation(-7.0F, 2.0F, 0.0F, 1.0402973E-9F, 0.0F, 0.2617993F)
        );
        PartDefinition tipVert = headPoint.addOrReplaceChild("tipVert",
                CubeListBuilder.create()
                        .texOffs(29, 23)
                        .addBox(-0.5F, -0.5F, -0.5F, 4, 4, 1),
                PartPose.offsetAndRotation(-10.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853976F)
        );
        PartDefinition tipHoriz = headPoint.addOrReplaceChild("tipHoriz",
                CubeListBuilder.create()
                        .texOffs(40, 23)
                        .addBox(-0.5F, -0.5F, -0.5F, 4, 1, 4),
                PartPose.offsetAndRotation(-10.0F, 0.0F, 0.0F, 0.0F, 0.7853982F, 0.0F)
        );
        PartDefinition tipShaft = headPoint.addOrReplaceChild("tipShaft",
                CubeListBuilder.create()
                        .texOffs(0, 23)
                        .addBox(-0.5F, -2.0F, -2.0F, 10, 4, 4),
                PartPose.offsetAndRotation(-7.0F, 0.0F, 0.0F, 0.7853982F, 0.0F, -8.530438E-8F)
        );

        return LayerDefinition.create(mesh, 192, 36);
    }

}