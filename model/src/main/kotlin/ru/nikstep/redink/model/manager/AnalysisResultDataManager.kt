package ru.nikstep.redink.model.manager

import org.springframework.transaction.annotation.Transactional
import ru.nikstep.redink.model.data.AnalysisResult
import ru.nikstep.redink.model.data.AnalysisSettings
import ru.nikstep.redink.model.entity.Analysis
import ru.nikstep.redink.model.entity.AnalysisPair
import ru.nikstep.redink.model.entity.AnalysisPairLines
import ru.nikstep.redink.model.repo.AnalysisPairLinesRepository
import ru.nikstep.redink.model.repo.AnalysisPairRepository
import ru.nikstep.redink.model.repo.AnalysisRepository

/**
 * Data manager of [AnalysisResult]
 */
@Transactional
class AnalysisResultDataManager(
    private val analysisRepository: AnalysisRepository,
    private val analysisPairRepository: AnalysisPairRepository,
    private val analysisPairLinesRepository: AnalysisPairLinesRepository
) {

    /**
     * Save all analysis results
     */
    @Transactional
    fun saveAnalysis(analysisSettings: AnalysisSettings, analysisResults: AnalysisResult): Analysis {
        val analysis = analysisRepository.save(
            Analysis(
                repository = analysisSettings.repository,
                executionDate = analysisResults.executionDate,
                language = analysisSettings.language,
                analyser = analysisSettings.analyser,
                branch = analysisSettings.branch,
                resultLink = analysisResults.resultLink,
                hash = analysisResults.hash
            )
        )
        val analysisPairs = analysisResults.matchData.map {
            val analysisPair = analysisPairRepository.save(
                AnalysisPair(
                    student1 = it.students.first,
                    student2 = it.students.second,
                    lines = it.lines,
                    percentage = it.percentage,
                    analysis = analysis,
                    sha1 = it.sha.first,
                    sha2 = it.sha.second,
                    createdAt1 = it.createdAt.first,
                    createdAt2 = it.createdAt.second
                )
            )
            val analysisPairLines = analysisPairLinesRepository.saveAll(it.matchedLines.map {
                AnalysisPairLines(
                    from1 = it.match1.first,
                    to1 = it.match1.second,
                    from2 = it.match2.first,
                    to2 = it.match2.second,
                    fileName1 = it.files.first,
                    fileName2 = it.files.second,
                    analysisPair = analysisPair
                )
            })
            analysisPair to analysisPairLines
        }
        val res = analysisPairs.map { analysisPairRepository.save(it.first.copy(analysisPairLines = it.second)) }
        return analysisRepository.save(analysis.copy(analysisPairs = res))
    }
}