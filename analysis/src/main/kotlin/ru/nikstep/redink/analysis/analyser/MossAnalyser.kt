package ru.nikstep.redink.analysis.analyser

import mu.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.nikstep.redink.analysis.solutions.SourceCodeStorage
import ru.nikstep.redink.model.data.*
import ru.nikstep.redink.model.enums.AnalysisMode
import ru.nikstep.redink.util.inTempDirectory
import java.time.LocalDateTime

/**
 * Moss client wrapper
 */
class MossAnalyser(
    private val sourceCodeStorage: SourceCodeStorage,
    private val mossPath: String
) : Analyser {
    private val logger = KotlinLogging.logger {}

    private val extensionRegex = "\\.[a-zA-Z]+$".toRegex()

    override fun analyse(settings: AnalysisSettings): AnalysisResult =
        inTempDirectory { tempDir ->
            logger.info { "Analysis:Moss:1.Gathering files for analysis. ${repoInfo(settings)}" }
            val analysisFiles = sourceCodeStorage.loadBasesAndSeparatedSolutions(settings, tempDir)

            logger.info { "Analysis:Moss:2.Start analysis. ${repoInfo(settings)}" }
            val resultLink = MossClient(analysisFiles, mossPath).run()

            val matchData =
                if (settings.mode.order > AnalysisMode.LINK.order) {
                    logger.info { "Analysis:Moss:3.Start parsing of results. ${repoInfo(settings)}" }
                    parseResult(settings, analysisFiles.solutions, resultLink)
                } else {
                    logger.info { "Analysis:JPlag:3.Skipped parsing. ${repoInfo(settings)}" }
                    emptyList()
                }

            logger.info { "Analysis:Moss:4.End of analysis. ${repoInfo(settings)}" }
            val executionDate = LocalDateTime.now()
            AnalysisResult(settings, resultLink, executionDate, matchData)
        }

    private fun repoInfo(analysisSettings: AnalysisSettings): String =
        analysisSettings.run { "repo ${repository.name}, branch $branch" }

    private fun parseResult(
        analysisSettings: AnalysisSettings,
        solutions: List<Solution>,
        resultLink: String
    ): List<AnalysisMatch> {
        return Jsoup.connect(resultLink).get()
            .body()
            .getElementsByTag("table")
            .select("tr")
            .drop(1)
            .map { tr -> tr.select("td") }
            .mapNotNull { tds ->
                val firstATag = tds[0].selectFirst("a")
                val secondATag = tds[1].selectFirst("a")

                val firstPath = firstATag.text().split(" ").first().split("/")
                val secondPath = secondATag.text().split(" ").first().split("/")

                val students =
                    firstPath.zip(secondPath)
                        .dropWhile { it.first == it.second }
                        .first().let {
                            it.first.replace(extensionRegex, "") to it.second.replace(extensionRegex, "")
                        }

                val lines = tds[2].text().toInt()

                val percentage = firstATag.text().split(" ")
                    .last()
                    .removeSurrounding("(", "%)")
                    .toInt()

                val matchedLines =
                    if (analysisSettings.mode == AnalysisMode.FULL)
                        findMatchedLines(firstATag, solutions, students)
                    else listOf()

                AnalysisMatch(
                    students = students.first to students.second,
                    lines = lines,
                    percentage = percentage,
                    matchedLines = matchedLines,
                    sha = findSolutionByStudent(solutions, students.first).sha
                            to findSolutionByStudent(solutions, students.second).sha
                )
            }
    }

    private fun findMatchedLines(
        a: Element,
        solutions: List<Solution>,
        students: Pair<String, String>
    ): List<MatchedLines> {
        val allMatchedRows = Jsoup.connect(a.attr("href").replace(".html", "-top.html"))
            .get().getElementsByTag("tr")
        val leftMatchedLines = mutableListOf<Pair<Int, Int>>()
        val rightMatchedLines = mutableListOf<Pair<Int, Int>>()
        for (row in allMatchedRows.subList(1, allMatchedRows.size)) {
            val cells = row.getElementsByTag("td")
            val firstMatch = cells[0].selectFirst("a").text().split("-")
            val secondMatch = cells[2].selectFirst("a").text().split("-")
            leftMatchedLines += firstMatch[0].toInt() to firstMatch[1].toInt()
            rightMatchedLines += secondMatch[0].toInt() to secondMatch[1].toInt()
        }

        val solution1 = findSolutionByStudent(solutions, students.first)
        val solution2 = findSolutionByStudent(solutions, students.second)

        val leftFilesToMatchedLines = filesWithMatchedLines(leftMatchedLines, solution1)
        val rightFilesToMatchedLines = filesWithMatchedLines(rightMatchedLines, solution2)

        if (leftFilesToMatchedLines.size != rightFilesToMatchedLines.size) return emptyList()

        return (0 until leftFilesToMatchedLines.size).map { i ->
            MatchedLines(
                match1 = leftFilesToMatchedLines[i].second.first to leftFilesToMatchedLines[i].second.second,
                match2 = rightFilesToMatchedLines[i].second.first to rightFilesToMatchedLines[i].second.second,
                files = leftFilesToMatchedLines[i].first to rightFilesToMatchedLines[i].first
            )
        }
    }

    private fun filesWithMatchedLines(
        matchedLines: List<Pair<Int, Int>>,
        solution: Solution
    ): List<Pair<String, Pair<Int, Int>>> =
        matchedLines.flatMap {
            var index = 0
            var pairs = listOf(it)
            for (i in solution.includedFilePositions) {
                if (it.first >= i) {
                    index++
                } else break
            }
            for (i in index until solution.includedFilePositions.size) {
                if (pairs.last().second > solution.includedFilePositions[i]) {
                    pairs = splitPairs(pairs, solution.includedFilePositions[i])
                } else break
            }
            if (index > 0) pairs.mapIndexed { order, pair ->
                solution.includedFileNames[index + order] to
                        (pair.first - solution.includedFilePositions[index + order - 1]
                                to pair.second - solution.includedFilePositions[index + order - 1])
            } else pairs.mapIndexed { order, pair ->
                solution.includedFileNames[index + order] to pair
            }
        }

    private fun splitPairs(pairs: List<Pair<Int, Int>>, index: Int): List<Pair<Int, Int>> {
        val lastPair = pairs.last()
        return pairs.dropLast(1).plus(listOf(lastPair.first to index, index + 1 to lastPair.second))
    }

}