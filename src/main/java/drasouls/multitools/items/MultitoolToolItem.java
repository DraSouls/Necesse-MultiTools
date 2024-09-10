package drasouls.multitools.items;

import drasouls.multitools.ObjectCategories;
import drasouls.multitools.TileCategories;
import drasouls.multitools.items.multitool.MultitoolAttackHandler;
import drasouls.multitools.ui.MultitoolSidebarForm;
import necesse.engine.localization.Localization;
import necesse.engine.network.PacketReader;
import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketChangeObject;
import necesse.engine.network.packet.PacketChangeTile;
import necesse.engine.network.server.ServerClient;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.levelEvent.toolItemEvent.ToolItemEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.attackHandler.AttackHandler;
import necesse.gfx.GameColor;
import necesse.gfx.forms.presets.sidebar.SidebarForm;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.item.toolItem.pickaxeToolItem.CustomPickaxeToolItem;
import necesse.level.maps.Level;

import java.util.HashMap;

public class MultitoolToolItem extends CustomPickaxeToolItem {
    protected int maxTargets;
    protected int maxMining;
    protected float targetRangeFactor;

    public MultitoolToolItem(int animSpeed, int toolDps, int toolTier, int attackDmg, int attackRange, int knockback, int enchantCost, Item.Rarity rarity, int addedRange, int maxTargets, int maxMining, float targetRangeFactor) {
        this(animSpeed, toolDps, toolTier, attackDmg, attackRange, knockback, enchantCost);
        this.addedRange = addedRange;
        this.maxTargets = maxTargets;
        this.maxMining = maxMining;
        this.targetRangeFactor = targetRangeFactor;
        this.rarity = rarity;
        this.toolType = ToolType.ALL;
        this.hungerUsage = 1e-3f;
    }

    public MultitoolToolItem(int animSpeed, int toolDps, int toolTier, int attackDmg, int attackRange, int knockback, int enchantCost) {
        super(animSpeed, toolDps, toolTier, attackDmg, attackRange, knockback, enchantCost);
    }

    @Override
    public void addToolTooltips(ListGameTooltips tooltips) {
        GameWindow window = WindowManager.getWindow();
        if (!window.isKeyDown(340) && !window.isKeyDown(344)) {
            tooltips.add(new StringTooltips(Localization.translate("ui", "shiftmoreinfo"), GameColor.LIGHT_GRAY));
        } else {
            tooltips.add(Localization.translate("itemtooltip", "drs_pivelaxe_tip1"));
            tooltips.add(Localization.translate("itemtooltip", "drs_pivelaxe_tip2"));
            tooltips.add(Localization.translate("itemtooltip", "drs_pivelaxe_tip3"));
        }
    }

    @Override
    public SidebarForm getSidebar(InventoryItem item) {
        return new MultitoolSidebarForm(item, getCategoryFilter(item));
    }

    public int getMaxMining() {
        return this.maxMining;
    }

    public int getMaxTargets() {
        return this.maxTargets;
    }

    public float getTargetRangeFactor() {
        return this.targetRangeFactor;
    }

    public static GNDItemMap getCategoryFilter(InventoryItem item) {
        GNDItem gndItem = item.getGndData().getItem("filter");
        if (gndItem instanceof GNDItemMap) {
            return (GNDItemMap) gndItem;
        } else {
            GNDItemMap gndMap = new GNDItemMap();
            gndMap.setBoolean(ObjectCategories.ORDERING[0], true);
            item.getGndData().setItem("filter", gndMap);
            return gndMap;
        }
    }

    public static boolean isInCategory(Level level, int tileX, int tileY, String cat) {
        if (level.getObjectID(tileX, tileY) != 0) {
            return cat.equals(ObjectCategories.findCategory(level.getObject(tileX, tileY).getStringID()));
        } else if (level.getTileID(tileX, tileY) != 0) {
            return cat.equals(TileCategories.findCategory(level.getTile(tileX, tileY).getStringID()));
        }

        return false;
    }

    @Override
    public boolean canDamageTile(Level level, int tileX, int tileY, PlayerMob player, InventoryItem item) {
        if (level.getObjectID(tileX, tileY) != 0) {
            String sid = level.getObject(tileX, tileY).getStringID();
            String cat = ObjectCategories.findCategory(sid);
            return cat != null && getCategoryFilter(item).getBoolean(cat);
        } else if (level.getTileID(tileX, tileY) != 0) {
            String tid = level.getTile(tileX, tileY).getStringID();
            String cat = TileCategories.findCategory(tid);
            return cat != null && getCategoryFilter(item).getBoolean(cat);
        }

        return false;
    }

    @Override
    public boolean canSmartMineTile(Level level, int tileX, int tileY, PlayerMob player, InventoryItem item) {
        if (!canDamageTile(level, tileX, tileY, player, item)) return false;

        AttackHandler currentAttackHandler = player.getAttackHandler();
        if (currentAttackHandler instanceof MultitoolAttackHandler) {
            return ((MultitoolAttackHandler) currentAttackHandler).event.canSmartMine(tileX, tileY);
        }
        return true;
    }

    @Override
    public InventoryItem runLevelDamage(Level level, int levelX, int levelY, int tileX, int tileY, PlayerMob player, InventoryItem item, int animAttack, PacketReader contentReader) {
        // do nothing
        return item;
    }

    // make this thing public
    @Override
    public void runTileDamage(Level level, int levelX, int levelY, int tileX, int tileY, PlayerMob player, InventoryItem item, int damage) {
        super.runTileDamage(level, levelX, levelY, tileX, tileY, player, item, damage);
    }

    // onAttack rate is animSpeed / animAttacks
    // Modified to remove animAttack == 0 requirement
    @Override
    public InventoryItem onAttack(Level level, int x, int y, PlayerMob player, int attackHeight, InventoryItem item, PlayerInventorySlot slot, int animAttack, int seed, PacketReader contentReader) {
        // ToolDamageItem.onAttack
        int expectedTileID = contentReader.getNextShortUnsigned();
        int expectedObjectID = contentReader.getNextShortUnsigned();
        int expectedObjectRotation = 0;
        if (expectedObjectID != 0) {
            expectedObjectRotation = contentReader.getNextByteUnsigned();
        }

        int tileX = contentReader.getNextShortUnsigned();
        int tileY = contentReader.getNextShortUnsigned();
        if (player != null && player.isServerClient()) {
            ServerClient client = player.getServerClient();
            int currentTileID = level.getTileID(tileX, tileY);
            if (expectedTileID != currentTileID) {
                client.sendPacket(new PacketChangeTile(level, tileX, tileY, currentTileID));
            }

            int currentObjectID = level.getObjectID(tileX, tileY);
            int currentObjectRotation = currentObjectID == 0 ? 0 : level.getObjectRotation(tileX, tileY);
            if (expectedObjectID != currentObjectID || expectedObjectRotation != currentObjectRotation) {
                client.sendPacket(new PacketChangeObject(level, tileX, tileY, currentObjectID, currentObjectRotation));
            }
        }

        // ToolItem.onAttack
        if (player == null) return item;
        int animSpeed = this.getAttackAnimTime(item, player);
        ToolItemEvent toolEvent = new ToolItemEvent(player, seed, item, x - player.getX(), y - player.getY() + attackHeight, animSpeed, animSpeed);
        level.entityManager.addLevelEventHidden(toolEvent);

        // Our stuff
        if (!(player.getAttackHandler() instanceof MultitoolAttackHandler)) {
            player.startAttackHandler(new MultitoolAttackHandler(player, slot, item, x, y, this.animAttacks, animSpeed, seed));
        }
        return item;
    }
}
