package advRocketry.Render.starmap;

import ARLib.utils.VertexBufferCleaner;
import advRocketry.Data.DataTypes;
import advRocketry.Dimension.*;
import advRocketry.GlobalTime;
import advRocketry.Items.ItemGalaxyDatabase;
import advRocketry.Render.SkyRenderer;
import advRocketry.Render.shaderUtils;
import advRocketry.Utils.CelestialUtils;
import advRocketry.Utils.ClientUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.lwjgl.opengl.GL30;

import java.lang.Math;

import static advRocketry.Utils.CelestialUtils.fromAU;
import static advRocketry.Utils.CelestialUtils.fromEarthMasses;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class SpaceMapScreen extends Screen {
    private final int SIDEBAR_WIDTH = 180;

    private PlanetDimension selectedPlanet = null;

    private net.minecraft.client.gui.components.Button actionButton;

    private float camX = 0;
    private float camY = 0;
    private float zoom = 1000f;
    private float rotY = 0;
    private float logScale = 0.5f;
    private float scale = 0.3f;
    private float sidebarScrollAmount = 0;
    private int lastMaxScroll = 0; // To clamp scrolling

    private String planetInfoText = "";

    private VertexBuffer vertexBufferOrbitCircle = null;

    public SpaceMapScreen() {
        super(Component.literal("space map"));
    }

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

            planetInfoText = getPlanetInfoText(selectedPlanet.getDimensionId(), null);
            if (planetInfoText == null)
                planetInfoText = "";

        } else {
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

    public String getPlanetInfoText(ResourceLocation dimensionId, ItemGalaxyDatabase.PlanetInfo planetInfo) {
        PlanetDimension planet = ((PlanetDimension) DimensionManager.INSTANCE_CLIENT.get(dimensionId));
        if (planet == null) return "";
        String description = "";
        int distance = 0;
        int mass = 0;
        int composition = 0;
        if (planetInfo != null) {
            distance = planetInfo.get(DataTypes.distance);
            mass = planetInfo.get(DataTypes.mass);
            composition = planetInfo.get(DataTypes.composition);
        }
        int dataMax = ItemGalaxyDatabase.POINTS_UNLOCKED(planet);
        description += "distance:    " + distance + " / " + dataMax + "\n";
        description += "mass:        " + mass + " / " + dataMax + "\n";
        description += "composition: " + composition + " / " + dataMax + "\n";
        description += "\n";

        if (ClientUtils.getPlayerDimension() != null && distance >= dataMax) {
            double distanceAu = ClientUtils.getPlayerDimension().getPosition(0).distanceTo(planet.getPosition(0));
            description += "Distance: " + String.format("%.2f", distanceAu) + " AU\n\n";
        }
        if (mass >= dataMax) {
            description += "Mass: " + String.format("%.2f", planet.getGravitationalMultiplier()) + "g\n\n";
        }
        if (composition >= dataMax) {
            description += "Composition:\n(atm, surface, underground)\n";
            for (String gas : GasRegistry.gases.keySet()) {
                description += gas + ": " + String.format("%.2f", planet.getGasProperty(gas).in_atm) + ", " + String.format("%.2f", planet.getGasProperty(gas).frozen_surface) + ", " + String.format("%.2f", planet.getGasProperty(gas).frozen_deep_below_surface) + "\n";
            }
            description += "\n";
        }
        if (composition >= dataMax && mass >= dataMax) {
            if (!planet.getGasMiningOptions().isEmpty())
                description += "Gas mining possible!\n\n";
        }
        if (mass >= dataMax) {
            description += "Can visit: " + planet.canVisit() + "\n\n";
        }
        if (mass >= dataMax && composition >= dataMax) {
            if (planet.canVisit())
                description += "Can breath: " + planet.canBreathe() + "\n\n";

            description += "Temperature: " + String.format("%.2f", planet.getCurrentTemp()) + "\n\n";

            if (planet.canVisit()) {
                description += "Sea level: " + planet.getSeaLevel() + "\n";
                description += "Humidity: " + String.format("%.2f", planet.getHumidity()) + "\n";
                description += "Liquid water possible: " + (planet.warmEnoughForWater() && planet.getCurrentTemp() < 373) + "\n\n";
            }


            // composition analysis

            if (planet.getFrozenGasCoverage() > 0.3 && planet.getFrozenGasCoverage() < 0.6) {
                description += "Surface partially covered in ice, reducing energy gain.\n\n";
            }
            if (planet.getFrozenGasCoverage() >= 0.6) {
                description += "Surface mostly covered in ice, significantly reducing energy gain.\n\n";
            }

            if (planet.getHumidity() > 0.3) {
                if (planet.getSeaLevel() < 45)
                    description += "Extreme heat has forced most water into the atmosphere.\n\n";

                description += "Humidity contributes to greenhouse effect.\n\n";
            }

            if (planet.getSeaLevel() > 45 && planet.warmEnoughForWater())
                description += "A healthy sea level keeps co2 levels low.\n\n";

            if(planet.getSeaLevel() > 0 && planet.getCurrentTemp() > 375){
                description += "The planet is too hot! Water slowly boils away.\n\n";
            }

            if (planet.getGasProperty(GasRegistry.co2).in_atm > 0.1) {
                description += "Lots of CO2 in atmosphere increases greenhouse effect.\n\n";
            }
            if (planet.getGasProperty(GasRegistry.methane).in_atm > 0.01) {
                description += "Lots of Methane in atmosphere increases greenhouse effect.\n\n";
            }

            if (planet.getGasProperty(GasRegistry.co2).in_atm > 0 &&
                    planet.getCurrentTemp() < GasRegistry.gases.get(GasRegistry.co2).freezingTemp) {
                description += "CO2 is freezing and snowing to the surface, significantly reducing future temperature.\n\n";
            }
            if ((planet.getGasProperty(GasRegistry.co2).frozen_surface > 0 || planet.getGasProperty(GasRegistry.co2).frozen_deep_below_surface > 0) &&
                    planet.getCurrentTemp() > GasRegistry.gases.get(GasRegistry.co2).sublimationTemp) {
                description += "Frozen CO2 is quickly evaporating, significantly increasing future temperature.\n\n";
            }

        }

        return description;
    }

    public boolean shouldRenderPlanet(ResourceLocation dimensionId) {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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

        // Check if mouse is over the sidebar
        if (selectedPlanet != null && mouseX > this.width - SIDEBAR_WIDTH) {
            // scrollY is positive for scrolling up, negative for down
            // We subtract it because we want the text to move UP when we scroll DOWN
            sidebarScrollAmount -= (float) (scrollY * 20);
            sidebarScrollAmount = Math.max(0, Math.min(sidebarScrollAmount, lastMaxScroll));
            return true;
        }

        // scrollY is the vertical scroll amount.
        // We use an exponential zoom so it feels smooth at all distances.
        float zoomSpeed = zoom * 0.3f;
        zoom -= (float) (scrollY * zoomSpeed);

        // Clamp zoom so we don't go past the planets or infinitely far away
        zoom = Math.max(1f, Math.min(zoom, 2000000f));

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
            float sensitivity = zoom / 310f;

            // Shift key multiplier for "Fast Pan"
            if (hasShiftDown()) {
                sensitivity *= 4.0f;
            }

            camX += (float) (dragX * sensitivity);
            camY += (float) (dragY * sensitivity); // dragY is positive downward
        }
        if (button == 1) {
            rotY += (float) dragY / 100;
            rotY = Math.clamp(rotY, -2.1f, 0f);
        }
        return true;
    }

    private Matrix4f viewMat() {
        // 1. Target is fixed to the map center
        Vector3f target = new Vector3f(camX, 0, camY);

        // 2. Camera position (Eye)
        // We keep X/Z at the target, only Y changes based on pitch
        // 'zoom' acts as the radius of our arc
        float eyeY = (float) (zoom * Math.cos(rotY));
        float offset = (float) (zoom * Math.sin(rotY));

        // We offset the eye on the Y-axis to create the tilt
        Vector3f eye = new Vector3f(camX, eyeY, camY + offset);

        // 3. Up vector
        // Since we are only tilting, we keep the Up vector consistent.
        // If you are looking down, Up is Z. If you are horizontal, Up is Y.
        // A safe middle ground for a top-down-to-side view is:
        Vector3f up = new Vector3f(0, 1, 1);

        return new Matrix4f().lookAt(eye, target, up);
    }

    private Matrix4f projMat() {
        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();
        Matrix4f projMatrix = new Matrix4f();
        float fov = (float) Math.toRadians(45.0f); // less stretching
        float aspect = (float) windowWidth / windowHeight;
        float near = 0.1f;
        float far = zoom * 2;
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
            if (!shouldRenderPlanet(planet.getDimensionId()))
                continue;

            float pTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            Vector3f planetWorldPos = getPlanetTranslation(planet, pTicks);
            float renderScale = getPlanetRenderScale(planet);

            // 4. Pass RAW pixels and RAW window size to the check
            if (isHoveringPlanet(rawMouseX, rawMouseY, windowWidth, windowHeight, planetWorldPos, renderScale, viewMatrix, projMatrix)) {
                selectedPlanet = planet;
                sidebarScrollAmount = 0;
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
        Vector4f near = new Vector4f(x, y, -1.0f, 1.0f);
        Vector4f far = new Vector4f(x, y, 1.0f, 1.0f);

        invVP.transform(near);
        invVP.transform(far);

        near.div(near.w);
        far.div(far.w);

        Vector3d rayOrigin = new Vector3d(near.x, near.y, near.z);
        Vector3d rayDir = new Vector3d(far.x - near.x, far.y - near.y, far.z - near.z).normalize();

        // 3. Ray-Sphere Intersection
        Vector3d oc = new Vector3d(rayOrigin).sub(planetPos);
        double b = oc.dot(rayDir);
        double c = oc.dot(oc) - radius * radius;
        double discriminant = b * b - c;

        // If discriminant < 0, the ray missed entirely.
        return (discriminant > 0);
    }

    // i render the map as background so the buttons and sliders and whatever are on top correctly
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();

        if (windowHeight * windowHeight < 1000)
            return;

        guiGraphics.fill(0, 0, width, height, 0xff000000);

        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);

        // 2. VIEW MATRIX (Camera)
        Matrix4f viewMatrix = viewMat();

        // 3. PROJECTION MATRIX
        Matrix4f projMatrix = projMat();

        SkyRenderer.adjustRenderTargetSize(SkyRenderer.PlanetsTarget, windowWidth, windowHeight, 1f);
        SkyRenderer.adjustRenderTargetSize(SkyRenderer.AtmosphereTarget, windowWidth, windowHeight, 0.25f);

        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 1f);

        SkyRenderer.PlanetsTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);

        ShaderInstance shader;

        // render star background first
        RenderSystem.setShader(shaderUtils::getstarBackgroundShader);
        shader = RenderSystem.getShader();
        shader.getUniform("ViewMat").set(new Matrix4f());
        shader.getUniform("WorldMat").set(new Matrix4f());
        shader.getUniform("ModelMat").set(new Matrix4f());
        shader.getUniform("ProjMat").set(new Matrix4f().setPerspective(90F, (float) (windowWidth / windowHeight), 10F, 1000000F));
        shader.apply();
        SkyRenderer.vertexBufferStarBackground.bind();
        SkyRenderer.vertexBufferStarBackground.draw();
        shader.clear();
        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);

        // for the orbit lines
        if (vertexBufferOrbitCircle == null) {
            vertexBufferOrbitCircle = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            VertexBufferCleaner.register(this, vertexBufferOrbitCircle);
        }

        LEQUAL_DEPTH_TEST.setupRenderState();

        for (PlanetDimension planet : SpaceMapPlanetRenderCache.INSTANCE.getPlanetsToRenderInSky()) {
            if (!shouldRenderPlanet(planet.getDimensionId()))
                continue;

            Matrix4f planetMatrix = new Matrix4f();

            Vector3f pos = getPlanetTranslation(planet, partialTick);
            planetMatrix.translate(pos.x, pos.y, pos.z);

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

            float renderScale = getPlanetRenderScale(planet);
            planetMatrix.scale(renderScale);

            // render the orbit lines
            float colorModulator = 0.03f * Math.clamp(1.3f + rotY, 0, 1);
            if (planet.getParentDimensionId() != null && colorModulator > 0) {
                Vector3f parentPosition = getPlanetTranslation(DimensionManager.INSTANCE_CLIENT.get(planet.getParentDimensionId()), partialTick);
                Vector3f parentToPlanet = new Vector3f(pos).sub(parentPosition);

                ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(1024);
                BufferBuilder builder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
                int segments = 100;
                for (int i = 0; i <= segments; i++) {
                    double angleDegrees = (i * 360.0) / segments;
                    Vec3 rotatedOffset = CelestialUtils.rotate(new Vec3(parentToPlanet), planet.getOrbitAxis().normalize(), angleDegrees);

                    float x = (float) rotatedOffset.x;
                    float y = (float) rotatedOffset.y;
                    float z = (float) rotatedOffset.z;

                    builder.addVertex(x, y, z).setColor(1.0f * colorModulator, 1.0f * colorModulator, 1.0f * colorModulator, 1f);
                }
                MeshData mesh = builder.build();
                vertexBufferOrbitCircle.bind();
                vertexBufferOrbitCircle.upload(mesh);
                byteBufferBuilder.close();

                Matrix4f parentMatrix = new Matrix4f();
                parentMatrix.translate(parentPosition);
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                shader = RenderSystem.getShader();
                shader.setDefaultUniforms(
                        VertexFormat.Mode.DEBUG_LINE_STRIP,
                        new Matrix4f(viewMatrix).mul(parentMatrix),
                        projMatrix,
                        Minecraft.getInstance().getWindow()
                );
                shader.apply();
                vertexBufferOrbitCircle.draw();
                shader.clear();
            }

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

            // The planets render depth sorted
            // but it looks strange when you rotate it
            // i choose to clear only when we view top down because this is how it is depth sorted
            if (rotY > -0.2)
                RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);

        }

        LEQUAL_DEPTH_TEST.clearRenderState();

        // this one is only required for the blit shader later bc i dont want to write another shader
        SkyRenderer.AtmosphereTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);

        // post processing

        GlStateManager._depthMask(false);

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

        GlStateManager._depthMask(true);

        VertexBuffer.unbind();

        // Clear depth buffer for subsequent rendering
        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);


        if (selectedPlanet != null) {
            int xStart = this.width - SIDEBAR_WIDTH;
            int xText = xStart + 10;
            int yTop = 30; // Start below the title
            int yBottom = this.height - 40; // End above the action button
            int viewHeight = yBottom - yTop;

            // 1. Draw Sidebar Background & Border
            guiGraphics.fill(xStart, 0, this.width, this.height, 0xAA000000);
            guiGraphics.vLine(xStart, 0, this.height, 0xFFFFFFFF);

            // 2. Draw Title (Fixed at top)
            guiGraphics.drawString(this.font, selectedPlanet.getName(), xText, 10, 0xFFFFFF);

            // 3. Prepare Scissoring for Scrollable Text
            // enableScissor(x1, y1, x2, y2)
            guiGraphics.enableScissor(xStart, yTop, this.width, yBottom);

            guiGraphics.pose().pushPose();
            // Move the text up based on scroll amount
            guiGraphics.pose().translate(0, -sidebarScrollAmount, 0);

            // Draw the text
            // We need to calculate how tall this text is to clamp the scroll
            int textHeight = this.font.wordWrapHeight(planetInfoText, SIDEBAR_WIDTH - 20);
            guiGraphics.drawWordWrap(this.font, Component.literal(planetInfoText), xText, yTop, SIDEBAR_WIDTH - 20, 0xCCCCCC);

            // Update max scroll: total height minus what's visible
            this.lastMaxScroll = Math.max(0, textHeight - viewHeight);

            guiGraphics.pose().popPose();
            guiGraphics.disableScissor();

            // 4. Draw Scrollbar Track (Optional but helpful for UX)
            if (lastMaxScroll > 0) {
                int sbWidth = 2;
                int sbX = this.width - 4;
                float scrollPercentage = sidebarScrollAmount / (float) lastMaxScroll;
                int barHeight = (int) ((viewHeight / (float) textHeight) * viewHeight);
                int barPos = (int) (yTop + (scrollPercentage * (viewHeight - barHeight)));
                guiGraphics.fill(sbX, barPos, sbX + sbWidth, barPos + barHeight, 0xAAFFFFFF);
            }

            // 5. Ensure Action Button is positioned correctly
            // The button is added via init(), so we just make sure its position is fixed
            this.actionButton.setY(this.height - 30);
        }
    }

    // i will use some stuff from the skyrenderer here and also reuse the skybox shaders
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public Vector3f getPlanetTranslation(Dimension planet, float pTicks) {
        Vec3 pos = getPositionScaled(planet, pTicks);
        return new Vector3f((float) pos.x * 100, (float) pos.y * 100, (float) pos.z * 100);
    }

    public float getPlanetRenderScale(PlanetDimension planet) {
        float renderScale = (float) Math.pow(planet.getEarthRadiusMultiplier(), 1 - (logScale * 0.95 + 0.05)) * (1 + (this.scale * 100)) / 20;
        if (planet.isStar())
            renderScale *= Math.max(1, zoom / 1000); // make larger on high zoom to keep stars visible
        return renderScale;
    }

    //applies a scale to orbit distance for better rendering on map
    public Vec3 getPositionScaled(Dimension dimension, float partialTick) {
        /// we !need! this or small orbits like moon are way too small
        if (dimension instanceof PlanetDimension planet) {
            if (planet.getParentDimensionId() != null) {
                Dimension parent = DimensionManager.INSTANCE_CLIENT.get(planet.getParentDimensionId());
                if (parent != null) {
                    double ticksPerOrbit = CelestialUtils.calculateOrbitalPeriodTicks(fromEarthMasses(planet.getGravitationalMultiplier()), fromEarthMasses(parent.getGravitationalMultiplier()), fromAU(planet.getOrbitalDistanceToParent()));
                    double orbitalProgress = (GlobalTime.getGlobalTime() % ticksPerOrbit) + (GlobalTime.getGlobalTimeClientCorrection() % ticksPerOrbit);
                    double orbitAngleDegrees = orbitalProgress * (360.0 / ticksPerOrbit) + planet.getOrbitalBaseOffsetDegrees();

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
                    float orbitDistance = planet.getOrbitalDistanceToParent();
                    Vec3 baseOffset = startDirection.normalize().scale(Math.pow(orbitDistance, 0.5));

                    // 5. Rotate the baseOffset around the orbitAxis by the current angle
                    // baseOffset is now the vector V_start, and orbitAxis is the vector A.
                    Vec3 rotatedOffset = CelestialUtils.rotate(baseOffset, planet.getOrbitAxis(), orbitAngleDegrees);

                    // 6. Add parent's position to get global position
                    return getPositionScaled(parent, partialTick).add(rotatedOffset);
                }
            }
        }
        return dimension.getPosition(partialTick);
    }
}
