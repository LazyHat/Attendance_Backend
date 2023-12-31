package ru.lazyhat.plugins

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.koin.ktor.ext.inject
import ru.lazyhat.models.StudentCreate
import ru.lazyhat.models.TeacherCreate
import ru.lazyhat.repository.AdminRepository

fun Application.configureParsingData() {
    val adminRepository by inject<AdminRepository>()
    val scope = CoroutineScope(SupervisorJob())
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json()
        }
    }

    scope.launch {
        if (adminRepository.getAllTeachers().isEmpty())
            adminRepository.createTeacher(TeacherCreate("lazy", "LazyHat", "pass"))
        if (adminRepository.getAllStudents().isEmpty()) {
            adminRepository.putSomeStudents(
                parseStudentsByGroup(client, "2091") +
                        parseStudentsByGroup(client, "2092") +
                        parseStudentsByGroup(client, "2093")
            )
        }
    }
}

suspend fun parseStudentsByGroup(client: HttpClient, group: String): List<StudentCreate> {
    val responseText =
        client.get("https://portal.novsu.ru/search/groups/i.2500/?page=search&grpname=$group").bodyAsText()
    val responseHtml = Jsoup.parse(responseText)
    val headers = responseHtml.getElementById("npe_instance_2500_npe_content")?.getElementsByTag("ul")
    return headers?.let { _ ->
        val headersContainingOchnGroup = headers.indices.associateWith { headers[it] }
            .filter { entry: Map.Entry<Int, Element> ->
                entry.value.getElementsContainingText("Форма обучения: очная").isNotEmpty()
            }
        val headersNormalYears = headersContainingOchnGroup.filter { entry: Map.Entry<Int, Element> ->
            val elemsContaining = entry.value.getElementsByTag("li")
                .filter { e: Element -> e.getElementsContainingText("Год поступления").isNotEmpty() }
            (elemsContaining.firstOrNull()?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 2000) >= 2016
        }
        val studentHeaderIndex = headersNormalYears.entries.firstOrNull()?.key
        val studentTable = responseHtml.getElementsByClass("viewtable").getOrNull(studentHeaderIndex ?: -1)
        studentTable?.getElementsByTag("a")
            ?.map {
                StudentCreate(it.attr("href").filter { it.isDigit() }.let { "s$it" },
                    it.text(),
                    "pass",
                    group
                )
            }
    } ?: listOf()
}