package advRocketry.Render.starmap;

import advRocketry.Config;
import advRocketry.Dimension.*;
import advRocketry.Main;
import advRocketry.Render.SkyRenderer;
import advRocketry.Render.shaderUtils;
import advRocketry.utils.CelestialUtils;
import advRocketry.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.security.auth.callback.Callback;
import java.lang.Math;
import java.util.ArrayList;

import static advRocketry.utils.CelestialUtils.fromAU;
import static advRocketry.utils.CelestialUtils.fromEarthMasses;
import static net.minecraft.client.renderer.RenderStateShard.*;

// TODO: DEPTH SORT for rendering and reverse depth sort for click check so we click top planet

public class SpaceMapScreen extends Screen {
    public SpaceMapScreen() {
        super(Component.literal("space map"));
    }

    private PlanetDimension selectedPlanet = null;
    private net.minecraft.client.gui.components.Button actionButton;
    private final int SIDEBAR_WIDTH = 120;

    private float camX = 0;
    private float camY = 0;
    private float zoom = 3000f;

    private float logScale = 0.5f;
    private float scale = 0.3f;

    private String planetInfoText = "";


    @Override
    public void tick() {
        super.tick();


        if (selectedPlanet != null) {
            String actionBtnText = getInteractText(selectedPlanet.getDimensionId());
            if (actionBtnText != null && !actionBtnText.isEmpty()) {
                actionButton.visible = true;
                actionButton.setMessage(Component.literal(actionBtnText));
            } else
                actionButton.visible = false;

            planetInfoText = getPlanetInfoText(selectedPlanet.getDimensionId());
            if(planetInfoText == null)
                planetInfoText = "";

        }else {
            actionButton.visible = false;
        }

        // depth sort the planets
        SpaceMapPlanetRenderCache.INSTANCE.updatePlanetsToRenderInSky(new Vec3(camX, zoom, camY));
    }

    public void interact(ResourceLocation dimensionId) {

    }

    public String getInteractText(ResourceLocation dimensionId) {
        return "interact";
    }

    public String getPlanetInfoText(ResourceLocation dimensionId) {
        return "This is planet info text...";
    }

    public boolean shouldRenderPlanet(ResourceLocation dimensionId) {
        return true;
    }

    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(new MapSlider(
                10, this.height - 20, 100, 10,
                Component.literal("scale"), scale,
                (newValue) -> {
                    this.scale = (float) newValue;
                }
        ));

        this.addRenderableWidget(new MapSlider(
                120, this.height - 20, 100, 10,
                Component.literal("logScale"), logScale,
                (newValue) -> {
                    this.logScale = (float) newValue;
                }
        ));

        this.actionButton = net.minecraft.client.gui.components.Button.builder(Component.literal("Write to Chip"), (btn) -> {
                    if (selectedPlanet != null) {
                        interact(selectedPlanet.getDimensionId());
                    }
                })
                .bounds(this.width - SIDEBAR_WIDTH + 10, this.height - 30, SIDEBAR_WIDTH - 20, 20)
                .build();

        this.addRenderableWidget(this.actionButton);
        this.actionButton.visible = false; // Hide until a planet is clicked
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

    private Matrix4f viewMat() {
        Matrix4f viewMatrix = new Matrix4f().lookAt(
                new Vector3f(camX, zoom, camY),
                new Vector3f(camX, 0, camY),
                new Vector3f(0, 0, 1)
        );
        return viewMatrix;
    }

