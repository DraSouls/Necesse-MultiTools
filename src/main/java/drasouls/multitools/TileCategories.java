package drasouls.multitools;

import necesse.engine.GameLog;
import necesse.level.gameTile.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TileCategories {
    public static final String EXCLUDED_TAG = "EXCLUDED";
    public static final String[] ORDERING = {
            "flooring", "terrain"
    };
    public static final Map<String, Set<String>> COMPUTED = new LinkedHashMap<>();
    static { for (String s : ORDERING) COMPUTED.computeIfAbsent(s, k -> ConcurrentHashMap.newKeySet()); }

    public static String findCategory(String tileId) {
        for (String cat : ORDERING) {
            if (COMPUTED.containsKey(cat) && COMPUTED.get(cat).contains(tileId)) {
                return cat;
            }
        }
        return null;
    }

    public static void categorize(GameTile tile) {
        String[] cat;

        if (!tile.canBeMined
                || tile instanceof LiquidTile) {
            cat = CAT(EXCLUDED_TAG);
        } else if (tile instanceof EdgedTiledTexture
                || tile instanceof FarmlandTile
                || tile instanceof DungeonFloorTile
                || tile instanceof SandBrickTile
                || tile instanceof SimpleFloorTile
                || tile instanceof SimpleTiledFloorTile) {
            cat = CAT("flooring");
        } else if (tile instanceof TerrainSplatterTile) {
            cat = CAT("terrain");
        } else {
            GameLog.warn.printf("Uncategorized object %s of type %s\n", tile.getDisplayName(), tile.getClass());
            cat = CAT("terrain");
        }

        if (EXCLUDED_TAG.equals(cat[0])) return;
        if (COMPUTED.containsKey(cat[cat.length-1]))
            COMPUTED.get(cat[cat.length-1]).add(tile.getStringID());
    }

    private static String[] CAT(String... cats) {
        if (cats.length == 0) throw new IllegalArgumentException("Categories must not be blank.");
        return cats;
    }
}
