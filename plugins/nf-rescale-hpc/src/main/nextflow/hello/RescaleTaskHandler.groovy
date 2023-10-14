package nextflow.hello

import java.nio.file.Paths

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import nextflow.fusion.FusionAwareTask
import nextflow.processor.TaskRun
import nextflow.processor.TaskHandler
import nextflow.processor.TaskStatus
import nextflow.exception.AbortOperationException




@Slf4j
class RescaleTaskHandler extends TaskHandler implements FusionAwareTask {
    private RescaleExecutor executor

    protected volatile String jobId

    protected RescaleJob rescaleJobConfig

    protected String RESCALE_CLUSTER_TOKEN
    protected String RESCALE_PLATFORM_URL

    protected String getRESCALE_CLUSTER_TOKEN() {
        return this.RESCALE_CLUSTER_TOKEN
    }

    protected String getRESCALE_PLATFORM_URL() {
        return this.RESCALE_PLATFORM_URL
    }

    protected String setJobId(String jobId) { this.jobId = jobId }

    RescaleTaskHandler(TaskRun task, RescaleExecutor executor) {
        super(task)
        this.executor = executor
        this.rescaleJobConfig = new RescaleJob(task, executor)
    }
    // Initalization required after constractor
    protected RescaleTaskHandler initialize() {
        getConfigEnviromentVariable()

        return this
    }

    protected void getConfigEnviromentVariable() {
        List<String> errorMessages = []

        Map<String,String> environment = task.getEnvironment()
        if (!environment.containsKey('RESCALE_CLUSTER_TOKEN')) {
            errorMessages << "RESCALE_CLUSTER_TOKEN env in nextflow.config was not set"
        }
        if (!environment.containsKey('RESCALE_PLATFORM_URL')) {
            errorMessages << "RESCALE_PLATFORM_URL env in nextflow.config was not set"
        }
        if (!errorMessages.isEmpty()) {
            throw new AbortOperationException(errorMessages.join("\n"))
        }

        RESCALE_CLUSTER_TOKEN = environment['RESCALE_CLUSTER_TOKEN']
        RESCALE_PLATFORM_URL = environment['RESCALE_PLATFORM_URL']

    }

    protected HttpURLConnection createConnection(String endpoint) {
        HttpURLConnection connection = new URL("${RESCALE_PLATFORM_URL}${endpoint}").openConnection() as HttpURLConnection
        connection.setRequestProperty('Authorization', "Token $RESCALE_CLUSTER_TOKEN")

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
        def bodyJson = rescaleJobConfig.jobConfigurationJson()

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
    
            throw new AbortOperationException(errorMessage)
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
    
            throw new AbortOperationException(errorMessage)
        }
    }

    protected List<Map<String,String>> getStatuses(String jobId) {
        def connection = this.createConnection("/api/v2/jobs/$jobId/statuses/")
        connection.setRequestMethod('GET')

        connection.doInput = true

        if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
            def slurper = new JsonSlurper()
            def content = slurper.parseText(connection.inputStream.text)
            
            return content['results']

        } else {
            def errorMessage = "Error: ${connection.getResponseCode()} - ${connection.getResponseMessage()}"
            
            if (connection.errorStream != null) {
                errorMessage += "\nError Message: $connection.errorStream.text"
            }
    
            throw new AbortOperationException(errorMessage)
        }
    }

    protected Map<String,String> attachStorage(String jobId) {
        def connection = this.createConnection("/api/v2/jobs/$jobId/storage-devices/")
        connection.setRequestMethod('POST')

        // Header
        connection.setRequestProperty('Content-Type', 'application/json')

        connection.doOutput = true
        connection.doInput = true

        // Body
        def bodyJson = rescaleJobConfig.storageConfigurationJson()

        connection.outputStream.withWriter {
            writer -> writer.write(bodyJson)
        }

        if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
            def slurper = new JsonSlurper()
            def content = slurper.parseText(connection.inputStream.text)

            println content
            return content

        } else {
            def errorMessage = "Error: ${connection.getResponseCode()} - ${connection.getResponseMessage()}"
            
            if (connection.errorStream != null) {
                errorMessage += "\nError Message: $connection.errorStream.text"
            }
    
            throw new AbortOperationException(errorMessage)
        }
    }

    @Override
    void submit() {
        task.workDir = executor.getPublishDir()
        log.info "[Rescale Executor] WorkDir: ${task.workDir.toString()}"

        status = TaskStatus.SUBMITTED

        // Rescale Job 
        def content = createJob()
        
        setJobId(content['id'])

        // Temporary HPS Solution
        attachStorage(jobId)

        submitJob(jobId)

        task.stdout = task.workDir.resolve(TaskRun.CMD_OUTFILE)
        task.exitStatus = 0
    }

    private List<String> RUNNING_AND_COMPLETED = ["Completed", "Executing", "Validated", "Started", "Queued", "Pending", "Waiting for Queue", "Stopping"]
    private final String COMPLETED = "Completed"
    private final String STOPPING = "Stopping"

    @Override
    boolean checkIfRunning() {
        if(!jobId || !isSubmitted()) {
            return false
        }

        def jobStatus = getStatuses(jobId)[0]["status"]
        def result = jobStatus in RUNNING_AND_COMPLETED

        if (result) {
            log.info "[Rescale Executor] Job $jobId is running"
            status = TaskStatus.RUNNING
        }
        
        return result
    }

    @Override
    boolean checkIfCompleted() {
        assert jobId
        if ( !isRunning()) {
            return false
        }


        def jobStatus = getStatuses(jobId).collect { it["status"] }
        def result = COMPLETED in jobStatus
        def terminated = STOPPING in jobStatus

        if (terminated) {
            throw new AbortOperationException("Error: Job $jobId has stopped")
        }

        if (result) {
            log.info "[Rescale Executor] Job $jobId is completed"
            status = TaskStatus.COMPLETED
        }

        return result
    }

    @Override
    void kill() {

    }

}