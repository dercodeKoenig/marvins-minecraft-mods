package advRocketry.Render;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.mixins.ShaderInstanceMixin;
import advRocketry.utils.AxisDirections;
import advRocketry.utils.ClientUtils;
import advRocketry.utils.RenderUtils;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class test1 {
    public static void setdefaultuniform(ShaderInstance shader){

        Dimension myCurrentSpaceObject = DimensionManager.get(ClientUtils.getPlayerLevel().dimension().location());
        Vec3 myCurrentPositionInSpace = myCurrentSpaceObject.getPosition(0);

        int totalLights = 0;
        for (ResourceLocation lightSourceId : myCurrentSpaceObject.getCurrentMainStars()) {
            Dimension star = DimensionManager.get(lightSourceId);
            Vec3 StarPos = star.getPosition(0);
            Vec3 LightVector = myCurrentPositionInSpace.subtract(StarPos).scale(-1); //shader uses planet to star for dot product
            Uniform lightVec  = shader.getUniform("LightVectors[" + totalLights + "]");
            if(lightVec != null){
                lightVec.set((float) LightVector.x, (float) LightVector.y, (float) LightVector.z);
            }
            Vector3f starColorLin = RenderUtils.gamma_reverse(star.getEmissiveColor());
            Uniform lightCol = shader.getUniform("LightColors[" + totalLights + "]");
            if(lightCol != null) {
                lightCol.set(starColorLin.x, starColorLin.y, starColorLin.z, star.getRadiationIntensity());
            }
            totalLights += 1;
        }

        Uniform lightNum = shader.getUniform("LightCount");
        if(lightNum != null)
            lightNum.set(totalLights);

        AxisDirections myGlobalAxis = myCurrentSpaceObject.getGlobalAxisDirections(0);
        // Create the base orientation for our skybox using the planet's axes.
        // This matrix transforms global space coordinates into world coordinates
        Matrix4f worldMatrix = new Matrix4f().lookAt(
                new Vector3f(0, 0, 0),
                myGlobalAxis.front.toVector3f(),
                myGlobalAxis.up.toVector3f()    // Up direction
        );
        Uniform worldMat =shader.getUniform("WorldMat");
        if(worldMat != null)
            worldMat.set(worldMatrix);

    }
}
