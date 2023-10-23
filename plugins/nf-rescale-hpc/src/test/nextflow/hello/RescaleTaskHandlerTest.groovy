package nextflow.hello

import java.net.URL
import java.net.HttpURLConnection
import java.nio.file.Paths

import nextflow.processor.TaskRun
import nextflow.processor.TaskStatus
import nextflow.executor.BashWrapperBuilder

import spock.lang.Specification

class RescaleTaskHandlerTest extends Specification {

    def 'should assign environmental variables' () {
        given: 'a RescaleTaskHandler'
        def executor = Mock(RescaleExecutor)
        def task = Mock(TaskRun) {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
        }

        when: 'getConfigEnviromentVariable is called'
        handlerSpy.getConfigEnviromentVariable()

        then: 'environmental variable is set'
        handlerSpy.RESCALE_PLATFORM_URL == "https://example.com"
        handlerSpy.RESCALE_CLUSTER_TOKEN == "test_token"
    }
    
    def 'should create an example rescale job' () {
        given: 'a RescaleTaskHandler'
        // Mock HttpURLConnection
        def inputStream = new ByteArrayInputStream('{"id":"job123"}'.getBytes())
        def outputStream = new ByteArrayOutputStream()
        def httpURLConnection = Mock(HttpURLConnection) {
            getInputStream() >> inputStream
            getOutputStream() >> outputStream
            getResponseCode() >> 200
        }

        def jobConfig = Mock(RescaleJob) {
            jobConfigurationJson(_) >> "{}"
        }

        // Spy on class
        def executor = Mock(RescaleExecutor)
        def task = Mock(TaskRun) {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            createConnection(_) >> httpURLConnection
        }
        handlerSpy.metaClass.setProperty(handlerSpy, 'rescaleJobConfig', jobConfig)


        when: 'createJob is called'
        def content = handlerSpy.createJob()

        then: 'return a job details' 
            content == ["id":"job123"]

    }

    def 'createJob should throw an exception' () {
        given: 'a RescaleTaskHandler'
        // Mock HttpURLConnection
        def outputStream = new ByteArrayOutputStream()
        def httpURLConnection = Mock(HttpURLConnection) {
            getOutputStream() >> outputStream
            getResponseMessage() >> "Bad Request"
            getResponseCode() >> 400
        }

        def jobConfig = Mock(RescaleJob) {
            jobConfigurationJson(_) >> "{}" 
        }
            
        // Spy on class
        def executor = Mock(RescaleExecutor)
        def task = Mock(TaskRun)  {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            createConnection(_) >> httpURLConnection
        }
        handlerSpy.metaClass.setProperty(handlerSpy, 'rescaleJobConfig', jobConfig)


        when: 'createJob is called incorrectly'
        def content = handlerSpy.createJob()

        then: 'throw an exception' 
            Exception e  = thrown()
            e.message == "Error: 400 - Bad Request"

    }

    def 'should start a rescale job' () {
        given: 'a RescaleTaskHandler'
        // Mock HttpURLConnection
        def inputStream = new ByteArrayInputStream(''.getBytes())
        def httpURLConnection = Mock(HttpURLConnection) {
            getInputStream() >> inputStream
            getResponseCode() >> 200
        }
            
        // Spy on class
        def executor = Mock(RescaleExecutor)
        def task = Mock(TaskRun)  {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            createConnection(_) >> httpURLConnection
        }


        when: 'submit is called'
        handlerSpy.submitJob("123")

        then: 'no errors were thrown'
        // If error is thrown the unit test will fail
    }

    def 'submitJob should throw an exception' () {
        given: 'a RescaleTaskHandler'
        // Mock HttpURLConnection
        def httpURLConnection = Mock(HttpURLConnection) {
            getResponseMessage() >> "Bad Request"
            getResponseCode() >> 400
        }
            
        // Spy on class
        def executor = Mock(RescaleExecutor)
        def task = Mock(TaskRun) {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            createConnection(_) >> httpURLConnection
        }


        when: 'submit is called'
        handlerSpy.submitJob()

        then: 'throw an exception' 
            Exception e  = thrown()
            e.message == "Error: 400 - Bad Request"
    }

