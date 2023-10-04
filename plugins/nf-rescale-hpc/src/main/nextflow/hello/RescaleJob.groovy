package nextflow.hello

import java.io.File

import groovy.util.logging.Slf4j

import nextflow.processor.TaskRun
import nextflow.exception.AbortOperationException

@Slf4j
class RescaleJob {
    
    protected TaskRun task 
    
    
    RescaleJob(TaskRun task) {
        this.task = task
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
                    "envVars": {},
                    "command": "cd ~/storage*/projectdata; ${task.script.trim()}",
                    "hardware": {
                        "coreType": "${task.config.machineType}",
                        "coresPerSlot": ${task.config.cpus}
                    }
                }
            ]
        }
        """
    }

    // Temporary  HPS Solution
    protected String findStorageId() {
        def dir = new File(System.getProperty("user.home"))
        def filePattern = ~/storage_.*/

        def storageDir = dir.listFiles().find { File file -> 
            file.isDirectory() && file.name ==~ filePattern
        }

        if (storageDir) {
            String[] parts = storageDir.name.split("_")
            log.info "Directory Found: ${parts}"
            
            return parts[1]
        } else {
            log.error "Directory not found"
        }

    }

    protected String storageConfigurationJson() {
        def storageId = findStorageId()

        if (storageId == null) {
            throw new AbortOperationException("Can't find storageId")
        }

        return """
        {
            "storageDevice": { "id": "${storageId}" }
        }
        """
    }

}