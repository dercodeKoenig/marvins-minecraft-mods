package ARLib;

import ARLib.blockentities.*;
import ARLib.holoProjector.RenderPreviewBlock;
import ARLib.multiblockCore.RenderMultiblockPlaceholder;
import ARLib.network.PacketBlockEntity;
import ARLib.network.PacketEntity;
import ARLib.network.PacketPlayerMainHand;
import ARLib.network.SimpleNetworkPacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static ARLib.ARLibRegistry.*;

@Mod(ARLib.MODID)
public class ARLib {
    public static final String MODID = "arlib";

    public ARLib(IEventBus modEventBus, ModContainer modContaine) {
        //NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(ARLib::addCreative);
        modEventBus.addListener(ARLib::registerCapabilities);
        modEventBus.addListener(ARLib::registerNetworkStuff);
        modEventBus.addListener(ARLib::registerRenderers);

        ARLibRegistry.register(modEventBus);
    }


    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ENTITY_STRUCTURE_PREVIEW.get(), RenderPreviewBlock::new);
        event.registerBlockEntityRenderer(ENTITY_PLACEHOLDER.get(), RenderMultiblockPlaceholder::new);
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTab() == CUSTOM_CREATIVE_TAB.get()) {
            e.accept(BLOCK_ENERGY_INPUT_BLOCK.get());
            //e.accept(BLOCK_ENERGY_OUTPUT_BLOCK.get());
            e.accept(BLOCK_ITEM_INPUT_BLOCK.get());
            e.accept(BLOCK_ITEM_OUTPUT_BLOCK.get());
            e.accept(BLOCK_FLUID_INPUT_BLOCK.get());
            e.accept(BLOCK_FLUID_OUTPUT_BLOCK.get());
            e.accept(BLOCK_MOTOR.get());
            e.accept(BLOCK_STRUCTURE.get());
            e.accept(BLOCK_COIL_COPPER.get());
            e.accept((ITEM_HOLOPROJECTOR.get()));
        }
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ARLibRegistry.ENTITY_ENERGY_INPUT_BLOCK.get(), (x, y) -> ((EntityEnergyInputBlock) x).energyStorage);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ARLibRegistry.ENTITY_ENERGY_OUTPUT_BLOCK.get(), (x, y) -> ((EntityEnergyOutputBlock) x).energyStorage);
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ARLibRegistry.ENTITY_ITEM_INPUT_BLOCK.get(), (x, y) -> ((EntityItemInputBlock) x).inventory);
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ARLibRegistry.ENTITY_ITEM_OUTPUT_BLOCK.get(), (x, y) -> ((EntityItemOutputBlock) x).inventory);
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ARLibRegistry.ENTITY_FLUID_INPUT_BLOCK.get(), (x, y) -> ((EntityFluidInputBlock) x).simpleFluidContainer);
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ARLibRegistry.ENTITY_FLUID_OUTPUT_BLOCK.get(), (x, y) -> ((EntityFluidOutputBlock) x).simpleFluidContainer);
    }

    public static void registerNetworkStuff(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        PacketBlockEntity.register(registrar);
        PacketPlayerMainHand.register(registrar);
        PacketEntity.register(registrar);
        SimpleNetworkPacket.register(registrar);
    }
}
