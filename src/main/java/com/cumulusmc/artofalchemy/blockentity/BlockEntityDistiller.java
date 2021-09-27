package com.cumulusmc.artofalchemy.blockentity;

import io.github.cottonmc.cotton.gui.PropertyDelegateHolder;
import com.cumulusmc.artofalchemy.AoAConfig;
import com.cumulusmc.artofalchemy.ArtOfAlchemy;
import com.cumulusmc.artofalchemy.block.BlockDissolver;
import com.cumulusmc.artofalchemy.essentia.EssentiaContainer;
import com.cumulusmc.artofalchemy.gui.handler.HandlerDistiller;
import com.cumulusmc.artofalchemy.item.AoAItems;
import com.cumulusmc.artofalchemy.transport.HasAlkahest;
import com.cumulusmc.artofalchemy.transport.HasEssentia;
import com.cumulusmc.artofalchemy.util.ImplementedInventory;
import com.cumulusmc.artofalchemy.util.FuelHelper;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class BlockEntityDistiller extends BlockEntity implements ImplementedInventory, BlockEntityTicker<BlockEntityDistiller>, PropertyDelegateHolder,
		BlockEntityClientSerializable, HasEssentia, HasAlkahest, SidedInventory, ExtendedScreenHandlerFactory {

	private static final int[] TOP_SLOTS = new int[]{0};
	private static final int[] BOTTOM_SLOTS = new int[]{0};
	private static final int[] SIDE_SLOTS = new int[]{0};
	
	// Constant TODO: Allow config
	private static final int TANK_MAX = 16000;
	private static final int PROGRESS_MAX = 100;
	private static final int DISTILL_GAIN = 1000;
	private static final int DISTILL_ESSENTIA_COST = 1200;
	private static final int DISTILL_AZOTH_COST = 1;
	private static final int SLOT_AZOTH = 0;
	private static final int SLOT_FUEL = 1;
	
	// Settable
	private int tankSize;
	private float speedMod;
	private float yield;
	
	
	protected int progress = 0;
	protected int fuel = 0;
	private int essentia = 0;
	private int alkahest = 0;
	
	private boolean lit = false;

	protected final DefaultedList<ItemStack> items = DefaultedList.ofSize(2, ItemStack.EMPTY);
	protected EssentiaContainer essentiaInput;
	protected final PropertyDelegate delegate = new PropertyDelegate() {
		@Override
		public int size() {
			return 5;
		}

		@Override
		public void set(int index, int value) {
		}

		@Override
		public int get(int index) {
			switch(index) {
			case 0:
				return progress;
			case 1:
				return PROGRESS_MAX;
			case 2:
				return fuel;
			case 3:
				return 20; // Fuel Indicator
			case 4:
				return essentia;
			case 5:
				return alkahest;
			case 6:
				return tankSize;
			default:
				return 0;
			}
		}

	};

	public BlockEntityDistiller(BlockPos pos, BlockState state) {
		this(AoABlockEntities.DISTILLER, pos, state);
		AoAConfig.DissolverSettings settings = AoAConfig.get().dissolverSettings;
		tankSize = TANK_MAX; //settings.tankBasic;
		speedMod = settings.speedBasic;
		this.yield = settings.yieldBasic;
		
		essentiaInput = new EssentiaContainer()
			.setCapacity(tankSize - essentia)
			.setInput(true)
			.setOutput(false);
	}

	protected BlockEntityDistiller(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
		return new HandlerDistiller(syncId, inv, ScreenHandlerContext.create(world, pos));
	}

	@Override
	public Text getDisplayName() {
		return new LiteralText("");
	}

	@Override
	public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
		buf.writeBlockPos(pos);
	}

	@Override
	public EssentiaContainer getContainer(Direction dir) {
		return getContainer(0);
	}

	@Override
	public EssentiaContainer getContainer(int id) {
		if (id == 0) {
			return essentiaInput;
		} else {
			return null;
		}
	}

	@Override
	public int getNumContainers() {
		return 2;
	}

	@Override
	public int getAlkahest() {
		return alkahest;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound tag) {
		tag.putInt("progress", progress);
		tag.putInt("fuel", fuel);
		tag.putInt("essentia", essentia);
		tag.putInt("alkahest", alkahest);
		tag.put("essentiaInput", essentiaInput.writeNbt());
		Inventories.writeNbt(tag, items);
		return super.writeNbt(tag);
	}

	@Override
	public void readNbt(NbtCompound tag) {
		super.readNbt(tag);
		Inventories.readNbt(tag, items);
		progress = tag.getInt("progress");
		fuel = tag.getInt("fuel");
		essentia = tag.getInt("essentia");
		alkahest = tag.getInt("alkahest");
		essentiaInput = new EssentiaContainer(tag.getCompound("essentiaInput"));
	}

	@Override
	public DefaultedList<ItemStack> getItems() {
		return items;
	}

	@Override
	public boolean isValid(int slot, ItemStack stack) {
		switch (slot) {
			case 0:
				return stack.isOf(AoAItems.AZOTH);
			case 1:
				return FuelHelper.isFuel(stack);
			default:
				return false;
		}
	}


	@Override
	public void tick(World world, BlockPos pos, BlockState state, BlockEntityDistiller blockEntity) {
		if (!world.isClient()) {
			tryConvertEssentia();
			if (fuel > 0) fuel -= 2;
			if (hasFuel() && hasInput() && !isFull()) {
				if (!lit) setLit(true);
				
				if (++progress >= PROGRESS_MAX) {
					progress = 0;
					distill();
				}
			} else {
				if (progress > 0) progress--;
				if (lit) setLit(true);
			}
			sync(); // KG: Maybe this is laggy?
		}
	}
	private void tryConvertEssentia() { // KG: Not clean, could have issues if this function is missed for a tick.
		if (!essentiaInput.isEmpty()) {
			essentia += essentiaInput.getCount();
			essentiaInput.emptyContents();
			essentiaInput.setCapacity(TANK_MAX - essentia);
		}
	}
	private void setLit(boolean lit) {
		this.lit = lit;
		updateLit();
	}
	private void updateLit() {
		world.setBlockState(pos, world.getBlockState(pos).with(BlockDissolver.LIT, lit));
	}
	// Assumes prerequisites have been met
	private void distill() {
		if (hasEnoughEssentia()) essentia -= DISTILL_ESSENTIA_COST;
		else if (hasAzoth()) items.get(SLOT_AZOTH).decrement(1);
		// Else: Throw?
		alkahest += DISTILL_GAIN;
	}
	
	private boolean isFull() {
		return this.alkahest > (this.tankSize - DISTILL_GAIN);
	}
	private boolean hasInput() {
		return hasEnoughEssentia() || hasAzoth();
	}
	private boolean hasEnoughEssentia() {
		return this.essentia >= DISTILL_ESSENTIA_COST;
	}
	private boolean hasAzoth() {
		ItemStack azothSlot = items.get(SLOT_AZOTH);
		return azothSlot.getItem().equals(AoAItems.AZOTH) && azothSlot.getCount() >= DISTILL_AZOTH_COST;
	}
	
	private boolean hasFuel() {
		if (this.fuel <= 0) {
			ItemStack fuelSlot = items.get(SLOT_FUEL);
			if (FuelHelper.isFuel(fuelSlot)) {
				this.fuel = FuelHelper.fuelTime(fuelSlot);
				fuelSlot.decrement(1);
			}
			else return false;
		}
		return true;
	}

	@Override
	public PropertyDelegate getPropertyDelegate() {
		return delegate;
	}

	@Override
	public void markDirty() {
		super.markDirty();
		if (!world.isClient()) {
			sync();
		}
	}

	@Override
	public void sync() {
		//AoANetworking.sendEssentiaPacket(world, pos, 0, essentiaInput);
		BlockEntityClientSerializable.super.sync();
	}

	@Override
	public void fromClientTag(NbtCompound tag) {
		readNbt(tag);
	}

	@Override
	public NbtCompound toClientTag(NbtCompound tag) {
		return writeNbt(tag);
	}

	@Override
	public int[] getAvailableSlots(Direction side) {
		if (side == Direction.UP) {
			return TOP_SLOTS;
		} else if (side == Direction.DOWN) {
			return BOTTOM_SLOTS;
		} else {
			return SIDE_SLOTS;
		}
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack, Direction dir) {
		return isValid(slot, stack);
	}

	@Override
	public boolean canExtract(int slot, ItemStack stack, Direction dir) {
		if (dir == Direction.DOWN) {
			return TagRegistry.item(ArtOfAlchemy.id("containers")).contains(stack.getItem());
		} else {
			return true;
		}
	}
	
	public int getTankSize() {
		return tankSize;
	}

	public float getSpeedMod() {
		return speedMod;
	}

	public float getEfficiency() {
		return yield;
	}

	@Override
	public boolean setAlkahest(int amount) {
		return false; // Alkahest is output
	}
	public boolean withdrawAlkahest(int amount) {
		if (this.alkahest > amount) {
			this.alkahest -= amount;
			return true;
		}
		return false; // Alkahest is output
	}
}
