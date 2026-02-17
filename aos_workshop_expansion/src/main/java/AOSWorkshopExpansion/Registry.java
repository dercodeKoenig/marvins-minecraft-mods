package AOSWorkshopExpansion;

import AOSWorkshopExpansion.MillStone.BlockMillStone;
import AOSWorkshopExpansion.MillStone.EntityMillStone;
import AOSWorkshopExpansion.Sieve.Items.ItemSieveUpgrade;
import AOSWorkshopExpansion.Sieve.Items.Mesh.StringMesh;
import AOSWorkshopExpansion.Sieve.BlockSieve;
import AOSWorkshopExpansion.Sieve.EntitySieve;
import AOSWorkshopExpansion.SpinningWheel.BlockSpinningWheel;
import AOSWorkshopExpansion.SpinningWheel.EntitySpinningWheel;
import AOSWorkshopExpansion.WoodMill.BlockWoodMill;
import AOSWorkshopExpansion.WoodMill.EntityWoodMill;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Supplier;

public class Registry {
    public static final net.neoforged.neoforge.registries.DeferredRegister<Block> BLOCKS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.BLOCK, Main.MODID);
    public static final net.neoforged.neoforge.registries.DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Main.MODID);
    public static final net.neoforged.neoforge.registries.DeferredRegister<Item> ITEMS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.ITEM, Main.MODID);

    public static Supplier<Item> registerBlockItem(String name, Supplier<Block> b){
        return ITEMS.register(name,() -> new BlockItem(b.get(), new Item.Properties()));
    }

    public static final Supplier<Block> SIEVE = BLOCKS.register(
            "sieve",
            () -> new BlockSieve()
    );
    public static final Supplier<BlockEntityType<EntitySieve>> ENTITY_SIEVE = BLOCK_ENTITIES.register(
            "entity_sieve",
            () -> BlockEntityType.Builder.of(EntitySieve::new, SIEVE.get()).build(null)
    );

    public static final Supplier<Item> SIEVE_HOPPER_UPGRADE = ITEMS.register(
            "sieve_hopper_upgrade",
            () -> new ItemSieveUpgrade()
    );

public static final Supplier<Item> STRING_MESH = ITEMS.register(
        "string_mesh",
        () -> new StringMesh()
);


    public static final Supplier<Block> WOODMILL = BLOCKS.register(
            "woodmill",
            () -> new BlockWoodMill()
    );
    public static final Supplier<BlockEntityType<EntityWoodMill>> ENTITY_WOODMILL = BLOCK_ENTITIES.register(
            "entity_woodmill",
            () -> BlockEntityType.Builder.of(EntityWoodMill::new, WOODMILL.get()).build(null)
    );



    public static final Supplier<Block> SPINNING_WHEEL = BLOCKS.register(
            "spinning_wheel",
            () -> new BlockSpinningWheel()
    );
    public static final Supplier<BlockEntityType<EntitySpinningWheel>> ENTITY_SPINNING_WHEEL = BLOCK_ENTITIES.register(
            "entity_spinning_wheel",
            () -> BlockEntityType.Builder.of(EntitySpinningWheel::new, SPINNING_WHEEL.get()).build(null)
    );



    public static final Supplier<Block> MILLSTONE = BLOCKS.register(
            "millstone",
            () -> new BlockMillStone()
    );
    public static final Supplier<BlockEntityType<EntityMillStone>> ENTITY_MILLSTONE = BLOCK_ENTITIES.register(
            "entity_millstone",
            () -> BlockEntityType.Builder.of(EntityMillStone::new, MILLSTONE.get()).build(null)
    );
    public static final Supplier<Item> FLOUR = ITEMS.register("flour", () -> new Item(new Item.Properties()));

    static {
        registerBlockItem("sieve", SIEVE);
        registerBlockItem("spinning_wheel", SPINNING_WHEEL);
        registerBlockItem("woodmill", WOODMILL);
        registerBlockItem("millstone", MILLSTONE);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

}
