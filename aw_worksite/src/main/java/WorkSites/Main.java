package WorkSites;

import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.IOException;

import static WorkSites.Registry.*;

@Mod(Main.MODID)
public class Main {

public static final String MODID = "aw_worksite";

    public Main(IEventBus modEventBus, ModContainer modContaine) throws IOException {
        //modEventBus.register(this);

        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::loadComplete);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::RegisterCapabilities);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::loadShaders);
        modEventBus.addListener(this::registerNetworkStuff);
        Registry.register(modEventBus);


    }

    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent login){

    }

    public void onClientSetup(FMLClientSetupEvent event) {

    }


    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ENTITY_CROP_FARM.get(), RenderWorkSiteBounds::new);
        event.registerBlockEntityRenderer(ENTITY_TREE_FARM.get(), RenderWorkSiteBounds::new);
        event.registerBlockEntityRenderer(ENTITY_FISH_FARM.get(), RenderWorkSiteBounds::new);
        event.registerBlockEntityRenderer(ENTITY_QUARRY.get(), RenderWorkSiteBounds::new);
        event.registerBlockEntityRenderer(ENTITY_WAREHOUSE.get(), RenderWorkSiteBounds::new);
    }

    public void registerNetworkStuff(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTab().equals(CREATIVETAB.get())) {
            e.accept(CROP_FARM.get());
            e.accept(TREE_FARM.get());
            e.accept(FISH_FARM.get());
            e.accept(QUARRY.get());
            e.accept(WAREHOUSE.get());
        }
    }

    private void loadShaders(RegisterShadersEvent e) {

    }

    private void RegisterCapabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY_CROP_FARM.get(), (x, y) -> {
            if (y == Direction.DOWN)return x.mainInventory;
            if (y == Direction.UP)return x.inputsInventory;
            else return x.specialResourcesInventory;
        });

        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY_TREE_FARM.get(), (x, y) -> {
            if (y == Direction.DOWN)return x.mainInventory;
            if (y == Direction.UP)return x.inputsInventory;
            else return x.specialResourcesInventory;
        });

        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY_FISH_FARM.get(), (x, y) -> {
            if (y == Direction.DOWN)return x.mainInventory;
            else return x.specialResourcesInventory;
        });

        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY_QUARRY.get(), (x, y) -> {
            if (y == Direction.DOWN)return x.mainInventory;
            else return x.specialResourcesInventory;
        });
    }

    private void loadComplete(FMLLoadCompleteEvent e) {
    }
}