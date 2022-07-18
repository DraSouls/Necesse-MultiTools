package necesse.engine.registries;

import necesse.level.gameObject.GameObject;
import necesse.level.gameTile.GameTile;

import java.util.List;
import java.util.stream.Stream;

// In order to access protected/package-private stuff
public class RegistryAccessor {
    public static Stream<GameObject> getObjectsParallelStream(ObjectRegistry registry) {
        return ((List<ObjectRegistry.ObjectRegistryElement>)registry.getElements())
                .parallelStream()
                .map(e -> e.object);
    }

    public static Stream<GameTile> getTilesParallelStream(TileRegistry registry) {
        return ((List<TileRegistry.TileRegistryElement>)registry.getElements())
                .parallelStream()
                .map(e -> e.tile);
    }
}
