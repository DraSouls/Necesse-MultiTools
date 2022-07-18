package drasouls.multitools.ui._unused_;

import drasouls.multitools.ui._unused_.custom.CustomFormGeneralList;
import drasouls.multitools.ui._unused_.custom.CustomFormListElement;
import necesse.engine.control.InputEvent;
import necesse.engine.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;

public class FormCheckboxList extends CustomFormGeneralList<FormCheckboxList, FormCheckboxList.CheckboxElement> {
    public FormCheckboxList(int x, int y, int width, int height, int elementHeight) {
        super(x, y, width, height, elementHeight);
    }

    public class CheckboxElement extends CustomFormListElement<FormCheckboxList, CheckboxElement> {
        public String str;

        public CheckboxElement(String str) {
            this.str = str;
        }

        void draw(FormCheckboxList parent, TickManager tickManager, PlayerMob perspective, int elementIndex) {

        }

        @Override
        void onClick(FormCheckboxList parent, int elementIndex, InputEvent event, PlayerMob perspective) {
            super.onClick(parent, elementIndex, event, perspective);
        }
    }
}
