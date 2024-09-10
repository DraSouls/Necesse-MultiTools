package drasouls.multitools;

import necesse.engine.GameLog;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.*;
import necesse.level.gameObject.furniture.InventoryObject;
import necesse.level.gameObject.furniture.RoomFurniture;
import necesse.level.maps.levelData.settlementData.SettlementWorkstationObject;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.StaticMultiTile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectCategories {
    public static final String EXCLUDED_TAG = "EXCLUDED";
    public static final String[] ORDERING = {
            "ore", "walls", "light", "harvestable", "crafting", "storage", "objects", "transport", "wiring", "clutter", "summon", "other"
    };
    public static final Map<String, Set<String>> COMPUTED = new LinkedHashMap<>();
    static { for (String s : ORDERING) COMPUTED.computeIfAbsent(s, k -> ConcurrentHashMap.newKeySet()); }

    public static String findCategory(String objectId) {
        for (String cat : ORDERING) {
            if (COMPUTED.containsKey(cat) && COMPUTED.get(cat).contains(objectId)) {
                return cat;
            }
        }
        return null;
    }

    public static void categorize(GameObject object) {
        String[] cat;

        final MultiTile mt = object.getMultiTile(0);
        if (object.toolType == ToolType.UNBREAKABLE
                || !mt.isMaster
                || (mt instanceof StaticMultiTile && mt.ids[0] != object.getID())
                || object instanceof AirObject) {
            // excluded
            cat = CAT(EXCLUDED_TAG);
        } else if (object instanceof AncientTotemObject
                || object instanceof RoyalEggObject
                || object instanceof SpiderEggObject) {
            // misc
            cat = CAT("summon");
            //cat = CAT(EXCLUDED_TAG);
        } else if (object instanceof GrassObject
                || object instanceof CoinPileObject
                || object instanceof SnowPileObject
                || object instanceof SingleRockObject
                || object instanceof SingleRockSmall) {
            // clutter 1
            // cat = CAT("misc", "clutter");
            cat = CAT("clutter");
        } else if (object instanceof GravestoneObject
                || object instanceof StoneCoffinObject
                || object instanceof RandomBreakObject
                || object instanceof KatanaStoneObject
                || object instanceof FallenAltarObject
                || object instanceof ChristmasWreathObject) {
            // clutter 2 (breakables)
            // cat = CAT("misc", "lootable");
            cat = CAT("clutter");
        } else if (object instanceof TorchObject
                || object instanceof TikiTorchObject
                || object instanceof WallTorchObject) {
            cat = CAT("light");
        } else if (object instanceof BannerStandObject
                || object instanceof SpideriteArmorStandObject
                || object instanceof CampfireObject
                || object instanceof ColumnObject
                || object instanceof FeedingTroughObject
                || object instanceof FireChaliceObject
                || object instanceof SettlementFlagObject
                || object instanceof SignObject
                || object instanceof TorchObject
                || object instanceof TikiTorchObject
                || object instanceof WallTorchObject
                || object instanceof CandlePedestalObject
                || object instanceof StreetlampObject
                || object instanceof SnowManTrainingDummyObject
                || object instanceof TrainingDummyObject) {
            // other objects
            cat = CAT("objects");
        } else if (object instanceof HomestoneObject
                || object instanceof WaystoneObject
                || object instanceof LadderDownObject
                || object instanceof MinecartTrackObject
                || object instanceof TravelstoneObject) {
            // transport
            // cat = CAT("objects", "transport");
            cat = CAT("transport");
        } else if (object instanceof InventoryObject) {
            // inventory objects
            // cat = CAT("objects", "storage");
            cat = CAT("storage");
        } else if (object instanceof SettlementWorkstationObject
                || object instanceof CartographerTableObject
                || object instanceof IncineratorInventoryObject
                || object instanceof UpgradeStationObject
                || object instanceof SalvageStationObject) {
            // crafting and processing
            // cat = CAT("objects", "crafting");
            cat = CAT("crafting");
        } else if (object instanceof RockOreObject
                || object instanceof CrystalClusterObject
                || object instanceof CrystalClusterSmallObject
                || object instanceof SingleOreRockSmall
                || object instanceof PearlescentShardObject) {
            // ore
            cat = CAT("ore");
        } else if (object instanceof RockObject
                || object instanceof WallObject
                || object instanceof DoorObject
                || object instanceof FenceObject) {
            // walls
            cat = CAT("walls");
        } else if (object instanceof RoomFurniture
                || object instanceof FlowerPotObject
                || object instanceof SpiderThroneObject) {
            // furniture
            // cat = CAT("furniture");
            cat = CAT("objects");
        } else if (object instanceof FruitBushObject
                || object instanceof FruitTreeObject
                || object instanceof BeeHiveObject
                || object instanceof ApiaryObject) {
            // fruit trees and bushes
            // cat = CAT("plants", "harvestable");
            cat = CAT("harvestable");
        } else if (object instanceof ForestryJobObject
                || object instanceof SaplingObject
                || object.toolType == ToolType.AXE
//                || object instanceof CutTreeTrunkObject
//                || object instanceof FallenTreeTrunk1Object
//                || object instanceof SmallSingleCactusObject
) {
            // trees
            // cat = CAT("plants", "trees");
            cat = CAT("harvestable");
        } else if (object instanceof CustomWildFlowerObject
                || object instanceof SeedObject) {
            // farmables
            // cat = CAT("plants", "farmable");
            cat = CAT("harvestable");
        } else if (object instanceof FireworkDispenserObject
                || object instanceof LEDPanelObject
                || object instanceof MaskedPressurePlateObject
                || object instanceof PressurePlateObject
                || object instanceof SpikeTrapObject
                || object instanceof SwitchObject
                || object instanceof TNTObject
                || object instanceof TrapObject
                || object instanceof WallTrapObject) {
            // wiring objects
            cat = CAT("wiring");
        } else {
            GameLog.warn.printf("Uncategorized object %s of type %s, id %s, added to \"other\" category\n", object.getDisplayName(), object.getClass(), object.getStringID());
            cat = CAT("other");
        }

        if (EXCLUDED_TAG.equals(cat[0])) return;

        // WIP, will use category tree in the future.
        if (COMPUTED.containsKey(cat[cat.length-1]))
            COMPUTED.get(cat[cat.length-1]).add(object.getStringID());
    }

    private static String[] CAT(String... cats) {
        if (cats.length == 0) throw new IllegalArgumentException("Categories must not be blank.");
        return cats;
    }
}
