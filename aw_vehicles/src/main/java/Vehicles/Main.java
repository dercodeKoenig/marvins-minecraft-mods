package Vehicles;


import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;

import static Vehicles.Registry.*;


@Mod(Main.MODID)
public class Main {

    public static final String MODID = "aw_vehicles";

    public Main(IEventBus modEventBus) {
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
        if (e.getTab().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            e.accept(ITEM_BALLISTA_BOLT.get());
            e.accept(ITEM_BALLISTA_SPAWN.get());
            e.accept(ITEM_BALLISTA_REPAIR.get());
            e.accept(ITEM_WOODEN_HAMMER.get());
        }
    }
}