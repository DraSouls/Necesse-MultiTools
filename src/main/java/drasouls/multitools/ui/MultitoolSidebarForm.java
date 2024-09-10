package drasouls.multitools.ui;

import drasouls.multitools.ObjectCategories;
import drasouls.multitools.TileCategories;
import drasouls.multitools.items.MultitoolToolItem;
import drasouls.multitools.packet.PacketUpdateGNDData;
import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.forms.components.*;
import necesse.gfx.forms.components.localComponents.FormLocalCheckBox;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.presets.sidebar.SidebarForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.Renderer;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.InventoryItem;
import necesse.level.maps.hudManager.HudDrawElement;
import necesse.level.maps.light.GameLight;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MultitoolSidebarForm extends SidebarForm {
    private static final int CHECK_INTERVAL = 40;
    private static final int SINE_PULSE_PERIOD = 400;

    private static boolean isCollapsed = false;
    private static int formHeight = 0;

    private volatile boolean shouldUpdate = false;
    private volatile float sineTimer;
    private final Map<String, FormCheckBox> checkBoxes = new ConcurrentHashMap<>();
    private final List<Point> highlights = new ArrayList<>();
    private HudDrawElement highlightDrawElement;
    private Client client;
    private float checkTimer;

    public MultitoolSidebarForm(InventoryItem item, GNDItemMap categoryFilter) {
        super("drs_multitoolsidebar", 160, 120, item);
        Objects.requireNonNull(categoryFilter);

        FormFlow listFlow = new FormFlow(5);
        this.addComponent(new FormLocalLabel("ui", "drs_miningfilter", new FontOptions(16), -1, 5, listFlow.next(25)));

        FormTextButton collapse = this.addComponent(new FormTextButton(isCollapsed ? "+" : "-", 135, 5, 20, FormInputSize.SIZE_20, ButtonColor.BASE));
        FormTextButton selectAll = this.addComponent(new FormLocalTextButton("ui", "drs_setall", 5, listFlow.next(0), 70, FormInputSize.SIZE_16, ButtonColor.BASE));
        FormTextButton selectNone = this.addComponent(new FormLocalTextButton("ui", "drs_setnone", 85, listFlow.next(20), 70, FormInputSize.SIZE_16, ButtonColor.BASE));

        //List<FormCheckBox> checkBoxes = new ArrayList<>();

        ObjectCategories.COMPUTED.keySet().forEach(category ->
                checkBoxes.put(category, this.addComponent(new FormLocalCheckBox("categories", "drs_" + category, 5, listFlow.next(20), categoryFilter.getBoolean(category)) {
                    @Override
                    public GameTooltips getTooltip() {
                        return new StringTooltips(Localization.translate("categories", "drs_" + category + "_desc"));
                    }
                }).onClicked(e -> {
                    MultitoolToolItem.getCategoryFilter(item).setBoolean(category, e.from.checked);
                    if (this.client != null)
                        this.client.network.sendPacket(new PacketUpdateGNDData(item, "filter"));
                }))
        );

        TileCategories.COMPUTED.keySet().forEach(category ->
                checkBoxes.put(category, this.addComponent(new FormLocalCheckBox("categories", "drs_" + category, 5, listFlow.next(20), categoryFilter.getBoolean(category)) {
                    @Override
                    public GameTooltips getTooltip() {
                        return new StringTooltips(Localization.translate("categories", "drs_" + category + "_desc"));
                    }
                }).onClicked(e -> {
                    MultitoolToolItem.getCategoryFilter(item).setBoolean(category, e.from.checked);
                    if (this.client != null)
                        this.client.network.sendPacket(new PacketUpdateGNDData(item, "filter"));
                }))
        );

        selectAll.onClicked(e -> {
            GNDItemMap map = MultitoolToolItem.getCategoryFilter(item);
            checkBoxes.forEach((cat, cb) -> {
                cb.checked = true;
                map.setBoolean(cat, true);
            });
            item.getGndData().setItem("filter", map);
            if (this.client != null)
                this.client.network.sendPacket(new PacketUpdateGNDData(item, "filter"));
        });
        selectNone.onClicked(e -> {
            checkBoxes.values().forEach(cb -> cb.checked = false);
            item.getGndData().setItem("filter", new GNDItemMap());
            if (this.client != null)
                this.client.network.sendPacket(new PacketUpdateGNDData(item, "filter"));
        });
        collapse.onClicked(e -> {
            FormTextButton btn = (FormTextButton)e.from;
            if (isCollapsed) {
                btn.setText("-");
                isCollapsed = false;
            } else {
                btn.setText("+");
                isCollapsed = true;
            }
        });

        formHeight = listFlow.next() + 5;
        this.setHeight(formHeight);
    }

    @Override
    public void onAdded(Client client) {
        super.onAdded(client);
        this.client = client;
        client.getLevel().hudManager.addElement(this.highlightDrawElement = new HudDrawElement() {
            @Override
            public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob player) {
                if (shouldUpdate) {
                    shouldUpdate = false;
                    highlights.clear();
                    final String currentCat = checkBoxes.entrySet().parallelStream()
                            .filter(e -> e.getValue().isHovering())
                            .map(Map.Entry::getKey)
                            .findAny()
                            .orElse("");
                    if (currentCat.isEmpty()) {
                        sineTimer = 0;
                        return;
                    }

                    final int lx = (camera.getX() / 32) - 1;
                    final int ly = (camera.getY() / 32) - 1;
                    final int hx = ((camera.getX() + camera.getWidth()) / 32) + 1;
                    final int hy = ((camera.getY() + camera.getHeight()) / 32) + 1;

                    for (int ty = ly; ty <= hy; ty++) {
                        for (int tx = lx; tx <= hx; tx++) {
                            if (MultitoolToolItem.isInCategory(player.getLevel(), tx, ty, currentCat)) {
                                highlights.add(new Point(tx, ty));
                            }
                        }
                    }
                }

                final int alpha = (int) (64 - (48 * Math.cos((sineTimer / (float)SINE_PULSE_PERIOD) * 2 * Math.PI)));
                final DrawOptionsList drawOptions = new DrawOptionsList();
                highlights.forEach(p -> {
                    final int drawX = camera.getTileDrawX(p.x);
                    final int drawY = camera.getTileDrawY(p.y);
                    if (drawX < -32 || drawX > camera.getWidth() + 32 || drawY < -32 || drawY > camera.getHeight() + 32)
                        return;

                    final GameLight light = player.getLevel().getLightLevel(p.x, p.y);
                    drawOptions.add(() -> Renderer.initQuadDraw(32, 32).color(new Color(64, 24, 192,  alpha), true).light(light).draw(drawX, drawY));
                });

                list.add(new SortedDrawable() {
                    @Override public int getPriority() { return 80; }
                    @Override public void draw(TickManager tm) { drawOptions.draw(); }
                });
            }
        });
    }

    @Override
    public void onRemoved(Client client) {
        super.onRemoved(client);
        if (this.highlightDrawElement != null) this.highlightDrawElement.remove();
    }

    @Override
    public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
        // for some reason this will be wonky when done from collapse.onClicked
        this.setHeight(isCollapsed ? 30 : formHeight);

        super.draw(tickManager, perspective, renderBox);

        float sineTimer = this.sineTimer + tickManager.getDelta();
        this.sineTimer = sineTimer % SINE_PULSE_PERIOD;

        if (!shouldUpdate) this.checkTimer += tickManager.getDelta();
        if (this.checkTimer > CHECK_INTERVAL) {
            this.checkTimer = 0;
            shouldUpdate = true;
        }
    }
}
