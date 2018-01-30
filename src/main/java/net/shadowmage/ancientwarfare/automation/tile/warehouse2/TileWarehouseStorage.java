package net.shadowmage.ancientwarfare.automation.tile.warehouse2;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.shadowmage.ancientwarfare.automation.container.ContainerWarehouseStorage;
import net.shadowmage.ancientwarfare.core.interfaces.IInteractableTile;
import net.shadowmage.ancientwarfare.core.inventory.InventorySlotlessBasic;
import net.shadowmage.ancientwarfare.core.inventory.ItemQuantityMap;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.tile.IBlockBreakHandler;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.NBTSerializableUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TileWarehouseStorage extends TileControlled implements IWarehouseStorageTile, IInteractableTile, IBlockBreakHandler {

	private InventorySlotlessBasic inventory;
	private final List<WarehouseStorageFilter> filters = new ArrayList<>();

	private final Set<ContainerWarehouseStorage> viewers = new HashSet<>();

	public TileWarehouseStorage() {
		inventory = new InventorySlotlessBasic(getStorageAdditionSize());
	}

	@Override
	public ItemStack tryAdd(ItemStack cursorStack) {
		int moved = insertItem(cursorStack, cursorStack.getCount());
		TileWarehouseBase twb = (TileWarehouseBase) getController();
		if (twb != null) {
			ItemStack filter = cursorStack.copy();
			filter.setCount(1);
			twb.changeCachedQuantity(filter, moved);
		}
		cursorStack.shrink(moved);
		if (cursorStack.getCount() <= 0) {
			return ItemStack.EMPTY;
		}
		return cursorStack;
	}

	@Override
	protected void updateTile() {

	}

	@Override
	public void onBlockBroken() {
		ItemQuantityMap qtm = new ItemQuantityMap();
		addItems(qtm);
		NonNullList<ItemStack> list = qtm.getItems();
		for (ItemStack stack : list) {
			InventoryTools.dropItemInWorld(world, stack, pos);
		}
	}

	@Override
	public int getStorageAdditionSize() {
		return 9 * 64;
	}

	@Override
	public void onWarehouseInventoryUpdated(TileWarehouseBase warehouse) {

	}

	@Override
	public List<WarehouseStorageFilter> getFilters() {
		return filters;
	}

	@Override
	public void setFilters(List<WarehouseStorageFilter> filters) {
		List<WarehouseStorageFilter> old = new ArrayList<>();
		old.addAll(this.filters);
		this.filters.clear();
		this.filters.addAll(filters);
		if (this.getController() != null) {
			((TileWarehouseBase) this.getController()).onStorageFilterChanged(this, old, this.filters);
		}
		updateViewers();
		markDirty();
	}

	@Override
	public void addItems(ItemQuantityMap map) {
		inventory.getItems(map);
	}

	@Override
	public int getQuantityStored(ItemStack filter) {
		return inventory.getQuantityStored(filter);
	}

	@Override
	public int getAvailableSpaceFor(ItemStack filter) {
		return inventory.getAvailableSpaceFor(filter);
	}

	@Override
	public int extractItem(ItemStack filter, int amount) {
		int removed = inventory.extractItem(filter, amount);
		updateViewersForInventory();
		return removed;
	}

	@Override
	public int insertItem(ItemStack filter, int amount) {
		int inserted = inventory.insertItem(filter, amount);
		updateViewersForInventory();
		return inserted;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		inventory.readFromNBT(tag.getCompoundTag("inventory"));
		filters.addAll(NBTSerializableUtils.read(tag, "filterList", WarehouseStorageFilter.class));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setTag("inventory", inventory.writeToNBT(new NBTTagCompound()));
		NBTSerializableUtils.write(tag, "filterList", filters);
		return tag;
	}

	@Override
	public void addViewer(ContainerWarehouseStorage containerWarehouseStorage) {
		if (!hasWorld() || world.isRemote) {
			return;
		}
		viewers.add(containerWarehouseStorage);
	}

	@Override
	public void removeViewer(ContainerWarehouseStorage containerWarehouseStorage) {
		viewers.remove(containerWarehouseStorage);
	}

	protected void updateViewers() {
		for (ContainerWarehouseStorage viewer : viewers) {
			viewer.onFilterListUpdated();
		}
	}

	protected void updateViewersForInventory() {
		for (ContainerWarehouseStorage viewer : viewers) {
			viewer.onStorageInventoryUpdated();
		}
		markDirty();
	}

	@Override
	public boolean onBlockClicked(EntityPlayer player, @Nullable EnumHand hand) {
		if (!player.world.isRemote) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WAREHOUSE_STORAGE, pos);
		}
		return true;
	}

	@Override
	public void handleSlotClick(EntityPlayer player, ItemStack filter, boolean shiftClick) {
		if (!filter.isEmpty() && player.inventory.getItemStack().isEmpty()) {
			tryGetItem(player, filter, shiftClick);
		} else if (!player.inventory.getItemStack().isEmpty()) {
			tryAddItem(player, player.inventory.getItemStack());
		}
	}

	private void tryAddItem(EntityPlayer player, ItemStack cursorStack) {
		int stackSize = cursorStack.getCount();
		int moved;
		moved = insertItem(cursorStack, cursorStack.getCount());
		TileWarehouseBase twb = (TileWarehouseBase) getController();
		if (twb != null) {
			ItemStack filter = cursorStack.copy();
			filter.setCount(1);
			twb.changeCachedQuantity(filter, moved);
		}
		cursorStack.shrink(moved);
		if (cursorStack.getCount() <= 0) {
			player.inventory.setItemStack(ItemStack.EMPTY);
		}
		if (stackSize != cursorStack.getCount()) {
			((EntityPlayerMP) player).updateHeldItem();
		}
	}

	private void tryGetItem(EntityPlayer player, ItemStack filter, boolean shiftClick) {
		@Nonnull ItemStack newCursorStack = filter.copy();
		newCursorStack.setCount(0);
		int count;
		int toMove;
		count = getQuantityStored(filter);
		toMove = newCursorStack.getMaxStackSize() - newCursorStack.getCount();
		toMove = toMove > count ? count : toMove;
		if (toMove > 0) {
			newCursorStack.grow(toMove);
			extractItem(filter, toMove);
			TileWarehouseBase twb = (TileWarehouseBase) getController();
			if (twb != null) {
				twb.changeCachedQuantity(filter, -toMove);
			}
		}
		InventoryTools.updateCursorItem((EntityPlayerMP) player, newCursorStack, shiftClick);
	}
}
