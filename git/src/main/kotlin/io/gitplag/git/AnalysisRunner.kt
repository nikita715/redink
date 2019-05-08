package io.gitplag.git

import io.gitplag.analysis.AnalysisRunner
import io.gitplag.analysis.analyzer.Analyzer
import io.gitplag.git.payload.PayloadProcessor
import io.gitplag.model.data.AnalysisSettings
import io.gitplag.model.entity.Analysis
import io.gitplag.model.enums.AnalyzerProperty
import io.gitplag.model.enums.GitProperty
import io.gitplag.model.manager.AnalysisResultDataManager
import mu.KotlinLogging

/**
 * Main analysis class
 */
class GitAnalysisRunner(
    private val analyzers: Map<AnalyzerProperty, Analyzer>,
    private val payloadProcessors: Map<GitProperty, PayloadProcessor>,
    private val analysisResultDataManager: AnalysisResultDataManager
) : AnalysisRunner {
    private val logger = KotlinLogging.logger {}

    /**
     * Run analysis with [settings]
     */
    override fun run(settings: AnalysisSettings): Analysis {
        if (settings.updateFiles) {
            payloadProcessors.getValue(settings.repository.gitService)
                .downloadAllPullRequestsOfRepository(settings.repository)
        }
        val analysisService = analyzers.getValue(settings.analyzer)
        return logger.loggedAnalysis(settings) { analysisService.analyze(settings) }
            .let { analysisResultDataManager.saveAnalysis(settings, it) }
    }

    /**
     * Run all analyzes with [settingsList]
     */
    override fun run(settingsList: List<AnalysisSettings>): List<Analysis> =
        settingsList.map { logger.loggedAnalysis(it) { run(it) } }

}