package WorkSites;

import WorkSites.CropFarm.BlockCropFarm;
import WorkSites.CropFarm.EntityCropFarm;
import WorkSites.FishFarm.BlockFishFarm;
import WorkSites.FishFarm.EntityFishFarm;
import WorkSites.Quarry.BlockQuarry;
import WorkSites.Quarry.EntityQuarry;
import WorkSites.TreeFarm.BlockTreeFarm;
import WorkSites.TreeFarm.EntityTreeFarm;
import WorkSites.Warehouse.BlockWarehouse;
import WorkSites.Warehouse.EntityWarehouse;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Supplier;

public class Registry {
    public static final net.neoforged.neoforge.registries.DeferredRegister<Block> BLOCKS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.BLOCK, Main.MODID);
    public static final net.neoforged.neoforge.registries.DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Main.MODID);
    public static final net.neoforged.neoforge.registries.DeferredRegister<Item> ITEMS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.ITEM, Main.MODID);
    public static final net.neoforged.neoforge.registries.DeferredRegister<CreativeModeTab> CREATIVE_TAB = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, Main.MODID);

    public static Supplier<Item> registerBlockItem(String name, Supplier<Block> b){
        return ITEMS.register(name,() -> new BlockItem(b.get(), new Item.Properties()));
    }

    public static final Supplier<Block> CROP_FARM = BLOCKS.register(
            "crop_farm",
            () -> new BlockCropFarm()
    );
    public static final Supplier<BlockEntityType<EntityCropFarm>> ENTITY_CROP_FARM = BLOCK_ENTITIES.register(
            "entity_crop_farm",
            () -> BlockEntityType.Builder.of(EntityCropFarm::new, CROP_FARM.get()).build(null)
    );

    public static final Supplier<Block> TREE_FARM = BLOCKS.register(
            "tree_farm",
            () -> new BlockTreeFarm()
    );
    public static final Supplier<BlockEntityType<EntityTreeFarm>> ENTITY_TREE_FARM = BLOCK_ENTITIES.register(
            "entity_tree_farm",
            () -> BlockEntityType.Builder.of(EntityTreeFarm::new, TREE_FARM.get()).build(null)
    );

    public static final Supplier<Block> FISH_FARM = BLOCKS.register(
            "fish_farm",
            () -> new BlockFishFarm()
    );
    public static final Supplier<BlockEntityType<EntityFishFarm>> ENTITY_FISH_FARM = BLOCK_ENTITIES.register(
            "entity_fish_farm",
            () -> BlockEntityType.Builder.of(EntityFishFarm::new, FISH_FARM.get()).build(null)
    );

    public static final Supplier<Block> QUARRY = BLOCKS.register(
            "quarry",
            () -> new BlockQuarry()
    );
    public static final Supplier<BlockEntityType<EntityQuarry>> ENTITY_QUARRY = BLOCK_ENTITIES.register(
            "entity_quarry",
            () -> BlockEntityType.Builder.of(EntityQuarry::new, QUARRY.get()).build(null)
    );

    public static final Supplier<Block> WAREHOUSE = BLOCKS.register(
            "warehouse",
            () -> new BlockWarehouse()
    );
    public static final Supplier<BlockEntityType<EntityWarehouse>> ENTITY_WAREHOUSE = BLOCK_ENTITIES.register(
            "entity_warehouse",
            () -> BlockEntityType.Builder.of(EntityWarehouse::new, WAREHOUSE.get()).build(null)
    );

    public static final Supplier<CreativeModeTab> CREATIVETAB = CREATIVE_TAB.register(
            Main.MODID,()->new CustomCreativeTab()
    );

    static {
        registerBlockItem("crop_farm", CROP_FARM);
        registerBlockItem("tree_farm", TREE_FARM);
        registerBlockItem("fish_farm", FISH_FARM);
        registerBlockItem("quarry", QUARRY);
        registerBlockItem("warehouse", WAREHOUSE);
    }

    public static void register(IEventBus modBus) {
        CREATIVE_TAB.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

}
