package advRocketry.SpaceSuit;

import advRocketry.Config;
import advRocketry.GlobalTime;
import advRocketry.Items.ItemPortablePressureTank;
import advRocketry.Main;
import advRocketry.Registry.Fluids;
import advRocketry.Registry.GeneralRegistry;
import advRocketry.Render.Particles.RocketParticle;
import advRocketry.Utils.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

public abstract class SpaceSuit extends ArmorItem implements ISpaceSuitInventory {
    public static final List<ArmorMaterial.Layer> spaceSuitLayers = List.of(
            new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Main.MODID, "space_suit"))
    );
    public static final HashMap<ArmorItem.Type, Integer> protection = new HashMap<>();

    static {
        protection.put(Type.HELMET, 5);
        protection.put(Type.CHESTPLATE, 5);
        protection.put(Type.LEGGINGS, 5);
        protection.put(Type.BOOTS, 5);
    }


    public SpaceSuit(Type type, Properties properties) {
        super(GeneralRegistry.SPACE_SUIT_MATERIAL, type, properties);
    }

    public static void sharedJetpackTick(Player player, int flightSpeedUpgrades) {
        player.addDeltaMovement(new Vec3(0, player.getGravity() + 0.04, 0));
        player.resetFallDistance();

        // Get the direction the player is looking (a normalized vector where x, y, z are between -1.0 and 1.0)
        Vec3 lookDirection = player.getLookAngle();
        double forwardSpeed = 0.1 * flightSpeedUpgrades; // Adjust this to change how fast they fly horizontally
        // Check if they are actually pressing W (zza > 0) or S (zza < 0)
        if (player.zza != 0) {
            // If pressing S, move backwards instead of forwards
            double directionMultiplier = player.zza > 0 ? 1.0 : -0.5;
            // Add horizontal momentum based on look direction
            player.addDeltaMovement(new Vec3(
                    lookDirection.x * forwardSpeed * directionMultiplier,
                    0, // Handled separately or slightly modified by look pitch
                    lookDirection.z * forwardSpeed * directionMultiplier
            ));
        }
    }

    public static void serverTick() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        HolderLookup.Provider provider = server.registryAccess();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack chestPlateStack = player.getItemBySlot(EquipmentSlot.CHEST);
            ItemStack helmetStack = player.getItemBySlot(EquipmentSlot.HEAD);
            ItemStack legsStack = player.getItemBySlot(EquipmentSlot.LEGS);
            //ItemStack bootsStack = player.getItemBySlot(EquipmentSlot.FEET);

            int flightSpeedUpgrades = 0;
            if (helmetStack.getItem() instanceof Helmet helmetItem) {
                CompoundTag data = helmetItem.getCachedDataUnsafe(helmetStack);
                if (data.contains("nightVisionUpgrade") && data.getBoolean("nightVisionUpgrade") && GlobalTime.getGlobalTime() % 20 == 0) {
                    // higher timer to prevent the "about to end" effect
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, true));
                }
                if (data.contains("flightSpeedUpgrades")) {
                    flightSpeedUpgrades = data.getInt("flightSpeedUpgrades");
                }
            }

            if (legsStack.getItem() instanceof Leggings leggingsItem) {
                CompoundTag data = leggingsItem.getCachedDataUnsafe(legsStack);
                if (data.contains("legsUpgrade") && data.getBoolean("legsUpgrade") && GlobalTime.getGlobalTime() % 20 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0, false, false, true));
                }
            }

            // handle active jetpack
            if ((chestPlateStack.getItem() instanceof ChestPlate chestPlateItem) && chestPlateItem.isJetpackActive(chestPlateStack)) {

                ItemStackHandler inventory = ISpaceSuitInventory.loadInventory(chestPlateStack, provider);
                ItemStack jetpackStack = inventory.getStackInSlot(ChestPlate.jetpack_slot);
                if (!(jetpackStack.getItem() instanceof Jetpack)) {
                    chestPlateItem.setJetpackActive(chestPlateStack, false);
                    continue;
                }

                CompoundTag cachedData = chestPlateItem.getCachedDataUnsafe(chestPlateStack);
                int hydrogen_required = Config.INSTANCE.jetpack_hydrogen_per_tick;
                int oxygen_required = Config.INSTANCE.jetpack_oxygen_per_tick;
                int hydrogen_available = cachedData.contains("hydrogen") ? cachedData.getInt("hydrogen") : 0;
                int oxygen_available = cachedData.contains("oxygen") ? cachedData.getInt("oxygen") : 0;

                if (hydrogen_available >= hydrogen_required && oxygen_available >= oxygen_required) {

                    // drain oxygen
                    int toDrain = oxygen_required;
                    for (int i = 0; i < inventory.getSlots(); i++) {
                        ItemStack stack = inventory.getStackInSlot(i);
                        if (stack.getItem() instanceof ItemPortablePressureTank) {
                            IFluidHandler fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
                            if (fluidHandler.getFluidInTank(0).getFluid().equals(Fluids.OXYGEN.get())) {
                                toDrain -= fluidHandler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE).getAmount();
                            }
                        }
                    }

                    // drain hydrogen from jetpack
                    toDrain = hydrogen_required;
                    ItemStackHandler jetpackInventory = ISpaceSuitInventory.loadInventory(jetpackStack, provider);
                    for (int i = 0; i < jetpackInventory.getSlots(); i++) {
                        ItemStack stack = jetpackInventory.getStackInSlot(i);
                        if (stack.getItem() instanceof ItemPortablePressureTank) {
                            IFluidHandler fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
                            if (fluidHandler.getFluidInTank(0).getFluid().equals(Fluids.HYDROGEN.get())) {
                                toDrain -= fluidHandler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE).getAmount();
                            }
                        }
                    }

                    // save everything back to the itemstacks
                    // save jetpack stack first, it holds a reference to the stack in the main inventory
                    // save main inventory after jetpack is saved
                    ISpaceSuitInventory.saveInventory(jetpackInventory, jetpackStack, player.registryAccess());
                    ISpaceSuitInventory.saveInventory(inventory, chestPlateStack, player.registryAccess());

                    sharedJetpackTick(player, flightSpeedUpgrades);
                } else {
                    // out of fuel
                    chestPlateItem.setJetpackActive(chestPlateStack, false);
                }
            }
        }
    }

    public static void clientTick() {
        Player player = ClientUtils.getSinglePlayer();
        ItemStack chestPlateStack = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack helmetStack = player.getItemBySlot(EquipmentSlot.HEAD);

        int flightSpeedUpgrades = 0;
        if (helmetStack.getItem() instanceof Helmet helmetItem) {
            CompoundTag data = helmetItem.getCachedDataUnsafe(helmetStack);
            if (data.contains("flightSpeedUpgrades")) {
                flightSpeedUpgrades = data.getInt("flightSpeedUpgrades");
            }
        }
        if (chestPlateStack.getItem() instanceof ChestPlate chestPlate) {
            if (ClientUtils.getOptions().keyJump.isDown()) {
                if (!chestPlate.isJetpackActive(chestPlateStack) && chestPlate.hasJetpack(chestPlateStack)) {
                    ChestPlate.ActivateJetpack.sendActivate(true);
                }
            } else {
                if (chestPlate.isJetpackActive(chestPlateStack)) {
                    ChestPlate.ActivateJetpack.sendActivate(false);
                }
            }

            if (chestPlate.isJetpackActive(chestPlateStack)) {
                sharedJetpackTick(player, flightSpeedUpgrades);
            }
        }
        tickJetpackParticles();
    }

    public static void tickJetpackParticles() {
        ClientLevel level = (ClientLevel) ClientUtils.getPlayerLevel();
        for (Player player : level.players()) {
            ItemStack chestPlateStack = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!(chestPlateStack.getItem() instanceof ChestPlate chestPlate))
                continue;
            if (!chestPlate.isJetpackActive(chestPlateStack))
                continue;


            RandomSource random = player.getRandom();

            // 1. Get the player's body rotation in radians
            double rad = Math.toRadians(player.yBodyRot);

            // 2. Configuration values
            double sideOffset = 0.25;
            double backOffset = 0.3;
            double yOffset = 0.3;

            // 3. Calculate rotational offsets
            double backX = Math.sin(rad) * backOffset;
            double backZ = -Math.cos(rad) * backOffset;

            double leftX = Math.cos(rad) * sideOffset;
            double leftZ = Math.sin(rad) * sideOffset;

            // 4. Determine absolute base positions
            double baseX = player.getX() + backX;
            double baseY = player.getY() + yOffset;
            double baseZ = player.getZ() + backZ;

            double leftStartX = baseX + leftX;
            double leftStartZ = baseZ + leftZ;

            double rightStartX = baseX - leftX;
            double rightStartZ = baseZ - leftZ;

            // --- LEFT THRUSTER ---
            // Smoke
            new RocketParticle(
                    level,
                    leftStartX + (random.nextDouble() - 0.5) * 0.1,
                    baseY + (random.nextDouble() - 0.5) * 0.1,
                    leftStartZ + (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.05,
                    -0.3 - (random.nextDouble() * 0.15),
                    (random.nextDouble() - 0.5) * 0.05,
                    new Vector3f(0.5f, 0.5f, 0.5f).mul(1.2f),
                    0.6f,
                    0.3f,
                    100,
                    false
            );
            // Fire (Core Flame)
            level.addParticle(
                    ParticleTypes.FLAME,
                    leftStartX + (random.nextDouble() - 0.5) * 0.05, // Tighter position scatter
                    baseY,                                           // Spawn exactly at nozzle height
                    leftStartZ + (random.nextDouble() - 0.5) * 0.05, // Tighter position scatter
                    (random.nextDouble() - 0.5) * 0.02,              // Very little sideways drift
                    -0.4 - (random.nextDouble() * 0.1),              // Shoots down slightly faster than smoke
                    (random.nextDouble() - 0.5) * 0.02               // Very little sideways drift
            );

            // --- RIGHT THRUSTER ---
            // Smoke
            new RocketParticle(
                    level,
                    rightStartX + (random.nextDouble() - 0.5) * 0.1,
                    baseY + (random.nextDouble() - 0.5) * 0.1,
                    rightStartZ + (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.05,
                    -0.3 - (random.nextDouble() * 0.15),
                    (random.nextDouble() - 0.5) * 0.05,
                    new Vector3f(0.5f, 0.5f, 0.5f).mul(1.2f),
                    0.6f,
                    0.3f,
                    100,
                    false
            );
            // Fire (Core Flame)
            level.addParticle(
                    ParticleTypes.FLAME,
                    rightStartX + (random.nextDouble() - 0.5) * 0.05, // Tighter position scatter
                    baseY,                                            // Spawn exactly at nozzle height
                    rightStartZ + (random.nextDouble() - 0.5) * 0.05, // Tighter position scatter
                    (random.nextDouble() - 0.5) * 0.02,               // Very little sideways drift
                    -0.4 - (random.nextDouble() * 0.1),               // Shoots down slightly faster than smoke
                    (random.nextDouble() - 0.5) * 0.02                // Very little sideways drift
            );
        }
    }

    public abstract int getInventorySlots();

    public abstract boolean isItemValid(ItemStack stack, int slot);
}
