package com.example.taskboard.service;

import com.example.taskboard.service.MarkdownService.BlockQuoteBlock;
import com.example.taskboard.service.MarkdownService.CodeBlock;
import com.example.taskboard.service.MarkdownService.HeadingBlock;
import com.example.taskboard.service.MarkdownService.InlineSegment;
import com.example.taskboard.service.MarkdownService.ListItemBlock;
import com.example.taskboard.service.MarkdownService.MarkdownBlock;
import com.example.taskboard.service.MarkdownService.ParagraphBlock;
import com.example.taskboard.service.MarkdownService.TableBlock;
import com.example.taskboard.service.MarkdownService.TaskItemBlock;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure Markdown parsing and task-toggle logic.
 * All tests are deterministic and independent of the current date.
 */
class MarkdownServiceTest {

    private final MarkdownService service = new MarkdownService();

    // ---------- parse: blocks ----------

    @Test
    void parseNullOrBlankReturnsNoBlocks() {
        assertTrue(service.parse(null).isEmpty());
        assertTrue(service.parse("").isEmpty());
        assertTrue(service.parse("   ").isEmpty());
    }

    @Test
    void parseSingleLineBecomesParagraph() {
        List<MarkdownBlock> blocks = service.parse("Hello world");
        assertEquals(List.of(new ParagraphBlock("Hello world")), blocks);
    }

    @Test
    void parseMultiLineParagraphKeepsSourceText() {
        List<MarkdownBlock> blocks = service.parse("line one\nline two");
        assertEquals(List.of(new ParagraphBlock("line one\nline two")), blocks);
    }

    @Test
    void parseBlockQuote() {
        List<MarkdownBlock> blocks = service.parse("> quote line");
        assertEquals(List.of(new BlockQuoteBlock(List.of(new ParagraphBlock("quote line")))), blocks);
    }

    @Test
    void parseNestedBlockQuoteKeepsStructure() {
        List<MarkdownBlock> blocks = service.parse("> outer\n>> inner");
        assertEquals(List.of(new BlockQuoteBlock(List.of(
                new ParagraphBlock("outer"),
                new BlockQuoteBlock(List.of(new ParagraphBlock("inner")))))), blocks);
    }

    @Test
    void parseThematicBreakIsIgnored() {
        assertTrue(service.parse("---").isEmpty());
    }

    @Test
    void parseHeadingsWithLevels() {
        assertEquals(List.of(new HeadingBlock(1, "Title")), service.parse("# Title"));
        assertEquals(List.of(new HeadingBlock(2, "Sub")), service.parse("## Sub"));
        assertEquals(List.of(new HeadingBlock(6, "Deep")), service.parse("###### Deep"));
    }

    @Test
    void parseHeadingWithoutTextIsParagraph() {
        assertEquals(List.of(new ParagraphBlock("#")), service.parse("#"));
    }

    @Test
    void parseUnorderedList() {
        List<MarkdownBlock> blocks = service.parse("- a\n- b");
        assertEquals(List.of(new ListItemBlock(false, 0, "a", 0), new ListItemBlock(false, 0, "b", 0)), blocks);
    }

    @Test
    void parseOrderedListKeepsNumbers() {
        List<MarkdownBlock> blocks = service.parse("1. a\n2) b");
        assertEquals(List.of(new ListItemBlock(true, 1, "a", 0), new ListItemBlock(true, 2, "b", 0)), blocks);
    }

    @Test
    void parseNestedUnorderedListKeepsIndentation() {
        List<MarkdownBlock> blocks = service.parse("- a\n- b\n    - c\n    - d");
        assertEquals(List.of(
                new ListItemBlock(false, 0, "a", 0),
                new ListItemBlock(false, 0, "b", 0),
                new ListItemBlock(false, 0, "c", 1),
                new ListItemBlock(false, 0, "d", 1)), blocks);
    }

    @Test
    void parseNestedOrderedListKeepsIndentationAndRestartsNumbers() {
        List<MarkdownBlock> blocks = service.parse("1. a\n2. b\n   1. c\n   2. d");
        assertEquals(List.of(
                new ListItemBlock(true, 1, "a", 0),
                new ListItemBlock(true, 2, "b", 0),
                new ListItemBlock(true, 1, "c", 1),
                new ListItemBlock(true, 2, "d", 1)), blocks);
    }

