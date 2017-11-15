package mcjty.meecreeps.actions.workers;

import mcjty.meecreeps.api.IWorkerHelper;
import mcjty.meecreeps.api.PreferedChest;
import mcjty.meecreeps.varia.Counter;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;

public class ChopTreeAndCollectActionWorker extends ChopTreeActionWorker {

    private static final PreferedChest[] PREFERED_CHESTS = new PreferedChest[]{
            PreferedChest.MARKED,
            PreferedChest.FIND_MATCHING_INVENTORY};

    private AxisAlignedBB actionBox = null;

    public ChopTreeAndCollectActionWorker(IWorkerHelper helper) {
        super(helper);
    }

    @Override
    public AxisAlignedBB getActionBox() {
        if (actionBox == null) {
            // @todo config
            actionBox = new AxisAlignedBB(options.getTargetPos().add(-10, -5, -10), options.getTargetPos().add(10, 5, 10));
        }
        return actionBox;
    }

    private void harvest(BlockPos pos) {
        World world = entity.getWorld();
        BlockPlanks.EnumType woodType = getWoodType(world.getBlockState(pos));
        helper.harvestAndPickup(pos);
        findLeaves(pos, world, woodType);
    }

    @Override
    public void tick(boolean timeToWrapUp) {
        if (blocks.isEmpty()) {
            findTree();
        }
        if (blocks.isEmpty() && leavesToTick.isEmpty()) {
            // There is nothing left to do
            helper.done();
            return;
        }

        if (!leavesToTick.isEmpty()) {
            decayLeaves();
        }

        if (timeToWrapUp) {
            if (entity.hasStuffInInventory()) {
                // We need to find a suitable chest
                if (!helper.findSuitableInventory(getSearchBox(), entity.getInventoryMatcher(), helper::putInventoryInChest)) {
                    if (!helper.navigateTo(options.getPlayer(), (p) -> helper.giveToPlayerOrDrop(), 12)) {
                        entity.dropInventory();
                    }
                }
            } else {
                helper.done();
            }
        } else if (!blocks.isEmpty()) {
            harvest(blocks.remove(0));
            // @todo config
            helper.speedUp(5);
        } else {
            helper.taskIsDone();
        }
    }

    private void decayLeaves() {
        World world = entity.getWorld();
        Counter<BlockPos> newmap = new Counter<>();
        for (Map.Entry<BlockPos, Integer> entry : leavesToTick.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!world.isAirBlock(pos)) {
                IBlockState state = world.getBlockState(pos);
                helper.registerHarvestableBlock(pos);
                state.getBlock().updateTick(world, pos, state, entity.getRandom());

                if (!world.isAirBlock(pos)) {
                    Integer counter = entry.getValue();
                    counter--;
                    if (counter > 0) {
                        newmap.put(pos, counter);
                    }
                }
            }
        }
        leavesToTick = newmap;
    }

    @Override
    public PreferedChest[] getPreferedChests() {
        return PREFERED_CHESTS;
    }
}
