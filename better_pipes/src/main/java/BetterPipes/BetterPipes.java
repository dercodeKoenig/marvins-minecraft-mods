package BetterPipes;

import BetterPipes.PipeBase.RenderPipe;
import BetterPipes.Tank.RenderTank;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.io.IOException;

import static BetterPipes.Registry.*;

@Mod("betterpipes")
public class BetterPipes {

    public BetterPipes(IEventBus modEventBus, ModContainer modContaine) throws IOException {
        //NeoForge.EVENT_BUS.register(EntityPipe.class);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::loadComplete);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::RegisterCapabilities);
        modEventBus.addListener(this::registerEntityRenderers);
        Registry.register(modEventBus);


    }

    public void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(IRON_PIPE.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(TANK.get(), RenderType.cutout());
    }

    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ENTITY_IRON_PIPE.get(), (c) -> new RenderPipe(ResourceLocation.fromNamespaceAndPath("betterpipes", "textures/block/fluid_pipe1_structure.png")));
        event.registerBlockEntityRenderer(ENTITY_TANK.get(), RenderTank::new);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            e.accept(IRON_PIPE.get());
            e.accept(TANK.get());
        }
    }

    private void RegisterCapabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ENTITY_IRON_PIPE.get(), (tile, side) -> tile.getFluidHandler(side));



        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ENTITY_TANK.get(),
                (tank, side) -> {
                    return tank.myTank;
                }
        );
    }

    private void loadComplete(FMLLoadCompleteEvent e) {

    }
}