    @Test
    void parseNestedTaskListKeepsIndentation() {
        List<MarkdownBlock> blocks = service.parse("- [ ] a\n    - [x] b");
        assertEquals(List.of(
                new TaskItemBlock(false, "a", 0, 0),
                new TaskItemBlock(true, "b", 1, 1)), blocks);
    }

    @Test
    void parseTaskListAssignsDocumentOrderIndices() {
        List<MarkdownBlock> blocks = service.parse("- [ ] one\n- [x] two\n* [X] three");
        assertEquals(List.of(
                new TaskItemBlock(false, "one", 0, 0),
                new TaskItemBlock(true, "two", 1, 0),
                new TaskItemBlock(true, "three", 2, 0)), blocks);
    }

    @Test
    void parseClosedCodeBlock() {
        List<MarkdownBlock> blocks = service.parse("```java\ncode line\n```");
        assertEquals(List.of(new CodeBlock(List.of("code line"))), blocks);
    }

    @Test
    void parseFencedCodeBlockWithInfoString() {
        List<MarkdownBlock> blocks = service.parse("```mermaid\ngraph TD\n```");
        assertEquals(List.of(new CodeBlock(List.of("graph TD"))), blocks);
    }

    @Test
    void parseTable() {
        List<MarkdownBlock> blocks = service.parse(
                "| Left | Right |\n| ------ | :----: |\n| foo | bar |\n| baz | qux |");
        assertEquals(List.of(new TableBlock(List.of(
                List.of("Left", "Right"),
                List.of("foo", "bar"),
                List.of("baz", "qux")))), blocks);
    }

    @Test
    void parseUnclosedCodeBlockRunsToEnd() {
        List<MarkdownBlock> blocks = service.parse("```java\nfoo");
        assertEquals(List.of(new CodeBlock(List.of("foo"))), blocks);
    }

    @Test
    void parseMixedDocumentKeepsBlockOrder() {
        String markdown = "# H\npara line\n- [ ] t1\n- [x] t2\n```\ncode\n```\n1. item";
        List<MarkdownBlock> blocks = service.parse(markdown);
        assertEquals(List.of(
                new HeadingBlock(1, "H"),
                new ParagraphBlock("para line"),
                new TaskItemBlock(false, "t1", 0, 0),
                new TaskItemBlock(true, "t2", 1, 0),
                new CodeBlock(List.of("code")),
                new ListItemBlock(true, 1, "item", 0)), blocks);
    }

    @Test
    void parseCrlfLineEndings() {
        assertEquals(List.of(new ParagraphBlock("a\r\nb")), service.parse("a\r\nb"));
    }

    @Test
    void taskLikeLineInsideCodeBlockIsNotATask() {
        List<MarkdownBlock> blocks = service.parse("```\n- [ ] hidden\n```");
        assertEquals(List.of(new CodeBlock(List.of("- [ ] hidden"))), blocks);
    }

    // ---------- parseInline ----------

    @Test
    void parseInlinePlainText() {
        assertEquals(List.of(new InlineSegment("abc", false, false, false, false, null)),
                service.parseInline("abc"));
    }

    @Test
    void parseInlineBold() {
        assertEquals(List.of(new InlineSegment("bold", true, false, false, false, null)),
                service.parseInline("**bold**"));
    }

    @Test
    void parseInlineItalic() {
        assertEquals(List.of(new InlineSegment("it", false, true, false, false, null)),
                service.parseInline("*it*"));
    }

    @Test
    void parseInlineCode() {
        assertEquals(List.of(new InlineSegment("code", false, false, true, false, null)),
                service.parseInline("`code`"));
    }

    @Test
    void parseInlineHttpLink() {
        assertEquals(List.of(new InlineSegment("docs", false, false, false, true, "https://example.com")),
                service.parseInline("[docs](https://example.com)"));
    }

    @Test
    void parseInlineUnsafeLinkIsPlainText() {
        assertEquals(List.of(new InlineSegment("[x](javascript:alert(1))", false, false, false, false, null)),
                service.parseInline("[x](javascript:alert(1))"));
        assertEquals(List.of(new InlineSegment("[y](ftp://y)", false, false, false, false, null)),
                service.parseInline("[y](ftp://y)"));
    }

