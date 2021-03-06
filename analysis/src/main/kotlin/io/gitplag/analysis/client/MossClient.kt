package io.gitplag.analysis.client

import io.gitplag.model.data.PreparedAnalysisData
import mossclient.MossClient
import mossclient.MossLanguage

/**
 * Client of the Moss plagiarism analysis service.
 * See http://moss.stanford.edu
 */
internal class MossClient(analysisData: PreparedAnalysisData, private val mossId: String) {
    private val language = analysisData.language
    private val bases = analysisData.bases
    private val solutions = analysisData.solutions
    private val resultSize = analysisData.resultSize

    @Synchronized
    fun run(): String {
        val mossClient = MossClient(mossId, MossLanguage.valueOf(language.name))

        if (resultSize != null) mossClient.resultSize(resultSize.toLong())

        val files = solutions.asSequence().filter { !it.isEmpty }.map { it.student to it.file }.toList()
        return mossClient
            .submitFiles(bases, isBase = true)
            .submitNamedFiles(files)
            .getResult()
    }

}