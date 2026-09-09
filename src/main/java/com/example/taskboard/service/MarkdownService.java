package com.example.taskboard.service;

import com.vladsch.flexmark.ast.AutoLink;
import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.MailLink;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.ast.WhiteSpace;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItem;
import com.vladsch.flexmark.ext.gfm.tables.TableBlock;
import com.vladsch.flexmark.ext.gfm.tables.TableRow;
import com.vladsch.flexmark.ext.gfm.tables.TableSeparator;
import com.vladsch.flexmark.ext.gfm.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown parsing and task-toggle logic for card descriptions, backed by
 * the mature Flexmark parser (flexmark-java).
 *
 * <p>Card descriptions are stored as raw Markdown. This service converts
 * the text into blocks (headings, paragraphs, list items including nested
 * lists, task items, fenced code blocks, block quotes including nested
 * quotes, tables) and inline segments (bold, italic, code, links) that the
 * view layer renders as plain JavaFX nodes. No HTML
 * is generated and no script is executed: unknown or HTML-like constructs
 * are rendered as plain text, and only {@code http}/{@code https} links
 * are treated as links.</p>
 */
public class MarkdownService {

    /** A parsed Markdown block. */
    public sealed interface MarkdownBlock
            permits HeadingBlock, ParagraphBlock, ListItemBlock, TaskItemBlock, CodeBlock,
            BlockQuoteBlock, TableBlock {
    }

    /** A heading: {@code #} through {@code ######}. */
    public record HeadingBlock(int level, String text) implements MarkdownBlock {
    }

    /** A plain paragraph (consecutive non-special lines are joined). */
    public record ParagraphBlock(String text) implements MarkdownBlock {
    }

    /**
     * An unordered ({@code -}/{@code *}) or ordered ({@code 1.}) list item.
     * {@code indent} is the nesting depth (0 = top level).
     */
    public record ListItemBlock(boolean ordered, int number, String text, int indent) implements MarkdownBlock {
    }

    /**
     * A task-list item ({@code - [ ]} / {@code - [x]}).
     * {@code taskIndex} is the zero-based position among all task items
     * in document order. {@code indent} is the nesting depth (0 = top level).
     */
    public record TaskItemBlock(boolean checked, String text, int taskIndex, int indent) implements MarkdownBlock {
    }

    /** A fenced code block ({@code ```}). */
    public record CodeBlock(List<String> lines) implements MarkdownBlock {
    }

    /**
     * A block quote ({@code > ...}). {@code content} holds the blocks inside
     * the quote (paragraphs, nested quotes, lists, ...), so nested block
     * quotes render as nested quote boxes.
     */
    public record BlockQuoteBlock(List<MarkdownBlock> content) implements MarkdownBlock {
    }

    /** A table: rows of cell text; the first row is the header row. */
    public record TableBlock(List<List<String>> rows) implements MarkdownBlock {
    }

    /** A run of inline-formatted text. */
    public record InlineSegment(String text, boolean bold, boolean italic, boolean code,
                               boolean link, String url) {
    }

    private final Parser parser;

    public MarkdownService() {
        this.parser = Parser.builder()
                .extensions(List.of(TaskListExtension.create(), TablesExtension.create()))
                .build();
    }

