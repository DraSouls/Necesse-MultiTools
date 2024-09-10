package drasouls.multitools.items.planner;

import drasouls.multitools.MultiTools;
import drasouls.multitools.Util;
import drasouls.multitools.items.PlannerItem;
import necesse.engine.GlobalData;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.ui.HUD;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemInteractAction;
import necesse.level.maps.hudManager.HudDrawElement;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class PlannerLevelEvent extends LevelEvent {
    // statics... this is sooooo hacky... but i think it works
    public static boolean activeOnClient = false;
    public static boolean activeOnServer = false;
    private static final int[] NEIGH_SURROUND = {1, 3, 5, 7};
    private static final int[][] NEIGH_CONTIGUOUS = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};

    private final List<Point> validPlacements = Collections.synchronizedList(new ArrayList<>());
    private final PlayerMob player;
    private int tile1X, tile1Y;
    private int tile2X, tile2Y;
    private int placeInterval;
    private float placeTimer = 0;
    private boolean active = false;     // Currently on placing stage.
    private boolean useAlt = false;     // Note: only meant for buckets (canPlace reason "sametile")
    private InventoryItem item;
    private HudDrawElement cursorDrawElement;


    public PlannerLevelEvent(PlayerMob player, InventoryItem item) {
        super(false);
        if (! (item.item instanceof PlannerItem))
            throw new IllegalArgumentException("item using this event should be a PlannerItem");
        this.player = player;
        this.item = item;
        this.placeInterval = item.item.getAttackAnimTime(item, player) / 2;

        if (player.getLevel().isClientLevel()) {
            activeOnClient = true;
        } else {
            activeOnServer = true;
        }
    }

    @Override
    public void init() {
        super.init();
        if (this.level.isClientLevel()) {
            this.level.hudManager.addElement(this.cursorDrawElement = new HudDrawElement() {
                @Override
                public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
                    final DrawOptionsList drawOptions = new DrawOptionsList();

                    final float aimAlpha = active ? 0.5f : 1.0f;
                    GameTexture tgtTex = useAlt ? MultiTools.greenTargetTexture : MultiTools.blueTargetTexture;
                    GameTexture aimTex = useAlt ? MultiTools.greenAimTexture : MultiTools.blueAimTexture;
                    forEachPoint((tx, ty) -> {
                        final int drawX = camera.getTileDrawX(tx);
                        final int drawY = camera.getTileDrawY(ty);
                        // save some frames by not drawing beyond?
                        if (drawX < -32 || drawX > camera.getWidth() + 32 || drawY < -32 || drawY > camera.getHeight() + 32)
                            return;

                        drawOptions.add(() -> aimTex.initDraw().alpha(aimAlpha).draw(drawX, drawY));
                    });

                    if (active) {
                        synchronized (validPlacements) {
                            validPlacements.forEach(p -> {
                                final int drawX = camera.getTileDrawX(p.x);
                                final int drawY = camera.getTileDrawY(p.y);
                                if (drawX < -32 || drawX > camera.getWidth() + 32 || drawY < -32 || drawY > camera.getHeight() + 32)
                                    return;

                                drawOptions.add(() -> tgtTex.initDraw().draw(drawX, drawY));
                            });
                        }
                    }

                    int lx = Math.min(tile1X, tile2X);
                    int ly = Math.min(tile1Y, tile2Y);
                    int hx = Math.max(tile1X, tile2X);
                    int hy = Math.max(tile1Y, tile2Y);
                    drawOptions.add(HUD.tileBoundOptions(camera, new Color(128, 128, 255, 128), false, lx, ly, hx, hy));

                    list.add(new SortedDrawable() {
                        @Override public int getPriority() { return 100; }
                        @Override public void draw(TickManager tm) { drawOptions.draw(); }
                    });
                }
            });
        }
    }

    private void forEachPoint(BiConsumer<Integer, Integer> fn) {
        int lx = Math.min(this.tile1X, this.tile2X);
        int ly = Math.min(this.tile1Y, this.tile2Y);
        int hx = Math.max(this.tile1X, this.tile2X);
        int hy = Math.max(this.tile1Y, this.tile2Y);
        for (int ty = ly; ty <= hy; ty++) {
            for (int tx = lx; tx <= hx; tx++) {
                fn.accept(tx, ty);
            }
        }
    }

    @Override
    public void clientTick() {
        super.clientTick();
        if (!this.isOver()) {
            if (this.player == this.level.getClient().getPlayer()) {
                GNDItem p1 = this.item.getGndData().getItem("p1");
                GNDItem p2 = this.item.getGndData().getItem("p2");
                this.useAlt = this.item.getGndData().getBoolean("usealt");
                int tile1X, tile1Y;
                int tile2X, tile2Y;
                if (p2 instanceof GNDItemMap) {
                    tile1X = ((GNDItemMap)p1).getInt("x");
                    tile1Y = ((GNDItemMap)p1).getInt("y");
                    tile2X = ((GNDItemMap)p2).getInt("x");
                    tile2Y = ((GNDItemMap)p2).getInt("y");
                    this.active = true;
                } else if (p1 instanceof GNDItemMap) {
                    GameCamera camera = GlobalData.getCurrentState().getCamera();
                    tile1X = ((GNDItemMap)p1).getInt("x");
                    tile1Y = ((GNDItemMap)p1).getInt("y");
                    tile2X = camera.getMouseLevelTilePosX();
                    tile2Y = camera.getMouseLevelTilePosY();
                    this.active = false;
                } else {
                    this.active = false;
                    PlannerItem.PREVIEWS.clear();
                    return;
                }

                boolean update = tile1X != this.tile1X || tile1Y != this.tile1Y || tile2X != this.tile2X || tile2Y != this.tile2Y;
                this.tile1X = tile1X;
                this.tile1Y = tile1Y;
                this.tile2X = tile2X;
                this.tile2Y = tile2Y;
                if (update) {
                    PlannerItem.PREVIEWS.clear();
                    forEachPoint((tx, ty) -> PlannerItem.PREVIEWS.add(new Point(tx * 32, ty * 32)));
                }
            }
        }
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (!this.isOver()) {
            GNDItem p1 = this.item.getGndData().getItem("p1");
            GNDItem p2 = this.item.getGndData().getItem("p2");
            this.useAlt = this.item.getGndData().getBoolean("usealt");
            if (p1 instanceof GNDItemMap && p2 instanceof GNDItemMap) {
                this.tile1X = ((GNDItemMap)p1).getInt("x");
                this.tile1Y = ((GNDItemMap)p1).getInt("y");
                this.tile2X = ((GNDItemMap)p2).getInt("x");
                this.tile2Y = ((GNDItemMap)p2).getInt("y");
                this.active = true;
            } else {
                this.active = false;
            }
        }
    }

    // 3 out of 4 sides neighbored, or 4 5 6 7 8 contiguous neighbors
    private boolean shouldPlaceAt(int tileX, int tileY, BiFunction<Integer, Integer, Boolean> canPlace) {
        if (!canPlace.apply(tileX, tileY)) return false;

        int numNeighbors = 0;
        boolean[] neighbors = new boolean[NEIGH_CONTIGUOUS.length];
        for (int i = 0; i < NEIGH_CONTIGUOUS.length; i++) {
            int[] p = NEIGH_CONTIGUOUS[i];
            neighbors[i] = !canPlace.apply(tileX + p[0], tileY + p[1]);
            if (neighbors[i]) numNeighbors++;
        }

        int surround = 0;
        for (int p : NEIGH_SURROUND) {
            if (neighbors[p]) surround++;
            if (surround > 2) return true;
        }

        if (numNeighbors <= 3) return false;

        int contiguousNeighbors = 0;
        int maxContiguousNeighbors = 0;

        for (int i = 0; i < NEIGH_CONTIGUOUS.length * 2; i++) {
            boolean isNeighbor = neighbors[i % 8];
            if (isNeighbor) {
                contiguousNeighbors++;
                maxContiguousNeighbors = Math.max(maxContiguousNeighbors, contiguousNeighbors);
            } else {
                contiguousNeighbors = 0;
            }
        }

        return Math.min(8, maxContiguousNeighbors) == numNeighbors;
    }

    private Packet setupACP(InventoryItem invItem, int tx, int ty) {
        Packet packet = new Packet();
        invItem.item.setupAttackContentPacket(
                new PacketWriter(packet), this.level,
                tx * 32 + 16, ty * 32 + 16,
                this.player, invItem);
        return packet;
    }

    @Override
    public void tickMovement(float delta) {
        super.tickMovement(delta);

        GNDItem p1 = this.item.getGndData().getItem("p1");
        GNDItem p2 = this.item.getGndData().getItem("p2");
        if (!(p1 instanceof GNDItemMap) && !(p2 instanceof GNDItemMap)) {
            this.over();
            return;
        }

        InventoryItem current = this.player.getSelectedItem();

        if (current == null || current.item != this.item.item
                || ((PlannerItem)current.item).getCurrentItem(current).orElse(null) != ((PlannerItem)this.item.item).getCurrentItem(this.item).orElse(null)) {
            this.over();
            return;
        }
        if (current != this.item) {
            this.item = current;
            this.placeInterval = this.item.item.getAttackAnimTime(this.item, this.player) / 2;
        }
        if (! ((PlannerItem)this.item.item).getCurrentInventoryItem(this.item).isPresent()) {
            this.item.getGndData().setItem("p1", null);
            this.item.getGndData().setItem("p2", null);
            this.over();
            return;
        }


        this.placeTimer += delta;
        if (this.placeTimer > this.placeInterval) {
            this.placeTimer = 0;
            validPlacements.clear();
            int objDir = this.item.getGndData().getInt("dir", 0);

            ((PlannerItem)this.item.item).acceptItemPair(this.item, (invItem, item) ->
                Util.runWithModifierChange(this.player.buffManager, BuffModifiers.BUILD_RANGE, 1000f, () ->
                    forEachPoint((tx, ty) -> {
                        boolean shouldPlace = shouldPlaceAt(tx, ty, (x, y) ->
                                x >= Math.min(tile1X, tile2X)
                                && x <= Math.max(tile1X, tile2X)
                                && y >= Math.min(tile1Y, tile2Y)
                                && y <= Math.max(tile1Y, tile2Y)
                                && Util.wrapWithDirChange(this.player, objDir, () -> {
                                    if (this.useAlt && item instanceof ItemInteractAction) {
                                        return ("sametile").equals(item.canPlace(this.level, x * 32 + 16, y * 32 + 16, this.player, invItem,
                                                new PacketReader(setupACP(invItem, x, y))));
                                    } else {
                                        return item.canPlace(this.level, x * 32 + 16, y * 32 + 16, this.player, invItem,
                                                new PacketReader(setupACP(invItem, x, y))) == null;
                                    }
                                }));
                        if (shouldPlace) validPlacements.add(new Point(tx, ty));
                    })
                )
            );

            if (this.active && ((PlannerItem)current.item).getCurrentInventoryItem(this.item).map(invItem -> invItem.itemStackSize() == 1 || invItem.getAmount() > 1).orElse(false)) {
                float maxDist = 0;
                Point furthest = null;
                for (Point p : validPlacements) {
                    float dist = this.player.getDistance(p.x * 32 + 16, p.y * 32 + 16);
                    boolean canPlace = ((PlannerItem) this.item.item).applyItemPair(this.item, (invItem, item) ->
                            Util.wrapWithDirChange(this.player, objDir, () -> {
                                if (this.useAlt && item instanceof ItemInteractAction) {
                                    return ("sametile").equals(item.canPlace(this.level, p.x * 32 + 16, p.y * 32 + 16, this.player, invItem,
                                            new PacketReader(setupACP(invItem, p.x, p.y))));
                                } else {
                                    return item.canPlace(this.level, p.x * 32 + 16, p.y * 32 + 16, this.player, invItem,
                                            new PacketReader(setupACP(invItem, p.x, p.y))) == null;
                                }
                            }));
                    if (dist > maxDist && canPlace) {
                        maxDist = dist;
                        furthest = p;
                    }
                }

                if (furthest != null) {
                    Point p = furthest;

                    ((PlannerItem) this.item.item).acceptItemPair(this.item, (invItem, item) -> {
                        Packet packet = Util.wrapWithDirChange(this.player, objDir, () -> setupACP(invItem, p.x, p.y));
                        if (this.useAlt && item instanceof ItemInteractAction) {
                            ((ItemInteractAction)item).onLevelInteract(this.level,
                                    p.x * 32 + 16, p.y * 32 + 16,
                                    this.player, this.player.getCurrentAttackHeight(),
                                    invItem, this.player.getSelectedItemSlot(),
                                    Item.getRandomAttackSeed(GameRandom.globalRandom),
                                    new PacketReader(packet));
                        } else {
                            item.onAttack(this.level,
                                    p.x * 32 + 16, p.y * 32 + 16,
                                    this.player, this.player.getCurrentAttackHeight(),
                                    invItem, this.player.getSelectedItemSlot(),
                                    1, Item.getRandomAttackSeed(GameRandom.globalRandom),
                                    new PacketReader(packet));
                        }
                        this.player.getSelectedItemSlot().markDirty(player.getInv());
                    });
                }
            }
        }
    }

    @Override
    public void over() {
        PlannerItem.PREVIEWS.clear();
        if (this.level.isClientLevel()) {
            activeOnClient = false;
        } else {
            activeOnServer = false;
        }
        super.over();
        if (this.cursorDrawElement != null) this.cursorDrawElement.remove();
    }

    @Override
    public void onDispose() {
        super.onDispose();
        if (!this.isOver()) this.over();
    }

    @Override
    public void onUnloading() {
        super.onUnloading();
        if (!this.isOver()) this.over();
    }
}
