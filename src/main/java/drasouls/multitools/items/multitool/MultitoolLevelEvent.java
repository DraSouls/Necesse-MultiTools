package drasouls.multitools.items.multitool;

import drasouls.multitools.MultiTools;
import drasouls.multitools.items.MultitoolToolItem;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.ui.HUD;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.ToolDamageItem;
import necesse.level.maps.hudManager.HudDrawElement;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.zip.CRC32;

public class MultitoolLevelEvent extends LevelEvent {
    public int levelX;
    public int levelY;
    private final ConcurrentLinkedDeque<MiningTarget> targets = new ConcurrentLinkedDeque<>();
    private final List<MiningTarget> currentlyMining;
    private final Set<MiningTarget> farTargets;
    private final InventoryItem item;
    private final PlayerMob player;
    private final int attackInterval;
    private final int smartMineSelectInterval;
    private final int damagePerHit;
    private final int maxTargets;
    private final int maxMining;
    private final float targetRangeFactor;
    private MultitoolAttackHandler attackHandler = null;
    private HudDrawElement cursorDrawElement;
    private float attackTimer = 0;
    private long lastAddition = 0;
    private boolean startDamage = false;
    private boolean targetsValid = true;

    public MultitoolLevelEvent(PlayerMob player, int levelX, int levelY, InventoryItem item) {
        super(false);
        if (! (item.item instanceof MultitoolToolItem))
            throw new IllegalArgumentException("item using this event should be a MultitoolToolItem");
        this.levelX = levelX;
        this.levelY = levelY;
        this.player = player;
        this.item = item;
        this.maxTargets = ((MultitoolToolItem) item.item).getMaxTargets();
        this.maxMining = ((MultitoolToolItem) item.item).getMaxMining();
        this.targetRangeFactor = ((MultitoolToolItem) item.item).getTargetRangeFactor();

        this.attackInterval = item.item.getAttackAnimTime(item, player) / item.item.getAnimAttacks(item);
        this.smartMineSelectInterval = (int) (item.item.getAttackAnimTime(item, player) * 0.6);
        this.damagePerHit = (int) ((ToolDamageItem) item.item).getToolDamagePerHit(item, player);

        this.currentlyMining = new ArrayList<>(this.maxMining);
        this.farTargets = new HashSet<>(this.maxTargets);
    }

    public void setAttackHandler(MultitoolAttackHandler attackHandler) {
        this.attackHandler = attackHandler;
    }