    private Matrix4f projMat() {
        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();
        Matrix4f projMatrix = new Matrix4f();
        float fov = (float) Math.toRadians(60.0f); // 60 is usually better for maps than 90
        float aspect = (float) windowWidth / windowHeight;
        float near = 0.1f;
        float far = 100000f;
        projMatrix.setPerspective(fov, aspect, near, far);
        return projMatrix;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        // Don't select planets if clicking the sidebar UI
        if (mouseX > this.width - SIDEBAR_WIDTH && selectedPlanet != null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // 1. Get RAW pixel coordinates from Minecraft's MouseHandler
        // 'mouseX' from the method is scaled (e.g. 400), but we need pixels (e.g. 1920)
        double rawMouseX = Minecraft.getInstance().mouseHandler.xpos();
        double rawMouseY = Minecraft.getInstance().mouseHandler.ypos();

        // 2. Get RAW window dimensions
        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();

        // 3. Recreate matrices (Must match your render() exactly)
        Matrix4f viewMatrix = viewMat();
        Matrix4f projMatrix = projMat(); // Uses the same FOV and window aspect ratio

        for (PlanetDimension planet : SpaceMapPlanetRenderCache.INSTANCE.getPlanetsToRenderInSky().reversed()) {

            // dont test for hidden planets
            if(!shouldRenderPlanet(planet.getDimensionId()))
                continue;;

            float pTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            Vec3 pos = getPositionScaled(planet, pTicks);

            // MATCH YOUR RENDER TRANSLATION: (pos.x * 2000, pos.y * 2000, pos.z * 2000)
            Vector3f planetWorldPos = new Vector3f((float) pos.x * 2000, (float) pos.y * 2000, (float) pos.z * 2000);

            float renderScale = (float) Math.pow(planet.getEarthRadiusMultiplier(), 1 - (logScale * 0.95 + 0.05)) * (1 + (this.scale * 100));

            // 4. Pass RAW pixels and RAW window size to the check
            if (isHoveringPlanet(rawMouseX, rawMouseY, windowWidth, windowHeight, planetWorldPos, renderScale, viewMatrix, projMatrix)) {
                selectedPlanet = planet;
                return true;
            }
        }
        selectedPlanet = null;
        return false;
    }

    // made by gemini
    private boolean isHoveringPlanet(double rawX, double rawY, int winW, int winH, Vector3f planetPos, float radius, Matrix4f view, Matrix4f proj) {
        // 1. Convert Raw Pixel to NDC
        float x = (float) (2.0f * rawX / winW - 1.0f);
        float y = (float) (1.0f - 2.0f * rawY / winH);

        // 2. Unproject to find the Ray
        Matrix4f invVP = new Matrix4f(proj).mul(view).invert();

        // We shoot from the Near Plane to the Far Plane
        Vector4f near = new Vector4f(x, y, -1.0f, 1.0f).mul(invVP);
        Vector4f far = new Vector4f(x, y, 1.0f, 1.0f).mul(invVP);

        near.div(near.w);
        far.div(far.w);

        Vector3f rayOrigin = new Vector3f(near.x, near.y, near.z);
        Vector3f rayDir = new Vector3f(far.x - near.x, far.y - near.y, far.z - near.z).normalize();

        // 3. Ray-Sphere Intersection
        Vector3f oc = new Vector3f(rayOrigin).sub(planetPos);
        float b = oc.dot(rayDir);
        float c = oc.dot(oc) - radius * radius;
        float discriminant = b * b - c;

        // If discriminant < 0, the ray missed entirely.
        return (discriminant > 0);
    }

    // i render the map as background so the buttons and sliders and whatever are on top correctly
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();

        guiGraphics.fill(0, 0, width, height, 0xff000000);

        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);

        // 2. VIEW MATRIX (Camera)
        Matrix4f viewMatrix = viewMat();

        // 3. PROJECTION MATRIX
        Matrix4f projMatrix = projMat();

        SkyRenderer.adjustRenderTargetSize(SkyRenderer.PlanetsTarget, windowWidth, windowHeight, 1f); // TODO: can we use 1 again? this is not good for rendering close up planet with many fragments
        SkyRenderer.adjustRenderTargetSize(SkyRenderer.AtmosphereTarget, windowWidth, windowHeight, 0.25f);

        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 1f);

        SkyRenderer.PlanetsTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);

        for (PlanetDimension planet : SpaceMapPlanetRenderCache.INSTANCE.getPlanetsToRenderInSky()) {
            if (!shouldRenderPlanet(planet.getDimensionId()))
                continue;

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

            float renderScale = (float) Math.pow(planet.getEarthRadiusMultiplier(), 1 - (logScale * 0.95 + 0.05)) * (1 + (this.scale * 100));
            planetMatrix.scale(renderScale);


            // render the planet as if we observe it from space (0 atm density, no sky color...)
            SkyRenderer.renderPlanet(
                    planet,
                    projMatrix,
                    viewMatrix,
                    new Matrix4f(),
                    planetMatrix,
                    0,
                    new Vector3f(0, 0, 0),
                    new Vector3f(0, 0, 0),
                    0,
                    false,
                    partialTick
            );

            if (planet.hasRings()) {
                SkyRenderer.renderRingSystem(
                        planet,
                        projMatrix,
                        viewMatrix,
                        new Matrix4f(),
                        planetMatrix,
                        renderScale,
                        partialTick
                );
            }

            RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false); // we do manual depth sorting to avoid geometry mix

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

        if (selectedPlanet != null) {
            int xStart = this.width - SIDEBAR_WIDTH;

            // 1. Draw Background
            guiGraphics.fill(xStart, 0, this.width, this.height, 0xAA000000); // Semi-transparent black
            guiGraphics.vLine(xStart, 0, this.height, 0xFFFFFFFF); // White border line

            // 2. Draw Title
            guiGraphics.drawString(this.font, selectedPlanet.getDimensionId().getPath().toUpperCase(), xStart + 10, 10, 0xFFFFFF);

            // 3. Draw Description with Newlines/Wrapping
            String description = planetInfoText;

            // drawWordWrap handles the "\n" and automatically wraps text based on width
            guiGraphics.drawWordWrap(this.font, Component.literal(description), xStart + 10, 30, SIDEBAR_WIDTH - 20, 0xCCCCCC);
        }
    }

    // i will use some stuff from the skyrenderer here and also reuse the skybox shaders
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    //applies a scale to orbit distance for better rendering on map
    public Vec3 getPositionScaled(Dimension dimension, float partialTick) {
        if (dimension instanceof PlanetDimension planet) {
            if (planet.getParentDimensionId() != null) {
                Dimension parent = DimensionManager.INSTANCE_CLIENT.get(planet.getParentDimensionId());
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
        }
        return dimension.getPosition(partialTick);
    }
}
