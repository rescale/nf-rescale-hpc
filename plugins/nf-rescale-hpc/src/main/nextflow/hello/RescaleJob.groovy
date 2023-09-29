package nextflow.hello

import nextflow.processor.TaskRun
import nextflow.exception.AbortOperationException


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
                    "useRescaleLicense": true,
                    "command": "${task.script.trim()}",
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
        if (task.config.ext.storageId == null) {
            throw new AbortOperationException("Error: HPS Storage is not set. Set storageId using ext.storageId in process directives.")
        }

        return """
        {
            "storageDevice": { "id": "${task.config.ext.storageId}" }
        }
        """
    }

}