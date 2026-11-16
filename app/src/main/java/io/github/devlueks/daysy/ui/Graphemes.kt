package io.github.devlueks.daysy.ui

import android.icu.text.BreakIterator

/*
 * "Characters" as the user sees them. An emoji like 😃 is two UTF-16 chars, a family emoji
 * or a flag even more – taking String.take(1) would cut them in half and show "?".
 * ICU's character break iterator knows the full grapheme-cluster rules (incl. emoji ZWJ sequences).
 */

/** The first visible character of [text] (after trimming), or "" if there is none. */
fun firstGrapheme(text: String): String {
    val s = text.trim()
    if (s.isEmpty()) return ""
    val it = BreakIterator.getCharacterInstance()
    it.setText(s)
    val end = it.next()
    return if (end == BreakIterator.DONE) s else s.substring(0, end)
}

/** The last visible character of [text], or "" if there is none. */
fun lastGrapheme(text: String): String {
    if (text.isEmpty()) return ""
    val it = BreakIterator.getCharacterInstance()
    it.setText(text)
    val end = it.last()
    val start = it.previous()
    return if (start == BreakIterator.DONE) text else text.substring(start, end)
}
