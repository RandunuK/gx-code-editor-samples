package com.fusionlab.codeeditorv1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ReplacementSpan;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class PythonEditorView extends AppCompatEditText implements View.OnTouchListener {
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }

    public interface OnTextChangedListener {
        void onTextChanged(String text);
    }

    public static final String CONDITION_LINE = "＿＿＿";
    public static final String STATEMENT_LINE = "＿＿＿＿";

    private static final String[] SELECTABLE_WORD_LIST = {CONDITION_LINE, STATEMENT_LINE};

    private static final Pattern PATTERN_LINE = Pattern.compile(
            ".*\\n");
    private static final Pattern PATTERN_NUMBERS = Pattern.compile("(?:\\b(?=\\d)|\\B(?=\\.))(?:0[bo])?(?:(?:\\d|0x[\\da-f])[\\da-f]*\\.?\\d*|\\.\\d+)(?:e[+-]?\\d+)?j?\\b", CASE_INSENSITIVE);
    private static final Pattern PATTERN_PREPROCESSOR = Pattern.compile("^[\t ]*(__debug__)\\b", Pattern.MULTILINE);
    private static final Pattern PATTERN_KEYWORDS = Pattern.compile("\\b(?:as|assert|async|await|break|class|continue|def|del|elif|else|except|exec|finally|for|from|global|if|import|in|is|lambda|nonlocal|pass|print|raise|return|try|while|with|yield)\\b");
    private static final Pattern PATTERN_BUILTINS = Pattern.compile("\\b(?:__import__|abs|all|any|apply|ascii|basestring|bin|bool|buffer|bytearray|bytes|callable|chr|classmethod|cmp|coerce|compile|complex|delattr|dict|dir|divmod|enumerate|eval|execfile|file|filter|float|format|frozenset|getattr|globals|hasattr|hash|help|hex|id|input|int|intern|isinstance|issubclass|iter|len|list|locals|long|map|max|memoryview|min|next|object|oct|open|ord|pow|property|range|raw_input|reduce|reload|repr|reversed|round|set|setattr|slice|sorted|staticmethod|str|sum|super|tuple|type|unichr|unicode|vars|xrange|zip)\\b");
    private static final Pattern PATTERN_COMMENTS = Pattern.compile("(^|[^\\\\])#.*");
    private static final Pattern PATTERN_TRAILING_WHITE_SPACE = Pattern.compile(
            "[\\t ]+$",
            Pattern.MULTILINE);
    private static final Pattern PATTERN_INSERT_UNIFORM = Pattern.compile(
            "^([ \t]*uniform.+)$",
            Pattern.MULTILINE);
    private static final Pattern PATTERN_ENDIF = Pattern.compile(
            "(#endif)\\b");
    private static final Pattern PATTERN_OPERATOR = Pattern.compile("[-+%=]=?|!=|\\*\\*?=?|\\/\\/?=?|<[<=>]?|>[=>]?|[&|^~]|\\b(?:or|and|not)\\b");
    private static final Pattern PATTERN_PUNCTUATION = Pattern.compile("[{}\\[\\];(),.:]");
    private static final Pattern PATTERN_CLASS_NAME = Pattern.compile("(\\bclass\\s+)\\w+", CASE_INSENSITIVE);
    private static final Pattern PATTERN_FUNCTION = Pattern.compile("((?:^|\\s)def[ \\t]+)[a-zA-Z_]\\w*(?=\\s*\\()");
    private static final Pattern PATTERN_STRING = Pattern.compile("(\"|')(?:\\\\.|(?!\\1)[^\\\\\\r\\n])*\\1");
    private static final Pattern PATTERN_TRIPLE_QUOTED_STRING = Pattern.compile("(\"|')(?:\\\\.|(?!\\1)[^\\\\\\r\\n])*\\1");
    private static final Pattern PATTERN_BOOLEAN = Pattern.compile("\\b(?:True|False|None)\\b");

    private final Handler updateHandler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Editable e = getText();

            if (onTextChangedListener != null) {
                onTextChangedListener.onTextChanged(
                        removeNonAscii(e.toString()));
            }

            highlightWithoutChange(e);
        }
    };

    private OnSelectionChangeListener onSelectionChangeListener;
    private OnTextChangedListener onTextChangedListener;
    private int updateDelay = 1000;
    private int errorLine = 0;
    private boolean dirty = false;
    private boolean modified = true;
    private int colorString;
    private int colorClassName;
    private int colorFunction;
    private int colorOperator;
    private int colorPunctuation;
    private int colorBool;
    private int colorError;
    private int colorNumber;
    private int colorKeyword;
    private int colorBuiltin;
    private int colorComment;
    private int tabWidthInCharacters = 0;
    private int tabWidth = 0;
    private float lineNumberWidth;
    private Rect rect;
    private Paint paint;
    private SelectedWord mSelectedWord;
    private boolean selected;
    private ArrayMap<Integer, Sentence> mLineMap;
    private ArrayMap<String, String> mAliasMap;
    private List<String> mAliasList;
    private List<String> mModuleNames;
    private int lastLineStartCursorPosition;
    private int lastLineEndCursorPosition;

    public static String removeNonAscii(String text) {
        return text.replaceAll("[^\\x0A\\x09\\x20-\\x7E]", "");
    }

    public PythonEditorView(Context context) {
        super(context);
        init(context);
    }

    public PythonEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /*@Override
    protected void onDraw(Canvas canvas) {
        //draw line numbers & vertical line
        int baseline = getBaseline();
        for (int i = 0; i < getLineCount(); i++) {
            canvas.drawText(" " + (i + 1), rect.left, baseline, paint);
            canvas.drawLine(lineNumberWidth, baseline, lineNumberWidth, baseline - getLineHeight(), paint);
            baseline += getLineHeight();
        }
        super.onDraw(canvas);
    }*/


    public void setOnTextChangedListener(OnTextChangedListener listener) {
        onTextChangedListener = listener;
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        onSelectionChangeListener = listener;
    }

    public void setUpdateDelay(int ms) {
        updateDelay = ms;
    }

    public void setTabWidth(int characters) {
        if (tabWidthInCharacters == characters) {
            return;
        }

        tabWidthInCharacters = characters;
        tabWidth = Math.round(getPaint().measureText("m") * characters);
    }

    public boolean hasErrorLine() {
        return errorLine > 0;
    }

    public void setErrorLine(int line) {
        errorLine = line;
    }

    public void updateHighlighting() {
        highlightWithoutChange(getText());
    }

    public boolean isModified() {
        return dirty;
    }

    public void setTextHighlighted(CharSequence text) {
        if (text == null) {
            text = "";
        }

        cancelUpdate();

        errorLine = 0;
        dirty = false;

        modified = false;
        String src = removeNonAscii(text.toString());
        setText(highlight(new SpannableStringBuilder(src)));
        modified = true;

        if (onTextChangedListener != null) {
            onTextChangedListener.onTextChanged(src);
        }
    }

    public String getCleanText() {
        return PATTERN_TRAILING_WHITE_SPACE
                .matcher(getText())
                .replaceAll("");
    }

    public String getPreparedText() {
        return getText().toString()
                .replaceAll("\r\n|\n", "\n").replace("\n", "\\n");
    }


    public void insertTab() {
        int start = getSelectionStart();
        int end = getSelectionEnd();

        getText().replace(
                Math.min(start, end),
                Math.max(start, end),
                "\t",
                0,
                1);
    }

    public void addUniform(String statement) {
        if (statement == null) {
            return;
        }

        Editable e = getText();
        removeUniform(e, statement);

        Matcher m = PATTERN_INSERT_UNIFORM.matcher(e);
        int start = -1;

        while (m.find()) {
            start = m.end();
        }

        if (start > -1) {
            // add line break before statement because it's
            // inserted before the last line-break
            statement = "\n" + statement;
        } else {
            // add a line break after statement if there's no
            // uniform already
            statement += "\n";

            // add an empty line between the last #endif
            // and the now following uniform
            if ((start = endIndexOfLastEndIf(e)) > -1) {
                statement = "\n" + statement;
            }

            // move index past line break or to the start
            // of the text when no #endif was found
            ++start;
        }

        e.insert(start, statement);
    }

    private void removeUniform(Editable e, String statement) {
        if (statement == null) {
            return;
        }

        String regex = "^(" + statement.replace(" ", "[ \\t]+");
        int p = regex.indexOf(";");
        if (p > -1) {
            regex = regex.substring(0, p);
        }
        regex += ".*\\n)$";

        Matcher m = Pattern.compile(regex, Pattern.MULTILINE).matcher(e);
        if (m.find()) {
            e.delete(m.start(), m.end());
        }
    }

    private int endIndexOfLastEndIf(Editable e) {
        Matcher m = PATTERN_ENDIF.matcher(e);
        int idx = -1;

        while (m.find()) {
            idx = m.end();
        }

        return idx;
    }

    private void init(Context context) {

        setHorizontallyScrolling(true);
        setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(
                    CharSequence source,
                    int start,
                    int end,
                    Spanned dest,
                    int dstart,
                    int dend) {
                if (modified && end - start == 1 && start < source.length() && dstart < dest.length()) {
                    char c = source.charAt(start);

                    if (c == '\n') {
                        return autoIndent(source, dest, dstart, dend);
                    }
                }
                return source;
            }
        }});

        addTextChangedListener(new TextWatcher() {
            private int start = 0;
            private int count = 0;

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {
                this.start = start;
                this.count = count;
            }

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                cancelUpdate();
                convertTabs(e, start, count);

                if (!modified) {
                    return;
                }

                dirty = true;
                updateHandler.postDelayed(updateRunnable, updateDelay);
            }
        });

        setSyntaxColors(context);
        setUpdateDelay(400);
        setTabWidth(2);

       /* float editorTextSize = getTextSize();
        rect = new Rect();
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.LTGRAY);
        paint.setTextSize(editorTextSize * 0.8f);
        float mWidth = paint.measureText("M");
        int paddingLeft = (int) ((int) mWidth * 3.2);
        lineNumberWidth = mWidth * 2.8f;
        setPadding(paddingLeft, 0, 0, 0);*/
    }

    private void setSyntaxColors(Context context) {
        colorString = ContextCompat.getColor(
                context,
                R.color.syntax_string);
        colorFunction = ContextCompat.getColor(
                context,
                R.color.syntax_function);
        colorClassName = ContextCompat.getColor(
                context,
                R.color.syntax_class);
        colorPunctuation = ContextCompat.getColor(
                context,
                R.color.syntax_punctuation);
        colorBool = ContextCompat.getColor(
                context,
                R.color.syntax_bool);
        colorOperator = ContextCompat.getColor(
                context,
                R.color.syntax_operator);
        colorError = ContextCompat.getColor(
                context,
                R.color.syntax_error);
        colorNumber = ContextCompat.getColor(
                context,
                R.color.syntax_number);
        colorKeyword = ContextCompat.getColor(
                context,
                R.color.syntax_keyword);
        colorBuiltin = ContextCompat.getColor(
                context,
                R.color.syntax_builtin);
        colorComment = ContextCompat.getColor(
                context,
                R.color.syntax_comment);
    }

    private void cancelUpdate() {
        updateHandler.removeCallbacks(updateRunnable);
    }

    private void highlightWithoutChange(Editable e) {
        modified = false;
        highlight(e);
        modified = true;
    }

    private Editable highlight(Editable e) {
        try {
            int length = e.length();

            // don't use e.clearSpans() because it will
            // remove too much
            clearSpans(e, length);

            if (length == 0) {
                return e;
            }

            if (errorLine > 0) {
                Matcher m = PATTERN_LINE.matcher(e);

                for (int i = errorLine; i-- > 0 && m.find(); ) {
                    // {} because analyzers don't like for (); statements
                }

                e.setSpan(
                        new BackgroundColorSpan(colorError),
                        m.start(),
                        m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            /*if (ShaderEditorApp.preferences.disableHighlighting() &&
                    length > 4096) {
                return e;
            }*/


            for (Matcher m = PATTERN_NUMBERS.matcher(e); m.find(); ) {
                e.setSpan(
                        new ForegroundColorSpan(colorNumber),
                        m.start(),
                        m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            for (Matcher m = PATTERN_STRING.matcher(e); m.find(); ) {
                e.setSpan(
                        new ForegroundColorSpan(colorString),
                        m.start(),
                        m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            for (Matcher m = PATTERN_CLASS_NAME.matcher(e); m.find(); ) {
                e.setSpan(
                        new ForegroundColorSpan(colorClassName),
                        m.start(),
                        m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            for (Matcher m = PATTERN_FUNCTION.matcher(e); m.find(); ) {
                e.setSpan(
                        new ForegroundColorSpan(colorFunction),
                        m.start(),
                        m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            for (Matcher m = PATTERN_TRIPLE_QUOTED_STRING.matcher(e); m.find(); ) {
                e.setSpan(
                        new ForegroundColorSpan(colorString),
                        m.start(),
                        m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            for (Matcher m = PATTERN_BOOLEAN.matcher(e); m.find(); ) {
                e.setSpan(
                        new ForegroundColorSpan(colorBool),
                        m.start(),
                        m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            for (Matcher m = PATTERN_PUNCTUATION.matcher(e); m.find(); ) {
                e.setSpan(
                        new ForegroundColorSpan(colorPunctuation),
                        m.start(),
                        m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }


            for (Matcher m = PATTERN_PREPROCESSOR.matcher(e); m.find(); ) {
                e.setSpan(
                        new ForegroundColorSpan(colorKeyword),
                        m.start(),
                        m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            for (Matcher m = PATTERN_OPERATOR.matcher(e); m.find(); ) {
                e.setSpan(
                        new ForegroundColorSpan(colorOperator),
                        m.start(),
                        m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            for (Matcher m = PATTERN_KEYWORDS.matcher(e); m.find(); ) {
                e.setSpan(
                        new ForegroundColorSpan(colorKeyword),
                        m.start(),
                        m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            for (Matcher m = PATTERN_BUILTINS.matcher(e); m.find(); ) {
                e.setSpan(
                        new ForegroundColorSpan(colorBuiltin),
                        m.start(),
                        m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            for (Matcher m = PATTERN_COMMENTS.matcher(e); m.find(); ) {
                e.setSpan(
                        new ForegroundColorSpan(colorComment),
                        m.start(),
                        m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } catch (IllegalStateException ex) {
            // raised by Matcher.start()/.end() when
            // no successful match has been made what
            // shouldn't ever happen because of find()
        }

        return e;
    }

    private static void clearSpans(Editable e, int length) {
        // remove foreground color spans
        {
            ForegroundColorSpan[] spans = e.getSpans(
                    0,
                    length,
                    ForegroundColorSpan.class);

            for (int i = spans.length; i-- > 0; ) {
                e.removeSpan(spans[i]);
            }
        }

        // remove background color spans
        {
            BackgroundColorSpan[] spans = e.getSpans(
                    0,
                    length,
                    BackgroundColorSpan.class);

            for (int i = spans.length; i-- > 0; ) {
                e.removeSpan(spans[i]);
            }
        }
    }

    private CharSequence autoIndent(
            CharSequence source,
            Spanned dest,
            int dstart,
            int dend) {
        String indent = "";
        int istart = dstart - 1;

        // find start of this line
        boolean dataBefore = false;
        int pt = 0;

        for (; istart > -1; --istart) {
            char c = dest.charAt(istart);

            if (c == '\n') {
                break;
            }

            if (c != ' ' && c != '\t') {
                if (!dataBefore) {
                    // indent always after those characters
                    if (c == '{' ||
                            c == '+' ||
                            c == '-' ||
                            c == '*' ||
                            c == '/' ||
                            c == '%' ||
                            c == '^' ||
                            c == '=') {
                        --pt;
                    }

                    dataBefore = true;
                }

                // parenthesis counter
                if (c == '(') {
                    --pt;
                } else if (c == ')') {
                    ++pt;
                }
            }
        }

        // copy indent of this line into the next
        if (istart > -1) {
            char charAtCursor = dest.charAt(dstart);
            int iend;

            for (iend = ++istart; iend < dend; ++iend) {
                char c = dest.charAt(iend);

                // auto expand comments
                if (charAtCursor != '\n' &&
                        c == '/' &&
                        iend + 1 < dend &&
                        dest.charAt(iend) == c) {
                    iend += 2;
                    break;
                }

                if (c != ' ' && c != '\t') {
                    break;
                }
            }

            indent += dest.subSequence(istart, iend);
        }

        // add new indent
        if (pt < 0) {
            indent += "\t";
        }

        // append white space of previous line and new indent
        return source + indent;
    }

    private void convertTabs(Editable e, int start, int count) {
        if (tabWidth < 1) {
            return;
        }

        String s = e.toString();

        for (int stop = start + count;
             (start = s.indexOf("\t", start)) > -1 && start < stop;
             ++start) {
            e.setSpan(
                    new TabWidthSpan(tabWidth),
                    start,
                    start + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static class TabWidthSpan extends ReplacementSpan {
        private int width;

        private TabWidthSpan(int width) {
            this.width = width;
        }

        @Override
        public int getSize(
                @NonNull Paint paint,
                CharSequence text,
                int start,
                int end,
                Paint.FontMetricsInt fm) {
            return width;
        }

        @Override
        public void draw(
                @NonNull Canvas canvas,
                CharSequence text,
                int start,
                int end,
                float x,
                int top,
                int y,
                int bottom,
                @NonNull Paint paint) {
        }
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {

        return super.onCreateInputConnection(outAttrs);
    }

    public void setModuleNames(ArrayList<String> moduleNames) {
        this.mModuleNames = moduleNames;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        if (onSelectionChangeListener != null) {
            onSelectionChangeListener.onSelectionChanged(selStart, selEnd);
        }

        if (selected) {
            super.onSelectionChanged(selStart, selEnd);
            return;
        }
        String source = this.getText().toString();

        // get all sentences
        BreakIterator iterator = BreakIterator.getSentenceInstance();
        iterator.setText(source);

        if (mLineMap == null) {
            mLineMap = new ArrayMap<>();
            mAliasMap = new ArrayMap<>();
            mAliasList = new ArrayList<>();
        } else {
            mAliasList.clear();
            mAliasMap.clear();
            mLineMap.clear();
        }


        int lineNumber = 0;
        int start = iterator.first();

        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sText = source.substring(start, end);
            Sentence sentence = new Sentence(sText, start);
            String moduleName = sentence.getModuleName();
            String alias = sentence.getAlias();

            if (sentence.getModuleName() != null) {
                mAliasMap.put(alias, moduleName);
                mAliasList.add(alias);
            }
            mLineMap.put(++lineNumber, sentence);
            lastLineEndCursorPosition = end;
            lastLineStartCursorPosition = start;
        }

        // assuming that only the cursor is showing, no selected range
        if (selStart < 0) {
            mSelectedWord = null;
            return;
        }

        // initialize the BreakIterator
        iterator = BreakIterator.getWordInstance();
        iterator.setText(source);

        // find the word boundaries before and after the cursor position
        int wordStart;

        if (iterator.isBoundary(selStart)) {
            wordStart = selStart;
        } else {
            wordStart = iterator.preceding(selStart);
        }
        int wordEnd = iterator.following(selStart);

        if (wordEnd < 0) {
            mSelectedWord = null;
            return;
        }

        // get the word
        String word = this.getText().subSequence(wordStart, wordEnd).toString();
        boolean contains = Arrays.asList(SELECTABLE_WORD_LIST).contains(word);

        if (!contains) {
            mSelectedWord = null;
            return;
        }

        if (mSelectedWord == null || !mSelectedWord.toString().equals(word + '#' + wordStart)) {
            mSelectedWord = new SelectedWord(word, wordStart);
            selected = true;
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    setSelection(wordStart, wordEnd);
                    selected = false;
                }
            }, 50);
        }
    }

    private void addExtraLines(int lineCount, int currentCursor) {

        if (currentCursor >= lastLineStartCursorPosition) {
            getText().insert(currentCursor, "\n");
        }
    }


    public void insertTextBlock(int type) {
        int currentCursor = getSelectionStart();

        addExtraLines(3, currentCursor);
        // initialize the BreakIterator
        BreakIterator iterator = BreakIterator.getLineInstance();
        iterator.setText(this.getText().toString());
        int textLength = this.getText().toString().length();

        int lineStart;
        if (iterator.isBoundary(currentCursor)) {
            lineStart = currentCursor;
        } else {
            lineStart = iterator.preceding(currentCursor);
        }
        int lineEnd = iterator.following(currentCursor);

        // get the line
        String line = this.getText().toString().substring(lineStart, lineEnd);
        int nestedLevel = countChar(line, '\t');
        TextBlock textBlock = TextBlock.generateTextBlock(nestedLevel, type);
        getText().insert(currentCursor, textBlock.getText());
        setSelection(currentCursor + textBlock.getFocusPoint());
    }

    public void replaceTextBlock(int type) {
        int currentCursorStart = getSelectionStart();
        int currentCursorEnd = getSelectionEnd();

        getText().delete(currentCursorStart, currentCursorEnd);

        addExtraLines(3, currentCursorStart);

        // initialize the BreakIterator
        BreakIterator iterator = BreakIterator.getLineInstance();
        iterator.setText(this.getText().toString());

        int lineStart;
        if (iterator.isBoundary(currentCursorStart)) {
            lineStart = currentCursorStart;
        } else {
            lineStart = iterator.preceding(currentCursorStart);
        }
        int lineEnd = iterator.following(currentCursorStart);

        // get the line
        String line = this.getText().toString().substring(lineStart, lineEnd);
        int nestedLevel = countChar(line, '\t');
        TextBlock textBlock = TextBlock.generateTextBlock(nestedLevel, type);
        getText().insert(currentCursorStart, textBlock.getText());
        setSelection(currentCursorStart + textBlock.getFocusPoint());
        //getText().replace(currentCursorStart, currentCursorEnd, TextBlock.generateTextBlock(nestedLevel, type));
    }


    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selStart, int selEnd);
    }

    public static class SelectedWord {
        String word;
        int startIndex;
        int endIndex;

        public SelectedWord(@NonNull String word, int startIndex) {
            this.word = word;
            this.startIndex = startIndex;
            this.endIndex = this.startIndex + word.length();
        }

        @Override
        public String toString() {
            return word + '#' + startIndex;
        }
    }

    public static class Sentence {
        String text;
        List<String> words;
        int lastWordIndex;
        int startIndex;
        int endIndex;

        public Sentence(@NonNull String text, int startIndex) {
            this.text = text;
            this.startIndex = startIndex;
            this.endIndex = this.startIndex + text.length();
            words = Arrays.asList(text.trim().split("\\b"));
            lastWordIndex = words.size() - 1;
        }

        public String getModuleName() {
            if (words.contains("import")) {
                int moduleIndex = words.indexOf("import") + 2;
                if (moduleIndex <= lastWordIndex) {
                    return words.get(moduleIndex);
                }
            }
            return null;
        }

        public String getAlias() {
            if (words.contains("import")) {
                return words.get(lastWordIndex);
            }
            return null;
        }

        @Override
        public String toString() {
            return text + '#' + startIndex;
        }

    }

    public static int countChar(String str, char c) {
        int count = 0;

        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c)
                count++;
        }

        return count;
    }
}
