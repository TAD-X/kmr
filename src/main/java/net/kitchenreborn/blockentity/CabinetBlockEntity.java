package net.kitchenreborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.kitchenreborn.block.CabinetBlock;
import net.kitchenreborn.registry.KitchenBlockEntities;

/**
 * The kitchen cabinet's storage: 27 slots, opened like a barrel (the doors swing open while someone has it open).
 * Everything else is vanilla container behaviour: hoppers, comparators and the drop-on-break come with it.
 */
public class CabinetBlockEntity extends RandomizableContainerBlockEntity {
	public static final int SIZE = 27;
	private static final Component DEFAULT_NAME = Component.translatable("container.kitchen.cabinet");

	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			CabinetBlockEntity.this.playSound(state, SoundEvents.BARREL_OPEN);
			CabinetBlockEntity.this.setOpen(state, true);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			CabinetBlockEntity.this.playSound(state, SoundEvents.BARREL_CLOSE);
			CabinetBlockEntity.this.setOpen(state, false);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {
		}

		@Override
		public boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof ChestMenu chest && chest.getContainer() == CabinetBlockEntity.this;
		}
	};

	public CabinetBlockEntity(BlockPos pos, BlockState state) {
		super(KitchenBlockEntities.CABINET, pos, state);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!trySaveLootTable(output)) {
			ContainerHelper.saveAllItems(output, items);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		if (!tryLoadLootTable(input)) {
			ContainerHelper.loadAllItems(input, items);
		}
	}

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected Component getDefaultName() {
		return DEFAULT_NAME;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return ChestMenu.threeRows(containerId, inventory, this);
	}

	@Override
	public void startOpen(ContainerUser user) {
		if (!remove && !user.getLivingEntity().isSpectator()) {
			openers.incrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState(), user.getContainerInteractionRange());
		}
	}

	@Override
	public void stopOpen(ContainerUser user) {
		if (!remove && !user.getLivingEntity().isSpectator()) {
			openers.decrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState());
		}
	}

	public void recheckOpen() {
		if (!remove) {
			openers.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
		}
	}

	private void setOpen(BlockState state, boolean open) {
		if (level != null) {
			level.setBlock(getBlockPos(), state.setValue(CabinetBlock.OPEN, open), 3);
		}
	}

	private void playSound(BlockState state, net.minecraft.sounds.SoundEvent sound) {
		if (level != null) {
			BlockPos at = getBlockPos();
			level.playSound(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, sound, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
		}
	}
}
