package NPCs.Npc;

import ARLib.gui.modules.guiModuleItemHandlerSlot;
import NPCs.Items.ItemWorkOrder;
import NPCs.Npc.programs.*;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;


public class CombatNPC extends NPCBase {

    public static EntityDataAccessor<Integer> DATA_WORKTYPE = SynchedEntityData.defineId(CombatNPC.class, EntityDataSerializers.INT);

    public enum WorkTypes {
        fighter,
        archer,
        medic
    }

    public RunForHelpProgram runForHelpProgram;
    public FighterFollowWorkOrderProgram fighterFollowWorkOrderProgram;
    public FighterFollowWorkOrderByStrategyTable fighterFollowWorkOrderByStrategyTable;

    public ItemStackHandler ordersStackHandler = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                if (stack.getItem() instanceof ItemWorkOrder) {
                    return true;
                }
            }
            return false;
        }
    };


    public CombatNPC(EntityType<CombatNPC> entityType, Level level) {
        super(entityType, level);
        guiModuleItemHandlerSlot workOrderSlot = new guiModuleItemHandlerSlot(19009, ordersStackHandler, 0, 1, 0, guiHandler, 140, 70);
        guiHandler.getModules().addFirst(workOrderSlot);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes() // Base attributes for mobs
                .add(Attributes.MAX_HEALTH, 30.0D) // Default health
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ATTACK_SPEED)
                .add(Attributes.LUCK)
                .add(Attributes.BLOCK_BREAK_SPEED)
                ;
    }

    @Override
    public void onAddedToLevel() {
        if (!level().isClientSide) {
            registerGoals();

            if(getEntityData().get(DATA_TEXTURE).isEmpty()) {
                int randomNumber = Math.abs(level().random.nextInt()) % 4 + 1;
                getEntityData().set(DATA_TEXTURE, "po_soldier_" + randomNumber + ".png");
            }
        }
        super.onAddedToLevel();
    }

    @Override
    protected void registerGoals() {
        List<WrappedGoal> activeGoals = new ArrayList<>(goalSelector.getAvailableGoals());
        for (WrappedGoal i : activeGoals) {
            if (i.isRunning())
                i.stop();
        }
        goalSelector.getAvailableGoals().clear();

        int priority = 0;


        Goal attackGoal0 = new MeleeAttackGoalWithHunger(this, 1.2, true);
        goalSelector.addGoal(priority++, attackGoal0);

        Goal attackGoal1 = new NearestAttackableTargetGoalWithHunger<>(this, LivingEntity.class, 20, true, true, (entity) -> HostileEntities.shouldAttack(entity, this));
        goalSelector.addGoal(priority++, attackGoal1);

        goalSelector.addGoal(priority++, new FollowOwnerProgram(this));

        runForHelpProgram = new RunForHelpProgram(this);
        goalSelector.addGoal(priority++, runForHelpProgram);

        goalSelector.addGoal(priority++, new FoodProgramFighter(this));

        fighterFollowWorkOrderProgram = new FighterFollowWorkOrderProgram(this);
        goalSelector.addGoal(priority++, fighterFollowWorkOrderProgram);

        fighterFollowWorkOrderByStrategyTable = new FighterFollowWorkOrderByStrategyTable(this);
        goalSelector.addGoal(priority++, fighterFollowWorkOrderByStrategyTable);

        goalSelector.addGoal(priority++, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(priority++, new FloatGoal(this));

        this.goalSelector.addGoal(priority++, new RandomStrollGoal(this, 1.0D, 120, false));
        this.goalSelector.addGoal(priority++, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(priority++, new RandomLookAroundGoal(this));

    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WORKTYPE, WorkTypes.fighter.ordinal());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide) {

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
        compound.put("orderInv",ordersStackHandler.serializeNBT(this.registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if(compound.contains("worktype")) {
            getEntityData().set(DATA_WORKTYPE, compound.getInt("worktype"));
        }
        ordersStackHandler.deserializeNBT(this.registryAccess(), compound.getCompound("orderInv"));
        registerGoals();
    }
}
