package nextflow.hello

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

import groovy.util.logging.Slf4j
import groovy.json.JsonBuilder

import nextflow.processor.TaskRun
import nextflow.exception.AbortOperationException

@Slf4j
class RescaleJob {
    
    private RescaleExecutor executor

    protected TaskRun task

    protected String storageId

    protected String PROJECT_DATA = "nextflow"
    
    RescaleJob(TaskRun task, RescaleExecutor executor) {
        this.task = task
        this.executor = executor
        this.storageId = executor.getStorageId()
    }

    protected Map envVarsJson() {
        if (task.getEnvironment() == null) {
            return [:]
        }

        return task.getEnvironment()
    }

    protected Path getCustomAbsolutePath(Path path) {
        def home = executor.getBaseDir()
        def absolutePath = path.toAbsolutePath().normalize().toString()

        if (absolutePath.startsWith(home)) {
            absolutePath = absolutePath.replace(home, '$HOME')
        }

        return Paths.get(absolutePath)
    }

    protected String commandString(Path wrapperFile) {
        def path = getCustomAbsolutePath(wrapperFile.getParent()).toString()
        def wrapper = getCustomAbsolutePath(wrapperFile).toString()

        return "cd $path\nchmod +x $wrapper\n$wrapper"
    }

    protected Map onDemandLicenseSeller() {
        return task.config.ext.onDemandLicenseSeller
    }

    protected Map userDefinedLicenseSettings() {
        return task.config.ext.userDefinedLicenseSettings
    }

    protected Map hardwareConfig() {
        List<String> errorMessages = []

        if (task.config.machineType == null) {
            errorMessages << "Error: Hardware type is not set in process. Set hardware type using machineType in process directives."
        }

        if (!errorMessages.isEmpty()) {
            throw new AbortOperationException(errorMessages.join("\n"))
        }

        def config = ["coreType": task.config.machineType,
                      "coresPerSlot": task.config.cpus]

        def wallTime = task.config.ext.wallTime
        
        // task.config.ext implementation either returns an empty list or the value
        if (wallTime != null) { 
            if (wallTime < 1 || wallTime > 2147483647) {
                config["walltime"] = 8
                log.warn "[Rescale Executor] Invalid wallTime of $wallTime set. Default set to 8."
            }
            config["walltime"] = wallTime
        }

        return config
    }

    protected Map jobAnalyseConfig(
        analysisCode,
        analysisVersion,
        command,
        rescaleLicense,
        onDemandLicenseSeller,
        userDefinedLicenseSettings
    ) {
        List<String> errorMessages = []
        if (analysisCode == null) {
            errorMessages << "Error: Job analysis software is not set in process. Set analysis software using ext.analysisCode in process directives."
        }

        if (analysisVersion == null) {
            errorMessages << "Error: Job analysis version is not set in process. Set analysis version using ext.analysisVersion in process directives."
        }

        // Rescale License defaults to false
        if (rescaleLicense == null) {
            rescaleLicense = false
        }

        if (!errorMessages.isEmpty()) {
            throw new AbortOperationException(errorMessages.join("\n"))
        }

        def config = [
            "analysis": [
                "code": analysisCode,
                "version": analysisVersion
            ],
            "useRescaleLicense": rescaleLicense,
            "envVars": envVarsJson(),
            "command": command,
            "hardware": hardwareConfig(),
            "onDemandLicenseSeller": onDemandLicenseSeller,
            "userDefinedLicenseSettings": userDefinedLicenseSettings
        ]

        return config
    }

    protected String jobConfigurationJson(Path wrapperFile) {
        List<String> errorMessages = []
        if (task.config.ext.jobAnalyses == null) {
            errorMessages << "Error: Job analysis software is not set in process. Set analysis software using ext.jobAnalyses in process directives."
        }

        if (!errorMessages.isEmpty()) {
            throw new AbortOperationException(errorMessages.join("\n"))
        }

        def jobAnalyses = task.config.ext.jobAnalyses


        for (int i = 0; i < jobAnalyses.size(); i++) {
            def command = ":"
            if (i == jobAnalyses.size() - 1) {
                command = commandString(wrapperFile)   
            }

            jobAnalyses[i] = jobAnalyseConfig(
                    jobAnalyses[i].analysisCode,
                    jobAnalyses[i].analysisVersion,
                    command,
                    jobAnalyses[i].rescaleLicense,
                    jobAnalyses[i].onDemandLicenseSeller,
                    jobAnalyses[i].userDefinedLicenseSettings
                )
        }
        

        def config = [
            "name": task.name,
            "jobanalyses": jobAnalyses
        ]

        if (task.config.ext.billingPriorityValue != null) {
            config["billingPriorityValue"] = task.config.ext.billingPriorityValue
        }

        if (task.config.ext.projectId != null) {
            config["project_id"] = task.config.ext.projectId
        }

        return new JsonBuilder(config).toString() 
    }

    protected String storageConfigurationJson() {
        return """
        {
            "storageDevice": { "id": "${storageId}" }
        }
        """
    }

}