    @Override
    public void init() {
        super.init();
        if (this.level.isClientLevel()) {
            this.level.hudManager.addElement(this.cursorDrawElement = new HudDrawElement() {
                @Override
                public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
                    final DrawOptionsList drawOptions = new DrawOptionsList();
                    targets.forEach(t -> {
                        final int drawX = camera.getTileDrawX(t.x);
                        final int drawY = camera.getTileDrawY(t.y);
                        if (farTargets.contains(t)) {
                            drawOptions.add(() -> MultiTools.aimTexture.initDraw().alpha(0.3f).draw(drawX, drawY));
                        } else {
                            drawOptions.add(() -> MultiTools.aimTexture.initDraw().draw(drawX, drawY));
                        }
                    });
                    currentlyMining.forEach(t -> {
                        final int drawX = camera.getTileDrawX(t.x);
                        final int drawY = camera.getTileDrawY(t.y);
                        drawOptions.add(() -> MultiTools.targetTexture.initDraw().draw(drawX, drawY));
                    });

                    list.add(new SortedDrawable() {
                        @Override public int getPriority() { return 100; }
                        @Override public void draw(TickManager tm) { drawOptions.draw(); }
                    });
                }
            });
        }
    }

    public MiningTarget getTarget(int tileX, int tileY) {
        int objectId = this.level.getObjectID(tileX, tileY);
        int tileId = this.level.getTileID(tileX, tileY);
        if (objectId != 0) {
            return new MiningTarget(true, objectId, tileX, tileY);
        } else if (tileId != 0) {
            return new MiningTarget(false, tileId, tileX, tileY);
        }
        return null;
    }

    public void onTargetUpdate(int tileX, int tileY, int targetsHash) {
        MiningTarget target = getTarget(tileX, tileY);
        if (target == null) {
            this.attackHandler.requestTargetRecheck();
            return;
        }

        targets.addLast(target);
        if (targetsHash != this.targetsHash()) {
            this.targetsValid = false;
            this.attackHandler.requestTargetRefresh();
        }
        this.startDamage = true;
    }

    public void writeTargetsToPacket(PacketWriter writer) {
        writer.putNextInt(targets.size());
        writer.putNextInt(this.targetsHash());
        targets.forEach(t -> t.serialize(writer));  // first H...T last
    }

    public void checkAndWriteTargetsToPacket(PacketWriter writer) {
        recheckTargets();
        writeTargetsToPacket(writer);
    }

    public void readTargetsFromPacket(PacketReader reader) {
        int size = reader.getNextInt();
        int hash = reader.getNextInt();
        targets.clear();
        for (int i = 0; i < size; i++) {
            targets.addLast(MiningTarget.deserialize(reader));  // H...T << t
        }
        if (hash != this.targetsHash()) {
            GameLog.warn.println("RECEIVED TARGETS PACKET MISMATCH!");
            return;
        }
        this.targetsValid = true;
    }


    public int targetsHash() {
        CRC32 hash = new CRC32();
        targets.forEach(target -> {
            hash.update(target.x >> 24);
            hash.update(target.x & 0xFF);
            hash.update(target.y >> 24);
            hash.update(target.y & 0xFF);
            hash.update(target.id >> 24);
            hash.update(target.id & 0xFF);
            hash.update(target.isObject ? 1 : 0);
        });
        return (int) hash.getValue();
    }

    @Override
    public void serverTick() {
        super.serverTick();
    }

    // Runs only on local client (not other clients) as long as this event isn't networked (it's fine for this to exist server side)
    @Override
    public void clientTick() {
        super.clientTick();
        if (!this.isOver()) {
            if (this.player == this.level.getClient().getPlayer()) {
                if (targets.size() >= this.maxTargets) {
                    return;
                }

                GameCamera camera = GlobalData.getCurrentState().getCamera();
                this.levelX = camera.getMouseLevelPosX();
                this.levelY = camera.getMouseLevelPosY();
                int tileX, tileY;
                if (Settings.smartMining) {
                    // TODO filter target priority
                    HUD.SmartMineTarget target = HUD.getFirstSmartHitTile(this.level, this.player, this.item, this.levelX, this.levelY);
                    if (target == null) {
                      return;
                    }
                    tileX = target.x;
                    tileY = target.y;
                } else {
                    tileX = this.levelX / 32;
                    tileY = this.levelY / 32;
                }

                for (MiningTarget target : targets) {
                    if (target.sameTileAs(tileX, tileY)) {
                       return;
                    }
                }

                ToolDamageItem toolItem = (ToolDamageItem)this.item.item;

                //if (toolItem.isTileInRange(this.level, tileX, tileY, this.player, this.item)) {
                // Yeah we're using Math.hypot and not Point2D.distance because the latter is JANK
                if (Math.hypot(this.player.x - (float)(tileX * 32 + 16), this.player.y - (float)(tileY * 32 + 16))
                        <= this.targetRangeFactor * toolItem.getMiningRange(this.item, this.player)) {
                    MiningTarget target = getTarget(tileX, tileY);
                    if (target != null && (toolItem.canDamageTile(this.level, tileX, tileY, this.player, this.item))) {
                        targets.addLast(target);  // H...T << t
                        this.lastAddition = this.level.getWorldEntity().getTime();
                        this.attackHandler.sendTargetUpdate(tileX, tileY, this.targetsHash());
                        if (!this.startDamage) {
                            // visual/audio stuff
                            this.startDamage = true;
                            doDamage();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void tickMovement(float delta) {
        super.tickMovement(delta);
        if (this.startDamage) {
            this.attackTimer -= delta;
            if (this.attackTimer <= 0) {
                this.attackTimer = this.attackInterval;
                doDamage();
            }
        }
    }

    private void recheckTargets() {
        ToolDamageItem toolItem = (ToolDamageItem)this.item.item;
        targets.removeIf(t -> {
            if (this.level.getObjectID(t.x, t.y) == 0 && this.level.getTileID(t.x, t.y) == 0) return true;
            if (Math.hypot(this.player.x - (float)(t.x * 32 + 16), this.player.y - (float)(t.y * 32 + 16)) > this.targetRangeFactor * toolItem.getMiningRange(this.item, this.player))
                return true;
            if (t.isObject) return this.level.getObjectID(t.x, t.y) != t.id;
            else return this.level.getTileID(t.x, t.y) != t.id;
        });
    }

    public void doDamage() {
        if (!this.targetsValid) {
            return;
        }
        MultitoolToolItem toolItem = (MultitoolToolItem)this.item.item;
        recheckTargets();
        farTargets.clear();
        currentlyMining.clear();
        Iterator<MiningTarget> iterator = targets.iterator();   // first H...T last
        for (int i = 0; i < this.maxMining; i++) {
            if (!iterator.hasNext()) break;
            MiningTarget target = iterator.next();
            if (!toolItem.isTileInRange(this.level, target.x, target.y, this.player, this.item)){
                farTargets.add(target);
                i--;
                continue;
            }
            toolItem.runTileDamage(this.level, target.x * 32, target.y * 32, target.x, target.y, this.player, this.item, this.damagePerHit);
            currentlyMining.add(target);
        }
    }

    public boolean canSmartMine(int tileX, int tileY) {
        MiningTarget recentTarget = targets.peekLast();
        if (recentTarget == null) {
          return true;
        }
        if (recentTarget.sameTileAs(tileX, tileY)) {
            return this.level.getWorldEntity().getTime() - this.lastAddition < this.smartMineSelectInterval;
        }
        for (MiningTarget target : targets) {
            if (target.sameTileAs(tileX, tileY)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void over() {
        super.over();
        if (this.cursorDrawElement != null) this.cursorDrawElement.remove();
    }


    public static class MiningTarget {
        public final boolean isObject;
        public final int id;
        public final int x;
        public final int y;

        public MiningTarget(boolean isObject, int id, int x, int y) {
            if (id == 0) throw new IllegalArgumentException("id must never be zero");
            this.isObject = isObject;
            this.id = id;
            this.x = x;
            this.y = y;
        }

        public static MiningTarget deserialize(PacketReader reader) {
            return new MiningTarget(
                    reader.getNextBoolean(),
                    reader.getNextInt(),
                    reader.getNextInt(),
                    reader.getNextInt()
            );
        }

        public void serialize(PacketWriter writer) {
            writer.putNextBoolean(isObject);
            writer.putNextInt(id);
            writer.putNextInt(x);
            writer.putNextInt(y);
        }

        public boolean sameTileAs(int otherX, int otherY) {
            return this.x == otherX && this.y == otherY;
        }

        public boolean sameTileAs(MiningTarget other) {
            return sameTileAs(other.x, other.y);
        }

        public boolean sameAs(MiningTarget other) {
            return sameTileAs(other) && this.isObject == other.isObject && this.id == other.id;
        }

        @Override
        public String toString() {
            return "MiningTarget{" +
                    "isObject=" + isObject +
                    ", id=" + id +
                    ", x=" + x +
                    ", y=" + y +
                    '}';
        }
    }
}
