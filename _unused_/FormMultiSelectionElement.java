package drasouls.multitools.ui._unused_;

import drasouls.multitools.ui._unused_.custom.CustomFormGeneralList;
import drasouls.multitools.ui._unused_.custom.CustomFormListElement;
import necesse.engine.control.InputEvent;
import necesse.engine.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;

// god knows why these generics are needed to prevent warnings
public abstract class FormMultiSelectionElement extends CustomFormListElement {
    private boolean selected;

    public FormMultiSelectionElement() {
    }

    @Override
    void draw(CustomFormGeneralList var1, TickManager var2, PlayerMob var3, int var4) {

    }


    public final boolean isSelected() {
        return this.selected;
    }

    public final void clearSelected() {
        this.selected = false;
    }

    final void makeSelected(FormMultiSelectionList<L, E> parent) {
        if (parent != null) {
            this.selected = true;
        }
    }

    protected boolean onlyAcceptLeftClick() {
        return true;
    }



    void onClick(L parent, int elementIndex, InputEvent event, PlayerMob perspective) {
        if (!this.onlyAcceptLeftClick() || event.getID() == -100) {
            parent.toggleSelection(elementIndex);
        }
    }
}
