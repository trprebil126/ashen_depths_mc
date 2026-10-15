package com.ashendepths;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;

public final class ModContent {
	public static final Block EMBERITE_ORE = registerBlock(
			"emberite_ore",
			Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_GOLD_ORE)
					.mapColor(MapColor.COLOR_BLACK)
					.strength(3.0F, 6.0F)
					.requiresCorrectToolForDrops()
	);

	public static final Block EMBERITE_BLOCK = registerBlock(
			"emberite_block",
			Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
					.mapColor(MapColor.COLOR_BLACK)
					.sound(SoundType.NETHERITE_BLOCK)
	);

	public static final Block EMBERITE_PORTAL = registerBlockWithoutItem(
			"emberite_portal",
			EmberitePortalBlock::new,
			BlockBehaviour.Properties.of()
					.noCollision()
					.strength(-1.0F)
					.noLootTable()
					.lightLevel(state -> 11)
	);

	public static final Item RAW_EMBERITE = registerItem("raw_emberite", Item::new, new Item.Properties());
	public static final Item EMBERITE_INGOT = registerItem("emberite_ingot", Item::new, new Item.Properties());

	private ModContent() {
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
			entries.accept(RAW_EMBERITE);
			entries.accept(EMBERITE_INGOT);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
			entries.accept(EMBERITE_ORE);
			entries.accept(EMBERITE_BLOCK);
		});
		ResourceKey<PlacedFeature> emberiteOreFeature =
				ResourceKey.create(Registries.PLACED_FEATURE, AshenDepths.id("emberite_ore"));
		BiomeModifications.addFeature(
				BiomeSelectors.includeByKey(Biomes.BASALT_DELTAS),
				GenerationStep.Decoration.UNDERGROUND_ORES,
				emberiteOreFeature
		);
		EmberitePortalBlock.registerActivationEvent();
	}

	private static Block registerBlock(String path, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		Identifier id = AshenDepths.id(path);
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = Registry.register(
				BuiltInRegistries.BLOCK,
				blockKey,
				factory.apply(properties.setId(blockKey))
		);

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BlockItem blockItem = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey));
		Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
		return block;
	}

	private static Block registerBlockWithoutItem(String path, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, AshenDepths.id(path));
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
	}

	private static Item registerItem(String path, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, AshenDepths.id(path));
		return Registry.register(BuiltInRegistries.ITEM, itemKey, factory.apply(properties.setId(itemKey)));
	}
}
