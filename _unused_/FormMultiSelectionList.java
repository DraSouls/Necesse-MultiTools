package drasouls.multitools.ui._unused_;

import drasouls.multitools.ui._unused_.custom.CustomFormGeneralList;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormEventsHandler;
import necesse.gfx.forms.events.FormIndexEvent;

// wtf is this recursive referencing
// god help me
public class FormMultiSelectionList<L extends FormMultiSelectionList<L, E>, E extends FormMultiSelectionElement<L, E>> extends CustomFormGeneralList<L, E> {
    private FormEventsHandler<FormIndexEvent<FormMultiSelectionList<L, E>>> onSelected = new FormEventsHandler<>();

    public FormMultiSelectionList(int x, int y, int width, int height, int elementHeight) {
        super(x, y, width, height, elementHeight);
    }

    public FormMultiSelectionList<L, E> onSelected(FormEventListener<FormIndexEvent<FormMultiSelectionList<L, E>>> listener) {
        this.onSelected.addListener(listener);
        return this;
    }

    public void toggleSelection(int index) {

    }
}
