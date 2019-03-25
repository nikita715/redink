package ru.nikstep.redink.analysis

import mu.KLogger
import ru.nikstep.redink.model.data.AnalysisSettings

internal inline fun <T> KLogger.loggedAnalysis(settings: AnalysisSettings, action: () -> T): T {
    info {
        "Analysis: start analysing of repository  ${settings.repository.name}"
    }
    val result = action()
    info {
        "Analysis: complete analysing of repository  ${settings.repository.name}"
    }
    return result
}