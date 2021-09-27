package com.cumulusmc.artofalchemy.blockentity;

import com.cumulusmc.artofalchemy.ArtOfAlchemy;
import com.cumulusmc.artofalchemy.block.AoABlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.registry.Registry;

public class AoABlockEntities {

	public static final BlockEntityType<?> CALCINATOR = FabricBlockEntityTypeBuilder.create(BlockEntityCalcinator::new, AoABlocks.CALCINATOR).build(null);
	public static final BlockEntityType<?> CALCINATOR_PLUS = FabricBlockEntityTypeBuilder.create(BlockEntityCalcinatorPlus::new, AoABlocks.CALCINATOR_PLUS).build(null);
	public static final BlockEntityType<?> DISSOLVER = FabricBlockEntityTypeBuilder.create(BlockEntityDissolver::new, AoABlocks.DISSOLVER).build(null);
	public static final BlockEntityType<?> DISSOLVER_PLUS = FabricBlockEntityTypeBuilder.create(BlockEntityDissolverPlus::new, AoABlocks.DISSOLVER_PLUS).build(null);
	public static final BlockEntityType<?> DISTILLER = FabricBlockEntityTypeBuilder.create(BlockEntityDistiller::new, AoABlocks.DISTILLER).build(null);
	public static final BlockEntityType<?> SYNTHESIZER = FabricBlockEntityTypeBuilder.create(BlockEntitySynthesizer::new, AoABlocks.SYNTHESIZER).build(null);
	public static final BlockEntityType<?> SYNTHESIZER_PLUS = FabricBlockEntityTypeBuilder.create(BlockEntitySynthesizerPlus::new, AoABlocks.SYNTHESIZER_PLUS).build(null);
	public static final BlockEntityType<?> PROJECTOR = FabricBlockEntityTypeBuilder.create(BlockEntityProjector::new, AoABlocks.PROJECTOR).build(null);
	public static final BlockEntityType<BlockEntityTank> TANK = FabricBlockEntityTypeBuilder.create(BlockEntityTank::new, AoABlocks.TANK).build(null);
	public static final BlockEntityType<?> ASTRO_CENTRIFUGE = FabricBlockEntityTypeBuilder.create(BlockEntityAstroCentrifuge::new, AoABlocks.ASTRO_CENTRIFUGE).build(null);
	public static final BlockEntityType<?> ELEMENT_CENTRIFUGE = FabricBlockEntityTypeBuilder.create(BlockEntityElementCentrifuge::new, AoABlocks.ELEMENT_CENTRIFUGE).build(null);
	public static final BlockEntityType<?> PIPE = FabricBlockEntityTypeBuilder.create(BlockEntityPipe::new, AoABlocks.PIPE).build(null);

	public static void registerBlockEntities() {
		register("calcination_furnace", CALCINATOR);
		register("calcination_furnace_plus", CALCINATOR_PLUS);
		register("dissolution_chamber", DISSOLVER);
		register("dissolution_chamber_plus", DISSOLVER_PLUS);
		register("distillation_aparatus", DISTILLER);
		register("synthesis_table", SYNTHESIZER);
		register("synthesis_table_plus", SYNTHESIZER_PLUS);
		register("projector", PROJECTOR);
		register("essentia_tank", TANK);
		register("astral_centrifuge", ASTRO_CENTRIFUGE);
		register("elemental_centrifuge", ELEMENT_CENTRIFUGE);
		register("pipe", PIPE);
	}

	public static void register(String name, BlockEntityType<? extends BlockEntity> blockEntity) {
		Registry.register(Registry.BLOCK_ENTITY_TYPE, ArtOfAlchemy.id(name), blockEntity);
	}

}
