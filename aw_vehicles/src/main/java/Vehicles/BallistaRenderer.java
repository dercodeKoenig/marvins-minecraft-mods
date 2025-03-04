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
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class BallistaRenderer extends EntityRenderer<Ballista> {

    RenderType r;
    ModelPart main;

    protected BallistaRenderer(EntityRendererProvider.Context context) {
        super(context);
        r = RenderType.entityCutout(getTextureLocation(null));

        main = createBodyLayer().bakeRoot();
    }

    @Override
    public void render(Ballista entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        double dp = entity.client_drawProgress - entity.client_drawProgressPrev;
        double drawProgress = Mth.clamp(entity.client_drawProgressPrev + dp * partialTick, -1, 1);

        // ---- RECOIL EFFECT ----
        double recoilOffset = 0.0;
        double recoilOffset2 = 0.0;

        if(entity.clien_ticksAfterShoot > 0) {
            double v = (entity.clien_ticksAfterShoot - 1 + partialTick);
            double timeSinceShot = 1 * Math.PI + v; // Scale time
            double amplitude = 15;  // How much it shakes
            double damping = 0.15;    // How fast it stops shaking
            if(v > 0.5*Math.PI)
                recoilOffset = amplitude * Math.exp(-damping * timeSinceShot) * (Math.cos(timeSinceShot));
            recoilOffset2 = amplitude * Math.exp(-damping * timeSinceShot) * (Math.sin(timeSinceShot));
        }



        double aMax = -67.5;
        double aMin = -30;
        double a = (aMax - aMin) * drawProgress + aMin + recoilOffset;
        double rA = (a - aMin) / (aMax - aMin);
        double p = drawProgress > 0 ? 0.38 : 0.6;
        double stringAngle = -aMin - Math.pow(Math.abs(rA), p) * Math.signum(rA) * 1.305f * a;

        main.getChild("armMain").z = (float) (Math.cos(entity.getYRot()/180*Math.PI)*(-recoilOffset2*0.1));
        main.getChild("armMain").x = (float) (Math.sin(entity.getYRot()/180*Math.PI)*(-recoilOffset2*0.1));
        // TODO: y recoil missing

        if(dp > 0){
            main.getChild("armMain").getChild("crankAxle").xRot = (float) (drawProgress*10);
        }

        float yRotDiff = (float) (entity.client_currentYRot - entity.client_lastYRot);
        float xRotDiff = (float) (entity.client_currentxRot - entity.client_lastxRot);
        main.getChild("armMain").yRot = (float) ((entity.client_currentYRot + partialTick*yRotDiff) / 180 * Math.PI);
        main.getChild("armMain").xRot = (float) ((entity.client_currentxRot + partialTick*xRotDiff) / 180 * Math.PI);

        // ---- APPLY ARM MOVEMENTS ----
        main.getChild("armMain").getChild("armLeftMain").yRot = (float) (a / 180 * Math.PI);
        main.getChild("armMain").getChild("armLeftMain").getChild("stringLeft").yRot = (float) (stringAngle / 180 * Math.PI);
        main.getChild("armMain").getChild("armLeftMain").getChild("stringLeft").xScale = (float) (1 + Math.abs(recoilOffset)/300f);

        main.getChild("armMain").getChild("armRightMain").yRot = -(float) (a / 180 * Math.PI);
        main.getChild("armMain").getChild("armRightMain").getChild("stringRight").yRot = -(float) (stringAngle / 180 * Math.PI);
        main.getChild("armMain").getChild("armRightMain").getChild("stringRight").xScale = (float) (1 + Math.abs(recoilOffset)/300f);

        if(entity.getDrawProgress()==1)
            main.getChild("armMain").getChild("trigger1").xRot=  (float) (-70f/180*Math.PI);
        else
            main.getChild("armMain").getChild("trigger1").xRot=  (float) (-20f/180*Math.PI);

        int bp = entity.getEntityData().get(Ballista.CONSTRUCTION_PROGRESS);
        main.getChild("armMain").getChild("armLeftMain").getChild("stringLeft").visible = bp >= 17;
        main.getChild("armMain").getChild("armRightMain").getChild("stringRight").visible = bp >= 17;
        main.getChild("armMain").getChild("crankAxle").visible = bp >= 16;
        main.getChild("armMain").getChild("armLeftMain").visible = bp >= 15;
        main.getChild("armMain").getChild("armRightMain").visible = bp >= 14;
        main.getChild("armMain").getChild("trigger1").visible = bp >= 13;
        main.getChild("armMain").getChild("catch1").visible = bp >= 13;
        main.getChild("armMain").getChild("catch2").visible = bp >= 13;
        main.getChild("armMain").getChild("rightTensionerRope").visible = bp >= 12;
        main.getChild("armMain").getChild("leftTensionerRope").visible = bp >= 11;
        main.getChild("armMain").getChild("turretHorizontalBrace4").visible = bp >= 10;
        main.getChild("armMain").getChild("armLeftVertical1").visible = bp >= 9;
        main.getChild("armMain").getChild("armLeftVertical2").visible = bp >= 9;
        main.getChild("armMain").getChild("armLeftVertical3").visible = bp >= 9;
        main.getChild("armMain").getChild("armRightVertical1").visible = bp >= 8;
        main.getChild("armMain").getChild("armRightVertical2").visible = bp >= 8;
        main.getChild("armMain").getChild("armRightVertical3").visible = bp >= 8;
        main.getChild("armMain").getChild("turretHorizontalBrace3").visible = bp >= 7;
        main.getChild("armMain").getChild("turretHorizontalBrace1").visible = bp >= 6;
        main.getChild("armMain").getChild("turretHorizontalBrace2").visible = bp >= 5;
        main.getChild("armMain").getChild("armSlotLeft").visible = bp >= 4;
        main.getChild("armMain").getChild("armSlotRight").visible = bp >= 3;
        main.getChild("armMain").getChild("armMidBrace").visible = bp >= 2;
        main.getChild("armMain").visible = bp >= 1;

        if(entity.getEntityData().get(Ballista.IS_BROKEN)){
            main.getChild("armMain").getChild("armLeftMain").yRot=-2.3f;
            main.getChild("armMain").getChild("turretHorizontalBrace4").xRot=0.4f;
            main.getChild("armMain").getChild("turretHorizontalBrace4").yRot=0.4f;
            main.getChild("armMain").getChild("rightTensionerRope").xRot=0.4f;
            main.getChild("armMain").getChild("rightTensionerRope").z=-6.5f;
            main.getChild("armMain").getChild("armRightMain").xRot = 0.4f;
            main.getChild("armMain").getChild("armRightMain").yRot = -1.2f;
            main.getChild("armMain").getChild("armRightMain").zRot = -0.5f;
            main.getChild("armMain").getChild("armLeftMain").getChild("stringLeft").visible = false;
            main.getChild("armMain").getChild("armRightMain").getChild("stringRight").yRot=0;
            main.getChild("armMain").getChild("armRightMain").getChild("stringRight").zRot=1.6f;
        }else{
            main.getChild("armMain").getChild("rightTensionerRope").z=-6.0f;
            main.getChild("armMain").getChild("turretHorizontalBrace4").xRot=0f;
            main.getChild("armMain").getChild("turretHorizontalBrace4").yRot=0f;
            main.getChild("armMain").getChild("rightTensionerRope").xRot=0f;
            main.getChild("armMain").getChild("armRightMain").xRot = 0f;
            main.getChild("armMain").getChild("armRightMain").zRot = 0f;
            main.getChild("armMain").getChild("armRightMain").getChild("stringRight").zRot=0f;
        }

        // ---- FINAL RENDER ----
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(1, 0, 0, 180));
        main.render(poseStack, bufferSource.getBuffer(r), packedLight, 0);
        poseStack.popPose();
    }


    @Override
    public ResourceLocation getTextureLocation(Ballista entityBallista) {
        return new ResourceLocation(Main.MODID, "textures/entity/ballista_stand_3.png");
    }

    public LayerDefinition createBodyLayer() {
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
        armMain.addOrReplaceChild("armLeftVertical3",
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
                        .addBox(0.0F, -0.33F, 0.0F, 17.0f, 0.66f, 0.66f),
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
                        .addBox(-17.0F, -0.33F, 0.0F, 17.0f, 0.66f, 0.66f),
                PartPose.offsetAndRotation(13.0F, 1.5F, 0.0F, 0.0F, 0.5235985F, 0.0F)
        );

        return LayerDefinition.create(mesh, 256, 256);
    }
}