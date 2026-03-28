package advRocketry.Render.starmap;

import ARLib.gui.IGuiHandler;
import ARLib.gui.modules.GuiModuleBase;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Render.SkyRenderer;
import advRocketry.Render.shaderUtils;
import advRocketry.Utils.CelestialUtils;
import advRocketry.Utils.ClientUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL30;

import java.util.Objects;

import static advRocketry.Render.SkyRenderer.*;
import static net.minecraft.client.renderer.RenderStateShard.NO_DEPTH_TEST;

public class GuiModulePlanetView extends GuiModuleBase {
    public int w;
    public int h;
    public ResourceLocation dimensionId;
    public float zoom = 500;
    public boolean renderRelativeToPlayer = false;

    public GuiModulePlanetView(int id, IGuiHandler guiHandler, int x, int y, int w, int h) {
        super(id, guiHandler, x, y);
        this.w = w;
        this.h = h;
    }

    public void setTargetAndSync(ResourceLocation target) {
        if (!Objects.equals(target, dimensionId)) {
            dimensionId = target;
            broadcastModuleUpdate();
        }
    }



    public void client_onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if(isMouseOver(mouseX, mouseY, onGuiX, onGuiY, w, h)) {
            float zoomSpeed = zoom * 0.1f;
            zoom -= (float) (scrollY * zoomSpeed);
            zoom = Math.max(250f, Math.min(zoom, 3000f));
        }
    }


    public void client_handleDataSyncedToClient(CompoundTag tag) {
        super.client_handleDataSyncedToClient(tag);
        if (tag.contains(getMyTagKey())) {
            CompoundTag myTag = tag.getCompound(getMyTagKey());
            if (myTag.contains("dimensionId")) {
                String _dimensionId = myTag.getString("dimensionId");
                if (_dimensionId.equals("null"))
                    dimensionId = null;
                else
                    dimensionId = ResourceLocation.parse(_dimensionId);
            }
        }

    }

    public void server_writeDataToSyncToClient(CompoundTag tag) {
        super.server_writeDataToSyncToClient(tag);
        CompoundTag myTag = new CompoundTag();
        String _dimensionId = "null";
        if (dimensionId != null)
            _dimensionId = dimensionId.toString();
        myTag.putString("dimensionId", _dimensionId);
        tag.put(getMyTagKey(), myTag);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        guiGraphics.fill(onGuiX, onGuiY, onGuiX+w, onGuiY+h, 0xff000000);

        if (dimensionId == null) return;

        PlanetDimension planet = (PlanetDimension) DimensionManager.INSTANCE_CLIENT.get(dimensionId);
        if (planet == null) return;

        Dimension myDim = DimensionManager.INSTANCE_CLIENT.get(ClientUtils.getPlayerLevel().dimension().location());
        if(myDim == null) return;

        Matrix4f projMatrix = new Matrix4f();
        float fov = (float) Math.toRadians(60.0f);
        float aspect = (float) w / h;
        float near = 0.1f;
        float far = 10000;
        projMatrix.setPerspective(fov, aspect, near, far);

        Vec3 planetPos = planet.getPosition(partialTick);

        Vector3f eyePos = new Vector3f(1,0,0).mul(zoom);

        if(renderRelativeToPlayer)
            eyePos = myDim.getPosition(partialTick).subtract(planetPos).normalize().scale(zoom).toVector3f();
        else{
            Vec3 strongestStarPos = null;
            double strongestValue = 0;
            for (ResourceLocation starId : planet.getCurrentMainStars()) {
                if (DimensionManager.INSTANCE_CLIENT.get(starId) instanceof PlanetDimension star){
                    Vec3 starPos = star.getPosition(partialTick);
                    double distanceToStar = starPos.distanceTo(planetPos);
                    double v = star.getRadiationIntensity() / distanceToStar / distanceToStar;
                    if(v > strongestValue){
                        strongestValue = v;
                        strongestStarPos = starPos;
                    }
                }
            }
            if (strongestStarPos != null) {
                Vec3 starToPlanet = planetPos.subtract(strongestStarPos);
                Vec3 right = starToPlanet.cross(new Vec3(0,1,0));
                if(right.length() < 0.0001)
                    right = new Vec3(1,0,0);
                right = CelestialUtils.rotate(right, starToPlanet, -30);
                eyePos = right.normalize().scale(zoom).toVector3f();
            }
        }

        Matrix4f viewMatrix = new Matrix4f().lookAt(
                new Vector3f(eyePos.x, eyePos.y, eyePos.z), // Camera Position
                new Vector3f(0, 0, 0),             // Look at Center
                new Vector3f(0, 1, 0)              // "Up" direction
        );

        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 1f);

        SkyRenderer.PlanetsAndStarsTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);

        // this makes sure the planet renders in center of the gui element
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        int pixelX = (int) (onGuiX * guiScale);
        int pixelY = (int) (onGuiY * guiScale);
        int pixelW = (int) (w * guiScale);
        int pixelH = (int) (h * guiScale);
        int screenHeight = Minecraft.getInstance().getWindow().getHeight();
        RenderSystem.viewport(pixelX, screenHeight - pixelY - pixelH, pixelW, pixelH);


        Matrix4f planetMatrix = new Matrix4f();

        Vec3 modelUp = new Vec3(0, 1, 0);
        Vec3 targetNorth = planet.getRotationAxis().normalize();
        Vec3 rotAxis = modelUp.cross(targetNorth);
        if (rotAxis.length() > 1e-9) {
            double rotAngleRad = Math.asin(rotAxis.length());
            planetMatrix.rotate(new Quaternionf().fromAxisAngleRad(rotAxis.toVector3f(), (float) rotAngleRad));
        } else if (modelUp.dot(targetNorth) < 0) {
            planetMatrix.rotate(new Quaternionf().fromAxisAngleDeg(new Vec3(1, 0, 0).toVector3f(), 180f));
        }

        double planetRotationAngle = planet.getRotationAngle(partialTick);
        planetMatrix.rotate(new Quaternionf().fromAxisAngleDeg(new Vector3f(0, 1, 0), (float) planetRotationAngle));

        float renderScale = Math.min(w, h);
        planetMatrix.scale(renderScale);

        // render the planet as if we observe it from space (0 atm density, no sky color...)
        SkyRenderer.renderPlanet(
                planet,
                projMatrix,
                viewMatrix,
                new Matrix4f(),
                planetMatrix,
                eyePos,
                0,
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, 0),
                0,
                false,
                1,
                partialTick
        );

        if (planet.hasRings()) {
            SkyRenderer.renderRingSystem(
                    planet,
                    projMatrix,
                    viewMatrix,
                    new Matrix4f(),
                    planetMatrix,
                    eyePos,
                    0,
                    new Vector3f(0, 0, 0),
                    0,
                    renderScale,
                    1,
                    partialTick
            );
        }

        vertexBufferSquare.bind();
        ShaderInstance shader;

        // blit extract bright regions
        bloomExtractBrightTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        RenderSystem.setShader(shaderUtils::getBlitExtractBrightShader);
        shader = RenderSystem.getShader();
        shader.setSampler("frame", PlanetsAndStarsTarget.getColorTextureId());
        shader.getUniform("threshold").set(1f);
        shader.getUniform("resolution").set(PlanetsAndStarsTarget.width, PlanetsAndStarsTarget.height);
        shader.apply();
        vertexBufferSquare.draw();
        shader.clear();

        // blit blur
        RenderSystem.setShader(shaderUtils::getBlitBlurShader);
        shader = RenderSystem.getShader();

        bloomBlurHorizontal.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        shader.setSampler("image", bloomExtractBrightTarget.getColorTextureId());
        shader.getUniform("resolution").set(bloomExtractBrightTarget.width);
        shader.getUniform("horizontal").set(1);
        shader.apply();
        vertexBufferSquare.draw();
        shader.clear();

        bloomBlurVertical.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        shader.setSampler("image", bloomBlurHorizontal.getColorTextureId());
        shader.getUniform("resolution").set(bloomBlurHorizontal.height);
        shader.getUniform("horizontal").set(0);
        shader.apply();
        vertexBufferSquare.draw();
        shader.clear();


        // enable scissors to not clean the entire gui away
        guiGraphics.enableScissor(onGuiX, onGuiY, onGuiX + w, onGuiY + h);

        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false); // always render on top
        RenderSystem.setShader(shaderUtils::getBlitPostProcessingShader);
        shader = RenderSystem.getShader();
        shader.setSampler("Frame", PlanetsAndStarsTarget.getColorTextureId());
        shader.setSampler("Bloom", bloomBlurVertical.getColorTextureId());
        shader.getUniform("bloomIntensity").set(1f);
        shader.apply();
        vertexBufferSquare.draw();
        shader.clear();

        guiGraphics.disableScissor();

        VertexBuffer.unbind();
    }
}
