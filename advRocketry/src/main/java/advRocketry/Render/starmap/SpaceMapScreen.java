package advRocketry.Render.starmap;

import ARLib.utils.VertexBufferCleaner;
import advRocketry.Data.DataTypes;
import advRocketry.Dimension.*;
import advRocketry.GlobalTime;
import advRocketry.Items.ItemGalaxyDatabase;
import advRocketry.Registry.GasRegistry;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.lwjgl.opengl.GL30;

import java.lang.Math;
import java.util.Objects;
import java.util.Set;

import static advRocketry.Utils.CelestialUtils.fromAU;
import static advRocketry.Utils.CelestialUtils.fromEarthMasses;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class SpaceMapScreen extends Screen {
    private final int SIDEBAR_WIDTH = 200;

    private PlanetDimension selectedPlanet = null;
    private net.minecraft.client.gui.components.Button actionButton;
    private float camX = 0;
    private float camY = 0;
    private float zoom = 100f;
    private float rotY = 0;
    private float logScale = 0.6f;
    private float scale = 0.25f;
    private float sidebarScrollAmount = 0;
    private int lastMaxScroll = 0; // To clamp scrolling
    private String planetInfoText = "";
    private Vector3f eyePos = new Vector3f(0, 0, 0);

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

    public String getPlanetInfoText(ResourceLocation dimensionId, ItemStack database) {
        PlanetDimension planet = ((PlanetDimension) DimensionManager.INSTANCE_CLIENT.get(dimensionId));
        if (planet == null) return "";
        String description = "";
        int distance = 0;
        int mass = 0;
        int composition = 0;
        ItemGalaxyDatabase.PlanetInfo planetInfo = ItemGalaxyDatabase.getPlanetInfo(database, dimensionId);
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
            description += String.format("Composition:\n");

            for (String gas : GasRegistry.gases.keySet()) {
                PlanetDimensionProperties.GasProperty prop = planet.getGasProperty(gas);
                if (prop.in_atm > 0)
                    description += "g  " + String.format("%-10s", gas) + String.format("%.5f", prop.in_atm) + "\n";
                if (prop.liquid > 0)
                    description += "l   " + String.format("%-10s", gas) + String.format("%.5f", prop.liquid) + "\n";
                if (prop.frozen_surface > 0)
                    description += "s  " + String.format("%-10s", gas) + String.format("%.5f", prop.frozen_surface) + "\n";
                if (prop.frozen_deep_below_surface > 0)
                    description += "sg " + String.format("%-10s", gas) + String.format("%.5f", prop.frozen_deep_below_surface) + "\n";
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
            if (planet.canVisit()) {
                Set<Dimension.SurvivalProblem> problems = planet.getSurvivalProblems();
                if (problems.isEmpty()) {
                    description += "Survival Possible\n";
                } else {
                    description += "Survival problems: \n";
                    for (Dimension.SurvivalProblem p : problems) {
                        description += p.reason + "\n";
                    }
                }
                description += "\n";
            }

            description += "Temperature: " + String.format("%.2f", planet.getCurrentTemp()) + "\n\n";

            if (planet.canVisit()) {
                description += "Water level: " + String.format("%.2f", planet.getGasProperty(GasRegistry.water).getSeaLevel()) + "\n";
                description += "Ocean Fraction: " + String.format("%.2f", planet.getOceanFraction(null)) + "\n";
                description += "Clouds: " + String.format("%.2f", planet.computeCloudValue()) + "\n";
                description += "Humidity: " + String.format("%.2f", planet.getHumidity(GasRegistry.water)) + "\n";
                GasRegistry.Gas water = GasRegistry.gases.get(GasRegistry.water);
                description += "Liquid water possible: " + (planet.getCurrentTemp() < water.getBoilingTemp(planet.getAtmosphereDensity()) && planet.getCurrentTemp() > water.getFreezeTemp(planet.getAtmosphereDensity())) + "\n\n";
            }
        }

        if (planet.getDescription() instanceof String d && !d.isEmpty())
            description += d + "\n\n";

        if (planet.isStar()) {
            // list planets orbiting the star
            int sats = 0;
            for (Dimension d : DimensionManager.INSTANCE_CLIENT.dimensions.values()) {
                // only consider planets
                if (d instanceof PlanetDimension otherPlanet) {
                    // check if parent dimension matches
                    if (Objects.equals(otherPlanet.getParentDimensionId(), planet.getDimensionId())) {
                        // check if sat is known by default or discovered
                        if (otherPlanet.isKnown() || ItemGalaxyDatabase.isDimensionKnown(database, otherPlanet.getDimensionId())) {
                            sats++;
                        }
                    }
                }
            }
            if(sats > 0)
                description += "Known planets: " + sats + "\n\n";
        }

        if (mass >= dataMax && composition >= dataMax) {
            // composition analysis

            description += "\nAnalysis:\n\n";

            double atmCo2 = planet.getGasProperty(GasRegistry.co2).in_atm;
            double atmMethane = planet.getGasProperty(GasRegistry.methane).in_atm;
            double atmWater = planet.getGasProperty(GasRegistry.water).in_atm;
            double humidity = planet.getHumidity(GasRegistry.water);
            double cloudValue = planet.computeCloudValue();
            double frozenGasCoverage = planet.getFrozenGasCoverage();
            double oceanFractionWater = planet.getOceanFraction(GasRegistry.water);

            if (frozenGasCoverage > 0.1 && frozenGasCoverage < 0.6) {
                description += "Surface partially covered in ice, reducing energy gain.\n\n";
            }
            if (frozenGasCoverage >= 0.6) {
                description += "Surface mostly covered in ice, significantly reducing energy gain.\n\n";
            }

            if (oceanFractionWater < 0.2 && humidity > 0.5)
                description += "Extreme heat has forced most water into the atmosphere.\n\n";

            if (humidity > 0.1)
                description += "Humidity contributes to greenhouse effect\n\n";
            if (cloudValue > 0.1)
                description += "Clouds reflect sunlight, reducing energy gain\n\n";

            double co2OceanReductionTargetPercent = PlanetEvents.handleOceanCo2Reduction(planet, true) * 100;
            if (co2OceanReductionTargetPercent > 0) {
                description += "A healthy sea level keeps co2 levels low (max " +
                        String.format("%.2f", co2OceanReductionTargetPercent) +
                        "%).\n\n";
            }

            if (PlanetEvents.handlePhotosynthesis(planet, true) > 0) {
                description += "Algae thrive under ideal temperatures, converting co2 into Oxygen.\n\n";
            }

            if (atmCo2 > 0.0002) {
                if (atmCo2 > 0.1)
                    description += "Lots of co2 in atmosphere significantly increases greenhouse effect.\n\n";
                else
                    description += "Co2 in the atmosphere increases greenhouse effect.\n\n";
            }
            if (atmMethane > 0.0001) {
                description += "Methane in the atmosphere significantly increases greenhouse effect.\n\n";
            }
            if (atmWater > 0.0001) {
                description += "Steam is a strong greenhouse gas.\n";
                if (planet.getCurrentTemp() < 350)
                    description += "Low atmosphere pressure turns water into steam at lower temperatures.\n";
                description += "\n";
            }

            if (planet.getAtmosphereDensity() < 0.1) {
                if (planet.getGasProperty(GasRegistry.co2).frozen_surface > 0 ||
                        planet.getOceanFraction(null) > 0.1
                )
                    description += "Railgun usage possible to extract fluids or ice.\n\n";
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
            sidebarScrollAmount -= (float) (scrollY * 20);
            sidebarScrollAmount = Math.max(0, Math.min(sidebarScrollAmount, lastMaxScroll));
            return true;
        }

        // 1. Save the old zoom before we change it
        float oldZoom = zoom;

        // 2. Apply zoom logic
        float scrollFactor = 1.5f;
        if (scrollY < 0) {
            zoom *= scrollFactor; // Zoom out
        } else if (scrollY > 0) {
            zoom /= scrollFactor; // Zoom in
        }

        // 3. Clamp zoom
        zoom = Math.max(0.01f, Math.min(zoom, 2000000f));

        // --- ZOOM TO MOUSE LOGIC ---
        // Find how far the mouse is from the exact center of the screen
        double dx = mouseX - (this.width / 2.0);
        double dy = mouseY - (this.height / 2.0);

        // We use the same '310f' sensitivity ratio from your mouseDragged method.
        // The camera shifts by the mouse offset multiplied by the difference in scale.
        float sensitivityDiff = (oldZoom - zoom) / 310f;

        camX -= (float) (dx * sensitivityDiff);
        camY -= (float) (dy * sensitivityDiff);

        return true;
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
        // 1. Target is fixed to the galaxy center
        // Planets will be translated to match cam view to avoid float precision errors
        Vector3f target = new Vector3f(0, 0, 0);

        // 2. Camera position (Eye)
        // We keep X/Z at the target, only Y changes based on pitch
        // 'zoom' acts as the radius of our arc
        float eyeY = (float) (zoom * Math.cos(rotY));
        float offset = (float) (zoom * Math.sin(rotY));

        // We offset the eye on the Y-axis to create the tilt
        Vector3f eye = new Vector3f(0, eyeY, 0 + offset);

        // 3. Up vector
        // Since we are only tilting, we keep the Up vector consistent.
        // If you are looking down, Up is Z. If you are horizontal, Up is Y.
        // A safe middle ground for a top-down-to-side view is:
        Vector3f up = new Vector3f(0, 1, 1);

        eyePos = eye;

        return new Matrix4f().lookAt(eye, target, up);
    }

    private Matrix4f projMat() {
        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();
        Matrix4f projMatrix = new Matrix4f();
        float fov = (float) Math.toRadians(45.0f); // less stretching
        float aspect = (float) windowWidth / windowHeight;
        float near = (zoom + 1) * 0.001f;
        float far = (zoom + 1) * 1000;
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

        if (getHoveredPlanet() instanceof PlanetDimension planetDimension) {
            selectedPlanet = planetDimension;
            sidebarScrollAmount = 0;
            return true;
        }

        selectedPlanet = null;
        return false;
    }

    public PlanetDimension getHoveredPlanet() {
        for (PlanetDimension planet : SpaceMapPlanetRenderCache.INSTANCE.getPlanetsToRenderInSky().reversed()) {

            // dont test for hidden planets
            if (!shouldRenderPlanet(planet.getDimensionId()))
                continue;

            float pTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            Vector3f planetWorldPos = getPlanetTranslation(planet, pTicks);
            float renderScale = getPlanetRenderScale(planet);

            if (tooSmallToRender(planetWorldPos, renderScale) && !planet.isStar())
                continue;

            double pixelPadding = 1;
            if(planet.isStar())
                // when zoomed far out on space map only stars are visible but they are very small
                pixelPadding = 20;
            if (isHoveringPlanet(planetWorldPos, renderScale, pixelPadding)) {
                return planet;
            }
        }
        return null;
    }

    // made by gemini
    private boolean isHoveringPlanet(Vector3f planetPos, float radius, double pixelPadding) {

        // 1. Get RAW pixel coordinates from Minecraft's MouseHandler
        double rawX = Minecraft.getInstance().mouseHandler.xpos();
        double rawY = Minecraft.getInstance().mouseHandler.ypos();

        // 2. Get RAW window dimensions
        int winW = Minecraft.getInstance().getWindow().getScreenWidth();
        int winH = Minecraft.getInstance().getWindow().getScreenHeight();

        // 3. Recreate matrices
        Matrix4f view = viewMat();
        Matrix4f proj = projMat();
        Matrix4f invVP = new Matrix4f(proj).mul(view).invert();

        // 4. Convert Raw Pixel to NDC (Original Mouse)
        float x = (float) (2.0f * rawX / winW - 1.0f);
        float y = (float) (1.0f - 2.0f * rawY / winH);

        // Calculate NDC for an offset mouse (e.g., 3 pixels to the right)
        float xOffset = (float) (2.0f * (rawX + pixelPadding) / winW - 1.0f);

        // 5. Unproject the Original Ray
        Vector4f near = new Vector4f(x, y, -1.0f, 1.0f);
        Vector4f far = new Vector4f(x, y, 1.0f, 1.0f);
        invVP.transform(near);
        invVP.transform(far);
        near.div(near.w);
        far.div(far.w);

        Vector3d rayOrigin = new Vector3d(near.x, near.y, near.z);
        Vector3d rayDir = new Vector3d(far.x - near.x, far.y - near.y, far.z - near.z).normalize();

        // 6. Unproject the Offset Ray (to figure out how big 3 pixels are in 3D)
        Vector4f nearOffset = new Vector4f(xOffset, y, -1.0f, 1.0f);
        Vector4f farOffset = new Vector4f(xOffset, y, 1.0f, 1.0f);
        invVP.transform(nearOffset);
        invVP.transform(farOffset);
        nearOffset.div(nearOffset.w);
        farOffset.div(farOffset.w);

        Vector3d rayOriginOffset = new Vector3d(nearOffset.x, nearOffset.y, nearOffset.z);
        Vector3d rayDirOffset = new Vector3d(farOffset.x - nearOffset.x, farOffset.y - nearOffset.y, farOffset.z - nearOffset.z).normalize();

        // 7. Calculate Dynamic Padding
        Vector3d oc = new Vector3d(rayOrigin).sub(planetPos);
        double distToPlanet = oc.length();

        // Calculate where both rays are at the specific depth of the planet
        Vector3d hitOriginal = new Vector3d(rayOrigin).add(new Vector3d(rayDir).mul(distToPlanet));
        Vector3d hitOffset = new Vector3d(rayOriginOffset).add(new Vector3d(rayDirOffset).mul(distToPlanet));

        // The distance between these two 3D points is exactly our pixel padding in world-space
        double worldPadding = hitOriginal.distance(hitOffset);
        double paddedRadius = radius + worldPadding;

        // 8. Ray-Sphere Intersection with Padded Radius
        double b = oc.dot(rayDir);
        double c = oc.dot(oc) - (paddedRadius * paddedRadius);
        double discriminant = b * b - c;

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
        shader.getUniform("BrightnessModifier").set(1f);
        shader.getUniform("WarpMovement").set(new Vector3f(0,0,0));
        shader.getUniform("ScreenSize").set(windowWidth, windowHeight);
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

            if (tooSmallToRender(pos, renderScale) && !planet.isStar()) {
                continue;
            }

            // render the orbit lines
            float colorModulator = 0.05f * Math.clamp(1.3f + rotY, 0, 1);
            colorModulator *= Math.clamp((1 - zoom / renderScale / 5000), 0, 1);
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

            // The planets render depth sorted
            // but it looks strange when you rotate it
            // i choose to clear only when we view top down
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

        if (mouseX < this.width - SIDEBAR_WIDTH || selectedPlanet == null) {
            if (getHoveredPlanet() instanceof PlanetDimension planet) {
                guiGraphics.renderTooltip(Minecraft.getInstance().font, Component.literal(planet.getName()), mouseX, mouseY);
            }
        }

        if (selectedPlanet != null) {
            int xStart = this.width - SIDEBAR_WIDTH;
            int xText = xStart + 10;
            int yTop = 30; // Start below the title
            int yBottom = this.height - 40; // End above the action button
            int viewHeight = yBottom - yTop;

            // 1. Draw Sidebar Background & Border
            guiGraphics.fill(xStart, 0, this.width, this.height, 0xDA000000);
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
        return getPositionScaled(planet, pTicks).toVector3f();
    }

    public boolean tooSmallToRender(Vector3f pos, float renderScale) {
        return renderScale / pos.length() < 0.001;
    }

    public float getPlanetRenderScale(PlanetDimension planet) {
        float renderScale = (float) Math.pow(planet.getEarthRadiusMultiplier(), 1 - (logScale * 0.95 + 0.05)) * (0.1f + (this.scale * 1f)) / 20;
        if (planet.isStar())
            renderScale *= Math.max(1, zoom / 50); // make larger on high zoom to keep stars visible
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
        Vec3 pos = dimension.getPosition(partialTick);
        // map requires y=0, also lower translation or shit will break due to fp precision errors
        return new Vec3(pos.x / 1000 - camX, 0, pos.z / 1000 - camY);
    }
}
