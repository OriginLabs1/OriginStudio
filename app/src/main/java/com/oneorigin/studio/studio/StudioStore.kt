package com.oneorigin.studio.studio

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** How often a scheduled command runs. Times are local to the device, printed as such. */
enum class Cadence { INTERVAL, DAILY, WEEKLY, MANUAL }

/** A command that can run on a schedule, in a real shell session you can read afterwards. */
data class StudioAutomation(
    val id: String,
    val name: String,
    val command: String,
    val cadence: Cadence,
    val everyMinutes: Int = 60,
    val hour: Int = 8,
    val minute: Int = 30,
    val weekday: Int = Calendar.MONDAY,
    val enabled: Boolean = false,
    val nextRunMs: Long = 0L,
    val lastRunMs: Long = 0L,
)

/** An agent the user can make the default for new sessions. Nothing is signed out. */
data class StudioAccount(
    val id: String,
    val name: String,
    val provider: String,
    val detail: String,
)

data class StudioState(
    val environment: String = "debian",
    val finish: String = "ink",
    val activeAgentId: String = "",
    val dailyBudget: Int = 40,
    val usedToday: Int = 0,
    val dayStamp: String = "",
    val automations: List<StudioAutomation> = defaults(),
) {
    companion object {
        fun defaults(): List<StudioAutomation> = listOf(
            StudioAutomation(
                id = "tree",
                name = "Working tree check",
                command = "git status --short",
                cadence = Cadence.DAILY,
                hour = 9,
                minute = 0,
            ),
            StudioAutomation(
                id = "tests",
                name = "Recent commits",
                command = "git log --oneline -10",
                cadence = Cadence.WEEKLY,
                weekday = Calendar.MONDAY,
                hour = 8,
                minute = 30,
            ),
            StudioAutomation(
                id = "packages",
                name = "Debian security updates",
                command = "apt-get update",
                cadence = Cadence.WEEKLY,
                weekday = Calendar.SUNDAY,
                hour = 3,
                minute = 0,
            ),
        )
    }
}

/**
 * Local, inspectable state for the Studio surface: which environment is chosen, the premium
 * finish, the agent new sessions default to, a device prompt budget, and scheduled commands.
 *
 * Nothing here talks to a provider and nothing is invented. The budget counts prompts this
 * device actually sent; provider quotas stay where they belong, in the agent's own output.
 */
object StudioStore {

