package Vehicles;

import Vehicles.Ballista.Ballista;
import Vehicles.Ballista.BallistaBolt;
import Vehicles.Ballista.BallistaSpawnItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class Registry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Main.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Main.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Main.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, Main.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Main.MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Main.MODID);

    public static Supplier<Item> registerBlockItem(String name, Supplier<Block> b) {
        return ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
    }

    public static final Supplier<CreativeModeTab> CREATIVETAB = CREATIVE_TAB.register(
            Main.MODID, () -> new CustomCreativeTab()
    );


    public static final Supplier<EntityType<Ballista>> ENTITY_BALLISTA = ENTITIES.register(
            "ballista",
            () -> EntityType.Builder.of(Ballista::new, MobCategory.MISC).sized(1,2).build(Main.MODID+":ballista")
    );

    public static final Supplier<EntityType<BallistaBolt>> ENTITY_BALLISTA_BOLT = ENTITIES.register(
            "ballista_bolt",
            () -> EntityType.Builder.of(BallistaBolt::new, MobCategory.MISC).sized(0.25f,0.25f).build(Main.MODID+":ballista_bolt")
    );
    public static final Supplier<Item> ITEM_BALLISTA_BOLT = ITEMS.register("ballista_bolt", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> ITEM_BALLISTA_SPAWN = ITEMS.register("ballista", () -> new BallistaSpawnItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> ITEM_BALLISTA_REPAIR = ITEMS.register("ballista_repair", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final Supplier<Block> GHOST_BLOCK = BLOCKS.register("ballista_ghost", ()-> new GhostBlock());

    public static Supplier<Item> ITEM_WOODEN_HAMMER = null;
    static {
        if(!ModList.get().isLoaded("age_of_steam")){
            ITEM_WOODEN_HAMMER = ITEMS.register("wooden_hammer", () -> new Item(new Item.Properties().stacksTo(1)));
        }
    }

    public static final Supplier<SoundEvent> SOUND_BALLISTA_RELOAD = SOUND_EVENTS.register("ballista_reload", SoundEvent::createVariableRangeEvent);
    public static final Supplier<SoundEvent> SOUND_BALLISTA_LAUNCH = SOUND_EVENTS.register("ballista_launch", SoundEvent::createVariableRangeEvent);
    public static final Supplier<SoundEvent> SOUND_BALLISTA_ENTITY_HIT = SOUND_EVENTS.register("ballista_bolt_hit_entity", SoundEvent::createVariableRangeEvent);
    public static final Supplier<SoundEvent> SOUND_BALLISTA_GROUND_HIT = SOUND_EVENTS.register("ballista_bolt_hit_ground", SoundEvent::createVariableRangeEvent);

    public static void register(IEventBus modBus) {
        CREATIVE_TAB.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        SOUND_EVENTS.register(modBus);
        ENTITIES.register(modBus);
    }

}
