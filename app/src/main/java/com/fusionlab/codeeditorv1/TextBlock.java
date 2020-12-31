package com.fusionlab.codeeditorv1;

public class TextBlock {

    public static final int TYPE_IF = 10;
    public static final int TYPE_IF_ELSE = 11;
    public static final int TYPE_EL_IF = 12;
    public static final int TYPE_ELSE = 13;
    public static final int TYPE_FOR_IN = 20;
    public static final int TYPE_WHILE = 21;
    public static final int TYPE_FUNCTION = 30;

    public static final int TYPE_LCD_PRINT = 100;
    public static final int TYPE_LCD_CLEAR = 101;
    public static final int TYPE_LCD_DRAW_BOX = 102;
    public static final int TYPE_LCD_DRAW_CIRCLE = 103;
    public static final int TYPE_LCD_DRAW_LINE = 104;

    public static final int TYPE_MONITOR_PRINT = 100;
    public static final int TYPE_MONITOR_CLEAR = 101;
    public static final int TYPE_MONITOR_DRAW_BOX = 102;
    public static final int TYPE_MONITOR_DRAW_CIRCLE = 103;
    public static final int TYPE_MONITOR_DRAW_LINE = 104;

    private static final String[] IF_TEMPLATE_TEXT_ITEMS = {"if ＿＿＿ :\n", "\t＿＿＿＿\n"};
    private static final String[] IF_ELSE_TEMPLATE_TEXT_ITEMS = {"if ＿＿＿ :\n", "\t＿＿＿＿\n", "else :\n", "\t＿＿＿＿\n"};

    private static final String[] MONITOR_PRINT_TEXT_ITEMS = {"display.print_line(＿＿＿＿)"};
    private static final String[] MONITOR_CLEAR_ITEMS = {"display.clear()"};
    private static final String[] MONITOR_DRAW_LINE_ITEMS = {"display.draw_line(＿＿＿＿, ＿＿＿＿)"};
    private static final String[] MONITOR_DRAW_CIRCLE_ITEMS = {"display.draw_circle(＿＿＿＿)"};
    private static final String[] MONITOR_DRAW_BOX_ITEMS = {"display.draw_box(＿＿＿＿)"};

    private String text;
    private int focusPoint;

    public TextBlock(String text, int focusPoint) {
        this.text = text;
        this.focusPoint = focusPoint;
    }

    public String getText() {
        return text;
    }

    public int getFocusPoint() {
        return focusPoint;
    }

    public static TextBlock generateTextBlock(int nestedLevel, int type) {

        switch (type) {
            case TYPE_LCD_CLEAR: {
                StringBuffer buffer = new StringBuffer(MONITOR_CLEAR_ITEMS[0]);
                return new TextBlock(buffer.toString(), 0);
            }
            case TYPE_LCD_PRINT: {
                StringBuffer buffer = new StringBuffer(MONITOR_PRINT_TEXT_ITEMS[0]);
                return new TextBlock(buffer.toString(), 0);
            }
            case TYPE_LCD_DRAW_LINE: {
                StringBuffer buffer = new StringBuffer(MONITOR_DRAW_LINE_ITEMS[0]);
                return new TextBlock(buffer.toString(), 0);
            }
            case TYPE_LCD_DRAW_CIRCLE: {
                StringBuffer buffer = new StringBuffer(MONITOR_DRAW_CIRCLE_ITEMS[0]);
                return new TextBlock(buffer.toString(), 0);
            }
            case TYPE_LCD_DRAW_BOX: {
                StringBuffer buffer = new StringBuffer(MONITOR_DRAW_BOX_ITEMS[0]);
                return new TextBlock(buffer.toString(), 0);
            }
            case TYPE_IF: {
                StringBuffer buffer = new StringBuffer(IF_TEMPLATE_TEXT_ITEMS[0]);

                for (int i = 1; i < IF_TEMPLATE_TEXT_ITEMS.length; i++) {
                    String line = IF_TEMPLATE_TEXT_ITEMS[i];

                    for (int j = 0; j < nestedLevel; j++) {
                        buffer.append('\t');
                    }
                    buffer.append(line);
                }
                return new TextBlock(buffer.toString(), 4);
            }
            case TYPE_IF_ELSE: {
                StringBuffer buffer = new StringBuffer(IF_ELSE_TEMPLATE_TEXT_ITEMS[0]);

                for (int i = 1; i < IF_ELSE_TEMPLATE_TEXT_ITEMS.length; i++) {
                    String line = IF_ELSE_TEMPLATE_TEXT_ITEMS[i];

                    for (int j = 0; j < nestedLevel; j++) {
                        buffer.append('\t');
                    }
                    buffer.append(line);
                }
                return new TextBlock(buffer.toString(), 0);
            }
        }
        return new TextBlock("", 0);
    }
}
