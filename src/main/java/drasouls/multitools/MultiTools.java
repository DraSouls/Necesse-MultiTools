package drasouls.multitools;

import drasouls.multitools.items.MultitoolToolItem;
import drasouls.multitools.items.PlannerItem;
import drasouls.multitools.packet.PacketUpdateGNDData;
import drasouls.multitools.ui.PlannerContainerForm;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.client.Client;
import necesse.engine.registries.*;
import necesse.gfx.forms.presets.containerComponent.item.ItemInventoryContainerForm;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.container.item.ItemInventoryContainer;
import necesse.inventory.item.Item;

@ModEntry
public class MultiTools {
    public static GameTexture aimTexture;
    public static GameTexture targetTexture;
    public static int plannerContainer;


    public void init() {
        plannerContainer = Containers.registerContainer(PlannerContainerForm::new, ItemInventoryContainer::new);

        PacketRegistry.registerPacket(PacketUpdateGNDData.class);

        ItemRegistry.registerItem("drs_planner", new PlannerItem(), 5, true);
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
                        toolDps * 8/10,
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


    private static class Containers {
        private static int registerContainer(IICFormConstructor formCtor, IICConstructor containerCtor) {
            return ContainerRegistry.registerContainer(
                    (client, uniqueSeed, packet) ->
                            formCtor.create(client, containerCtor.create(client.getClient(), uniqueSeed, packet)),
                    (client, uniqueSeed, packet, serverObject) ->
                            containerCtor.create(client, uniqueSeed, packet)
            );
        }

        @FunctionalInterface
        private interface IICConstructor {
            <C extends NetworkClient> ItemInventoryContainer create(C client, int uniqueSeed, Packet packet);
        }

        @FunctionalInterface
        private interface IICFormConstructor {
            <I extends ItemInventoryContainer> ItemInventoryContainerForm<? extends ItemInventoryContainer> create(Client client, I container);
        }
    }
}
