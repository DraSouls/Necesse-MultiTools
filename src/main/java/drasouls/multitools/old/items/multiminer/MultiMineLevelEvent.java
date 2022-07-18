package drasouls.multitools.old.items.multiminer;

import drasouls.multitools.Util;
import drasouls.multitools.old.items.MultiMineToolItem;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelDrawUtils;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.ui.HUD;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;

import java.awt.geom.Point2D;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;

public class MultiMineLevelEvent extends LevelEvent {
    public final Deque<MiningTarget> targets = new ConcurrentLinkedDeque<>();
    private final int selectInterval = 300;
    private final int attackInterval = 200;
    private final int maxConcurrency = 5;
    private final int maxTargetQueue = 16;
    private final InventoryItem item;
    private final PlayerMob player;
    private final GameTexture targetTexture;

    private Function<MiningTarget, Boolean> canTargetFilter = t -> true;
    // nextTarget is smart mining target
    private MiningTarget nextTarget;
    protected float updateTimer = 0;
    protected float selectTimer = 0;

    public int aimAngle;

    // now THIS class is jslkjdhskajvfbjhbdfefdf
    public MultiMineLevelEvent(PlayerMob player, int x, int y, InventoryItem item, GameTexture targetTexture) {
        super(false);
        this.item = item;
        this.player = player;
        this.targetTexture = targetTexture;

        this.aimAngle = (int) GameMath.fixAngle(GameMath.getAngle(new Point2D.Float((float)x - player.x, (float)y - player.y)));
    }

    @Override
    public boolean isNetworkImportant() {
        return true;
    }

    @Override
    public void applySpawnPacket(PacketReader reader) {
        super.applySpawnPacket(reader);
        Util.printSide(level);
    }

    @Override
    public void setupSpawnPacket(PacketWriter writer) {
        super.setupSpawnPacket(writer);
        Util.printSide(level);
    }

    public MiningTarget setNextTarget(int tileX, int tileY) {
        if (this.nextTarget != null && this.nextTarget.x == tileX && this.nextTarget.y == tileY) {
            return this.nextTarget;
        }
        int objectId = level.getObjectID(tileX, tileY);
        int tileId = level.getTileID(tileX, tileY);
        if (objectId != 0) {
            this.nextTarget = new MiningTarget(true, objectId, tileX, tileY);
        } else if (tileId != 0) {
            this.nextTarget = new MiningTarget(false, tileId, tileX, tileY);
        }

        return this.nextTarget;
    }

    public void updateDamage() {
        targets.removeIf(t -> {
            if (!canTargetFilter.apply(t)) return true;
            if (t.isObject) {
                return level.getObjectID(t.x, t.y) != t.id;
            } else {
                return level.getTileID(t.x, t.y) != t.id;
            }
        });
        //System.out.println((level.isServerLevel() ? "[server]" : "[client]") + " update, nextTarget: "+nextTarget);

        // TODO clean
        MultiMineToolItem toolItem = (MultiMineToolItem) item.item;

        // NOTE levelX levelY is mouseX mouseY to spawn debris on. tileX*32 to get local tile pos?
        // TODO: Calc rectangle bound hit
        // nextTarget is smart mining target
        //if (nextTarget != null && toolItem.isTileInRange(level, nextTarget.x, nextTarget.y, player, item)
        //        && canTargetFilter.apply(nextTarget) && targets.size() < maxTargetQueue) {
        //    System.out.println("added "+nextTarget);
        //    targets.push(nextTarget);
        //    nextTarget = null;
        //}

        float dmg = toolItem.getToolDps(item, player) * attackInterval / 1000f;

        //Util.printSide(level, "#updateDamage targets: " + targets.size());
        Iterator<MiningTarget> currentlyMining = targets.descendingIterator();
        for (int i = 0; currentlyMining.hasNext() && i < maxConcurrency; i++) {
            MiningTarget target = currentlyMining.next();
            //this.runTileDamage(level, levelX, levelY, tileX, tileY, player, item, dmg);
            //System.out.println("dmg "+dmg+" at "+target.x+","+target.y );
            toolItem.runTileDamage(level, target.x*32, target.y*32, target.x, target.y, player, item, (int) dmg);
        }
    }

