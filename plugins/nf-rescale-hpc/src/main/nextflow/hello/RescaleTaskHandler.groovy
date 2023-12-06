package nextflow.hello

import java.nio.file.Paths
import java.nio.file.Path

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import nextflow.fusion.FusionAwareTask
import nextflow.processor.TaskRun
import nextflow.processor.TaskHandler
import nextflow.processor.TaskStatus
import nextflow.executor.BashWrapperBuilder
import nextflow.exception.AbortOperationException




@Slf4j
class RescaleTaskHandler extends TaskHandler implements FusionAwareTask {
    private RescaleExecutor executor

    private final Path exitFile

    private final Path wrapperFile

    private final Path outputFile

    private final Path errorFile

    private final Path logFile

    private final Path scriptFile

    private final Path inputFile

    private final Path traceFile

    protected volatile String jobId

    protected String currentStatus

    protected RescaleJob rescaleJobConfig

    protected String RESCALE_CLUSTER_TOKEN
    protected String RESCALE_PLATFORM_URL
    protected String RESCALE_JOB_ID

    protected String getRESCALE_CLUSTER_TOKEN() {
        return this.RESCALE_CLUSTER_TOKEN
    }

    protected String getRESCALE_PLATFORM_URL() {
        return this.RESCALE_PLATFORM_URL
    }

    protected String getRESCALE_JOB_ID() {
        return this.RESCALE_JOB_ID
    }

    protected String setJobId(String jobId) { this.jobId = jobId }

    RescaleTaskHandler(TaskRun task, RescaleExecutor executor) {
        super(task)
        this.executor = executor
        this.rescaleJobConfig = new RescaleJob(task, executor)
        this.logFile = task.workDir.resolve(TaskRun.CMD_LOG)
        this.scriptFile = task.workDir.resolve(TaskRun.CMD_SCRIPT)
        this.inputFile =  task.workDir.resolve(TaskRun.CMD_INFILE)
        this.outputFile = task.workDir.resolve(TaskRun.CMD_OUTFILE)
        this.errorFile = task.workDir.resolve(TaskRun.CMD_ERRFILE)
        this.exitFile = task.workDir.resolve(TaskRun.CMD_EXIT)
        this.wrapperFile = task.workDir.resolve(TaskRun.CMD_RUN)
        this.traceFile = task.workDir.resolve(TaskRun.CMD_TRACE)
    }
    // Initalization required after constractor
    protected RescaleTaskHandler initialize() {
        getConfigEnviromentVariable()
        getEnvironmentVariable()

        return this
    }

    protected void getEnvironmentVariable() {
        List<String> errorMessages = []
        Map<String,String> environment = System.getenv()
        if (!environment.containsKey("RESCALE_JOB_ID")) {
            errorMessages << "unable to find RESCALE_JOB_ID in the environmental variable"
        }
        if (!errorMessages.isEmpty()) {
            throw new AbortOperationException(errorMessages.join("\n"))
        }

        RESCALE_JOB_ID = environment["RESCALE_JOB_ID"]

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

    protected String parseError(Object json, String path = "") {
        String result = ""

        if (json instanceof List) {
            json.each {item -> 
                result += parseError(item, path)
            }
            
        } else if (json instanceof Map) {
            json.each { key, value ->
                if (key == 'projectId') {
                    List projectIds = getProjectId().collect { "${it.id} -> ${it.name}" }

                    String availableIds = "Available project (id -> name): ${projectIds.join(', ')}. Please select the approprate project id."
                    value.add(availableIds)
                }

                if (value instanceof List && value.every { it instanceof String }) {
                    result += "${path}${key} has an error: ${value.join(', ')}\n"

                } else {
                    result += parseError(value, "${path}${key} -> ")
                }
            }
        }

        return result
    }

    protected Map<String,String> createJob() {
        HttpURLConnection connection = this.createConnection('/api/v2/jobs/')
        connection.setRequestMethod('POST')

        // Header
        connection.setRequestProperty('Content-Type', 'application/json')

        connection.doOutput = true
        connection.doInput = true

        // Body
        def bodyJson = rescaleJobConfig.jobConfigurationJson(wrapperFile)

        connection.outputStream.withWriter {
            writer -> writer.write(bodyJson)
        }

        def slurper = new JsonSlurper()

        if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
            def content = slurper.parseText(connection.inputStream.text)

            return content

        } else {
            def errorMessage = "Error: ${connection.getResponseCode()} - ${connection.getResponseMessage()}"
            
            if (connection.errorStream != null) {
                def errorJson = slurper.parse(connection.errorStream)

                errorMessage += "\nError Message: ${parseError(errorJson)}"
            }
    
            throw new AbortOperationException(errorMessage)
        }
    }

