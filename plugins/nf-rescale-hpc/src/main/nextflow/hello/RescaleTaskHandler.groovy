package nextflow.hello

import java.nio.file.Paths

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import nextflow.fusion.FusionAwareTask
import nextflow.processor.TaskRun
import nextflow.processor.TaskHandler
import nextflow.processor.TaskStatus




@Slf4j
class RescaleTaskHandler extends TaskHandler implements FusionAwareTask {
    private RescaleExecutor executor


    RescaleTaskHandler(TaskRun task, RescaleExecutor executor) {
        super(task)
        this.executor = executor
    }

    protected HttpURLConnection createConnection(String endpoint) {
        HttpURLConnection connection = new URL("${executor.RESCALE_PLATFORM_URL}${endpoint}").openConnection() as HttpURLConnection
        connection.setRequestProperty('Authorization', "Token $executor.RESCALE_CLUSTER_TOKEN")

        return connection
    }

    protected Map<String,String> createJob() {
        HttpURLConnection connection = this.createConnection('/api/v2/jobs/')
        connection.setRequestMethod('POST')

        // Header
        connection.setRequestProperty('Content-Type', 'application/json')

        connection.doOutput = true
        connection.doInput = true

        // Body
        def bodyJson = '''
        {
            "name": "Example Job",
            "jobanalyses": [
                {
                    "analysis": {
                        "code": "user_included",
                        "version": "0"
                    },
                    "command": "echo \\"First Job Run\\"",
                    "hardware": {
                        "coreType": "emerald",
                        "coresPerSlot": 1
                    }
                }
            ]
        }
        '''

        connection.outputStream.withWriter {
            writer -> writer.write(bodyJson)
        }

        if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
            def slurper = new JsonSlurper()
            def content = slurper.parseText(connection.inputStream.text)

            return content

        } else {
            def errorMessage = "Error: ${connection.getResponseCode()} - ${connection.getResponseMessage()}"
            
            if (connection.errorStream != null) {
                errorMessage += "\nError Message: $connection.errorStream.text"
            }
    
            throw new Exception(errorMessage)
        }
    }

    private void submitJob(String jobId) {
        def connection = this.createConnection("/api/v2/jobs/$jobId/submit/")
        connection.setRequestMethod('POST')

        connection.doInput = true

        if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
            log.trace "[Rescale Executor]: Job $jobId Submitted"
        } else {
            def errorMessage = "Error: ${connection.getResponseCode()} - ${connection.getResponseMessage()}"
            
            if (connection.errorStream != null) {
                errorMessage += "\nError Message: $connection.errorStream.text"
            }
    
            throw new Exception(errorMessage)
        }
    }

    @Override
    void submit() {
        task.workDir = Paths.get('.').complete() // Don't know the purpose
        status = TaskStatus.SUBMITTED

        // Rescale Job
        def content = this.createJob()
        def jobId = content['id']
        this.submitJob(jobId)

        task.stdout = task.script
        task.exitStatus = 0
    }

    @Override
    boolean checkIfRunning() {
        if (isSubmitted()) {
            status = TaskStatus.RUNNING
            return true
        }

        return false
    }

    @Override
    boolean checkIfCompleted() {
        if (isRunning()) {
            status = TaskStatus.COMPLETED
            return true
        }
        return false
    }

    @Override
    void kill() {}

}