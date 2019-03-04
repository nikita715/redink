package ru.nikstep.redink.core.rest

import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ru.nikstep.redink.git.webhook.GitlabWebhookService
import javax.servlet.http.HttpServletRequest

@RestController
class GitlabWebhookController(private val gitlabWebhookService: GitlabWebhookService) {
    private val logger = KotlinLogging.logger {}

    @PostMapping("/webhook/gitlab", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun processGithubWebhookRequest(@RequestBody payload: String, httpServletRequest: HttpServletRequest) {
        val event = httpServletRequest.getHeader("X-Gitlab-Event")
        logger.info { "Webhook: got new $event" }
        when (event) {
            "Merge Request Hook" -> gitlabWebhookService.saveNewPullRequest(payload)
            else -> logger.info { "Webhook: $event is not supported" }
        }
    }
}