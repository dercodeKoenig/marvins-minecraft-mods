package advRocketry;

import advRocketry.Dimension.DimensionManager;
import advRocketry.Render.Fog;
import advRocketry.Render.shaderUtils;
import advRocketry.Render.skyrenderer;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Matrix4f;

import java.io.IOException;

@Mod(Main.MODID)
public class Main {

public static final String MODID ="adv_rocketry";

    public Main(IEventBus modEventBus, ModContainer modContaine) {
        //modEventBus.register(this);
        if(FMLLoader.getDist().isClient()) {
            NeoForge.EVENT_BUS.addListener(this::onRenderStage);
            NeoForge.EVENT_BUS.addListener(Fog::renderFogEvent);
            NeoForge.EVENT_BUS.addListener(Fog::computeFogColorEvent);
            NeoForge.EVENT_BUS.addListener(DimensionManager.INSTANCE::clientTick);
        }

        NeoForge.EVENT_BUS.addListener(this::onWorldLoad);
        NeoForge.EVENT_BUS.addListener(DimensionManager.INSTANCE::serverTick);

        modEventBus.addListener(this::loadShaders);
    }

    public void onWorldLoad(LevelEvent.Load event) {

    }

    private void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            PoseStack poseStack = event.getPoseStack();
            Matrix4f proj = event.getProjectionMatrix();
            Matrix4f view = event.getModelViewMatrix();
            skyrenderer.INSTANCE.renderSky(poseStack,proj,view,event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            // cloud come now, disable stupid fog
            FogRenderer.setupFog(Minecraft.getInstance().gameRenderer.getMainCamera(), FogRenderer.FogMode.FOG_SKY,999990,false,0);
        }
    }

    private void loadShaders(RegisterShadersEvent event){
        // 3. Register the shader and set the static field in the callback
        try {
            ShaderInstance atmosphereShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "atmosphere_shader"), shaderUtils.POSITION_NORMAL);
            event.registerShader(atmosphereShader,atmosphereShaderInstance -> {
                shaderUtils.atmosphereShader = atmosphereShaderInstance;
            });

            ShaderInstance planetShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "planet_shader"),shaderUtils.POSITION_TEXTURE_NORMAL);
            event.registerShader(planetShader,planetShaderInstance -> {
                shaderUtils.planetShader = planetShaderInstance;
            });

            ShaderInstance blitAddTonemapShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "blit_add_tonemap"), shaderUtils.POSITION);
            event.registerShader(blitAddTonemapShader,blitAddTonemapShaderInstance -> {
                shaderUtils.blitAddTonemapShader = blitAddTonemapShader;
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}