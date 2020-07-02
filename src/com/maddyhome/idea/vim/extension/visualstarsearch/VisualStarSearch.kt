package com.maddyhome.idea.vim.extension.visualstarsearch

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.exitVisualMode

/**
 * Port of vim-visual-star-search
 * https://github.com/bronson/vim-visual-star-search
 *
 * @author patrick-elmquist
 */
class VisualStarSearch : VimExtension {
  override fun getName(): String = "VisualStarSearch"

  override fun init() {
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.X, StringHelper.parseKeys(SEARCH_FWD), owner, Search(Direction.FWD), false)
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.X, StringHelper.parseKeys(SEARCH_REV), owner, Search(Direction.REV), false)

    VimExtensionFacade.putKeyMapping(MappingMode.X, StringHelper.parseKeys("*"), owner, StringHelper.parseKeys(SEARCH_FWD), true)
    VimExtensionFacade.putKeyMapping(MappingMode.X, StringHelper.parseKeys("#"), owner, StringHelper.parseKeys(SEARCH_REV), true)
  }

  private enum class Direction { FWD, REV }

  private class Search(private val direction: Direction) : VimExtensionHandler {
    private val logger = Logger.getInstance(Search::class.java.name)

    private val escapeMap = when (direction) {
      Direction.FWD -> mapOf("\n" to "\\n", "/" to "\\/")
      Direction.REV -> mapOf("\n" to "\\n")
    }

    override fun execute(editor: Editor, context: DataContext) {
      val selectedText = editor.caretModel.primaryCaret.selectedText ?: return

      val escapedText = "\\V" + escapeMap.entries.fold(selectedText) { q, e -> q.replace(e.key, e.value) }

      val (dir, count) = if (direction == Direction.FWD) {
        enumSetOf(CommandFlags.FLAG_SEARCH_FWD) to 1
      } else {
        enumSetOf(CommandFlags.FLAG_SEARCH_REV) to 2
      }

      VimPlugin.getSearch().search(editor, escapedText, count, dir, true)
      editor.exitVisualMode()

      logger.warn("Last search: '${VimPlugin.getSearch().lastSearch}'")
    }
  }

  companion object {
    private const val SEARCH_FWD = "<Plug>VisualStarSearchForward"
    private const val SEARCH_REV = "<Plug>VisualStarSearchReversed"
  }
}
/*
testing

testing

^test^testing

test^testing

test.ing

test.ing

*testing*

*testing*

test~ing

test~ing

[testing]

[testing]

/testing
/testing
/testing
#testing

?testing     not working for #
?testing
?testing

test
ing

test
ing

test
ing

Visual star: [http://vimcasts.org/search?q=visual-star][q]
[q]: http://vimcasts.org/search?q=visual-star

Visual star: [http://vimcasts.org/search?q=visual-star][q]
[q]: http://vimcasts.org/search?q=visual-star

Visual star: [http://vimcasts.org/search?q=visual-star][q]
[q]: http://vimcasts.org/search?q=visual-star

don't
'don't'
'don''t'
"amy's quote"
a[bc]d
g~re
hello.
helloo

don't
'don't'
'don''t'
"amy's quote"
a[bc]d
g~re
hello.
helloo

*/

// **
// **

// {,*/}
// {,*/}
