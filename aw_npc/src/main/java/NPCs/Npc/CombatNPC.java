package NPCs.Npc;

import AgeOfSteam.Registry;
import NPCs.Npc.programs.*;
import NPCs.Npc.programs.Combat.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;


public class CombatNPC extends NPCBase {

    public static EntityDataAccessor<Integer> DATA_WORKTYPE = SynchedEntityData.defineId(CombatNPC.class, EntityDataSerializers.INT);

    public enum WorkTypes {
        fighter,
        archer,
        siege_engineer,
        medic,
        arbalist
    }

    public RunForHelpProgram runForHelpProgram;
    public FighterFollowWorkOrderProgram fighterFollowWorkOrderProgram;
    public FighterFollowWorkOrderByStrategyTable fighterFollowWorkOrderByStrategyTable;

    public TakeMeleeWeaponProgram takeWeaponProgram;
    public TakeBowWeaponProgram takeBowWeaponProgram;

    public CombatNPC(EntityType<CombatNPC> entityType, Level level) {
        super(entityType, level);
        takeWeaponProgram = new TakeMeleeWeaponProgram(this);
        takeBowWeaponProgram = new TakeBowWeaponProgram(this);
        setCustomName(Component.literal("Fighter"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes() // Base attributes for mobs
                .add(Attributes.MAX_HEALTH, 30.0D) // Default health
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.ATTACK_DAMAGE)
                .add(Attributes.ATTACK_SPEED)
                .add(Attributes.LUCK)
                .add(Attributes.BLOCK_BREAK_SPEED);
    }

    @Override
    public void onAddedToLevel() {
        if (!level().isClientSide) {
            registerGoals();

            if (getEntityData().get(DATA_TEXTURE).isEmpty()) {
                int randomNumber = Math.abs(level().random.nextInt()) % 4 + 1;
                getEntityData().set(DATA_TEXTURE, "po_soldier_" + randomNumber + ".png");
            }
        }
        super.onAddedToLevel();
        if (!level().isClientSide) {
            takeWeaponProgram.findBestWeaponIndex();
            takeBowWeaponProgram.findBestWeaponIndex();
        }
    }

    @Override
    protected void registerGoals() {
        for (WrappedGoal i : goalSelector.getAvailableGoals()) {
            i.stop();
        }
        goalSelector.getAvailableGoals().clear();

        int priority = 0;


        if (getEntityData().get(DATA_WORKTYPE) == WorkTypes.fighter.ordinal()) {
            Goal attackGoal0 = new MeleeAttackGoalWithHunger(this, 1.2, true);
            goalSelector.addGoal(priority++, attackGoal0);

            Goal attackGoal1 = new NearestAttackableTargetGoalWithHunger<>(this, LivingEntity.class, 10, true, true, (entity) -> HostileEntities.shouldAttack(entity, this));
            goalSelector.addGoal(priority++, attackGoal1);
        }
        if (getEntityData().get(DATA_WORKTYPE) == WorkTypes.archer.ordinal()) {
            Goal attackGoal2 = new BallistaProgram(this, 1.2);
            goalSelector.addGoal(priority++, attackGoal2);

            Goal attackGoal0 = new RangedBowAttackProgram(this, 1.2, 20, 25);
            goalSelector.addGoal(priority++, attackGoal0);

            Goal attackGoal1 = new NearestAttackableTargetGoalWithHunger<>(this, LivingEntity.class, 10, true, false, (entity) -> HostileEntities.shouldAttack(entity, this));
            goalSelector.addGoal(priority++, attackGoal1);
        }
        if (getEntityData().get(DATA_WORKTYPE) == WorkTypes.siege_engineer.ordinal()) {
            Goal ballistaProgram = new BallistaProgram(this, 1.2);
            goalSelector.addGoal(priority++, ballistaProgram);
        }

        goalSelector.addGoal(priority++, new NPCHurtByTargetProgram(this, true, true));

        goalSelector.addGoal(priority++, new FollowOwnerProgram(this));

        goalSelector.addGoal(priority++, new ArmoryProgram(this));

        if (getEntityData().get(DATA_WORKTYPE) == WorkTypes.siege_engineer.ordinal()) {
            Goal attackGoal0 = new MeleeAttackGoalWithHunger(this, 1.2, true);
            goalSelector.addGoal(priority++, attackGoal0);
        }

        runForHelpProgram = new RunForHelpProgram(this);
        if (getEntityData().get(DATA_WORKTYPE) != WorkTypes.siege_engineer.ordinal()) {
            goalSelector.addGoal(priority++, runForHelpProgram);
        }

        goalSelector.addGoal(priority++, new FoodProgramFighter(this));

        goalSelector.addGoal(priority++, new PickupItemsOnGroundProgram(this, 8));
        goalSelector.addGoal(priority++, new FighterDropLootProgram(this));

        fighterFollowWorkOrderProgram = new FighterFollowWorkOrderProgram(this);
        goalSelector.addGoal(priority++, fighterFollowWorkOrderProgram);

        fighterFollowWorkOrderByStrategyTable = new FighterFollowWorkOrderByStrategyTable(this);
        goalSelector.addGoal(priority++, fighterFollowWorkOrderByStrategyTable);

        goalSelector.addGoal(priority++, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(priority++, new FloatGoal(this));

        this.goalSelector.addGoal(priority++, new RandomStrollGoal(this, 0.8, 120, false));
        this.goalSelector.addGoal(priority++, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(priority++, new RandomLookAroundGoal(this));

    }

    @Override
    public void onInventoryChange() {
        takeWeaponProgram.findBestWeaponIndex();
        takeBowWeaponProgram.findBestWeaponIndex();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WORKTYPE, WorkTypes.fighter.ordinal());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && isFriendlyTo(player)) {
            if (player.getItemInHand(hand).getItem().equals(Items.BOW)) {
                getEntityData().set(DATA_WORKTYPE, WorkTypes.archer.ordinal());
                player.setItemInHand(hand, ItemStack.EMPTY);
                registerGoals();
                setCustomName(Component.literal("Archer"));
                int randomNumber = Math.abs(level().random.nextInt()) % 4 + 1;
                getEntityData().set(DATA_TEXTURE, "po_soldier_" + randomNumber + ".png");
                return InteractionResult.SUCCESS;
            }

            if (player.getItemInHand(hand).getItem().equals(Items.WOODEN_SWORD)) {
                getEntityData().set(DATA_WORKTYPE, WorkTypes.fighter.ordinal());
                player.setItemInHand(hand, ItemStack.EMPTY);
                registerGoals();
                setCustomName(Component.literal("Fighter"));
                int randomNumber = Math.abs(level().random.nextInt()) % 4 + 1;
                getEntityData().set(DATA_TEXTURE, "po_soldier_" + randomNumber + ".png");
                return InteractionResult.SUCCESS;
            }

            if (player.getItemInHand(hand).getItem().equals(Registry.ITEM_WOODEN_HAMMER.get())) {
                getEntityData().set(DATA_WORKTYPE, WorkTypes.siege_engineer.ordinal());
                player.setItemInHand(hand, ItemStack.EMPTY);
                registerGoals();
                setCustomName(Component.literal("Siege Engineer"));
                int randomNumber = Math.abs(level().random.nextInt()) % 1 + 1;
                getEntityData().set(DATA_TEXTURE, "po_engineer_" + randomNumber + ".png");
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("worktype", getEntityData().get(DATA_WORKTYPE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("worktype")) {
            getEntityData().set(DATA_WORKTYPE, compound.getInt("worktype"));
        }
        registerGoals();
    }
}