     def 'should get status of an example rescale job' () {
        given: 'a RescaleTaskHandler'
        // Mock HttpURLConnection
        def inputStream = new ByteArrayInputStream('{"results":[{"status":"Completed"}]}'.getBytes())
        def httpURLConnection = Mock(HttpURLConnection) {
            getInputStream() >> inputStream
            getResponseCode() >> 200
        }
            
        // Spy on class
        def executor = Mock(RescaleExecutor)
        def task = Mock(TaskRun)  {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            createConnection(_) >> httpURLConnection
        }


        when: 'getStatuses is called'
        def content = handlerSpy.getStatuses("123")

        then: 'return a job details' 
            content == [["status":"Completed"]]

    }

    def 'getStatuses should throw an exception' () {
        given: 'a RescaleTaskHandler'
        def executor = Mock(RescaleExecutor)
        // Mock HttpURLConnection
        def httpURLConnection = Mock(HttpURLConnection) {
            getResponseMessage() >> "Bad Request"
            getResponseCode() >> 400
        }
            
        // Spy on class
        def task = Mock(TaskRun) {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            createConnection(_) >> httpURLConnection
        }


        when: 'getStatuses is called'
        handlerSpy.getStatuses("123")

        then: 'throw an exception' 
            Exception e  = thrown()
            e.message == "Error: 400 - Bad Request"
    }

    def 'should return true and status RUNNING when job has status' () {
        given: 'a RescaleTaskHandler'
        // Spy on class
        def executor = Mock(RescaleExecutor)
        def task = Mock(TaskRun) {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            getStatuses(_) >> [["status":"Pending"]]
            isSubmitted() >> true
        }
        handlerSpy.setJobId("123")
        

        when: 'checkIfRunning is called'
        def value = handlerSpy.checkIfRunning()


        then: 'return true and set status to running'
        value == true
        handlerSpy.status == TaskStatus.RUNNING
    }

    def 'should return false when the job is not submitted' () {
        given: 'a RescaleTaskHandler'
        // Spy on class
        def executor = Mock(RescaleExecutor)
        def task = Mock(TaskRun) {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            getStatuses(_) >> []
            isSubmitted() >> false
        }
        handlerSpy.setJobId("123")
        

        when: 'checkIfRunning is called'
        def value = handlerSpy.checkIfRunning()


        then: 'return false'
        value == false
        handlerSpy.status != TaskStatus.RUNNING
    }

    def 'should return false when the jobId is null' () {
        given: 'a RescaleTaskHandler'
        def executor = Mock(RescaleExecutor)
        // Spy on class
        def task = Mock(TaskRun)  {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            getStatuses(_) >> []
            isSubmitted() >> true
        }
        // jobId is initialized as null
        

        when: 'checkIfRunning is called'
        def value = handlerSpy.checkIfRunning()


        then: 'return true and set status to running'
        value == false
        handlerSpy.status != TaskStatus.RUNNING
    }

    def 'should return true and status COMPLETED' () {
        given: 'a RescaleTaskHandler'
        // Spy on class
        def executor = Mock(RescaleExecutor)
        def task = Mock(TaskRun) {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            getStatuses(_) >> [["status":"Completed"]]
            isRunning() >> true
        }
        handlerSpy.setJobId("123")
        

        when: 'checkIfCompleted is called with a status Completed'
        def value = handlerSpy.checkIfCompleted()


        then: 'return true and set status to COMPLETED'
        value == true
        handlerSpy.status == TaskStatus.COMPLETED
    }

