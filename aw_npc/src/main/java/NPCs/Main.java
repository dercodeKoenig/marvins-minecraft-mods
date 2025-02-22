package NPCs;

import NPCs.Blocks.Armory.RenderArmory;
import NPCs.Npc.CombatNPC;
import NPCs.Npc.HostileEntities;
import NPCs.Npc.NPCRenderer;
import NPCs.Npc.WorkerNPC;
import NPCs.Blocks.TownHall.TownHallNames;
import NPCs.Blocks.TownHall.TownHallOwners;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.IOException;

import static BetterPipes.Registry.PIPE;
import static NPCs.Registry.*;


@Mod(Main.MODID)
public class Main {

    public static final String MODID = "aw_npc";

    public Main(IEventBus modEventBus, ModContainer modContaine) throws IOException {
        //modEventBus.register(this);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerNetworkStuff);
        modEventBus.addListener(this::entityAttributeCreation);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerCapabilities);

        NeoForge.EVENT_BUS.addListener(TownHallOwners::onLevelSave);
        NeoForge.EVENT_BUS.addListener(TownHallOwners::onLevelLoad);
        NeoForge.EVENT_BUS.addListener(TownHallNames::onLevelSave);
        NeoForge.EVENT_BUS.addListener(TownHallNames::onLevelLoad);

        NeoForge.EVENT_BUS.addListener(HostileEntities::onEntityJoin);

        Registry.register(modEventBus);

    }
    public void onClientSetup(FMLClientSetupEvent event) {
    }

    private void registerCapabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY_TOWNHALL.get(), (x, y) -> (x.inventory));
    }

    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ENTITY_WORKER.get(), NPCRenderer::new);
        event.registerEntityRenderer(ENTITY_FIGHTER.get(), NPCRenderer::new);
        event.registerBlockEntityRenderer(ENTITY_ARMORY.get(), RenderArmory::new);
    }

    public void registerNetworkStuff(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
    }

    public void entityAttributeCreation(EntityAttributeCreationEvent event) {
        // Register attributes for your custom entity
        event.put(ENTITY_WORKER.get(), WorkerNPC.createAttributes().build());
        event.put(ENTITY_FIGHTER.get(), CombatNPC.createAttributes().build());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTab().equals(Registry.CREATIVETAB.get())) {
            e.accept(TOWNHALL.get());
            e.accept(STRATEGY_TABLE.get());
            e.accept(ARMORY.get());
            e.accept(ITEM_FOOD_ORDER.get());
            e.accept(ITEM_WORK_ORDER.get());
            e.accept(ITEM_WORKER_SPAWN.get());
        }
    }
}