package Vehicles;


import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.io.IOException;

import static Vehicles.Registry.*;


@Mod(Main.MODID)
public class Main {

    public static final String MODID = "aw_vehicles";

    public Main(IEventBus modEventBus, ModContainer modContaine) throws IOException {
        //modEventBus.register(this);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerEntityRenderers);

        Registry.register(modEventBus);

    }

    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ENTITY_BALLISTA.get(), BallistaRenderer::new);
        event.registerEntityRenderer(ENTITY_BALLISTA_BOLT.get(), BallistaBoltRenderer::new);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTab().equals(Registry.CREATIVETAB.get())) {
            e.accept(ITEM_BALLISTA_BOLT.get());
            e.accept(ITEM_BALLISTA_SPAWN.get());
            e.accept(ITEM_BALLISTA_REPAIR.get());
            e.accept(ITEM_WOODEN_HAMMER.get());
        }
    }
}