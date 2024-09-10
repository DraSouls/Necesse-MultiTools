package drasouls.multitools.ui;

import necesse.engine.network.client.Client;
import necesse.gfx.forms.components.containerSlot.FormContainerSlot;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.presets.containerComponent.item.ItemInventoryContainerForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.container.item.ItemInventoryContainer;

public class PlannerContainerForm extends ItemInventoryContainerForm<ItemInventoryContainer> {
    public PlannerContainerForm(Client client, ItemInventoryContainer container) {
        super(client, container);
    }

    @Override
    protected void addSlots(FormFlow flow) {
        this.slots = new FormContainerSlot[this.container.INVENTORY_END - this.container.INVENTORY_START + 1];
        int currentY = flow.next(34);
        FormLocalLabel label = this.addComponent(new FormLocalLabel("ui", "drs_plannerputitem", new FontOptions(16), -1, 5, currentY+10));
        this.slots[0] = this.addComponent(new FormContainerSlot(this.client, (this.container).INVENTORY_START, label.getBoundingBox().width + 10, currentY));
    }
}
