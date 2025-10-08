package advRocketry;

import advRocketry.Dimension.BiomeConfig;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.GlobalTime;

import advRocketry.Render.Fog;
import advRocketry.Render.shaderUtils;
import advRocketry.Render.skyrenderer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Matrix4f;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Mod(Main.MODID)
public class Main {

    public static final String MODID = "adv_rocketry";

    public static Path worldPath;
    public static Path myConfigDir;

    public Main(IEventBus modEventBus, ModContainer modContaine) {
        //modEventBus.register(this);
        if (FMLLoader.getDist().isClient()) {
            NeoForge.EVENT_BUS.addListener(this::onRenderStage);
            NeoForge.EVENT_BUS.addListener(Fog::renderFogEvent);
            NeoForge.EVENT_BUS.addListener(Fog::computeFogColorEvent);
            NeoForge.EVENT_BUS.addListener(this::onCLientTick);
        }
        NeoForge.EVENT_BUS.addListener(this::onServerTick);

        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStop);

        modEventBus.addListener(this::loadShaders);


        Path configDir = FMLPaths.CONFIGDIR.get();
        myConfigDir = Path.of(String.valueOf(configDir), Main.MODID);
        File myConfigDirFile = new File(String.valueOf(myConfigDir));
        if (!myConfigDirFile.exists()) {
            myConfigDirFile.mkdirs();
        }

        BiomeConfig test = new BiomeConfig();
        BiomeConfig.BiomeDefinition x1 = new BiomeConfig.BiomeDefinition();
        x1.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "desert");
        x1.temperaturesList.addAll(List.of(BiomeConfig.temperature.FROZEN, BiomeConfig.temperature.LOW));
        x1.humidityList.addAll(List.of(BiomeConfig.humidity.DRY, BiomeConfig.humidity.VERY_DRY));
        x1.continentalnessList.addAll(List.of(BiomeConfig.continentalness.MID_INLAND));
        x1.erosionList.addAll(List.of(BiomeConfig.erosion.values()));
        test.biomes.add(x1);

        BiomeConfig.BiomeDefinition x2 = new BiomeConfig.BiomeDefinition();
        x2.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "ocean");
        x2.temperaturesList.addAll(List.of(BiomeConfig.temperature.values()));
        x2.humidityList.addAll(List.of(BiomeConfig.humidity.values()));
        x2.continentalnessList.addAll(List.of(BiomeConfig.continentalness.values()));
        x2.erosionList.addAll(List.of(BiomeConfig.erosion.values()));
        test.biomes.add(x2);

        BiomeConfig.BiomeDefinition x3 = new BiomeConfig.BiomeDefinition();
        x3.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "plains");
        x3.temperaturesList.addAll(List.of(BiomeConfig.temperature.values()));
        x3.humidityList.addAll(List.of(BiomeConfig.humidity.values()));
        x3.continentalnessList.addAll(List.of(BiomeConfig.continentalness.values()));
        x3.erosionList.addAll(List.of(BiomeConfig.erosion.values()));
        test.biomes.add(x3);

        String configStr = new GsonBuilder().setPrettyPrinting().create().toJson(test);

        try {
            Files.writeString(Path.of(Main.myConfigDir.toString(),"preset1.json"), configStr, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void onServerTick(ServerTickEvent.Post event) {
        DimensionManager.serverTick(event);
        GlobalTime.tickServer();
    }

    public void onCLientTick(ClientTickEvent.Post event) {
        DimensionManager.clientTick(event);
        GlobalTime.tickClient();
    }

    public void onServerStarted(ServerStartedEvent event) {
        Main.worldPath = event.getServer().getWorldPath(LevelResource.ROOT);
        System.out.println("set world path: " + worldPath);
        GlobalTime.load();
        DimensionManager.init();
    }

    public void onServerStop(ServerStoppingEvent event) {
        GlobalTime.save();
        DimensionManager.save();
    }

    private void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            //if(true)return;
            Matrix4f proj = event.getProjectionMatrix();
            Matrix4f view = event.getModelViewMatrix();
            skyrenderer.INSTANCE.renderSky(proj, view, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            // clouds will render next, disable stupid fog
            FogRenderer.setupFog(Minecraft.getInstance().gameRenderer.getMainCamera(), FogRenderer.FogMode.FOG_SKY, 999990, false, 0);
        }
    }

    private void loadShaders(RegisterShadersEvent event) {
        // 3. Register the shader and set the static field in the callback
        try {
            shaderUtils.localAtmosphereShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "atmosphere_shader"), shaderUtils.POSITION_NORMAL);
            event.registerShader(shaderUtils.localAtmosphereShader, x -> {
            });

            shaderUtils.planetShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "planet_shader"), shaderUtils.POSITION_TEXTURE_NORMAL);
            event.registerShader(shaderUtils.planetShader, x -> {
            });

            shaderUtils.blitAddTonemapShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "blit_add_tonemap"), shaderUtils.POSITION);
            event.registerShader(shaderUtils.blitAddTonemapShader, x -> {
            });

            shaderUtils.blitExtractBright = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "blit_extract_bright"), shaderUtils.POSITION);
            event.registerShader(shaderUtils.blitExtractBright, x -> {
            });

            shaderUtils.blitBlur = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "blit_blur"), shaderUtils.POSITION);
            event.registerShader(shaderUtils.blitBlur, x -> {
            });

            shaderUtils.starBackgroundShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "star_background_shader"), shaderUtils.POSITION_COLOR);
            event.registerShader(shaderUtils.starBackgroundShader, x -> {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}