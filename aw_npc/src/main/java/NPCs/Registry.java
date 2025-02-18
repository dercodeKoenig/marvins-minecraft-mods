package NPCs;

import NPCs.Items.ItemSetHomeTool;
import NPCs.Items.ItemFoodOrder;
import NPCs.Items.ItemSpawnNpc;
import NPCs.TownHall.BlockTownHall;
import NPCs.TownHall.EntityTownHall;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class Registry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Main.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Main.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Main.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, Main.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Main.MODID);

    public static Supplier<Item> registerBlockItem(String name, Supplier<Block> b) {
        return ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
    }

    public static final Supplier<CreativeModeTab> CREATIVETAB = CREATIVE_TAB.register(
            Main.MODID, () -> new CustomCreativeTab()
    );

    public static final Supplier<Block> TOWNHALL = BLOCKS.register(
            "townhall",
            () -> new BlockTownHall()
    );
    public static final Supplier<BlockEntityType<EntityTownHall>> ENTITY_TOWNHALL = BLOCK_ENTITIES.register(
            "entity_townhall",
            () -> BlockEntityType.Builder.of(EntityTownHall::new,TOWNHALL.get()).build(null)
    );

    public static final Supplier<EntityType<WorkerNPC>> ENTITY_WORKER = ENTITIES.register(
            "worker",
            () -> EntityType.Builder.of(WorkerNPC::new, MobCategory.CREATURE).build(Main.MODID+":worker")
    );

    public static final Supplier<EntityType<CombatNPC>> ENTITY_FIGHTER = ENTITIES.register(
            "fighter",
            () -> EntityType.Builder.of(CombatNPC::new, MobCategory.CREATURE).build(Main.MODID+":fighter")
    );

    public static final Supplier<Item> ITEM_SET_HOME_TOOL = ITEMS.register(
            "set_home_tool",
            () -> new ItemSetHomeTool()
    );

    public static final Supplier<Item> ITEM_FOOD_ORDER = ITEMS.register(
            "food_order",
            () -> new ItemFoodOrder()
    );
    public static final Supplier<Item> ITEM_WORKER_SPAWN = ITEMS.register(
            "spawn_worker",
            () -> new ItemSpawnNpc()
    );

    static {
        registerBlockItem("townhall", TOWNHALL);
    }

    public static void register(IEventBus modBus) {
        CREATIVE_TAB.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
    }

}