    private const val PREFS = "studio"
    private const val KEY = "state"

    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(StudioState())
    val state: StateFlow<StudioState> = _state.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _state.value = load().rollDay()
        save()
    }

    val current: StudioState get() = _state.value

    fun selectEnvironment(id: String) {
        update { it.copy(environment = id) }
    }

    fun selectFinish(id: String) {
        update { it.copy(finish = id) }
    }

    fun selectAgent(id: String) {
        update { it.copy(activeAgentId = id) }
    }

    fun setDailyBudget(budget: Int) {
        update { it.copy(dailyBudget = budget.coerceIn(0, 1000)) }
    }

    /** One prompt was sent to [agentId]; counts towards today's device budget. */
    fun recordPrompt(agentId: String) {
        if (agentId.isBlank()) return
        update { state ->
            state.copy(activeAgentId = state.activeAgentId.ifBlank { agentId }, usedToday = state.usedToday + 1)
        }
    }

    fun setAutomationEnabled(id: String, enabled: Boolean) {
        update { state ->
            state.copy(
                automations = state.automations.map { automation ->
                    if (automation.id != id) automation
                    else automation.copy(
                        enabled = enabled,
                        nextRunMs = if (enabled) scheduleFrom(automation, System.currentTimeMillis()) else 0L,
                    )
                },
            )
        }
    }

    /** Replaces a scheduled command with the next occurrence, after a run. */
    fun markRun(id: String, atMs: Long = System.currentTimeMillis()) {
        update { state ->
            state.copy(
                automations = state.automations.map { automation ->
                    if (automation.id != id) automation
                    else automation.copy(
                        lastRunMs = atMs,
                        nextRunMs = if (automation.enabled) scheduleFrom(automation, atMs) else 0L,
                    )
                },
            )
        }
    }

    /** Enabled automations whose next run has arrived. */
    fun due(nowMs: Long = System.currentTimeMillis()): List<StudioAutomation> =
        current.automations.filter { it.enabled && it.cadence != Cadence.MANUAL && it.nextRunMs in 1..nowMs }

    private fun update(block: (StudioState) -> StudioState) {
        _state.value = block(_state.value).rollDay()
        save()
    }

    private fun StudioState.rollDay(): StudioState {
        val stamp = dayStamp()
        return if (stamp == dayStamp) this else copy(dayStamp = stamp, usedToday = 0)
    }

    private fun dayStamp(): String {
        val now = Calendar.getInstance()
        return "${now.get(Calendar.YEAR)}-${now.get(Calendar.DAY_OF_YEAR)}"
    }

    private fun load(): StudioState = runCatching {
        val raw = prefs.getString(KEY, null) ?: return StudioState()
        val root = JSONObject(raw)
        val defaults = StudioState.defaults()
        val saved = root.optJSONArray("automations")
        val automations = defaults.map { base ->
            val found = (0 until (saved?.length() ?: 0))
                .mapNotNull { saved?.optJSONObject(it) }
                .firstOrNull { it.optString("id") == base.id }
            if (found == null) base else base.copy(
                enabled = found.optBoolean("enabled", base.enabled),
                nextRunMs = found.optLong("nextRunMs", 0L),
                lastRunMs = found.optLong("lastRunMs", 0L),
            )
        }
        StudioState(
            environment = root.optString("environment", "debian"),
            finish = root.optString("finish", "ink"),
            activeAgentId = root.optString("activeAgentId", ""),
            dailyBudget = root.optInt("dailyBudget", 40),
            usedToday = root.optInt("usedToday", 0),
            dayStamp = root.optString("dayStamp", ""),
            automations = automations,
        )
    }.getOrDefault(StudioState())

    private fun save() {
        if (!::prefs.isInitialized) return
        val state = _state.value
        val root = JSONObject()
            .put("environment", state.environment)
            .put("finish", state.finish)
            .put("activeAgentId", state.activeAgentId)
            .put("dailyBudget", state.dailyBudget)
            .put("usedToday", state.usedToday)
            .put("dayStamp", state.dayStamp)
            .put("automations", JSONArray().apply {
                state.automations.forEach { automation ->
                    put(
                        JSONObject()
                            .put("id", automation.id)
                            .put("enabled", automation.enabled)
                            .put("nextRunMs", automation.nextRunMs)
                            .put("lastRunMs", automation.lastRunMs),
                    )
                }
            })
        prefs.edit().putString(KEY, root.toString()).apply()
    }
}

/** The next run strictly after [fromMs], in local time. */
fun scheduleFrom(automation: StudioAutomation, fromMs: Long): Long {
    val from = Calendar.getInstance().apply { timeInMillis = fromMs }
    return when (automation.cadence) {
        Cadence.MANUAL -> 0L
        Cadence.INTERVAL -> fromMs + automation.everyMinutes.coerceAtLeast(1) * 60_000L
        Cadence.DAILY -> nextAt(from, automation.hour, automation.minute, weekday = null)
        Cadence.WEEKLY -> nextAt(from, automation.hour, automation.minute, weekday = automation.weekday)
    }
}

private fun nextAt(from: Calendar, hour: Int, minute: Int, weekday: Int?): Long {
    val next = (from.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (weekday != null) {
            while (get(Calendar.DAY_OF_WEEK) != weekday) add(Calendar.DAY_OF_MONTH, 1)
        }
        while (timeInMillis <= from.timeInMillis) add(Calendar.DAY_OF_MONTH, 1)
    }
    return next.timeInMillis
}
