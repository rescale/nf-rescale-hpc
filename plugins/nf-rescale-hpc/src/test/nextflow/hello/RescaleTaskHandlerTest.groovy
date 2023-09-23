package nextflow.hello

import java.net.URL
import java.net.HttpURLConnection

import nextflow.processor.TaskRun

import spock.lang.Specification

class RescaleTaskHandlerTest extends Specification {
    
    def 'should create an example rescale job' () {
        given: 'a RescaleTaskHandler'
        def executor = Mock(RescaleExecutor) {
            getRESCALE_PLATFORM_URL() >> "https://example.com"
            getRESCALE_CLUSTER_TOKEN() >> "test_token"
        }
        
        // Mock HttpURLConnection
        def inputStream = new ByteArrayInputStream('{"id":"job123"}'.getBytes())
        def outputStream = new ByteArrayOutputStream()
        def httpURLConnection = Mock(HttpURLConnection) {
            getInputStream() >> inputStream
            getOutputStream() >> outputStream
            getResponseCode() >> 200
        }
            
        // Spy on class
        def task = Mock(TaskRun)
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            createConnection(_) >> httpURLConnection
        }


        when: 'createJob is called'
        def content = handlerSpy.createJob()

        then: 'return a job details' 
            content == ["id":"job123"]

    }

    def 'createJob should throw an exception' () {
        given: 'a RescaleTaskHandler'
        def executor = Mock(RescaleExecutor) {
            getRESCALE_PLATFORM_URL() >> "https://example.com"
            getRESCALE_CLUSTER_TOKEN() >> "test_token"
        }
        
        // Mock HttpURLConnection
        def outputStream = new ByteArrayOutputStream()
        def httpURLConnection = Mock(HttpURLConnection) {
            getOutputStream() >> outputStream
            getResponseMessage() >> "Bad Request"
            getResponseCode() >> 400
        }
            
        // Spy on class
        def task = Mock(TaskRun)
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            createConnection(_) >> httpURLConnection
        }


        when: 'createJob is called incorrectly'
        def content = handlerSpy.createJob()

        then: 'throw an exception' 
            Exception e  = thrown()
            e.message == "Error: 400 - Bad Request"

    }

    def 'should start a rescale job' () {
        given: 'a RescaleTaskHandler'
        def executor = Mock(RescaleExecutor) {
            getRESCALE_PLATFORM_URL() >> "https://example.com"
            getRESCALE_CLUSTER_TOKEN() >> "test_token"
        }
        
        // Mock HttpURLConnection
        def inputStream = new ByteArrayInputStream(''.getBytes())
        def httpURLConnection = Mock(HttpURLConnection) {
            getInputStream() >> inputStream
            getResponseCode() >> 200
        }
            
        // Spy on class
        def task = Mock(TaskRun)
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            createConnection(_) >> httpURLConnection
        }


        when: 'submit is called'
        handlerSpy.submitJob()

        then: 'no errors were thrown'
        // If error is thrown the unit test will fail
    }

    def 'submitJob should throw an exception' () {
        given: 'a RescaleTaskHandler'
        def executor = Mock(RescaleExecutor) {
            getRESCALE_PLATFORM_URL() >> "https://example.com"
            getRESCALE_CLUSTER_TOKEN() >> "test_token"
        }
        
        // Mock HttpURLConnection
        def httpURLConnection = Mock(HttpURLConnection) {
            getResponseMessage() >> "Bad Request"
            getResponseCode() >> 400
        }
            
        // Spy on class
        def task = Mock(TaskRun)
        def handlerSpy = Spy(RescaleTaskHandler, constructorArgs: [task, executor]) {
            createConnection(_) >> httpURLConnection
        }


        when: 'submit is called'
        handlerSpy.submitJob()

        then: 'throw an exception' 
            Exception e  = thrown()
            e.message == "Error: 400 - Bad Request"
    }
}