package BetterPipes;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        modEventBus.addListener(this::registerNetworkStuff);
        Registry.register(modEventBus);


    }

    public void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(PIPE.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(TANK.get(), RenderType.cutout());
    }

    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ENTITY_PIPE.get(), RenderPipe::new);
        event.registerBlockEntityRenderer(ENTITY_TANK.get(), RenderTank::new);
    }

    public void registerNetworkStuff(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        PacketBlockEntity.register(registrar);
        PacketFluidUpdate.register(registrar);
        PacketFluidAmountUpdate.register(registrar);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            e.accept(PIPE.get());
            e.accept(TANK.get());
        }
    }

    private void RegisterCapabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ENTITY_PIPE.get(), (tile, side) -> tile.getFluidHandler(side));



        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ENTITY_TANK.get(),
                (tank, side) -> {
                    return tank.myTank;
                }
        );
    }

    private void loadComplete(FMLLoadCompleteEvent e) {

    }
}