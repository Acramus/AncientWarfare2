package net.shadowmage.ancientwarfare.structure.proxy;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.proxy.ClientProxyBase;
import net.shadowmage.ancientwarfare.structure.block.AWStructuresBlocks;
import net.shadowmage.ancientwarfare.structure.entity.EntityGate;
import net.shadowmage.ancientwarfare.structure.event.StructureBoundingBoxRenderer;
import net.shadowmage.ancientwarfare.structure.gui.GuiGateControl;
import net.shadowmage.ancientwarfare.structure.render.DraftingStationRenderer;
import net.shadowmage.ancientwarfare.structure.render.RenderGateHelper;
import net.shadowmage.ancientwarfare.structure.tile.TileSoundBlock;

@SideOnly(Side.CLIENT)
public class ClientProxyStructures extends ClientProxyBase {

	@Override
	public void preInit() {
		super.preInit();

		NetworkHandler.registerGui(NetworkHandler.GUI_GATE_CONTROL, GuiGateControl.class);
		MinecraftForge.EVENT_BUS.register(StructureBoundingBoxRenderer.INSTANCE);
		MinecraftForge.EVENT_BUS.register(this);

		RenderingRegistry.registerEntityRenderingHandler(EntityGate.class, RenderGateHelper::new);
	}

	@Override
	public void init() {
		super.init();

		Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler((state, world, pos, tintIndex) -> {
			TileEntity tileEntity = world.getTileEntity(pos);
			IBlockState disguiseState = Blocks.JUKEBOX.getDefaultState();
			if (tileEntity instanceof TileSoundBlock) {
				IBlockState tileState = ((TileSoundBlock) tileEntity).getDisguiseState();
				if (tileState != null) {
					disguiseState = tileState;
				}
			}
			return Minecraft.getMinecraft().getBlockColors().colorMultiplier(disguiseState, world, pos, 0);
		}, AWStructuresBlocks.soundBlock);
	}

	@SubscribeEvent
	public void onPreTextureStitch(TextureStitchEvent.Pre evt) {
		DraftingStationRenderer.INSTANCE.setSprite(evt.getMap().registerSprite(new ResourceLocation(AncientWarfareCore.modID + ":model/structure/tile_drafting_station")));
	}
}
