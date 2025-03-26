package ARMachines;


import ARMachines.lathe.BlockLathe;
import ARMachines.lathe.EntityLathe;
import ARMachines.lathe.RenderLathe;
import ARMachines.rollingMachine.BlockRollingMachine;
import ARMachines.rollingMachine.EntityRollingMachine;
import ARMachines.rollingMachine.RenderRollingMachine;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MultiblockRegistry {
    public static final net.neoforged.neoforge.registries.DeferredRegister<Block> BLOCKS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.BLOCK, ARMachines.MODID);
    public static final net.neoforged.neoforge.registries.DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ARMachines.MODID);
    public static final net.neoforged.neoforge.registries.DeferredRegister<Item> ITEMS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.ITEM, ARMachines.MODID);

    public static void registerBlockItem(String name, DeferredHolder<Block,Block> b){
        ITEMS.register(name,() -> new BlockItem(b.get(), new Item.Properties()));
    }

    // lathe
    public static final DeferredHolder<Block, Block> BLOCK_LATHE = BLOCKS.register(
            "block_lathe",
            () -> new BlockLathe(BlockBehaviour.Properties.of())
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EntityLathe>> ENTITY_LATHE = BLOCK_ENTITIES.register(
            "entity_lathe",
            () -> BlockEntityType.Builder.of(EntityLathe::new, BLOCK_LATHE.get()).build(null)
    );


    // rolling machine
    public static final DeferredHolder<Block, Block> BLOCK_ROLLINGMACHINE = BLOCKS.register(
            "block_rollingmachine",
            () -> new BlockRollingMachine(BlockBehaviour.Properties.of())
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EntityRollingMachine>> ENTITY_ROLLINGMACHINE = BLOCK_ENTITIES.register(
            "entity_rollingmachine",
            () -> BlockEntityType.Builder.of(EntityRollingMachine::new, BLOCK_ROLLINGMACHINE.get()).build(null)
    );
/*
    // crystallizer
    public static final DeferredHolder<Block, Block> BLOCK_CRYSTALLIZER = BLOCKS.register(
            "block_crystallizer",
            () -> new BlockCrystallizer(BlockBehaviour.Properties.of())
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EntityCrystallizer>> ENTITY_CRYSTALLIZER = BLOCK_ENTITIES.register(
            "entity_crystallizer",
            () -> BlockEntityType.Builder.of(EntityCrystallizer::new, BLOCK_CRYSTALLIZER.get()).build(null)
    );

    // electrolyzer
    public static final DeferredHolder<Block, Block> BLOCK_ELECTROLYZER = BLOCKS.register(
            "block_electrolyzer",
            () -> new BlockElectrolyzer(BlockBehaviour.Properties.of())
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EntityElectrolyzer>> ENTITY_ELECTROLYZER = BLOCK_ENTITIES.register(
            "entity_electrolyzer",
            () -> BlockEntityType.Builder.of(EntityElectrolyzer::new, BLOCK_ELECTROLYZER.get()).build(null)
    );
 */



    public static void register(IEventBus modBus) {
        registerBlockItem("block_lathe", BLOCK_LATHE);
        registerBlockItem("block_rollingmachine", BLOCK_ROLLINGMACHINE);
        //registerBlockItem("block_crystallizer", BLOCK_CRYSTALLIZER);
        //registerBlockItem("block_electrolyzer", BLOCK_ELECTROLYZER);

        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ENTITY_LATHE.get(), RenderLathe::new);
        event.registerBlockEntityRenderer(ENTITY_ROLLINGMACHINE.get(), RenderRollingMachine::new);
        //event.registerBlockEntityRenderer(ENTITY_CRYSTALLIZER.get(), RenderCrystallizer::new);
        //event.registerBlockEntityRenderer(ENTITY_ELECTROLYZER.get(), RenderElectrolyzer::new);
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent e){
        if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS){
            e.accept(BLOCK_LATHE.get());
            e.accept(BLOCK_ROLLINGMACHINE.get());
            //e.accept(BLOCK_CRYSTALLIZER.get());
            //e.accept(BLOCK_ELECTROLYZER.get());
        }
    }
}