    @Test
    void parseInlineAutoLink() {
        assertEquals(List.of(new InlineSegment("https://example.com", false, false, false, true,
                "https://example.com")),
                service.parseInline("<https://example.com>"));
    }

    @Test
    void parseInlineImageRendersAltText() {
        assertEquals(List.of(new InlineSegment("alt", false, false, false, false, null)),
                service.parseInline("![alt](https://example.com/img.png)"));
    }

    @Test
    void parseInlineHtmlIsPlainText() {
        // Flexmark splits inline HTML into separate plain-text segments
        // ("<b>", "bold", "</b>"); none of them is executed or styled.
        assertEquals(List.of(
                new InlineSegment("<b>", false, false, false, false, null),
                new InlineSegment("bold", false, false, false, false, null),
                new InlineSegment("</b>", false, false, false, false, null)),
                service.parseInline("<b>bold</b>"));
    }

    @Test
    void parseInlineHardLineBreak() {
        assertEquals(List.of(
                new InlineSegment("a", false, false, false, false, null),
                new InlineSegment("\n", false, false, false, false, null),
                new InlineSegment("b", false, false, false, false, null)),
                service.parseInline("a\\\nb"));
    }

    @Test
    void parseInlineUnclosedBoldIsLiteral() {
        assertEquals(List.of(new InlineSegment("**oops", false, false, false, false, null)),
                service.parseInline("**oops"));
    }

    @Test
    void parseInlineUnclosedCodeIsLiteral() {
        assertEquals(List.of(new InlineSegment("`oops", false, false, false, false, null)),
                service.parseInline("`oops"));
    }

    @Test
    void parseInlineMixedFormats() {
        assertEquals(List.of(
                new InlineSegment("bold", true, false, false, false, null),
                new InlineSegment(" and ", false, false, false, false, null),
                new InlineSegment("it", false, true, false, false, null),
                new InlineSegment(" and ", false, false, false, false, null),
                new InlineSegment("c", false, false, true, false, null)),
                service.parseInline("**bold** and *it* and `c`"));
    }

    @Test
    void parseInlineNestedBoldAndItalic() {
        assertEquals(List.of(
                new InlineSegment("bold ", true, false, false, false, null),
                new InlineSegment("it", true, true, false, false, null),
                new InlineSegment(" bold", true, false, false, false, null)),
                service.parseInline("**bold *it* bold**"));
    }

    @Test
    void parseInlineEmptyText() {
        assertTrue(service.parseInline(null).isEmpty());
        assertTrue(service.parseInline("").isEmpty());
    }

    // ---------- toggleTask ----------

    @Test
    void toggleTaskChecksUncheckedItem() {
        assertEquals("- [x] a", service.toggleTask("- [ ] a", 0));
    }

    @Test
    void toggleTaskUnchecksLowercaseX() {
        assertEquals("- [ ] a", service.toggleTask("- [x] a", 0));
    }

    @Test
    void toggleTaskUnchecksUppercaseX() {
        assertEquals("- [ ] a", service.toggleTask("- [X] a", 0));
    }

    @Test
    void toggleTaskOutOfRangeLeavesTextUnchanged() {
        assertEquals("- [ ] a", service.toggleTask("- [ ] a", 1));
        assertEquals("- [ ] a", service.toggleTask("- [ ] a", -1));
        assertNull(service.toggleTask(null, 0));
    }

    @Test
    void toggleTaskOnlyAffectsRequestedItem() {
        assertEquals("- [ ] a\n- [ ] b", service.toggleTask("- [ ] a\n- [x] b", 1));
    }

    @Test
    void toggleTaskIgnoresTaskLikeLinesInCodeBlocks() {
        String markdown = "```\n- [ ] hidden\n```\n- [ ] real";
        assertEquals("```\n- [ ] hidden\n```\n- [x] real", service.toggleTask(markdown, 0));
    }

    @Test
    void toggleTaskWorksAcrossSurroundingBlocks() {
        String markdown = "para\n- [ ] a\nmore\n- [x] b";
        assertEquals("para\n- [ ] a\nmore\n- [ ] b", service.toggleTask(markdown, 1));
    }
}
