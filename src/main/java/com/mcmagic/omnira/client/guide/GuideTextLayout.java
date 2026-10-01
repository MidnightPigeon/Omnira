package com.mcmagic.omnira.client.guide;

import com.ibm.icu.text.BreakIterator;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

/** Unicode line breaking for plain, resource-pack-authored guide paragraphs. */
public final class GuideTextLayout {
    private static final Pattern EFFECT_LEVEL = Pattern.compile(
            "(?:瞬间治疗|瞬间伤害|生命恢复|中毒|抗性提升|Instant Health|Instant Damage|Regeneration|Poison|Resistance)\\h+[IVX]+",
            Pattern.CASE_INSENSITIVE);
    private GuideTextLayout() {}

    public static List<String> wrap(String text, int width, ToIntFunction<String> measure) {
        if(text.startsWith(com.mcmagic.omnira.mana.DreamText.MARKER))
            return wrap(text.substring(com.mcmagic.omnira.mana.DreamText.MARKER.length()),width,measure).stream()
                    .map(line->com.mcmagic.omnira.mana.DreamText.MARKER+line).toList();
        // Carry paragraph-level formatting across wrapped lines without exposing codes to line breaking.
        var formatting=Pattern.compile("^(?:\u00a7[0-9a-fk-or])+",Pattern.CASE_INSENSITIVE).matcher(text);
        if(formatting.find()) {
            String prefix=formatting.group();
            return wrap(text.substring(prefix.length()),width,line->measure.applyAsInt(prefix+line)).stream()
                    .map(line->prefix+line+"\u00a7r").toList();
        }
        var result = new ArrayList<String>();
        for (String paragraph : text.split("\\R", -1)) {
            if (paragraph.isBlank()) { result.add(""); continue; }
            var breaks = BreakIterator.getLineInstance(Locale.ROOT);
            breaks.setText(paragraph);
            var protectedRanges = new ArrayList<int[]>();
            var matcher = EFFECT_LEVEL.matcher(paragraph);
            while (matcher.find()) {
                if (measure.applyAsInt(matcher.group()) <= width)
                    protectedRanges.add(new int[]{matcher.start(), matcher.end()});
            }
            int start = 0;
            while (start < paragraph.length()) {
                int end = start;
                for (int next = breaks.following(start); next != BreakIterator.DONE; next = breaks.next()) {
                    final int boundary = next;
                    if (protectedRanges.stream().anyMatch(r -> boundary > r[0] && boundary < r[1])) continue;
                    if (measure.applyAsInt(paragraph.substring(start, next).stripTrailing()) > width) break;
                    end = next;
                }
                // Oversized words still fit the page, without breaking surrogate pairs.
                if (end == start) {
                    end = paragraph.offsetByCodePoints(start, 1);
                    while (end < paragraph.length()) {
                        int next = paragraph.offsetByCodePoints(end, 1);
                        if (measure.applyAsInt(paragraph.substring(start, next)) > width) break;
                        end = next;
                    }
                }
                result.add(paragraph.substring(start, end).strip());
                start = end;
            }
        }
        return result;
    }

    public static List<List<String>> paginate(List<String> paragraphs, int width, int height, ToIntFunction<String> measure) {
        var pages = new ArrayList<List<String>>();
        var page = new ArrayList<String>();
        for (String text : paragraphs) {
            if (text.isBlank()) continue;
            var lines = wrap(text, width, measure);
            int offset = 0;
            if (!page.isEmpty()) {
                if (height - page.size() < Math.min(2, lines.size()) + 1) {
                    pages.add(List.copyOf(page)); page.clear();
                } else page.add("");
            }
            while (offset < lines.size()) {
                int count = Math.min(height - page.size(), lines.size() - offset);
                if (lines.size() - offset - count == 1 && count > 1) count--;
                page.addAll(lines.subList(offset, offset + count)); offset += count;
                if (offset < lines.size()) { pages.add(List.copyOf(page)); page.clear(); }
            }
        }
        if (!page.isEmpty()) pages.add(List.copyOf(page));
        return List.copyOf(pages);
    }
}
