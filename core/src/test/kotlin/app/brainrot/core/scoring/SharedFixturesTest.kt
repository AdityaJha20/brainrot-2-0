package app.brainrot.core.scoring

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.time.LocalDate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/** Runs every JSON file in shared-fixtures/scoring against the Kotlin engine (the TypeScript twin runs the same files). */
class SharedFixturesTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val tolerance = 1e-9

    private fun fixtureFiles(): List<File> {
        val dir = File(System.getProperty("fixturesDir") ?: error("fixturesDir system property not set"))
        return dir.listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
    }

    @Test
    fun allSharedFixturesPass() {
        val files = fixtureFiles()
        assertTrue(files.isNotEmpty(), "no fixtures found")
        val failures = mutableListOf<String>()
        for (file in files) {
            try {
                runFixture(file)
            } catch (e: AssertionError) {
                failures += "${file.nameWithoutExtension}: ${e.message}"
            }
        }
        println("Shared scoring fixtures: ${files.size - failures.size}/${files.size} passed")
        if (failures.isNotEmpty()) fail(failures.joinToString("\n"))
    }

    private fun runFixture(file: File) {
        val root = json.parseToJsonElement(file.readText()).jsonObject
        val input = json.decodeFromJsonElement(ScoringInput.serializer(), root)
        val date = LocalDate.parse(root.getValue("evaluateDate").jsonPrimitive.content)
        val result = ScoringEngine(input).evaluate(date)
        val expected = root.getValue("expected").jsonObject

        for ((habitId, fields) in expected.getValue("habits").jsonObject) {
            val actual = assertNotNull(result.habits[habitId], "missing habit $habitId")
            for ((key, value) in fields.jsonObject) {
                val path = "$habitId.$key"
                when (key) {
                    "status" -> assertEquals(value.jsonPrimitive.content, actual.status.name, path)
                    "recorded" -> assertClose(value, actual.recorded, path)
                    "progress" -> assertClose(value, actual.progress, path)
                    "streak" -> assertEquals(value.jsonPrimitive.int, actual.streak, path)
                    "streakPendingReview" -> assertEquals(value.jsonPrimitive.boolean, actual.streakPendingReview, path)
                    else -> fail("unknown expected field $path")
                }
            }
        }
        val completion = expected.getValue("dailyCompletion").jsonObject
        assertEquals(completion.getValue("completed").jsonPrimitive.int, result.dailyCompletion.completed, "completed")
        assertEquals(completion.getValue("eligible").jsonPrimitive.int, result.dailyCompletion.eligible, "eligible")
        assertClose(completion.getValue("ratio"), result.dailyCompletion.ratio, "dailyCompletion.ratio")
        assertClose(expected.getValue("dailyScore"), result.dailyScore, "dailyScore")
        assertEquals(
            expected.getValue("scoreIsProvisional").jsonPrimitive.boolean,
            result.scoreIsProvisional,
            "scoreIsProvisional",
        )
        assertEquals(expected.getValue("heatmapLevel").jsonPrimitive.int, result.heatmapLevel, "heatmapLevel")
    }

    private fun assertClose(
        expected: JsonElement,
        actual: Double?,
        path: String,
    ) {
        if (expected is JsonNull) {
            assertEquals(null, actual, path)
            return
        }
        check(expected is JsonPrimitive)
        val e = expected.double
        assertNotNull(actual, "$path: expected $e, got null")
        assertTrue(abs(actual - e) <= tolerance, "$path: expected $e, got $actual")
    }
}
