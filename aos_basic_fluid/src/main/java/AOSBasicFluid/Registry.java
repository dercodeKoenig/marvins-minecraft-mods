package AOSBasicFluid;

import AOSBasicFluid.Pump.BlockPump;
import AOSBasicFluid.Pump.BlockPumpExtension;
import AOSBasicFluid.Pump.EntityPump;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class Registry {
    public static final net.neoforged.neoforge.registries.DeferredRegister<Block> BLOCKS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.BLOCK, Main.MODID);
    public static final net.neoforged.neoforge.registries.DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Main.MODID);
    public static final net.neoforged.neoforge.registries.DeferredRegister<Item> ITEMS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.ITEM, Main.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.MENU, Main.MODID);

    public static Supplier<Item> registerBlockItem(String name, Supplier<Block> b){
        return ITEMS.register(name,() -> new BlockItem(b.get(), new Item.Properties()));
    }

    public static final Supplier<Block> PUMP = BLOCKS.register(
            "pump",
            () -> new BlockPump()
    );
    public static final Supplier<BlockEntityType<EntityPump>> ENTITY_PUMP = BLOCK_ENTITIES.register(
            "entity_pump",
            () -> BlockEntityType.Builder.of(EntityPump::new, PUMP.get()).build(null)
    );

    public static final Supplier<Block> PUMP_EXT = BLOCKS.register(
            "pump_ext",
            () -> new BlockPumpExtension()
    );


    static {
        registerBlockItem("pump", PUMP);
        registerBlockItem("pump_ext", PUMP_EXT);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
    }

}
