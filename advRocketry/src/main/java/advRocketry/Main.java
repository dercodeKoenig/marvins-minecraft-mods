package advRocketry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Matrix4f;

import static advRocketry.skyrenderer.CUSTOM_SKY_DIMENSIONS;

@Mod(Main.MODID)
public class Main {

public static final String MODID ="adv_rocketry";

    public Main(IEventBus modEventBus, ModContainer modContaine) {
        //modEventBus.register(this);
        NeoForge.EVENT_BUS.addListener(this::onRenderStage);
        NeoForge.EVENT_BUS.addListener(skyrenderer::onRenderFog );
        NeoForge.EVENT_BUS.addListener(this::onWorldLoad);
        NeoForge.EVENT_BUS.addListener(DimensionManager.INSTANCE::serverTick);
        NeoForge.EVENT_BUS.addListener(DimensionManager.INSTANCE::clientTick);
    }

    public void onWorldLoad(LevelEvent.Load event) {

    }

    private void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        PoseStack poseStack = event.getPoseStack();
        Matrix4f proj = event.getProjectionMatrix();
        Matrix4f view = event.getModelViewMatrix();

        skyrenderer.INSTANCE.renderSkyBox(poseStack,proj,view);
    }
}