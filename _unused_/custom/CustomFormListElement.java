package drasouls.multitools.ui._unused_.custom;

import necesse.engine.control.InputEvent;
import necesse.engine.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;

import java.awt.*;

// AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
// I have to do this because FormListElement's abstract methods can't be implemented because of type erasure or something
public abstract class CustomFormListElement {
    private InputEvent moveEvent;

    public CustomFormListElement() {
    }

    public void setMoveEvent(InputEvent moveEvent) {
        this.moveEvent = moveEvent;
    }

    public InputEvent getMoveEvent() {
        return this.moveEvent;
    }

    public boolean isHovering() {
        return this.moveEvent != null;
    }

    abstract void draw(CustomFormGeneralList var1, TickManager var2, PlayerMob var3, int var4);

    abstract void onClick(CustomFormGeneralList var1, int var2, InputEvent var3, PlayerMob var4);

    public boolean isMouseOver(CustomFormGeneralList parent) {
        InputEvent event = this.getMoveEvent();
        return event != null && (new Rectangle(0, 0, parent.width, parent.elementHeight)).contains(event.pos.hudX, event.pos.hudY);
    }
}
