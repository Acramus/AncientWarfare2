package net.shadowmage.ancientwarfare.automation.tile.warehouse2;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.util.INBTSerializable;
import net.shadowmage.ancientwarfare.automation.container.ContainerWarehouseStockViewer;
import net.shadowmage.ancientwarfare.core.interfaces.IInteractableTile;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.owner.IOwnable;
import net.shadowmage.ancientwarfare.core.owner.Owner;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.NBTSerializableUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TileWarehouseStockViewer extends TileControlled implements IOwnable, IInteractableTile {

	private static final String FILTER_LIST_TAG = "filterList";
	private final List<WarehouseStockFilter> filters = new ArrayList<>();
	private Owner owner = Owner.EMPTY;
	private boolean shouldUpdate = false;

	private final Set<ContainerWarehouseStockViewer> viewers = new HashSet<>();

	private void updateViewers() {
		for (ContainerWarehouseStockViewer viewer : viewers) {
			viewer.onFiltersChanged();
		}
	}

	public void addViewer(ContainerWarehouseStockViewer viewer) {
		viewers.add(viewer);
	}

	public void removeViewer(ContainerWarehouseStockViewer viewer) {
		viewers.remove(viewer);
	}

	public List<WarehouseStockFilter> getFilters() {
		return filters;
	}

	public void setFilters(List<WarehouseStockFilter> filters) {
		this.filters.clear();
		this.filters.addAll(filters);
		shouldUpdate = false;//set to false, as we are manually updating right now
		recountFilters(false);//recount filters, do not send update
		BlockTools.notifyBlockUpdate(this); //to re-send description packet to client with new filters
	}

	/*
	 * should be called whenever controller tile is set or warehouse inventory updated
	 */
	private void recountFilters(boolean sendToClients) {
		TileWarehouseBase twb = (TileWarehouseBase) getController();
		int count;
		int index = 0;
		if (twb == null) {
			count = 0;
			for (WarehouseStockFilter filter : this.filters) {
				if (count != filter.getQuantity()) {
					filter.setQuantity(0);
					if (sendToClients) {
						world.addBlockEvent(pos, getBlockType(), index, count);
					}
				}
				index++;
			}
		} else {
			for (WarehouseStockFilter filter : this.filters) {
				count = filter.getFilterItem().isEmpty() ? 0 : twb.getCountOf(filter.getFilterItem());
				if (count != filter.getQuantity()) {
					filter.setQuantity(count);
					if (sendToClients) {
						world.addBlockEvent(pos, getBlockType(), index, count);
					}
				}
				index++;
			}
		}
	}

	@Override
	public boolean isValidController(IControllerTile tile) {
		return BlockTools.isPositionWithinBounds(getPos(), tile.getWorkBoundsMin().add(-1, 0, -1), tile.getWorkBoundsMax().add(1, 0, 1));
	}

	@Override
	protected void onControllerChanged(IControllerTile oldController, IControllerTile newController) {
		onWarehouseInventoryUpdated();
	}

	@Override
	public boolean isOwner(EntityPlayer player) {
		return owner.isOwnerOrSameTeamOrFriend(player);
	}

	@Override
	public void setOwner(EntityPlayer player) {
		owner = new Owner(player);
	}

	@Override
	public void setOwner(Owner owner) {
		this.owner = owner;
	}

	@Override
	public Owner getOwner() {
		return owner;
	}

	@Override
	public boolean onBlockClicked(EntityPlayer player, @Nullable EnumHand hand) {
		if (!player.world.isRemote && isOwner(player)) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WAREHOUSE_STOCK, pos);
		}
		return true;
	}

	@Override
	protected void updateTile() {
		if (shouldUpdate) {
			shouldUpdate = false;
			recountFilters(true);
		}
	}

	/*
	 * should be called on SERVER whenever warehouse inventory changes
	 */
	void onWarehouseInventoryUpdated() {
		shouldUpdate = true;
	}

	@Override
	protected void writeUpdateNBT(NBTTagCompound tag) {
		super.writeUpdateNBT(tag);
		NBTSerializableUtils.write(tag, FILTER_LIST_TAG, filters);
	}

	@Override
	protected void handleUpdateNBT(NBTTagCompound tag) {
		super.handleUpdateNBT(tag);
		this.filters.clear();
		this.filters.addAll(NBTSerializableUtils.read(tag, TileWarehouseStockViewer.FILTER_LIST_TAG, WarehouseStockFilter.class));
		updateViewers();
	}

	@Override
	public boolean receiveClientEvent(int a, int b) {
		if (world.isRemote && a >= 0 && a < filters.size()) {
			filters.get(a).setQuantity(b);
			updateViewers();
		}
		return true;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		filters.addAll(NBTSerializableUtils.read(tag, TileWarehouseStockViewer.FILTER_LIST_TAG, WarehouseStockFilter.class));
		owner = Owner.deserializeFromNBT(tag);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		NBTSerializableUtils.write(tag, TileWarehouseStockViewer.FILTER_LIST_TAG, filters);
		owner.serializeToNBT(tag);

		return tag;
	}

	public static class WarehouseStockFilter implements INBTSerializable<NBTTagCompound> {
		private static final String ITEM_TAG = "item";
		private static final String QUANTITY_TAG = "quantity";
		@Nonnull
		private ItemStack item = ItemStack.EMPTY;
		private int quantity;

		//TODO see if reflection above that uses this constructor can be replaced
		public WarehouseStockFilter() {
		}

		public WarehouseStockFilter(ItemStack item, int qty) {
			setQuantity(qty);
			setItem(item);
		}

		public void setItem(ItemStack item) {
			this.item = item;
		}

		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}

		public ItemStack getFilterItem() {
			return item;
		}

		public int getQuantity() {
			return quantity;
		}

		@Override
		public NBTTagCompound serializeNBT() {
			NBTTagCompound tag = new NBTTagCompound();
			if (!item.isEmpty()) {
				tag.setTag(ITEM_TAG, item.writeToNBT(new NBTTagCompound()));
			}
			tag.setInteger(QUANTITY_TAG, quantity);
			return tag;
		}

		@Override
		public void deserializeNBT(NBTTagCompound tag) {
			setItem(tag.hasKey(ITEM_TAG) ? new ItemStack(tag.getCompoundTag(ITEM_TAG)) : ItemStack.EMPTY);
			setQuantity(tag.getInteger(QUANTITY_TAG));
		}
	}
}
