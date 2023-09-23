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

    // private HttpURLConnection rescaleConnection


    RescaleTaskHandler(TaskRun task, RescaleExecutor executor) {
        super(task)
        this.executor = executor
        // this.rescaleConnection = executor.getRescaleConnection()
    }

    private Map<String,String> createJob() {
        HttpURLConnection connection = new URL("$executor.RESCALE_PLATFORM_URL/api/v2/jobs/").openConnection() as HttpURLConnection
        connection.setRequestMethod('POST')

        // Header
        connection.setRequestProperty('Authorization', "Token $executor.RESCALE_CLUSTER_TOKEN")
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
                    "command": "echo 1",
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

        if (connection.getResponseCode() > 200 && connection.getResponseCode() < 400) {
            def slurper = new JsonSlurper()
            def content = slurper.parseText(connection.inputStream.text)
            println "Content: $content"

            return content

        } else {
            // Chance of being empty?
            def errorMessage = connection.errorStream.text

            throw new Exception("Error: ${connection.getResponseCode()} - ${connection.getResponseMessage()}\nError Message: $errorMessage")
        }
    }

    private void submitJob(String jobId) {
        def connection = new URL("$executor.RESCALE_PLATFORM_URL/api/v2/jobs/$jobId/submit/").openConnection() as HttpURLConnection
        connection.setRequestProperty('Authorization', "Token $executor.RESCALE_CLUSTER_TOKEN")
        connection.setRequestMethod('POST')

        connection.doInput = true

        if (connection.getResponseCode() > 200 && connection.getResponseCode() < 400) {
            // No response so this point it threw an error
            def submitContent = connection.inputStream.text
            println "Submit Content: $submitContent"
        } else {
             // Chance of being empty?
            def errorMessage = connection.errorStream.text

            throw new Exception("Error: ${connection.getResponseCode()} - ${connection.getResponseMessage()}\nError Message: $errorMessage")
        }
    }

    // void callRescale() {
    //     HttpURLConnection connection = new URL("$executor.RESCALE_PLATFORM_URL/api/v2/jobs/").openConnection() as HttpURLConnection
    //     connection.setRequestMethod('POST')

    //     // Header
    //     connection.setRequestProperty('Authorization', "Token $executor.RESCALE_CLUSTER_TOKEN")
    //     connection.setRequestProperty('Content-Type', 'application/json')

    //     connection.doOutput = true
    //     connection.doInput = true

    //     // Body
    //     def bodyJson = '''
    //     {
    //         "name": "Example Job",
    //         "jobanalyses": [
    //             {
    //                 "analysis": {
    //                     "code": "user_included",
    //                     "version": "0"
    //                 },
    //                 "command": "echo 1",
    //                 "hardware": {
    //                     "coreType": "emerald",
    //                     "coresPerSlot": 1
    //                 }
    //             }
    //         ]
    //     }
    //     '''

    //     connection.outputStream.withWriter {
    //         writer -> writer.write(bodyJson)
    //     }

    //     if (connection.getResponseCode() > 200 && connection.getResponseCode() < 400) {
    //         def slurper = new JsonSlurper()
    //         def content = slurper.parseText(connection.inputStream.text)
    //         println "Content: $content"

    //         def jobId = content['id']

    //         def submitUrl = new URL("https://platform-dev.rescale.com/api/v2/jobs/$jobId/submit/")

    //         def submitConnection = submitUrl.openConnection() as HttpURLConnection
    //         submitConnection.setRequestMethod('POST')

    //         submitConnection.setRequestProperty('Authorization', 'Token dc07ae15c5c7ad04639262786f8b4ed8a74d0f36')

    //         submitConnection.doInput = true

    //         if (connection.getResponseCode() > 200 && connection.getResponseCode() < 400) {
    //             // No response so this point it threw an error
    //             def submitContent = submitConnection.inputStream.text
    //             println "Submit Content: $submitContent"
    //         } else {
    //             println "Error: ${connection.getResponseCode()} - ${connection.getResponseMessage()}"

    //             // Read the error stream and print it
    //             def errorStream = connection.errorStream
    //             if (errorStream != null) {
    //                 def errorMessage = errorStream.text
    //                 println "Error Message: $errorMessage"
    //             }
    //         }

    //     } else {
    //         println "Error: ${connection.getResponseCode()} - ${connection.getResponseMessage()}"

    //         // Read the error stream and print it
    //         def errorStream = connection.errorStream
    //         if (errorStream != null) {
    //             def errorMessage = errorStream.text
    //             println "Error Message: $errorMessage"
    //         }
    //     }
    // }

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