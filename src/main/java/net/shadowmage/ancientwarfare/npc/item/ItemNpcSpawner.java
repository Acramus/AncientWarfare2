package net.shadowmage.ancientwarfare.npc.item;

import com.google.common.collect.Maps;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.npc.entity.AWNPCEntityLoader;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.NpcPlayerOwned;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;
import net.shadowmage.ancientwarfare.npc.registry.FactionDefinition;
import net.shadowmage.ancientwarfare.npc.registry.FactionRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ItemNpcSpawner extends ItemBaseNPC {

	public ItemNpcSpawner() {
		super("npc_spawner");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		tooltip.add(I18n.format("guistrings.npc.spawner.right_click_to_place"));
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		String npcName = getNpcType(stack);
		if (npcName != null) {
			npcName = npcName.replace("faction.", "");
			String npcSub = getNpcSubtype(stack);
			if (!npcSub.isEmpty()) {
				npcName = npcName + "." + npcSub;
			}
			return "entity.ancientwarfarenpc." + (getFaction(stack).map(s -> s + ".").orElse("")) + npcName;
		}
		return super.getUnlocalizedName(stack);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (world.isRemote) {
			return new ActionResult<>(EnumActionResult.SUCCESS, stack);
		}
		BlockPos hit = BlockTools.getBlockClickedOn(player, player.world, true);
		if (hit == null) {
			return new ActionResult<>(EnumActionResult.PASS, stack);
		}
		NpcBase npc = createNpcFromItem(player.world, stack);
		if (npc != null) {
			if (npc instanceof NpcPlayerOwned) {
				npc.setOwner(player);
			}
			npc.setPosition(hit.getX() + 0.5d, hit.getY(), hit.getZ() + 0.5d);
			npc.setHomeAreaAtCurrentPosition();
			player.world.spawnEntity(npc);
			if (!player.capabilities.isCreativeMode) {
				stack.shrink(1);
			}
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
	}

	/*
	 * create an NPC from the input item stack if valid, else return null<br>
	 * npc will have type, subtype, equipment, levels, health, food and owner set from item.
	 */
	public static NpcBase createNpcFromItem(World world, ItemStack stack) {
		String type = getNpcType(stack);
		if (type == null) {
			return null;
		}
		String subType = getNpcSubtype(stack);
		Optional<String> faction = getFaction(stack);
		NpcBase npc = AWNPCEntityLoader.createNpc(world, type, subType, faction.orElse(""));
		if (npc == null) {
			return null;
		}
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("npcStoredData")) {
			for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
				npc.setItemStackToSlot(slot, ItemStack.EMPTY);
			}
			npc.readAdditionalItemData(stack.getTagCompound().getCompoundTag("npcStoredData"));
		}
		return npc;
	}

	/*
	 * return an itemstack of npc spawner item that contains the data to spawn the input npc<br>
	 * npc type, subtype, equipment, levels health, food value, and owner will be stored.
	 */
	public static ItemStack getSpawnerItemForNpc(NpcBase npc) {
		String type = npc.getNpcType();
		String sub = npc.getNpcSubType();
		ItemStack stack = npc instanceof NpcFaction ? getStackForNpcType("faction." + type, sub, ((NpcFaction) npc).getFaction()) : getStackForNpcType(type, sub);
		NBTTagCompound tag = new NBTTagCompound();
		npc.writeAdditionalItemData(tag);
		stack.setTagInfo("npcStoredData", tag);
		return stack;
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (!isInCreativeTab(tab)) {
			return;
		}
		getSpawnerSubItems(items);
	}

	public static void getSpawnerSubItems(NonNullList<ItemStack> list) {
		for (AWNPCEntityLoader.NpcDeclaration dec : AWNPCEntityLoader.getNpcMap().values()) {
			if (dec.canSpawnBaseEntity()) {
				if (dec.getNpcType().startsWith("faction.")) {
					for (FactionDefinition faction : FactionRegistry.getFactions())
						list.add(getStackForNpcType(dec.getNpcType(), "", faction.getName()));
				} else {
					list.add(getStackForNpcType(dec.getNpcType(), "", ""));
				}
			}
			for (String sub : dec.getSubTypes()) {
				list.add(getStackForNpcType(dec.getNpcType(), sub));
			}
		}
	}

	public static ItemStack getStackForNpcType(String type, String npcSubtype) {
		return getStackForNpcType(type, npcSubtype, "");
	}

	public static ItemStack getStackForNpcType(String type, String npcSubtype, String faction) {
		@Nonnull ItemStack stack = new ItemStack(AWNPCItems.npcSpawner);
		stack.setTagInfo("npcType", new NBTTagString(type));
		stack.setTagInfo("npcSubtype", new NBTTagString(npcSubtype));
		if (!faction.isEmpty()) {
			stack.setTagInfo("faction", new NBTTagString(faction));
		}
		return stack;
	}

	@Nullable
	public static String getNpcType(ItemStack stack) {
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("npcType")) {
			return stack.getTagCompound().getString("npcType");
		}
		return null;
	}

	private static String getNpcSubtype(ItemStack stack) {
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("npcSubtype")) {
			return stack.getTagCompound().getString("npcSubtype");
		}
		return "";
	}

	public static Optional<String> getFaction(ItemStack stack) {
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("faction")) {
			return Optional.of(stack.getTagCompound().getString("faction"));
		}
		return Optional.empty();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerClient() {

		final Map<String, ModelResourceLocation> modelLocations = Maps.newHashMap();

		AWNPCEntityLoader.getNpcMap().values().stream().map(AWNPCEntityLoader.NpcDeclaration::getItemModelVariants).flatMap(Collection::stream).distinct()
				.forEach(v -> {
			modelLocations.put(v, getModelLocation(v));
			ModelLoader.registerItemVariants(this, modelLocations.get(v));
		});

		ModelLoader.setCustomMeshDefinition(this, stack -> {
			String npcType = getNpcType(stack);
			if (npcType == null) {
				npcType = "worker";
			}
			AWNPCEntityLoader.NpcDeclaration dec = AWNPCEntityLoader.getNpcDeclaration(npcType);
			return dec == null ? null : modelLocations.get(dec.getItemModelVariant(getNpcSubtype(stack)));
		});
	}

	private ModelResourceLocation getModelLocation(String modelVariant) {
		return new ModelResourceLocation(AncientWarfareCore.modID + ":npc/npc_spawner", "variant=" + modelVariant);
	}
}
