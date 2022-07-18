package drasouls.multitools.old.items;

import drasouls.multitools.Util;
import drasouls.multitools.old.items.multiminer.MultiMineAttackHandler;
import drasouls.multitools.old.items.multiminer.MultiMineLevelEvent;
import necesse.engine.localization.Localization;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.entity.mobs.AttackAnimMob;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.item.toolItem.ToolDamageItem;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;

public class MultiMineToolItem extends ToolDamageItem {
    private static final int MAX_MINING_CONCURRENCY = 5;
    private static final int MAX_TARGET_CONCURRENCY = 16;
    private MultiMineLevelEvent event;

    private GameTexture targetTexture;

    public MultiMineToolItem(int enchantCost) {
        super(enchantCost);
        this.setItemCategory("equipment", "tools", "pickaxes");
        this.keyWords.add("pickaxe");
        this.toolType = ToolType.ALL;
        this.width = 10.0F;
        this.animAttacks = 2;
        this.animSpeed = 500;
        this.cooldown = 500;
        this.toolDps = 90;
        this.toolTier = 0;
        this.attackDmg = new GameDamage(15);
        this.attackRange = 50;
        this.knockback = 50;
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        targetTexture = GameTexture.fromFile("other/drs_aim");
    }

    @Override
    protected void addToolTooltips(ListGameTooltips tooltips) {
        tooltips.add(Localization.translate("itemtooltip", "pickaxetip"));
    }

    @Override
    public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
        drawOptions.pointRotation(attackDirX, attackDirY);
    }

    @Override
    public int getAnimSpeed(InventoryItem item, PlayerMob player) {
        return this.animSpeed;
    }

    @Override
    public boolean animDrawBehindHand() {
        return false;
    }

    @Override
    public void showAttack(Level level, int x, int y, AttackAnimMob mob, int attackHeight, InventoryItem item, int seed, PacketReader contentReader) {
        Util.printSide(level);
    }


    // Client only
    @Override
    public boolean canSmartMineTile(Level level, int tileX, int tileY, PlayerMob player, InventoryItem item) {
        boolean inTargets = false;
        if (level.isServerLevel()) return false;
        if (event != null && !event.isOver()) {
            boolean a = !event.isTileTargeted(tileX, tileY);
            return a;
            //if (!inTargets) {
            //    return event.setNextTarget(tileX, tileY) != null;
            //}
        }

        return level.getObject(tileX, tileY).shouldSnapSmartMining(level, tileX, tileY);
    }

    @Override
    protected InventoryItem runLevelDamage(Level level, int levelX, int levelY, int tileX, int tileY, PlayerMob player, InventoryItem item, int animAttack, PacketReader contentReader) {
        if (this.toolType != ToolType.NONE && this.isTileInRange(level, tileX, tileY, player, item) && this.canDamageTile(level, tileX, tileY, player, item)) {
            int dmg = this.getToolHitDmg(item, animAttack, player);
            this.runTileDamage(level, levelX, levelY, tileX, tileY, player, item, dmg);
        }

        return item;
    }

    @Override
    public int getToolHitDmg(InventoryItem item, int hitNum, PlayerMob player) {
        return super.getToolHitDmg(item, hitNum, player);
    }

    @Override
    public void runTileDamage(Level level, int levelX, int levelY, int tileX, int tileY, PlayerMob player, InventoryItem item, int damage) {
        super.runTileDamage(level, levelX, levelY, tileX, tileY, player, item, damage);
    }

    @Override
    public InventoryItem onAttack(Level level, int x, int y, PlayerMob player, int attackHeight, InventoryItem item, PlayerInventorySlot slot, int animAttack, int seed, PacketReader contentReader) {
        super.onAttack(level, x, y, player, attackHeight, item, slot, animAttack, seed, contentReader);
        Util.printSide(level);
        this.event = new MultiMineLevelEvent(player, x, y, item, targetTexture);
        level.entityManager.addLevelEventHidden(this.event);
        player.startAttackHandler(new MultiMineAttackHandler(player, slot, 75, seed, event));

        return item;
    }

    @Override
    public void setupAttackContentPacket(PacketWriter writer, Level level, int x, int y, PlayerMob player, InventoryItem item) {
        super.setupAttackContentPacket(writer, level, x, y, player, item);
        Util.printSide(level);
    }

    /*

            @Override
            public InventoryItem onAttack(Level level, int x, int y, PlayerMob player, int attackHeight, InventoryItem item, PlayerInventorySlot slot, int animAttack, int seed, PacketReader contentReader) {
                super.onAttack(level, x, y, player, attackHeight, item, slot, animAttack, seed, contentReader);

                // runLevelDamage
                return item;
            }*/

}
