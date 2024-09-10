package drasouls.multitools.items;

import drasouls.multitools.MultiTools;
import drasouls.multitools.Util;
import drasouls.multitools.items.planner.PlannerLevelEvent;
import drasouls.multitools.ui.PlannerSidebarForm;
import necesse.engine.localization.Localization;
import necesse.engine.network.PacketReader;
import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.GameColor;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.forms.presets.sidebar.SidebarForm;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlaceableItemInterface;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.container.item.ItemInventoryContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.item.miscItem.PouchItem;
import necesse.inventory.item.placeableItem.PlaceableItem;
import necesse.inventory.item.placeableItem.consumableItem.ConsumableItem;
import necesse.inventory.item.placeableItem.fishingRodItem.FishingRodItem;
import necesse.inventory.item.placeableItem.followerSummonItem.FollowerSummonPlaceableItem;
import necesse.inventory.item.placeableItem.mapItem.MapItem;
import necesse.inventory.item.placeableItem.objectItem.ObjectItem;
import necesse.level.maps.Level;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class PlannerItem extends PouchItem implements PlaceableItemInterface {
    public static final List<Point> PREVIEWS = new ArrayList<>();

    public PlannerItem() {
        this.rarity = Rarity.RARE;
        this.combinePurposes.clear();
        this.insertPurposes.clear();
        this.drawStoredItems = true;
        this.animSpeed = 250;
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
    }

    public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
        tooltips.add(Localization.translate("itemtooltip", "rclickinvopentip"));
        acceptItemPair(item, (invItem, i) -> tooltips.add(new StringTooltips(i.getDisplayName(invItem), i.getRarityColor(invItem))));
        GameWindow window = WindowManager.getWindow();
        if (!window.isKeyDown(340) && !window.isKeyDown(344)) {
            tooltips.add(new StringTooltips(Localization.translate("ui", "shiftmoreinfo"), GameColor.LIGHT_GRAY));
        } else {
            tooltips.add(Localization.translate("itemtooltip", "drs_planner_tip1"));
            tooltips.add(Localization.translate("itemtooltip", "drs_planner_tip2"));
            tooltips.add(Localization.translate("itemtooltip", "drs_planner_tip3"));
        }
        return tooltips;
    }

    @Override
    public void drawIcon(InventoryItem item, PlayerMob perspective, int x, int y, int size) {
        super.drawIcon(item, perspective, x, y, size);
        acceptItemPair(item, (invItem, i) -> {
            this.drawStoredItems = invItem.getAmount() > 1;
            i.drawIcon(invItem, perspective, x, y, size);
        });
        if (! getCurrentInventoryItem(item).isPresent()) this.drawStoredItems = false;
    }

    @Override
    public SidebarForm getSidebar(InventoryItem item) {
        return new PlannerSidebarForm(item);
    }

    public Optional<PlaceableItem> getCurrentItem(InventoryItem item) {
        return this.getCurrentInventoryItem(item)
                .map(invItem -> (PlaceableItem) invItem.item);
    }

    public <T> T applyItemPair(InventoryItem item, BiFunction<InventoryItem, PlaceableItem, T> fn) {
        return this.getCurrentInventoryItem(item)
                .map(invItem -> fn.apply(invItem, (PlaceableItem) invItem.item))
                .orElse(null);
    }

    public void acceptItemPair(InventoryItem item, BiConsumer<InventoryItem, PlaceableItem> fn) {
        this.getCurrentInventoryItem(item)
                .ifPresent(invItem -> fn.accept(invItem, (PlaceableItem) invItem.item));
    }

    public Optional<InventoryItem> getCurrentInventoryItem(InventoryItem item) {
        if (! (item.item instanceof PlannerItem)) return Optional.empty();
        Inventory inventory = this.getInternalInventory(item);
        return Optional.ofNullable(inventory.getItem(0))
                .filter(invItem -> isValidRequestItem(invItem.item));
    }


    // PouchItem stuff
    protected void openContainer(ServerClient client, PlayerInventorySlot inventorySlot) {
        PacketOpenContainer p = new PacketOpenContainer(MultiTools.plannerContainer, ItemInventoryContainer.getContainerContent(this, inventorySlot));
        ContainerRegistry.openAndSendContainer(client, p);
    }

    public boolean isValidPouchItem(InventoryItem item) {
        return this.isValidRequestItem(item.item);
    }

    public boolean isValidRequestItem(Item item) {
        return item instanceof PlaceableItem && !(item instanceof ConsumableItem) && !(item instanceof FishingRodItem)
                && !(item instanceof FollowerSummonPlaceableItem) && !(item instanceof MapItem)
                && (! (item instanceof ObjectItem) ||
                        (((ObjectItem)item).getObject().getMultiTile(0).width == 1
                        && ((ObjectItem)item).getObject().getMultiTile(0).height == 1));
    }

    public boolean isValidRequestType(Item.Type type) {
        return false;
    }

    public int getInternalInventorySize() {
        return 1;
    }

    public boolean canQuickStackInventory() {
        return true;
    }

    public boolean canRestockInventory() {
        return false;
    }

    public boolean canSortInventory() {
        return false;
    }


    // PlaceableItem stuff
    public void updatePlannerEvent(InventoryItem item, PlayerMob player) {
        boolean hasFirst = item.getGndData().getItem("p1") instanceof GNDItemMap;
        boolean hasSecond = item.getGndData().getItem("p2") instanceof GNDItemMap;
        boolean hasAlt = getCurrentInventoryItem(item).map(i -> i.item instanceof ItemInteractAction).orElse(false);
        if (!hasAlt) item.getGndData().setBoolean("usealt", false);
        PlannerSidebarForm.updateState(hasFirst, hasSecond, hasAlt);

        Level level = player.getLevel();
        GNDItem fromLevel = item.getGndData().getItem("level");
        if (fromLevel == null || !level.getIdentifier().equals(fromLevel.toString())) {
            item.getGndData().setString("level", level.getIdentifier().toString());
            item.getGndData().setItem("p1", null);
            item.getGndData().setItem("p2", null);
            item.getGndData().setInt("dir", 0);
            return;
        }

        if (!hasFirst) return;
        if (player.getLevel().isClientLevel() ? PlannerLevelEvent.activeOnClient : PlannerLevelEvent.activeOnServer) return;

        level.entityManager.addLevelEventHidden(new PlannerLevelEvent(player, item));
    }

    public InventoryItem onAttack(Level level, int x, int y, PlayerMob player, int attackHeight, InventoryItem item, PlayerInventorySlot slot, int animAttack, int seed, PacketReader contentReader) {
        if (! getCurrentInventoryItem(item).isPresent()) return item;

        boolean hasFirst = item.getGndData().getItem("p1") instanceof GNDItemMap;
        boolean hasSecond = item.getGndData().getItem("p2") instanceof GNDItemMap;

        if (!hasFirst) {
            // None: first click
            item.getGndData().setItem("p1",
                    new GNDItemMap()
                            .setInt("x", x / 32)
                            .setInt("y", y / 32));
            item.getGndData().setItem("p2", null);
            item.getGndData().setInt("dir", 0);

        } else if (!hasSecond) {
            // Only first: second click
            item.getGndData().setItem("p2",
                    new GNDItemMap()
                            .setInt("x", x / 32)
                            .setInt("y", y / 32));
            item.getGndData().setInt("dir", 0x1000 | player.getDir());

        } else {
            // Both: click when actively placing; reset
            item.getGndData().setItem("p1", null);
            item.getGndData().setItem("p2", null);
            item.getGndData().setInt("dir", 0);
        }

        return item;
    }

    @Override
    public boolean getConstantUse(InventoryItem item) {
        return false;
    }


    // Call delegations
    @Override
    public void tickHolding(InventoryItem item, PlayerMob player) {
        // update if on same level still, create event if not exist
        if (! getCurrentInventoryItem(item).isPresent()) {
            item.getGndData().setItem("p1", null);
            item.getGndData().setItem("p2", null);
            item.getGndData().setInt("dir", 0);
            return;
        }
        updatePlannerEvent(item, player);
    }

    public void drawPlacePreview(Level level, int x, int y, GameCamera camera, PlayerMob player, InventoryItem item, PlayerInventorySlot slot) {
        // hax : temporarily change place range to trick previews to draw
        Util.runWithModifierChange(player.buffManager, BuffModifiers.BUILD_RANGE, 1000f, () -> {
            if (PREVIEWS.size() == 0) this.acceptItemPair(item, (invItem, i) -> i.drawPlacePreview(level, x, y, camera, player, invItem, slot));

            // hax : temporarily change direction for preview direction
            int newDir = item.getGndData().getInt("dir", 0);
            Util.runWithDirChange(player, newDir, () -> {
                for (Point p : PREVIEWS) {
                    final int drawX = camera.getDrawX(p.x);
                    final int drawY = camera.getDrawY(p.y);
                    // save some frames by not drawing beyond?
                    if (drawX < -32 || drawX > camera.getWidth() + 32 || drawY < -32 || drawY > camera.getHeight() + 32)
                        continue;
                    this.acceptItemPair(item, (invItem, i) -> i.drawPlacePreview(level, p.x, p.y, camera, player, invItem, slot));
                }
            });
        });
    }
}
