package ru.nikstep.redink.core.webhook

import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ru.nikstep.redink.git.webhook.BitbucketWebhookService
import javax.servlet.http.HttpServletRequest

/**
 * Receiver of bitbucket webhook messages
 */
@RestController("")
class BitbucketWebhookController(private val bitbucketWebhookService: BitbucketWebhookService) {
    private val logger = KotlinLogging.logger {}

    /**
     * Receive and process [payload] from bitbucket
     */
    @PostMapping("/webhook/bitbucket", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun processBitbucketWebhookRequest(@RequestBody payload: String, httpServletRequest: HttpServletRequest) {
        val event = httpServletRequest.getHeader("X-Event-Key")
        logger.info { "Webhook: got new $event" }
        when (event.substringBefore(":")) {
            "pullrequest" -> bitbucketWebhookService.updateSolutionsOfPullRequest(payload)
            "repo" -> bitbucketWebhookService.updateBasesOfRepository(payload)
            else -> logger.info { "Webhook: $event is not supported" }
        }
    }
}