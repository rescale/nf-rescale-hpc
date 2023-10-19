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

    protected String envVarsJson() {
        if (task.getEnvironment() == null) {
            return "{}"
        }

        def json = new JsonBuilder(task.getEnvironment())

        return json.toString()
    }

    protected Path getCustomAbsolutePath(Path path) {
        def home = executor.getBaseDir()
        def absolutePath = path.toAbsolutePath().normalize().toString()

        if (absolutePath.startsWith(home)) {
            absolutePath = absolutePath.replace(home, '$HOME')
        }

        return Paths.get(absolutePath)
    }

    protected String commandString() {
        def path = getCustomAbsolutePath(executor.getOutputDir()).toString()
        def command = "cd $path\n" + task.script.trim().split('\n').collect { it.trim() }.join('\n')
        def json = new JsonBuilder(command)

        return json.toString()
    }

    protected String jobConfigurationJson() {
        List<String> errorMessages = []
        if (task.config.ext.analysisCode == null) {
            errorMessages << "Error: Job analysis software is not set in process. Set analysis software using ext.analysisCode in process directives."
        }

        if (task.config.ext.analysisVersion == null) {
            errorMessages << "Error: Job analysis version is not set in process. Set analysis version using ext.analysisVersion in process directives."
        }

        if (task.config.machineType == null) {
            errorMessages << "Error: Hardware type is not set in process. Set hardware type using machineType in process directives."
        }
        // Rescale License defaults to false
        if (task.config.ext.rescaleLicense == null) {
            task.config.ext.rescaleLicense = false
        }

        if (!errorMessages.isEmpty()) {
            throw new AbortOperationException(errorMessages.join("\n"))
        }

        return """
        {
            "name": "${task.name}",
            "jobanalyses": [
                {
                    "analysis": {
                        "code": "${task.config.ext.analysisCode}",
                        "version": "${task.config.ext.analysisVersion}"
                    },
                    "useRescaleLicense": ${task.config.ext.rescaleLicense},
                    "envVars": ${envVarsJson()},
                    "command": ${commandString()},
                    "hardware": {
                        "coreType": "${task.config.machineType}",
                        "coresPerSlot": ${task.config.cpus}
                    }
                }
            ]
        }
        """
    }

    protected String storageConfigurationJson() {
        return """
        {
            "storageDevice": { "id": "${storageId}" }
        }
        """
    }

}