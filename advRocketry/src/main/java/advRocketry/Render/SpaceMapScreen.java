package advRocketry.Render;

import advRocketry.Config;
import advRocketry.Dimension.*;
import advRocketry.utils.CelestialUtils;
import advRocketry.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.lang.Math;

import static advRocketry.utils.CelestialUtils.fromAU;
import static advRocketry.utils.CelestialUtils.fromEarthMasses;
import static net.minecraft.client.renderer.RenderStateShard.NO_CULL;
import static net.minecraft.client.renderer.RenderStateShard.NO_DEPTH_TEST;

public class SpaceMapScreen extends Screen {
    public SpaceMapScreen() {
        super(Component.literal("space map"));
    }


    private float camX = 0;
    private float camY = 0;
    private float zoom = 3000f;

    private float logScale = 0.5f;
    private float scale = 0.5f;

    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(new MapSlider(
                10, this.height - 20, 100, 10,
                Component.literal("scale"), 0.5,
                (newValue) -> {
                    // Map the 0.0-1.0 slider value to your zoom range
                    this.scale = (float) newValue;
                }
        ));

        this.addRenderableWidget(new MapSlider(
                120, this.height - 20, 100, 10,
                Component.literal("logScale"), 0.5,
                (newValue) -> {
                    // Map the 0.0-1.0 slider value to your zoom range
                    this.logScale = (float) newValue;
                }
        ));
    }

    // This method is inherited from GuiEventListener
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // scrollY is the vertical scroll amount.
        // We use an exponential zoom so it feels smooth at all distances.
        float zoomSpeed = zoom * 0.3f;
        zoom -= (float) (scrollY * zoomSpeed);

        // Clamp zoom so we don't go past the planets or infinitely far away
        zoom = Math.max(1f, Math.min(zoom, 15000f));

        return true; // Return true to tell Minecraft we handled the input
    }

    // This method is inherited from GuiEventListener
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 1. Check if a UI element (like the slider) is being dragged first
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true; // Stop here! The slider handled it.
        }

        // Button 0 is left-click
        if (button == 0) {
            // Sensitivity scales with zoom.
            // If you are far away, mouse movement moves the camera more.
            float sensitivity = zoom / 500f;

            // Shift key multiplier for "Fast Pan"
            if (hasShiftDown()) {
                sensitivity *= 4.0f;
            }

            camX += (float) (dragX * sensitivity);
            camY += (float) (dragY * sensitivity); // dragY is positive downward
        }
        return true;
    }

    // i will use some stuff from the skyrenderer here and also reuse the skybox shaders
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();

        guiGraphics.fill(0, 0, windowWidth, windowHeight, 0xff000000);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 2. VIEW MATRIX (Camera)
        Matrix4f viewMatrix = new Matrix4f().lookAt(
                new Vector3f(camX, zoom, camY),
                new Vector3f(camX, 0, camY),
                new Vector3f(0, 0, 1)
        );

        // 3. PROJECTION MATRIX
        Matrix4f projMatrix = new Matrix4f();
        float fov = (float) Math.toRadians(60.0f); // 60 is usually better for maps than 90
        float aspect = (float) windowWidth / windowHeight;
        float near = 0.1f;
        float far = 100000f;
        projMatrix.setPerspective(fov, aspect, near, far);

        SkyRenderer.adjustRenderTargetSize(SkyRenderer.PlanetsTarget, windowWidth, windowHeight, 1f); // TODO: can we use 1 again? this is not good for rendering close up planet with many fragments
        SkyRenderer.adjustRenderTargetSize(SkyRenderer.AtmosphereTarget, windowWidth, windowHeight, 0.25f);

        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 1f);

        SkyRenderer.PlanetsTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);

        for (ResourceLocation dimId : DimensionManager.INSTANCE_CLIENT.dimensions.keySet()) {
            Dimension dim = DimensionManager.INSTANCE_CLIENT.get(dimId);
            if (!dim.getType().equals(DimensionProperties.DimensionType.PLANET))
                continue;
            PlanetDimension planet = (PlanetDimension) dim;
            //Vec3 pos = planet.getPosition(partialTick);
            Vec3 pos = getPositionScaled(planet, partialTick);

            Matrix4f planetMatrix = new Matrix4f();
            planetMatrix.translate((float) pos.x * 2000, (float) pos.y * 2000, (float) (pos.z * 2000));

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

            float renderScale = (float) Math.pow(planet.getEarthRadiusMultiplier(), 1-(logScale*0.95+0.05)) * (1+(this.scale*100));

            planetMatrix.scale(renderScale);


            ShaderInstance shader;
            RenderSystem.setShader(shaderUtils::getPlanetShader);
            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            texturemanager.getTexture(planet.getTexture()).setFilter(true, true);
            RenderSystem.setShaderTexture(0, planet.getTexture());
            shader = RenderSystem.getShader();

            shader.getUniform("ProjMat").set(projMatrix);
            shader.getUniform("ViewMat").set(viewMatrix);  // Pass the LookAt matrix here!
            shader.getUniform("WorldMat").set(new Matrix4f());
            shader.getUniform("ModelMat").set(planetMatrix);

            Vector3f emissiveColor = RenderUtils.gamma_reverse(planet.getEmissiveColor());
            shader.getUniform("emissiveColor").set(new Vector4f(emissiveColor.x, emissiveColor.y, emissiveColor.z, planet.getRadiationIntensity()));

            shader.getUniform("AtmDensity").set(0f);

            shader.getUniform("LocalSunriseColor").set(new Vector3f(0, 0, 0));

            shader.getUniform("TargetAtmDensity").set(planet.getAtmosphereDensity());

            Vector3f TargetSkyColor = RenderUtils.gamma_reverse(planet.getSkyColor());
            shader.getUniform("TargetSkyColor").set(TargetSkyColor);

            Vector3f TargetReflectiveTextureTintColor = RenderUtils.gamma_reverse(planet.getReflectiveTextureTintColor());
            shader.getUniform("TargetReflectiveTextureTintColor").set(TargetReflectiveTextureTintColor);

            shader.getUniform("playerHeight").set(0f);

            shader.getUniform("planetSkyHeight").set(1f);

            shader.getUniform("localTerrainFogColor").set(new Vector3f(0, 0, 0));

            int totalLights = 0;
            for (ResourceLocation lightSourceId : planet.getCurrentMainStars()) {
                Dimension star = DimensionManager.INSTANCE_CLIENT.get(lightSourceId);
                Vec3 StarPos = star.getPosition(partialTick);
                Vec3 LightVector = pos.subtract(StarPos).scale(-1); //shader uses planet to star for dot product
                shader.getUniform("LightVectors[" + totalLights + "]").set((float) LightVector.x, (float) LightVector.y, (float) LightVector.z);
                Vector3f lightColor = RenderUtils.gamma_reverse(star.getEmissiveColor());
                shader.getUniform("LightColors[" + totalLights + "]").set(lightColor.x, lightColor.y, lightColor.z, star.getRadiationIntensity());
                totalLights += 1;
            }
            shader.getUniform("LightCount").set(totalLights);

            shader.getUniform("isLocalPlanet").set(0);

            shader.apply();
            SkyRenderer.vertexBufferPlanet.bind();
            SkyRenderer.vertexBufferPlanet.draw();
            shader.clear();
            VertexBuffer.unbind();
        }

        // this one is only required for the blit shader later bc i dont want to write another shader
        SkyRenderer.AtmosphereTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);


        ShaderInstance shader;

        // post processing

        NO_DEPTH_TEST.setupRenderState();

        SkyRenderer.vertexBufferSquare.bind();

        float bloomWindowSizeMultiplier = 1f;
        SkyRenderer.adjustRenderTargetSize(SkyRenderer.bloomBlurTarget1, 480, 270, bloomWindowSizeMultiplier);
        SkyRenderer.adjustRenderTargetSize(SkyRenderer.bloomBlurTarget2, 480, 270, bloomWindowSizeMultiplier);
        SkyRenderer.adjustRenderTargetSize(SkyRenderer.bloomBrightTarget, 480, 270, bloomWindowSizeMultiplier);

        // blit extract bright regions
        SkyRenderer.bloomBrightTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        RenderSystem.setShader(shaderUtils::getBlitExtractBrightShader);
        shader = RenderSystem.getShader();
        shader.setSampler("frame", SkyRenderer.PlanetsTarget.getColorTextureId());
        shader.getUniform("threshold").set(1f);
        shader.apply();
        SkyRenderer.vertexBufferSquare.draw();
        shader.clear();

        // blit blur
        RenderSystem.setShader(shaderUtils::getBlitBlurShader);
        shader = RenderSystem.getShader();

        SkyRenderer.bloomBlurTarget1.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        shader.setSampler("image", SkyRenderer.bloomBrightTarget.getColorTextureId());
        shader.getUniform("resolution").set(SkyRenderer.bloomBrightTarget.width);
        shader.getUniform("horizontal").set(1);
        shader.apply();
        SkyRenderer.vertexBufferSquare.draw();
        shader.clear();

        SkyRenderer.bloomBlurTarget2.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        shader.setSampler("image", SkyRenderer.bloomBlurTarget1.getColorTextureId());
        shader.getUniform("resolution").set(SkyRenderer.bloomBlurTarget1.height);
        shader.getUniform("horizontal").set(0);
        shader.apply();
        SkyRenderer.vertexBufferSquare.draw();
        shader.clear();


        // Switch back to main render target, combine framebuffers
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        RenderSystem.setShader(shaderUtils::getBlitAddTonemapShader);
        shader = RenderSystem.getShader();
        shader.setSampler("SpaceBackground", SkyRenderer.PlanetsTarget.getColorTextureId());
        shader.setSampler("SpaceBackgroundBloom", SkyRenderer.bloomBlurTarget2.getColorTextureId());
        shader.setSampler("Atmosphere", SkyRenderer.AtmosphereTarget.getColorTextureId());
        shader.getUniform("bloomIntensity").set(1f);
        shader.apply();
        SkyRenderer.vertexBufferSquare.draw();
        shader.clear();


        VertexBuffer.unbind();
        NO_DEPTH_TEST.clearRenderState();

        // Clear depth buffer for subsequent rendering
        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);
    }

    //applies a scale to orbit distance for better rendering on map
    public Vec3 getPositionScaled(PlanetDimension planet, float partialTick) {
        if (planet.getParentDimensionId() != null) {
            PlanetDimension parent = (PlanetDimension) DimensionManager.INSTANCE_CLIENT.get(planet.getParentDimensionId());
            double ticksPerOrbit = CelestialUtils.calculateOrbitalPeriodTicks(fromEarthMasses(planet.getGravitationalMultiplier()), fromEarthMasses(parent.getGravitationalMultiplier()), fromAU(planet.getorbitalDistanceToParent()));
            double orbitalProgress = (GlobalTime.getGlobalTime() % ticksPerOrbit) + (GlobalTime.getGlobalTimeClientCorrection() % ticksPerOrbit);
            double orbitAngleDegrees = orbitalProgress * (360.0 / ticksPerOrbit) + planet.getorbitalBaseOffsetDegrees();

            // 1. Define a simple, non-zero vector to use for the cross-product
            // This is an arbitrary direction, often chosen to align with a major axis.
            Vec3 arbitraryVector = new Vec3(0, 0, 1); // e.g., the Z-axis

            // 2. Find a starting vector orthogonal to the orbitAxis
            Vec3 startDirection = planet.getOrbitAxis().cross(arbitraryVector);

            // 3. Handle the edge case where orbitAxis is parallel to arbitraryVector (e.g., orbitAxis is <0,0,1>)
            // If the cross-product is zero length, orbitAxis and arbitraryVector are parallel.
            if (startDirection.length() < 0.0001d) {
                // Fallback: cross with a different axis (e.g., the X-axis)
                arbitraryVector = new Vec3(1, 0, 0);
                startDirection = planet.getOrbitAxis().cross(arbitraryVector);
            }

            // 4. Normalize the orthogonal vector and scale it to the orbital distance
            // This is your correct 'baseOffset' vector, originating at the parent and orthogonal to the rotation axis.
            float orbitDistance = planet.getorbitalDistanceToParent();
            Vec3 baseOffset = startDirection.normalize().scale(Math.pow(orbitDistance, 0.5));

            // 5. Rotate the baseOffset around the orbitAxis by the current angle
            // baseOffset is now the vector V_start, and orbitAxis is the vector A.
            Vec3 rotatedOffset = CelestialUtils.rotate(baseOffset, planet.getOrbitAxis(), orbitAngleDegrees);

            // 6. Add parent's position to get global position
            return getPositionScaled(parent, partialTick).add(rotatedOffset);
        }
        return planet.getPosition(partialTick);
    }
}
