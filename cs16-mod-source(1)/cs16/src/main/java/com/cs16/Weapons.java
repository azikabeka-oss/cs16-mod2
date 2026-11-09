package com.cs16;

import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public class Weapons {
	public static final Map<String, Gun> ALL = new LinkedHashMap<>();
	public static Item KNIFE;

	static void init() {
		RegistryKey<Item> knifeKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("cs16", "knife"));
		KNIFE = Registry.register(Registries.ITEM, knifeKey, new Knife(
			new Item.Settings().registryKey(knifeKey).maxCount(1).sword(ToolMaterial.IRON, 3.0F, -2.4F)));

		//  id          урон кд дальн цена  обойма запас перезарядка(тики)
		reg("glock18",  5,   6,  50,  400,  20, 120, 40);
		reg("usp",      6,   8,  60,  500,  12, 100, 40);
		reg("p228",     6,   8,  60,  600,  13,  52, 40);
		reg("fiveseven",5,   6,  60,  750,  20, 100, 40);
		reg("elite",    5,   4,  50,  800,  30, 120, 50);
		reg("deagle",   12, 14,  80,  650,   7,  35, 45);
		reg("tmp",      3,   2,  40, 1250,  30, 120, 45);
		reg("mac10",    4,   2,  40, 1400,  30, 100, 45);
		reg("mp5",      4,   2,  50, 1500,  30, 120, 45);
		reg("ump45",    5,   3,  50, 1700,  25, 100, 50);
		reg("m3",       10, 20,  25, 1700,   8,  32, 45);
		reg("galil",    6,   3,  90, 2000,  35,  90, 50);
		reg("famas",    6,   3,  90, 2250,  25,  90, 50);
		reg("p90",      5,   2,  60, 2350,  50, 100, 60);
		reg("ak47",     7,   3, 100, 2500,  30,  90, 50);
		reg("scout",    14, 25, 150, 2750,  10,  90, 50);
		reg("xm1014",   8,   8,  25, 3000,   7,  32, 45);
		reg("m4a1",     6,   3, 100, 3100,  30,  90, 50);
		reg("sg552",    6,   3, 120, 3500,  30,  90, 50);
		reg("aug",      6,   3, 120, 3500,  30,  90, 50);
		reg("sg550",    14, 15, 160, 4200,  30,  90, 60);
		reg("awp",      30, 40, 200, 4750,  10,  30, 50);
		reg("g3sg1",    14, 12, 160, 5000,  20,  90, 60);
		reg("m249",     7,   2, 100, 5750, 100, 200, 90);
	}

	private static void reg(String id, float dmg, int cd, int range, int price, int mag, int reserve, int reload) {
		Identifier name = Identifier.of("cs16", id);
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, name);
		Gun gun = new Gun(new Item.Settings().registryKey(key).maxCount(1),
				id, dmg, cd, range, price, mag, reserve, reload);
		Registry.register(Registries.ITEM, key, gun);
		ALL.put(id, gun);
	}
}
