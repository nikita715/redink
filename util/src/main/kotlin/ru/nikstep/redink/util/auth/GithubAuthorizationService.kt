package ru.nikstep.redink.util.auth

import mu.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.util.ResourceUtils
import ru.nikstep.redink.util.sendGithubAccessTokenRequest

/**
 * Github authorisation service
 */
open class GithubAuthorizationService : AuthorizationService {

    private val logger = KotlinLogging.logger {}

    private val bearer = "Bearer "

    @Cacheable(cacheNames = ["githubAccessTokens"], sync = true)
    override fun getAuthorizationToken(installationId: String): String {
        logger.info { "Authorization: new request for access token from github" }
        return bearer + sendGithubAccessTokenRequest(installationId, getToken()).string("token")
    }

    private fun getToken(): String {
        val file = ResourceUtils.getFile("classpath:keygen.rb")
        val process = Runtime.getRuntime().exec("ruby $file")
        process.waitFor()
        val generatedKey = process.inputStream.bufferedReader().readText()
        if (generatedKey.isBlank()) throw RuntimeException("Authorization: github local token is empty")
        return bearer + generatedKey.replace("\n", "")
    }
}