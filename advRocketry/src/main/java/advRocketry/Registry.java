package advRocketry;

import advRocketry.BlockEntities.EntityRocketAssembler;
import advRocketry.Blocks.LaunchPad;
import advRocketry.Blocks.RocketAssembler;
import advRocketry.Blocks.StructureTower;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class Registry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Main.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Main.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Main.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, Main.MODID);

    public static final Supplier<CreativeModeTab> CUSTOM_CREATIVE_TAB = CREATIVE_TAB.register(Main.MODID, () -> new CustomCreativeTab());

    public static Supplier<Item> registerBlockItem(String name, Supplier<Block> b) {
        return ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
    }

    public static final Supplier<Block> LAUNCHPAD = BLOCKS.register("launchpad", () -> new LaunchPad());
    public static final Supplier<Item> ITEM_LAUNCHPAD = registerBlockItem("launchpad", LAUNCHPAD);
    ;

    public static final Supplier<Block> STRUCTURE_TOWER = BLOCKS.register("structure_tower", () -> new StructureTower());

    public static final Supplier<Block> ROCKET_ASSEMBLER = BLOCKS.register("rocket_assembler", () -> new RocketAssembler());
    public static final Supplier<BlockEntityType<?>> ENTITY_ROCKET_ASSEMBLER = BLOCK_ENTITIES.register("rocket_assembler", () -> BlockEntityType.Builder.of(EntityRocketAssembler::new, ROCKET_ASSEMBLER.get()).build(null));

    static {
        registerBlockItem("structure_tower", STRUCTURE_TOWER);
        registerBlockItem("rocket_assembler", ROCKET_ASSEMBLER);
    }
}
