package ru.nikstep.redink.checks

import com.google.gson.Gson
import mu.KotlinLogging
import ru.nikstep.redink.model.entity.PullRequest
import ru.nikstep.redink.util.asIsoString
import ru.nikstep.redink.util.auth.AuthorizationService
import ru.nikstep.redink.util.sendGithubStatusCheckRequest
import java.util.*

class GithubAnalysisStatusCheckService(private val authorizationService: AuthorizationService) :
    AnalysisStatusCheckService {
    private val logger = KotlinLogging.logger {}

    override fun send(pullRequest: PullRequest, analysisData: AnalysisResultData) {
        val accessToken = authorizationService.getAuthorizationToken(pullRequest.secretKey)
        val body = createBody(pullRequest, analysisData)
        sendGithubStatusCheckRequest(pullRequest.repoFullName, accessToken, body)
        logger.info { "AnalysisResult: sent for ${pullRequest.repoFullName}, creator ${pullRequest.creatorName}" }
    }

    override fun sendInProgressStatus(pullRequest: PullRequest) {
        AnalysisResultData(status = GithubAnalysisStatus.IN_PROGRESS.value).also {
            send(pullRequest, it)
        }
    }

    private fun createBody(
        pullRequest: PullRequest,
        analysisData: AnalysisResultData
    ): String {

        val body = mutableMapOf<String, Any?>(
            "name" to "Plagiarism tests",
            "head_sha" to pullRequest.headSha,
            "status" to analysisData.status
        )

        if (analysisData.status == GithubAnalysisStatus.COMPLETED.value) {
            body.putAll(
                mapOf(
                    "conclusion" to analysisData.conclusion,
                    "completed_at" to Date().asIsoString(),
                    "details_url" to analysisData.detailsUrl,
                    "output" to mapOf(
                        "title" to "Report",
                        "summary" to analysisData.summary
                    )
                )
            )
        }

        return Gson().toJson(body)
    }
}