    protected void submitJob(String jobId) {
        def connection = this.createConnection("/api/v2/jobs/$jobId/submit/")
        connection.setRequestMethod('POST')

        connection.doInput = true

        if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
            log.trace "[Rescale Executor]: Job $jobId Submitted"
        } else {
            def errorMessage = "Error: ${connection.getResponseCode()} - ${connection.getResponseMessage()}"
            
            if (connection.errorStream != null) {
                def errorJson = new JsonSlurper().parse(connection.errorStream)

                errorMessage += "\nError Message: ${parseError(errorJson)}"
            }
    
            throw new AbortOperationException(errorMessage)
        }
    }

    protected List<Map<String,String>> getStatuses(String jobId) {
        def connection = this.createConnection("/api/v2/jobs/$jobId/statuses/")
        connection.setRequestMethod('GET')

        connection.doInput = true

        def slurper = new JsonSlurper()
        
        if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
            def content = slurper.parseText(connection.inputStream.text)
            
            return content['results']

        } else {
            def errorMessage = "Error: ${connection.getResponseCode()} - ${connection.getResponseMessage()}"
            
            if (connection.errorStream != null) {
                def errorJson = slurper.parse(connection.errorStream)

                errorMessage += "\nError Message: ${parseError(errorJson)}"
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

        def slurper = new JsonSlurper()
        
        if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
            def content = slurper.parseText(connection.inputStream.text)

            return content

        } else {
            def errorMessage = "Error: ${connection.getResponseCode()} - ${connection.getResponseMessage()}"
            
            if (connection.errorStream != null) {
                def errorJson = slurper.parse(connection.errorStream)

                errorMessage += "\nError Message: ${parseError(errorJson)}"
            }
    
            throw new AbortOperationException(errorMessage)
        }
    }

    protected void stopJob(String jobId) {
        HttpURLConnection connection = this.createConnection("/api/v2/jobs/$jobId/stop/")
        connection.setRequestMethod('POST')

        connection.doInput = true

        if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
            log.trace "[Rescale Executor]: Job $jobId Stopped"
        } else {
            def errorMessage = "Error: ${connection.getResponseCode()} - ${connection.getResponseMessage()}"
            
            if (connection.errorStream != null) {
                def errorJson = new JsonSlurper().parse(connection.errorStream)

                errorMessage += "\nError Message: ${parseError(errorJson)}"
            }
    
            throw new AbortOperationException(errorMessage)
        }
    }

    protected List<Map> getProjectId() {
        def connection = this.createConnection("/api/v2/users/me/projects/")
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

    protected Map<String,String> postComment(String childJobId, String jobName, String status) {
        def connection = this.createConnection("/api/v3/jobs/$RESCALE_JOB_ID/comments/")
        connection.setRequestMethod('POST')

        // Header
        connection.setRequestProperty('Content-Type', 'application/json')

        connection.doOutput = true
        connection.doInput = true

        // Body
        String message = "$jobName (Job [$childJobId]($RESCALE_PLATFORM_URL/jobs/$childJobId)) has $status"
        def bodyJson = rescaleJobConfig.commentJson(message)

        connection.outputStream.withWriter {
            writer -> writer.write(bodyJson)
        }

        def slurper = new JsonSlurper()
        
        if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
            def content = slurper.parseText(connection.inputStream.text)

            return content

        } else {
            def errorMessage = "Error: ${connection.getResponseCode()} - ${connection.getResponseMessage()}"
            
            if (connection.errorStream != null) {
                def errorJson = slurper.parse(connection.errorStream)

                errorMessage += "\nError Message: Unable to post a comment to ${RESCALE_JOB_ID}. ${parseError(errorJson)}"
            }
    
            throw new AbortOperationException(errorMessage)
        }
    }

    @Override
    void submit() {

        buildTaskWrapper()
        correctWrapperPath()

        log.info "[Rescale Executor] WorkDir: ${task.workDir.toString()}"

        status = TaskStatus.SUBMITTED

        // Rescale Job 
        def content = createJob()
        
        setJobId(content['id'])

        // Temporary HPS Solution
        attachStorage(jobId)

        submitJob(jobId)
    }

    protected correctWrapperPath() {
        def file = new File(wrapperFile.toUri())
        def content = file.text

        content = content.replaceAll(executor.getBaseDir(), '\\$HOME')

        file.text = content
    }

    protected BashWrapperBuilder createTaskWrapper() {
        return new BashWrapperBuilder(task)
    }

    protected void buildTaskWrapper() {
        createTaskWrapper().build()
    }

    private List<String> RUNNING_AND_COMPLETED = ["Completed", "Executing", "Validated", "Started", "Queued", "Pending", "Waiting for Queue", "Stopping"]
    private final String COMPLETED = "Completed"
    private final String STOPPING = "Stopping"
    private final String RUN_FAILED = "A run failed"
    // For posting a comment (postComment)
    private final String FAILED = "Failed"

    @Override
    boolean checkIfRunning() {
        if(!jobId || !isSubmitted()) {
            return false
        }

        def jobStatus = getStatuses(jobId)[0]["status"]
        def result = jobStatus in RUNNING_AND_COMPLETED

        if (currentStatus != jobStatus) {
            currentStatus = jobStatus
            log.info "[Rescale Executor] Job $jobId is $currentStatus"
        }    

        if (result) {
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

        def jobStatus = getStatuses(jobId)
        def statusList = jobStatus.collect { it["status"] }

        if (currentStatus != statusList[0]) {
            currentStatus = statusList[0]
            log.info "[Rescale Executor] Job $jobId is ${currentStatus}"
        }

        def result = COMPLETED in statusList
        def terminated = STOPPING in statusList
        def statusReason = ""

        // Check before terminated logic for cases when COMPLETE
        // triggered without a STOPPING status
        if (result) {
            statusReason = jobStatus.find { it.status == this.@COMPLETED }.statusReason
            if (statusReason == "Exceeded max number of restarts" || 
                statusReason == "Could not allocate compute resources from the compute provider") {
            // Job stopped with status complete however, not properly executed
                terminated = true;
            }
        }

        if (terminated) {
            task.stdout = outputFile
            task.exitStatus = readExitFile()
            
            def stoppingStatus = jobStatus.find { it.status == this.@STOPPING }
            if (stoppingStatus) {
                statusReason = stoppingStatus.statusReason
            }

            task.error = new AbortOperationException("Error: Job $jobId has stopped. Reason: $statusReason")
            if (statusReason == RUN_FAILED) {
                task.stderr = errorFile
            }

            postComment(jobId, task.name, FAILED)
            
            log.info "Job $jobId is terminated. Reason: $statusReason"

            status = TaskStatus.COMPLETED
            return true
        }
        else if (result) {
            task.stdout = outputFile
            task.exitStatus = 0

            postComment(jobId, task.name, COMPLETED)

            log.info "[Rescale Executor] Job $jobId is Completed"

            status = TaskStatus.COMPLETED
            return true
        }

        return false
    }

    protected int readExitFile() {
        try {
            exitFile.text as Integer
        }
        catch( Exception e ) {
            log.debug "[Rescale Executor] Cannot read exitstatus for task: `$task.name` | ${e.message}"
            return Integer.MAX_VALUE
        }
    }

    @Override
    void kill() {
        assert jobId
        log.trace "[Rescale Executor] Killing Job $jobId"
        stopJob(jobId)
    }

}