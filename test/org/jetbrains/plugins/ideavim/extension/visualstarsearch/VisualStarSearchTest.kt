/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.extension.visualstarsearch

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.EffectType
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.OptionsManager
import junit.framework.Assert
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author patrick-elmquist
 */
@RunWith(Parameterized::class)
class VisualStarSearchTest(private val searchDir: Char) : VimTestCase() {

  companion object {
    @JvmStatic
    @Parameterized.Parameters
    public fun data(): Collection<Char> = listOf('*', '#')
  }

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    enableExtensions("VisualStarSearch")
    setHighlightSearch()
  }

  // Test that / is escaped when doing Search forward

//  don't
//  'don't'
//  'don''t'
//  "amy's quote"
//  {,*/}
//  **
//  a[bc]d
//  g~re
//  hello.
//  helloo
//  [http://vimcasts.org/search?q=visual-star]

//  don't
//  'don't'
//  'don''t'
//  "amy's quote"
//  {,*/}
//  **
//  a[bc]d
//  g~re
//  hello.
//  helloo
//  [http://vimcasts.org/search?q=visual-star]

  @Test
  fun testDotsAreEscaped() {
    // If dots aren't escaped 'user name' would be matched as well
    configureByText(
      """${c}user.name = "Jane Doe"
        println("user name set to: \$\{user.name\}")""".trimIndent())

    selectWordAndSearch(searchDir)

    assertSearchString("\\Vuser.name")
    assertMatchCount(2)
    assertHighlighting(
      """«user.name» = "Jane Doe"
        println("user name set to: \$\{«user.name»\}")""".trimIndent())
  }

  private fun selectWordAndSearch(dir: Char) = typeText(parseKeys("vE$dir"))

  fun disabledTestCaretsAreEscaped() {
    // TODO not done yet
    // also have a look here for more characters that needs to be escaped
    // https://docs.oracle.com/cd/E19253-01/806-7612/editorvi-62/index.html#:~:text=To%20escape%20a%20special%20character,commands%20to%20the%20search%20function.

    // TODO Add a known issues at some place with a link to
    // https://youtrack.jetbrains.com/issue/VIM-1719
    // to explain why \n queries only works somewhat
    configureByText(
      """${c}
        |~/vimrc""".trimMargin())

    typeText(parseKeys("vE*"))

    assertHighlighting(
      """«~/vimrc»
        |«~/vimrc»""".trimMargin())
  }

  private fun assertSearchString(expected: String) {
    Assert.assertEquals(expected, VimPlugin.getSearch().lastSearch)
  }

  private fun assertMatchCount(expected: Int) {
    Assert.assertEquals(expected, myFixture.editor.markupModel.allHighlighters.size)
  }

  private fun assertHighlighting(expected: String) {
    val allHighlighters = myFixture.editor.markupModel.allHighlighters
    val actual = StringBuilder(myFixture.editor.document.text)
    val inserts = mutableMapOf<Int, String>()
    val colorScheme = EditorColorsManager.getInstance().globalScheme

    // Digraphs:
    // <C-K>3" → ‷ + <C-K>3' → ‴ (current match)
    // <C-K><< → « + <C-K>>> → » (normal match)
    allHighlighters.forEach {
      // TODO: This is not the nicest way to check for current match. Add something to the highlight's user data?
      if (colorScheme.getAttributes(it.textAttributesKey)?.effectType == EffectType.ROUNDED_BOX) {
        inserts.compute(it.startOffset) { _, v -> if (v == null) "‷" else "$v‷" }
        inserts.compute(it.endOffset) { _, v -> if (v == null) "‴" else "$v‴" }
      } else {
        inserts.compute(it.startOffset) { _, v -> if (v == null) "«" else "$v«" }
        inserts.compute(it.endOffset) { _, v -> if (v == null) "»" else "$v»" }
      }
    }

    var offset = 0
    inserts.toSortedMap().forEach { (k, v) ->
      actual.insert(k + offset, v)
      offset += v.length
    }

    assertEquals(expected, actual.toString())
  }

  private fun setHighlightSearch() = OptionsManager.hlsearch.set()
}
