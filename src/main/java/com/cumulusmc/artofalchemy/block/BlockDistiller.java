package com.cumulusmc.artofalchemy.block;

import com.cumulusmc.artofalchemy.blockentity.AoABlockEntities;
import com.cumulusmc.artofalchemy.blockentity.BlockEntityCalcinatorPlus;
import com.cumulusmc.artofalchemy.blockentity.BlockEntityDissolver;
import com.cumulusmc.artofalchemy.blockentity.BlockEntityDistiller;
import com.cumulusmc.artofalchemy.item.AoAItems;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BlockDistiller extends BlockWithEntity {

	public static final BooleanProperty LIT = Properties.LIT;
	public static final Settings SETTINGS = Settings
		.of(Material.STONE)
		.strength(5.0f, 6.0f)
		.luminance((state) -> state.get(LIT) ? 15 : 0)
		.nonOpaque();

	public static Identifier getId() {
		return Registry.BLOCK.getId(AoABlocks.DISTILLER);
	}

	public BlockDistiller() {
		this(SETTINGS);
	}

	protected BlockDistiller(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(LIT, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return super.getPlacementState(ctx);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
			BlockHitResult hit) {

		ItemStack inHand = player.getStackInHand(hand);

		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof BlockEntityDistiller) {
			BlockEntityDistiller distiller = (BlockEntityDistiller) blockEntity;
			if (inHand.getItem() == Items.BUCKET && distiller.withdrawAlkahest(1000)) {
				if (!player.getAbilities().creativeMode) {
					inHand.decrement(1);
					player.giveItemStack(new ItemStack(AoAItems.ALKAHEST_BUCKET));
				}
				world.playSound(
					null,
					pos,
					SoundEvents.ITEM_BUCKET_FILL,
					SoundCategory.BLOCKS,
					1.0F,
					1.0F
				);
				return ActionResult.SUCCESS;
			} else if (inHand.getItem() == AoAItems.ESSENTIA_VESSEL) {
				ItemUsageContext itemContext = new ItemUsageContext(player, hand, hit);
				ActionResult itemResult = inHand.useOnBlock(itemContext);
				if (itemResult != ActionResult.PASS) {
					return itemResult;
				}
			}
			if (!world.isClient()) {
				player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
			}
			return ActionResult.SUCCESS;
		} else {
			return ActionResult.PASS;
		}

	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntityDistiller(pos, state);
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (state.getBlock() != newState.getBlock()) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof BlockEntityDistiller) {
				ItemScatterer.spawn(world, pos, (Inventory) blockEntity);
			}

			super.onStateReplaced(state, world, pos, newState, moved);
		}
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof BlockEntityDistiller) {
			int capacity = ((BlockEntityDistiller) be).getTankSize();
			int filled = ((BlockEntityDistiller) be).getAlkahest();
			double fillLevel = (double) filled / capacity;
			if (fillLevel == 0.0) {
				return 0;
			} else {
				return 1 + (int) (fillLevel * 14);
			}
		} else {
			return 0;
		}
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return checkType(type, AoABlockEntities.DISTILLER, (world2, pos, state2, entity) -> ((BlockEntityDistiller) entity).tick(world2, pos, state2, (BlockEntityDistiller) entity));
	}

}
