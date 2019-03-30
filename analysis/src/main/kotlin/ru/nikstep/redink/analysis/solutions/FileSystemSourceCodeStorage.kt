package ru.nikstep.redink.analysis.solutions

import mu.KotlinLogging
import ru.nikstep.redink.model.data.AnalysisSettings
import ru.nikstep.redink.model.data.PreparedAnalysisData
import ru.nikstep.redink.model.data.Solution
import ru.nikstep.redink.model.entity.BaseFileRecord
import ru.nikstep.redink.model.entity.PullRequest
import ru.nikstep.redink.model.entity.Repository
import ru.nikstep.redink.model.entity.SolutionFileRecord
import ru.nikstep.redink.model.enums.GitProperty
import ru.nikstep.redink.model.manager.RepositoryDataManager
import ru.nikstep.redink.model.repo.BaseFileRecordRepository
import ru.nikstep.redink.model.repo.PullRequestRepository
import ru.nikstep.redink.model.repo.SolutionFileRecordRepository
import ru.nikstep.redink.util.asPath
import ru.nikstep.redink.util.forEachFileInDirectory
import java.io.File
import java.nio.file.Files

/**
 * Storage of source files based on file system
 */
class FileSystemSourceCodeStorage(
    private val baseFileRecordRepository: BaseFileRecordRepository,
    private val repositoryDataManager: RepositoryDataManager,
    private val solutionFileRecordRepository: SolutionFileRecordRepository,
    private val pullRequestRepository: PullRequestRepository,
    private val solutionsDir: String
) : SourceCodeStorage {

    private val logger = KotlinLogging.logger {}
    private val baseDir = ".base"

    override fun loadBasesAndComposedSolutions(settings: AnalysisSettings, tempDir: String) =
        loadBasesAndSolutions(settings, tempDir, loadComposedSolutions(settings, tempDir))

    override fun loadBasesAndSeparatedSolutions(settings: AnalysisSettings, tempDir: String) =
        loadBasesAndSolutions(settings, tempDir, loadSeparateSolutions(settings, tempDir))

    override fun loadBasesAndSeparatedCopiedSolutions(settings: AnalysisSettings, tempDir: String) =
        loadBasesAndSolutions(settings, tempDir, loadSeparateCopiedSolutions(settings, tempDir))

    private fun loadBases(settings: AnalysisSettings, tempDir: String): List<File> {
        val filePatterns = repositoryDataManager.findFileNameRegexps(settings.repository)
        val baseDir = File("$tempDir/.base")
        Files.createDirectory(baseDir.toPath())
        var fileIterator = 0
        return baseFileRecordRepository.findAllByRepo(settings.repository)
            .mapNotNull { baseRecord ->
                if (nameMatchesRegex(baseRecord.fileName, filePatterns)) {
                    val generatedFileName = "${fileIterator++}.${baseRecord.fileName.toFileExtension()}"
                    val generatedFile = File(baseDir.absolutePath + "/" + generatedFileName)
                    Files.copy(
                        File(
                            pathToBase(
                                settings.repository.gitService, settings.repository.name,
                                baseRecord.branch, baseRecord.fileName
                            )
                        ).toPath(),
                        generatedFile.toPath()
                    )
                    generatedFile
                } else null
            }
    }

    private fun loadBasesAndSolutions(settings: AnalysisSettings, rootDir: String, solutions: List<Solution>) =
        PreparedAnalysisData(
            gitService = settings.repository.gitService,
            repoName = settings.repository.name,
            language = settings.language,
            bases = loadBases(settings, rootDir),
            solutions = solutions,
            rootDir = rootDir
        )

    private fun loadSeparateSolutions(analysisSettings: AnalysisSettings, tempDir: String): List<Solution> {
        val filePatterns = repositoryDataManager.findFileNameRegexps(analysisSettings.repository)
        return pullRequestRepository.findAllByRepoAndSourceBranchName(
            analysisSettings.repository,
            analysisSettings.branch
        ).flatMap { pullRequest ->
            solutionFileRecordRepository.findAllByPullRequest(pullRequest).mapNotNull { solutionRecord ->
                if (nameMatchesRegex(solutionRecord.fileName, filePatterns)) {
                    val file = File(pathToSolution(analysisSettings, solutionRecord))
                    val copiedFile = File("$tempDir/${pullRequest.creatorName}/${solutionRecord.fileName}")
                    copiedFile.parentFile.mkdirs()
                    Files.copy(file.toPath(), copiedFile.toPath())
                    Solution(
                        student = pullRequest.creatorName,
                        fileName = solutionRecord.fileName,
                        file = copiedFile, sha = pullRequest.headSha
                    )
                } else null
            }
        }
    }

    private fun loadSeparateCopiedSolutions(analysisSettings: AnalysisSettings, tempDir: String): List<Solution> {
        val filePatterns = repositoryDataManager.findFileNameRegexps(analysisSettings.repository)
        return pullRequestRepository.findAllByRepoAndSourceBranchName(
            analysisSettings.repository,
            analysisSettings.branch
        ).flatMap { pullRequest ->
            val solutionRecords = solutionFileRecordRepository.findAllByPullRequest(pullRequest)
            val indexToFileName = mutableListOf<Pair<String, String>>()
            val studentDir = File("$tempDir/${pullRequest.creatorName}")
            Files.createDirectory(studentDir.toPath())
            var fileIterator = 0
            solutionRecords.mapNotNull { solutionRecord ->
                if (nameMatchesRegex(solutionRecord.fileName, filePatterns)) {
                    val generatedFileName = "${fileIterator++}.txt"
                    val generatedFile = File(studentDir.absolutePath + "/" + generatedFileName)
                    indexToFileName += generatedFileName to solutionRecord.fileName
                    Files.copy(
                        File(pathToSolution(analysisSettings, solutionRecord)).toPath(),
                        generatedFile.toPath()
                    )
                    Solution(
                        student = pullRequest.creatorName,
                        fileName = generatedFileName,
                        file = generatedFile,
                        sha = pullRequest.headSha,
                        realFileName = solutionRecord.fileName
                    )
                } else null
            }
        }
    }

    private fun loadComposedSolutions(analysisSettings: AnalysisSettings, tempDir: String): List<Solution> {
        val filePatterns = repositoryDataManager.findFileNameRegexps(analysisSettings.repository)
        return pullRequestRepository.findAllByRepoAndSourceBranchName(
            analysisSettings.repository,
            analysisSettings.branch
        ).map { pullRequest ->
            val solutionRecords = solutionFileRecordRepository.findAllByPullRequest(pullRequest)
            val fileName = pullRequest.creatorName + ".txt"
            val composedFile = File("$tempDir/${pullRequest.creatorName}/$fileName")
            composedFile.parentFile.mkdir()
            var composedFileLength = 0
            val fileNames = mutableListOf<String>()
            val filePositions = mutableListOf<Int>()
            solutionRecords.forEach { solutionRecord ->
                if (nameMatchesRegex(solutionRecord.fileName, filePatterns)) {
                    val solFile = File(pathToSolution(analysisSettings, solutionRecord))
                    composedFile.appendText(solFile.readText())
                    filePositions += solutionRecord.countOfLines + composedFileLength
                    composedFileLength += solutionRecord.countOfLines
                    fileNames += solutionRecord.fileName
                }
            }
            Solution(
                student = pullRequest.creatorName,
                fileName = fileName,
                file = composedFile,
                includedFileNames = fileNames,
                includedFilePositions = filePositions,
                sha = pullRequest.headSha
            )
        }
    }

    override fun saveBasesFromDir(
        tempDir: String,
        repo: Repository,
        branchName: String
    ) {
        val pathToBases = pathToBases(repo.gitService, repo.name, branchName)
        File(pathToBases).deleteRecursively()
        baseFileRecordRepository.deleteAllByRepoAndBranch(repo, branchName)
        val filePatterns = repositoryDataManager.findFileNameRegexps(repo)
        forEachFileInDirectory(tempDir) { foundedFile ->
            val fileName = File(tempDir).toPath().relativize(foundedFile.toPath()).toString()
            if (nameMatchesRegex(fileName, filePatterns)) {
                foundedFile.copyTo(File("$pathToBases/$fileName"))
                baseFileRecordRepository.save(
                    BaseFileRecord(
                        repo = repo,
                        fileName = fileName,
                        branch = branchName
                    )
                )
                logger.info { "Storage: Saved base file with name = $fileName of repo ${repo.name}" }
            } else {
                logger.info { "Storage: Ignored base file with name = $fileName of repo ${repo.name}" }
            }
        }
    }

    override fun saveSolutionsFromDir(
        tempDir: String,
        pullRequest: PullRequest
    ) {
        val pathToSolutions = pathToSolutions(pullRequest)
        File(pathToSolutions).deleteRecursively()
        solutionFileRecordRepository.deleteAllByPullRequest(pullRequest)
        val filePatterns = repositoryDataManager.findFileNameRegexps(pullRequest.repo)
        forEachFileInDirectory(tempDir) { foundedFile ->
            val fileName = File(tempDir).toPath().relativize(foundedFile.toPath()).toString()
            if (nameMatchesRegex(fileName, filePatterns)) {
                foundedFile.copyTo(File("$pathToSolutions/$fileName"))
                solutionFileRecordRepository.save(
                    SolutionFileRecord(
                        pullRequest = pullRequest,
                        fileName = fileName,
                        countOfLines = Files.lines(foundedFile.toPath()).count().toInt()
                    )
                )
                logger.info {
                    "Storage: Saved solution file with name = $fileName of" +
                            " repo ${pullRequest.repo.name}, user ${pullRequest.creatorName}"
                }
            } else {
                logger.info {
                    "Storage: Ignored solution file with name = $fileName of" +
                            " repo ${pullRequest.repo.name}, user ${pullRequest.creatorName}"
                }
            }
        }
    }

    private fun String.toFileExtension() =
        substringAfterLast(".")

    private fun nameMatchesRegex(fileName: String, filePatterns: Collection<String>): Boolean {
        filePatterns.forEach {
            if (it.toRegex().matches(fileName)) return true
        }
        return false
    }

    private fun pathToBases(git: GitProperty, repo: String, branch: String): String =
        asPath(solutionsDir, git, repo, branch.toLowerCase(), baseDir)

    private fun pathToBase(git: GitProperty, repo: String, branch: String, file: String): String =
        asPath(solutionsDir, git, repo, branch.toLowerCase(), baseDir, file)

    private fun pathToSolution(git: GitProperty, repo: String, branch: String, creator: String, file: String): String =
        asPath(solutionsDir, git, repo, branch.toLowerCase(), creator, file)

    private fun pathToSolutions(git: GitProperty, repo: String, branch: String, creator: String): String =
        asPath(solutionsDir, git, repo, branch.toLowerCase(), creator)

    private fun pathToSolutions(pullRequest: PullRequest): String =
        pathToSolutions(
            pullRequest.repo.gitService, pullRequest.repo.name,
            pullRequest.sourceBranchName.toLowerCase(), pullRequest.creatorName
        )

    private fun pathToBase(analysisSettings: AnalysisSettings, fileName: String): String =
        pathToBase(
            analysisSettings.repository.gitService, analysisSettings.repository.name,
            analysisSettings.branch, fileName
        )

    private fun pathToSolution(analysisSettings: AnalysisSettings, solutionFileRecord: SolutionFileRecord): String =
        pathToSolution(
            analysisSettings.repository.gitService,
            analysisSettings.repository.name,
            solutionFileRecord.pullRequest.sourceBranchName,
            solutionFileRecord.pullRequest.creatorName,
            solutionFileRecord.fileName
        )

}