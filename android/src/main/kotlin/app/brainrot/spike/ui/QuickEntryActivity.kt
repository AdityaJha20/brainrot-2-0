package app.brainrot.spike.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brainrot.core.scoring.EventType
import app.brainrot.core.scoring.Status
import app.brainrot.spike.data.AppDatabase
import app.brainrot.spike.data.LogRepository
import app.brainrot.spike.widget.refreshWidgets
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Fallback candidate: a small quick-entry panel opened from the widget's "Log" button. Shown as a dialog over the
 * home screen and excluded from Recents, so it feels like part of the widget rather than opening the whole app.
 */
class QuickEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = BrainRotColors) {
                val db = remember { AppDatabase.get(this) }
                val repo = remember { LogRepository(db) }
                val scope = rememberCoroutineScope()
                val habits by db.dao().activeHabitsFlow().collectAsState(emptyList())
                val events by db.dao().allEventsFlow().collectAsState(emptyList())
                val summary =
                    remember(habits, events) { LogRepository.engine(habits, events).evaluate(LocalDate.now()) }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Background)
                        .padding(16.dp),
                ) {
                    Text("Quick log", color = Color.White, fontSize = 18.sp)
                    habits.forEach { h ->
                        val day = summary.habits[h.id]
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(h.name, color = Color.White)
                                if (h.measurement != "BINARY") {
                                    Text(
                                        "${day?.recorded ?: 0.0} / ${h.target} ${h.unit ?: ""}",
                                        color = Muted,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                            if (h.measurement == "BINARY") {
                                Checkbox(
                                    checked = day?.status == Status.COMPLETED,
                                    onCheckedChange = { c ->
                                        scope.launch {
                                            repo.setCompletedToday(h.id, c, "quick_screen")
                                            refreshWidgets(this@QuickEntryActivity)
                                        }
                                    },
                                )
                            } else {
                                val type = if (h.measurement == "DURATION") EventType.DURATION else EventType.QUANTITY
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        repo.log(h.id, type, -h.step, "quick_screen")
                                        refreshWidgets(this@QuickEntryActivity)
                                    }
                                }) { Text("−") }
                                Spacer(Modifier.width(6.dp))
                                Button(onClick = {
                                    scope.launch {
                                        repo.log(h.id, type, h.step, "quick_screen")
                                        refreshWidgets(this@QuickEntryActivity)
                                    }
                                }) { Text("+") }
                            }
                        }
                    }
                    Button(onClick = { finish() }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                }
            }
        }
    }
}