    /**
     * Parses raw Markdown into blocks.
     *
     * <p>Supported blocks: headings, paragraphs, unordered/ordered list
     * items (including nested lists), task-list items, fenced code blocks,
     * block quotes (including nested quotes) and tables.
     * Task-like lines inside a fenced code block are treated as code,
     * not as tasks.</p>
     */
    public List<MarkdownBlock> parse(String markdown) {
        List<MarkdownBlock> blocks = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return blocks;
        }
        Node document = parser.parse(markdown);
        TaskIndexCounter counter = new TaskIndexCounter();
        for (Node child : document.getChildren()) {
            appendBlocks(child, blocks, counter);
        }
        return blocks;
    }

    /**
     * Parses inline formatting within a single text run.
     *
     * <p>Supported: {@code **bold**}, {@code *italic*}, {@code `code`}
     * and {@code [label](https://url)} links. Unclosed markers are kept
     * as literal text. Only {@code http} and {@code https} URLs are
     * recognized as links; anything else is rendered as plain text.</p>
     */
    public List<InlineSegment> parseInline(String text) {
        List<InlineSegment> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }
        Node document = parser.parse(text);
        for (Node block : document.getChildren()) {
            for (Node child : block.getChildren()) {
                collectInline(child, false, false, segments);
            }
        }
        return segments;
    }

    /**
     * Toggles the {@code taskIndex}-th task item (zero-based, document
     * order) between {@code [ ]} and {@code [x]}. Out-of-range indices
     * leave the text unchanged. The raw Markdown is otherwise preserved
     * byte-for-byte.
     */
    public String toggleTask(String markdown, int taskIndex) {
        if (markdown == null || taskIndex < 0) {
            return markdown;
        }
        Node document = parser.parse(markdown);
        List<TaskListItem> tasks = new ArrayList<>();
        collectTaskItems(document, tasks);
        if (taskIndex >= tasks.size()) {
            return markdown;
        }
        TaskListItem item = tasks.get(taskIndex);
        BasedSequence chars = item.getChars();
        if (chars == null) {
            return markdown;
        }
        int bracket = chars.indexOf('[');
        if (bracket == -1 || bracket + 3 > chars.length()) {
            return markdown;
        }
        int absolute = item.getStartOffset() + bracket;
        char state = markdown.charAt(absolute + 1);
        if (state != ' ' && state != 'x' && state != 'X') {
            return markdown;
        }
        char next = (state == ' ') ? 'x' : ' ';
        return markdown.substring(0, absolute) + "[" + next + "]" + markdown.substring(absolute + 3);
    }

    private void appendBlocks(Node node, List<MarkdownBlock> blocks, TaskIndexCounter counter) {
        if (node instanceof Heading heading) {
            String text = heading.getText() == null ? "" : heading.getText().toString();
            if (text.isEmpty()) {
                // A bare "#" with no text is not a meaningful heading.
                blocks.add(new ParagraphBlock(node.getChars() == null ? "" : node.getChars().toString()));
            } else {
                blocks.add(new HeadingBlock(Math.min(heading.getLevel(), 6), text));
            }
        } else if (node instanceof Paragraph paragraph) {
            blocks.add(new ParagraphBlock(stripTrailingLineBreaks(paragraph.getChars().toString())));
        } else if (node instanceof com.vladsch.flexmark.ast.CodeBlock
                || node instanceof com.vladsch.flexmark.ast.FencedCodeBlock
                || node instanceof com.vladsch.flexmark.ast.IndentedCodeBlock) {
            // In flexmark, fenced and indented code blocks are sibling classes
            // of CodeBlock (all extend Block), so all three must be matched.
            blocks.add(new CodeBlock(codeLinesOf(node)));
        } else if (node instanceof BlockQuote quote) {
            List<MarkdownBlock> inner = new ArrayList<>();
            for (Node child : quote.getChildren()) {
                appendBlocks(child, inner, counter);
            }
            blocks.add(new BlockQuoteBlock(inner));
        } else if (node instanceof com.vladsch.flexmark.ext.gfm.tables.TableBlock table) {
            blocks.add(new TableBlock(tableRowsOf(table)));
        } else if (node instanceof BulletList || node instanceof OrderedList) {
            appendListItems(node, blocks, counter, 0);
        } else if (node instanceof ThematicBreak) {
            // Decorative separator; nothing to render.
        } else if (node instanceof HtmlBlock) {
            // Raw HTML: never executed, rendered as plain text.
            String text = node.getChars() == null ? "" : node.getChars().toString().trim();
            if (!text.isEmpty()) {
                blocks.add(new ParagraphBlock(text));
            }
        } else {
            // Unknown block type: render its raw text as a plain paragraph.
            String text = node.getChars() == null ? "" : node.getChars().toString().trim();
            if (!text.isEmpty()) {
                blocks.add(new ParagraphBlock(text));
            }
        }
    }

    private void appendListItems(Node list, List<MarkdownBlock> blocks, TaskIndexCounter counter, int indent) {
        int startNumber = (list instanceof OrderedList ordered) ? ordered.getStartNumber() : 1;
        int position = 0;
        for (Node item : list.getChildren()) {
            if (item instanceof TaskListItem task) {
                blocks.add(new TaskItemBlock(task.isItemDoneMarker(), itemText(item), counter.next(), indent));
            } else if (item instanceof OrderedListItem) {
                blocks.add(new ListItemBlock(true, startNumber + position, itemText(item), indent));
            } else {
                blocks.add(new ListItemBlock(false, 0, itemText(item), indent));
            }
            // Nested lists inside a list item (sub-items) render one level deeper.
            for (Node child : item.getChildren()) {
                if (child instanceof BulletList || child instanceof OrderedList) {
                    appendListItems(child, blocks, counter, indent + 1);
                }
            }
            position++;
        }
    }

    private String itemText(Node item) {
        StringBuilder text = new StringBuilder();
        for (Node child : item.getChildren()) {
            if (child instanceof Paragraph paragraph) {
                if (text.length() > 0) {
                    text.append(' ');
                }
                text.append(paragraph.getChars().toString());
            }
        }
        return text.toString().trim();
    }

    private static String plainTextOf(Node node) {
        StringBuilder text = new StringBuilder();
        if (node instanceof Text textNode) {
            text.append(textNode.getChars() == null ? "" : textNode.getChars().toString());
        } else if (node instanceof Code codeNode) {
            text.append(codeNode.getText() == null ? "" : codeNode.getText().toString());
        } else if (node instanceof Image imageNode) {
            text.append(imageNode.getText() == null ? "" : imageNode.getText().toString());
        } else if (node instanceof SoftLineBreak || node instanceof HardLineBreak) {
            text.append('\n');
        } else if (node instanceof WhiteSpace) {
            text.append(node.getChars() == null ? "" : node.getChars().toString());
        }
        for (Node child : node.getChildren()) {
            text.append(plainTextOf(child));
        }
        return text.toString().trim();
    }

    /**
     * Removes trailing line breaks from raw block text. Flexmark includes the
     * line break that separates a block from the following block in the
     * block's raw characters.
     */
    private static String stripTrailingLineBreaks(String text) {
        String result = text;
        while (result.endsWith("\r\n") || result.endsWith("\n")) {
            result = result.endsWith("\r\n")
                    ? result.substring(0, result.length() - 2)
                    : result.substring(0, result.length() - 1);
        }
        return result;
    }

    /** Extracts the rows of a table node as cell text (first row = header). */
    private static List<List<String>> tableRowsOf(com.vladsch.flexmark.ext.gfm.tables.TableBlock table) {
        List<List<String>> rows = new ArrayList<>();
        // TableBlock children are a TableHead, a TableSeparator and a TableBody;
        // the separator row is a marker, not data, so it is skipped.
        for (Node section : table.getChildren()) {
            if (section instanceof TableSeparator) {
                continue;
            }
            for (Node row : section.getChildren()) {
                if (row instanceof TableRow) {
                    List<String> cells = new ArrayList<>();
                    for (Node cell : row.getChildren()) {
                        cells.add(plainTextOf(cell));
                    }
                    rows.add(cells);
                }
            }
        }
        return rows;
    }

    /** Extracts the content lines of a code-block node (fenced or indented). */
    private static List<String> codeLinesOf(Node node) {
        List<String> lines = new ArrayList<>();
        if (node instanceof com.vladsch.flexmark.util.ast.ContentNode contentNode) {
            for (BasedSequence line : contentNode.getContentLines()) {
                String text = line == null ? "" : line.toString();
                // Content lines include their trailing line break; strip it so
                // the renderer can join lines with a single "\n".
                if (text.endsWith("\r\n")) {
                    text = text.substring(0, text.length() - 2);
                } else if (text.endsWith("\n")) {
                    text = text.substring(0, text.length() - 1);
                }
                lines.add(text);
            }
        }
        return lines;
    }

    private void collectInline(Node node, boolean bold, boolean italic, List<InlineSegment> segments) {
        if (node instanceof Text text) {
            addSegment(segments, text.getChars() == null ? "" : text.getChars().toString(),
                    bold, italic, false, false, null);
        } else if (node instanceof StrongEmphasis) {
            collectInlineChildren(node, true, italic, segments);
        } else if (node instanceof Emphasis) {
            collectInlineChildren(node, bold, true, segments);
        } else if (node instanceof Code code) {
            addSegment(segments, code.getText() == null ? "" : code.getText().toString(),
                    bold, italic, true, false, null);
        } else if (node instanceof Link link) {
            String label = link.getText() == null ? "" : link.getText().toString();
            String url = link.getUrl() == null ? "" : link.getUrl().toString();
            if (isSafeUrl(url)) {
                addSegment(segments, label, bold, italic, false, true, url);
            } else {
                // Unsafe or foreign URL: render the whole construct as plain text.
                addSegment(segments, "[" + label + "](" + url + ")", bold, italic, false, false, null);
            }
        } else if (node instanceof AutoLink autoLink) {
            String raw = autoLink.getChars() == null ? "" : autoLink.getChars().toString();
            String url = raw.startsWith("<") && raw.endsWith(">")
                    ? raw.substring(1, raw.length() - 1)
                    : raw;
            if (isSafeUrl(url)) {
                addSegment(segments, url, bold, italic, false, true, url);
            } else {
                addSegment(segments, raw, bold, italic, false, false, null);
            }
        } else if (node instanceof Image image) {
            // Images are rendered as their alt text only.
            addSegment(segments, image.getText() == null ? "" : image.getText().toString(),
                    bold, italic, false, false, null);
        } else if (node instanceof HardLineBreak) {
            addSegment(segments, "\n", bold, italic, false, false, null);
        } else if (node instanceof SoftLineBreak) {
            addSegment(segments, " ", bold, italic, false, false, null);
        } else if (node instanceof WhiteSpace) {
            addSegment(segments, node.getChars() == null ? "" : node.getChars().toString(),
                    bold, italic, false, false, null);
        } else if (node instanceof HtmlInline) {
            // Inline HTML: never executed, rendered as plain text.
            addSegment(segments, node.getChars() == null ? "" : node.getChars().toString(),
                    bold, italic, false, false, null);
        } else {
            // Unknown inline node: render as plain text.
            addSegment(segments, node.getChars() == null ? "" : node.getChars().toString(),
                    bold, italic, false, false, null);
        }
    }

    private void collectInlineChildren(Node parent, boolean bold, boolean italic,
                                      List<InlineSegment> segments) {
        for (Node child : parent.getChildren()) {
            collectInline(child, bold, italic, segments);
        }
    }

    private void collectTaskItems(Node node, List<TaskListItem> tasks) {
        if (node instanceof TaskListItem task) {
            tasks.add(task);
        }
        for (Node child : node.getChildren()) {
            collectTaskItems(child, tasks);
        }
    }

    private boolean isSafeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        if (url.indexOf(' ') != -1 || url.indexOf('\t') != -1) {
            return false;
        }
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private void addSegment(List<InlineSegment> segments, String text, boolean bold, boolean italic,
                           boolean code, boolean link, String url) {
        if (text.isEmpty()) {
            return;
        }
        segments.add(new InlineSegment(text, bold, italic, code, link, url));
    }

    /** Mutable document-order counter for task items. */
    private static final class TaskIndexCounter {
        private int nextIndex;

        int next() {
            return nextIndex++;
        }
    }
}
