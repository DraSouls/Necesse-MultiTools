package drasouls.multitools.ui._unused_.custom;

import necesse.engine.Screen;
import necesse.engine.Settings;
import necesse.engine.control.InputEvent;
import necesse.engine.control.MouseWheelBuffer;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.tickManager.TickManager;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.shader.FormShader;
import necesse.gfx.ui.HoverStateTextures;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class CustomFormGeneralList extends FormComponent implements FormPositionContainer {
    private FormPosition position;
    protected int width;
    protected int height;
    protected final int ELEMENT_PADDING = 16;
    protected List<E> elements;
    public final int elementHeight;
    protected int scroll;
    private final MouseWheelBuffer wheelBuffer = new MouseWheelBuffer(false);
    private int mouseDown;
    private long mouseDownTime;
    private float scrollBuffer;
    protected boolean isHoveringBot;
    protected boolean isHoveringTop;
    protected boolean isHoveringSpace;
    protected boolean acceptMouseRepeatEvents = false;

    public CustomFormGeneralList(int x, int y, int width, int height, int elementHeight) {
        this.position = new FormFixedPosition(x, y);
        this.width = width;
        this.height = height;
        this.elementHeight = elementHeight;
        this.reset();
    }

    public void reset() {
        this.elements = new ArrayList<>();
        this.resetScroll();
    }

    public void resetScroll() {
        this.scroll = 0;
    }

    public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
        MouseOverObject over;
        if (event.isMouseMoveEvent()) {
            this.isHoveringTop = this.isMouseOverTop(event);
            this.isHoveringBot = this.isMouseOverBot(event);
            this.isHoveringSpace = this.isMouseOverElementSpace(event);
            this.elements.forEach((e) -> {
                e.setMoveEvent(null);
            });
            over = this.getMouseOverObj(event);
            if (over != null) {
                this.elements.get(over.elementIndex).setMoveEvent(InputEvent.OffsetHudEvent(Screen.input(), event, -over.xOffset, -over.yOffset));
                event.useMove();
            }

            if (this.isHoveringTop || this.isHoveringBot || this.isHoveringSpace) {
                event.useMove();
            }
        } else if (event.isMouseWheelEvent()) {
            if (event.state && this.isMouseOverElementSpace(event)) {
                this.wheelBuffer.add(event, this.getScrollAmount());
                int amount = this.wheelBuffer.useAllScrollY();
                if (this.scroll(-amount)) {
                    this.playTickSound();
                    event.use();
                    this.handleInputEvent(InputEvent.MouseMoveEvent(event.pos, tickManager), tickManager, perspective);
                }
            }
        } else if (event.isMouseClickEvent() || this.acceptMouseRepeatEvents && event.getID() == -105) {
            if (event.isMouseClickEvent()) {
                if (event.state) {
                    this.mouseDownTime = System.currentTimeMillis() + 250L;
                    if (this.isMouseOverTop(event)) {
                        if (this.scrollUp()) {
                            this.playTickSound();
                        }

                        event.use();
                        this.mouseDown = 1;
                    } else if (this.isMouseOverBot(event)) {
                        if (this.scrollDown()) {
                            this.playTickSound();
                        }

                        event.use();
                        this.mouseDown = -1;
                    }
                } else {
                    this.mouseDown = 0;
                }
            }

            if (!event.isUsed()) {
                over = this.getMouseOverObj(event);
                if (over != null) {
                    if (event.state) {
                        this.elements.get(over.elementIndex).onClick((L) this, over.elementIndex, InputEvent.OffsetHudEvent(Screen.input(), event, -over.xOffset, -over.yOffset), perspective);
                        event.use();
                    } else {
                        event.use();
                    }
                }
            }
        }

    }

    public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
        this.handleDrawScroll(tickManager);
        if (this.elements.size() == 0) {
            this.drawEmptyMessage(tickManager);
        } else {
            int startIndex = Math.max(0, this.scroll / this.elementHeight);
            int spaceHeight = this.height - 32;
            int rowsPerSpace = spaceHeight / this.elementHeight + (spaceHeight % this.elementHeight == 0 ? 0 : 1);
            int endIndex = Math.min(this.elements.size(), startIndex + rowsPerSpace + (this.scroll % this.elementHeight == 0 ? 0 : 1));

            for (int i = startIndex; i < endIndex; ++i) {
                int elementY = i * this.elementHeight - this.scroll + 16;
                int drawX = this.getX();
                int drawY = this.getY() + elementY;
                int minDraw = Math.max(0, 16 - elementY);
                int maxDraw = Math.min(this.elementHeight, this.height - elementY - 16) - minDraw;
                FormShader.FormShaderState shaderState = GameResources.formShader.startState(new Point(drawX, drawY), new Rectangle(0, minDraw, this.width, maxDraw));

                try {
                    this.elements.get(i).draw((L) this, tickManager, perspective, i);
                } finally {
                    shaderState.end();
                }
            }
        }

        this.drawScrollButtons(tickManager);
    }

    public List<Rectangle> getHitboxes() {
        return singleBox(new Rectangle(this.getX(), this.getY(), this.width, this.height));
    }

    protected void handleDrawScroll(TickManager tickManager) {
        if (this.mouseDown > 0 && this.mouseDownTime < System.currentTimeMillis() && this.isHoveringTop) {
            this.scrollBuffer -= tickManager.getDelta() * 0.5F;
        } else if (this.mouseDown < 0 && this.mouseDownTime < System.currentTimeMillis() && this.isHoveringBot) {
            this.scrollBuffer += tickManager.getDelta() * 0.5F;
        }

        int scrollBuffer = (int)this.scrollBuffer;
        if (scrollBuffer != 0) {
            this.scroll(scrollBuffer);
            this.scrollBuffer -= (float)scrollBuffer;
        }

        this.limitMaxScroll();
    }

    protected void drawEmptyMessage(TickManager tickManager) {
        GameMessage message = this.getEmptyMessage();
        if (message != null) {
            FormShader.FormShaderState shaderState = GameResources.formShader.startState(null, new Rectangle(0, 16, this.width, this.height - 32));

            try {
                FontOptions options = (new FontOptions(this.getEmptyMessageFontOptions())).color(Settings.UI.activeTextColor);
                String s = message.translate();
                ArrayList<String> lines = GameUtils.breakString(s, options, this.width - 20);

                for(int i = 0; i < lines.size(); ++i) {
                    String line = lines.get(i);
                    int lineWidth = FontManager.bit.getWidthCeil(line, options);
                    FontManager.bit.drawString((float)(this.getX() + this.width / 2 - lineWidth / 2), (float)(this.getY() + 16 + i * options.getSize() + 4), line, options);
                }
            } finally {
                shaderState.end();
            }
        }

    }

    protected void drawScrollButtons(TickManager tickManager) {
        HoverStateTextures buttonTextures = Settings.UI.button_navigate_vertical;
        GameTexture topTexture = this.isHoveringTop ? buttonTextures.highlighted : buttonTextures.active;
        Color topColor = this.isHoveringTop ? Settings.UI.highlightElementColor : Settings.UI.activeElementColor;
        GameTexture botTexture = this.isHoveringBot ? buttonTextures.highlighted : buttonTextures.active;
        Color botColor = this.isHoveringBot ? Settings.UI.highlightElementColor : Settings.UI.activeElementColor;
        topTexture.initDraw().color(topColor).draw(this.getX() + this.width / 2 - topTexture.getWidth() / 2, this.getY() + 3);
        botTexture.initDraw().color(botColor).mirrorY().draw(this.getX() + this.width / 2 - botTexture.getWidth() / 2, this.getY() + this.height - 3 - botTexture.getHeight());
    }

    protected MouseOverObject getMouseOverObj(InputEvent event) {
        if (this.isMouseOverElementSpace(event)) {
            int startIndex = Math.max(0, this.scroll / this.elementHeight);
            int spaceHeight = this.height - 32;
            int rowsPerSpace = spaceHeight / this.elementHeight + (spaceHeight % this.elementHeight == 0 ? 0 : 1);
            int endIndex = Math.min(this.elements.size(), startIndex + rowsPerSpace + (this.scroll % this.elementHeight == 0 ? 0 : 1));

            for (int i = startIndex; i < endIndex; ++i) {
                MouseOverObject out = this.getMouseOffset(i, event);
                if (out.xOffset != -1 && out.yOffset != -1) {
                    return out;
                }
            }

        }
        return null;
    }

    protected E getMouseOverElement(InputEvent event) {
        MouseOverObject obj = this.getMouseOverObj(event);
        return obj != null ? this.elements.get(obj.elementIndex) : null;
    }

    protected MouseOverObject getMouseOffset(int index, InputEvent event) {
        int elementY = index * this.elementHeight - this.scroll + 16;
        int drawX = this.getX();
        int drawY = this.getY() + elementY;
        return this.getMouseOffset(index, event, drawX, drawY);
    }

    private MouseOverObject getMouseOffset(int index, InputEvent event, int drawX, int drawY) {
        int offsetX = event.pos.hudX - drawX;
        int offsetY = event.pos.hudY - drawY;
        if (offsetX < 0 || offsetX >= this.width) {
            drawX = -1;
        }

        if (offsetY < 0 || offsetY >= this.elementHeight) {
            drawY = -1;
        }

        return new MouseOverObject(index, drawX, drawY);
    }

    public boolean isMouseOverElementSpace(InputEvent event) {
        return !event.isMoveUsed() && (new Rectangle(this.getX(), this.getY() + 16, this.width, this.height - 32)).contains(event.pos.hudX, event.pos.hudY);
    }

    public boolean isMouseOverTop(InputEvent event) {
        return !event.isMoveUsed() && (new Rectangle(this.getX() + this.width / 2 - 16, this.getY() + 3, 32, 10)).contains(event.pos.hudX, event.pos.hudY);
    }

    public boolean isMouseOverBot(InputEvent event) {
        return !event.isMoveUsed() && (new Rectangle(this.getX() + this.width / 2 - 16, this.getY() + this.height - 13, 32, 10)).contains(event.pos.hudX, event.pos.hudY);
    }

    public int getScrollAmount() {
        return 20;
    }

    public boolean scrollUp() {
        return this.scroll(-this.getScrollAmount());
    }

    public boolean scrollDown() {
        return this.scroll(this.getScrollAmount());
    }

    public boolean scroll(int amount) {
        int oldScroll = this.scroll;
        this.scroll += amount;
        if (this.scroll < 0) {
            this.scroll = 0;
        }

        this.limitMaxScroll();
        if (oldScroll != this.scroll) {
            Screen.submitNextMoveEvent();
            return true;
        } else {
            return false;
        }
    }

    public void limitMaxScroll() {
        int maxScroll = Math.max(0, (this.elements.size() - 1) * this.elementHeight - (this.height - 32 - this.elementHeight));
        if (this.scroll > maxScroll) {
            this.scroll = maxScroll;
        }

    }

    public GameMessage getEmptyMessage() {
        return null;
    }

    public FontOptions getEmptyMessageFontOptions() {
        return new FontOptions(12);
    }

    public FormPosition getPosition() {
        return this.position;
    }

    public void setPosition(FormPosition position) {
        this.position = position;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    protected static class MouseOverObject {
        public int elementIndex;
        public int xOffset;
        public int yOffset;

        public MouseOverObject(int elementIndex, int xOffset, int yOffset) {
            this.elementIndex = elementIndex;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
        }
    }
}