    @Override
    public void tickMovement(float delta) {
        super.tickMovement(delta);
        Util.printSide(level);
        if (!this.player.isAttacking) {
            this.over();
            return;
        }

        if (level.isServerLevel()) return;

        if (Settings.smartMining) {
            this.selectTimer += delta;
            if (this.selectTimer >= this.selectInterval) {
                MultiMineToolItem toolItem = (MultiMineToolItem) item.item;
                //if (nextTarget != null && toolItem.isTileInRange(level, nextTarget.x, nextTarget.y, player, item)
                //        && canTargetFilter.apply(nextTarget) && targets.size() < maxTargetQueue) {
                //    this.selectTimer = 0;
                //    System.out.println((level.isServerLevel() ? "[server]" : "[client]") + " added "+nextTarget);
                //    targets.push(nextTarget);
                //    nextTarget = null;
                //}
                if (this.player == this.level.getClient().getPlayer()) {
                    GameCamera camera = GlobalData.getCurrentState().getCamera();
                    int mouseX = camera.getMouseLevelPosX();
                    int mouseY = camera.getMouseLevelPosY();
                    HUD.SmartMineTarget target = HUD.getFirstSmartHitTile(level, player, item, mouseX, mouseY);
                    if (target != null) this.setNextTarget(target.x, target.y);
                    if (nextTarget != null && canTargetFilter.apply(nextTarget)
                            && targets.size() < maxTargetQueue && !isTileTargeted(nextTarget.x, nextTarget.y)) {
                        this.selectTimer = 0;
                        //System.out.println((level.isServerLevel() ? "[server]" : "[client]") + " added "+nextTarget);
                        targets.push(nextTarget);
                        nextTarget = null;
                    }
                }
            }

            this.updateTimer += delta;
            while (this.updateTimer >= this.attackInterval) {
                this.updateTimer -= this.attackInterval;
                this.updateDamage();
            }
        } else {

        }
    }

    public void clientTick() {
        super.clientTick();
        Util.printSide(level);
        if (!this.isOver()) {
            if (this.player == this.level.getClient().getPlayer()) {
                GameCamera camera = GlobalData.getCurrentState().getCamera();
                int mouseX = camera.getMouseLevelPosX();
                int mouseY = camera.getMouseLevelPosY();
                int nextTargetAngle = (int)GameMath.fixAngle(GameMath.getAngle(new Point2D.Float((float)mouseX - this.player.x, (float)mouseY - this.player.y)));
                if (this.aimAngle != nextTargetAngle) {
                    this.aimAngle = nextTargetAngle;
                    //this.level.getClient().network.sendPacket(new PacketMouseBeamEventUpdate(this, this.targetAngle, this.currentAngle));
                }
            }

        }
    }

    @Override
    public void addDrawables(List<LevelSortedDrawable> sortedDrawables, OrderableDrawables tileDrawables, OrderableDrawables topDrawables, LevelDrawUtils.DrawArea drawArea, Level level, TickManager tickManager, GameCamera camera) {
        //topDrawables.add(tm -> );

        targets.forEach(t -> {
            final int drawX = camera.getTileDrawX(t.x);
            final int drawY = camera.getTileDrawY(t.y);
            //System.out.println("draw at " + t.x + "," + t.y);
            topDrawables.add(tm -> targetTexture.initDraw().draw(drawX, drawY));
        });
    }

    public boolean isTileTargeted(int tileX, int tileY) {
        //MiningTarget recentTarget = targets.peek();
        for (MiningTarget target : targets) {
            if (target.x == tileX && target.y == tileY) {
                return true;
            }
        }
        return false;
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

        public boolean sameTileAs(MiningTarget other) {
            return this.x == other.x && this.y == other.y;
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
