package hotkeys

import io.akryl.ComponentScope
import io.akryl.useRef
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

@DslMarker
annotation class HotKeysDsl

data class HotKey(val value: String)

@HotKeysDsl
class HotKeysBuilder {
  private val items = HashMap<PersistentSet<HotKey>, () -> Unit>()

  val control = HotKey("Control")
  val alt = HotKey("Alt")
  val shift = HotKey("Shift")
  val delete = HotKey("Delete")

  operator fun List<HotKey>.invoke(callback: () -> Unit) {
    items[this.toPersistentSet()] = callback
  }

  operator fun HotKey.invoke(callback: () -> Unit) {
    items[persistentSetOf(this)] = callback
  }

  operator fun HotKey.plus(other: HotKey) = listOf(this, other)

  fun build(): Map<PersistentSet<HotKey>, () -> Unit> = items
}

fun hotKeys(block: HotKeysBuilder.() -> Unit) =
  HotKeysBuilder().apply(block).build()

data class HotKeysEvents(
  val onKeyDown: (event: KeyboardEvent) -> Unit,
  val onKeyUp: (event: KeyboardEvent) -> Unit,
  val onFocus: (event: Event) -> Unit
)

private data class HotKeysState(
  val keys: PersistentSet<HotKey> = persistentSetOf(),
  val pressed: Boolean = false
)

private val codeToKey = mapOf(
  // todo
  46 to HotKey("Delete")
)

fun ComponentScope.useHotKeys(block: HotKeysBuilder.() -> Unit): HotKeysEvents {
  val keys = hotKeys(block)
  val state = useRef(HotKeysState())

  return HotKeysEvents(
    onKeyUp = { e ->
      val key = codeToKey[e.keyCode]
      if (key != null) {
        state.current = state.current.copy(
          keys = state.current.keys.remove(HotKey(e.key))
        )
        if (state.current.keys.isEmpty()) {
          state.current = state.current.copy(pressed = false)
        }
      }
    },
    onKeyDown = { e ->
      val key = codeToKey[e.keyCode]
      if (key != null && !state.current.pressed) {
        state.current = state.current.copy(
          keys = state.current.keys.add(key)
        )
        val handler = keys[state.current.keys]
        if (handler != null) {
          state.current = state.current.copy(pressed = true)
          handler()
        }
      }
    },
    onFocus = {
      state.current = HotKeysState()
    }
  )
}
