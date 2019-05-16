package io.gitplag.model.data

import io.gitplag.model.entity.Repository
import io.gitplag.model.enums.AnalysisMode
import io.gitplag.model.enums.AnalyzerProperty
import io.gitplag.model.enums.Language

/**
 * Input analysis properties
 */
class AnalysisSettings(
    val repository: Repository,
    val branch: String
) {
    var analyzer: AnalyzerProperty = repository.analyzer
        private set(value) {
            field = value
        }
    var language: Language = repository.language
        private set(value) {
            field = value
        }
    var analysisMode: AnalysisMode = repository.analysisMode
        private set(value) {
            field = value
        }
    var parameters: String = repository.parameters
        private set(value) {
            field = value
        }
    var updateFiles: Boolean = false
        private set(value) {
            field = value
        }

    constructor(
        repository: Repository, branch: String, analyzer: AnalyzerProperty? = repository.analyzer,
        language: Language?, parameters: String?,
        analysisMode: AnalysisMode?, updateFiles: Boolean?
    ) : this(repository, branch) {
        this.analyzer = analyzer ?: repository.analyzer
        this.parameters = parameters ?: repository.parametersOfAnalyzer(this.analyzer)
        this.language = language ?: repository.language
        this.analysisMode = analysisMode ?: repository.analysisMode
        this.updateFiles = updateFiles ?: false
    }
}