    def 'should throw an error when job is mannual stopped' () {
        given: 'a RescaleTaskHandler'
        // Spy on class
        def executor = Mock(RescaleExecutor)
        def task = Mock(TaskRun) {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            getStatuses(_) >> [["status":"Stopping"]]
            isRunning() >> true
        }
        handlerSpy.setJobId("123")
        

        when: 'checkIfCompleted is called with a status Stopping'
        def value = handlerSpy.checkIfCompleted()


        then: 'throw an exception'
        Exception e  = thrown()
        e.message == "Error: Job 123 has stopped"
    }

    def 'should return false when a job is not running' () {
        given: 'a RescaleTaskHandler'
        // Spy on class
        def executor = Mock(RescaleExecutor)
        def task = Mock(TaskRun) {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            isRunning() >> false
        }
        handlerSpy.setJobId("123")
        

        when: 'checkIfCompleted is called'
        def value = handlerSpy.checkIfCompleted()


        then: 'return false'
        value == false
        handlerSpy.status != TaskStatus.COMPLETED
    }

    def 'should throw an error when jobId is null' () {
        given: 'a RescaleTaskHandler'
        // Spy on class
        def executor = Mock(RescaleExecutor)
        def task = Mock(TaskRun) {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            isRunning() >> true
        }
        

        when: 'checkIfCompleted is called with a status Stopping'
        def value = handlerSpy.checkIfCompleted()


        then: 'throw an exception'
        thrown(AssertionError)
    }

    def 'should attach storage to job when attachStorage is called' () {
         given: 'a RescaleTaskHandler'
        def executor = Mock(RescaleExecutor)
        // Mock HttpURLConnection
        def inputStream = new ByteArrayInputStream('{"id":"storage123"}'.getBytes())
        def outputStream = new ByteArrayOutputStream()
        def httpURLConnection = Mock(HttpURLConnection) {
            getInputStream() >> inputStream
            getOutputStream() >> outputStream
            getResponseCode() >> 200
        }

        def jobConfig = Mock(RescaleJob) {
            storageConfigurationJson() >> "{}"
        }

        // Spy on class
        def task = Mock(TaskRun) {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            createConnection(_) >> httpURLConnection
        }
        handlerSpy.metaClass.setProperty(handlerSpy, 'rescaleJobConfig', jobConfig)


        when: 'attachStorage is called'
        def content = handlerSpy.attachStorage()

        then: 'return a storage details' 
            content == ["id":"storage123"]
    }

    def 'attachStorage should throw an exception' () {
        given: 'a RescaleTaskHandler'
        // Mock HttpURLConnection
        def outputStream = new ByteArrayOutputStream()
        def httpURLConnection = Mock(HttpURLConnection) {
            getOutputStream() >> outputStream
            getResponseMessage() >> "Bad Request"
            getResponseCode() >> 400
        }

        def jobConfig = Mock(RescaleJob) {
            storageConfigurationJson() >> "{}" 
        }
            
        // Spy on class
        def executor = Mock(RescaleExecutor)
        def task = Mock(TaskRun)  {
            workDir >> Paths.get('/work/dir')
            getEnvironment() >> ["RESCALE_CLUSTER_TOKEN":"test_token", "RESCALE_PLATFORM_URL":"https://example.com"]
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            createConnection(_) >> httpURLConnection
        }
        handlerSpy.metaClass.setProperty(handlerSpy, 'rescaleJobConfig', jobConfig)


        when: 'attachStorage is called incorrectly'
        def content = handlerSpy.attachStorage()

        then: 'throw an exception' 
            Exception e  = thrown()
            e.message == "Error: 400 - Bad Request"

    }

    def 'should initialize RescaleTaskHandler' () {
        given: 'a RescaleTaskHandler'
        def executor = Mock(RescaleExecutor)
        def task = Mock(TaskRun) {
            workDir >> Paths.get('/work/dir')
        }
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            getConfigEnviromentVariable() >> null
        }

        when: 'initialize is called'
        def value = handlerSpy.initialize()

        then: 'return RescaleTaskHandler and calls functions'
        value == handlerSpy
    }
}