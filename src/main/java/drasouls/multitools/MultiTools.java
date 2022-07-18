package drasouls.multitools;

import drasouls.multitools.items.MultitoolToolItem;
import drasouls.multitools.packet.PacketUpdateGNDData;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.registries.*;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;

@ModEntry
public class MultiTools {
    public static GameTexture aimTexture;
    public static GameTexture targetTexture;


    public void init() {
        registerPivelaxe("gold",
                500, 90, 0, 15, 50, 50,
                450, Item.Rarity.COMMON, 0,
                40,
                6, 2, 2f);
        registerPivelaxe("frost",
                475, 100, 0, 16, 50, 50,
                500, Item.Rarity.COMMON, 0,
                60,
                8, 3, 2.1f);
        registerPivelaxe("demonic",
                450, 115, 1, 17, 50, 50,
                600, Item.Rarity.UNCOMMON, 0,
                60,
                10, 4, 2.2f);
        registerPivelaxe("ivy",
                400, 125, 1, 18, 50, 50,
                700, Item.Rarity.UNCOMMON, 0,
                100,
                12, 5, 2.5f);
        registerPivelaxe("tungsten",
                300, 150, 2, 20, 55, 55,
                800, Item.Rarity.RARE, 1,
                160,
                16, 6, 3f);
        registerPivelaxe("ice",
                200, 160, 2, 23, 60, 60,
                800, Item.Rarity.EPIC, 2,
                160,
                24, 8, 3.5f);

        PacketRegistry.registerPacket(PacketUpdateGNDData.class);
    }

    public void initResources() {
        aimTexture = GameTexture.fromFile("other/drs_aim");
        targetTexture = GameTexture.fromFile("other/drs_target");
    }

    public void postInit() {
        RegistryAccessor.getObjectsParallelStream(ObjectRegistry.instance)
                .forEach(ObjectCategories::categorize);
        RegistryAccessor.getTilesParallelStream(TileRegistry.instance)
                .forEach(TileCategories::categorize);
    }


    private void registerPivelaxe(String kind, int animSpeed, int toolDps, int toolTier, int attackDmg, int attackRange, int knockback, int enchantCost, Item.Rarity rarity, int addedRange, int brokerValue, int maxTargets, int maxMining, float targetRangeFactor) {
        ItemRegistry.registerItem("drs_pivelaxe_" + kind,
                new MultitoolToolItem(
                        animSpeed,
                        toolDps * 9/10,
                        toolTier,
                        attackDmg * 4/3,
                        attackRange,
                        knockback * 6/5,
                        enchantCost * 6/5,
                        rarity,
                        addedRange,
                        maxTargets, maxMining, targetRangeFactor
                ),
                (float)brokerValue * 9/4,
                true);
    }
}
