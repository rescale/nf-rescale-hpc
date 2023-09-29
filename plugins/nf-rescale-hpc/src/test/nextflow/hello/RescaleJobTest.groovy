package nextflow.hello

import java.net.URL
import java.net.HttpURLConnection

import nextflow.processor.TaskRun
import nextflow.processor.TaskConfig
import nextflow.processor.TaskStatus
import nextflow.exception.AbortOperationException

import spock.lang.Specification

class RescaleJobTest extends Specification {

    def 'should return proper rescale job json when jobConfigurationJson called'() {
        given: "a RescaleJob"

        // Spy on class
        def taskConfig = new TaskConfig()
        taskConfig.ext = ["analysisCode":"testCode", "analysisVersion":"testVersion"]
        taskConfig.cpus = 123
        taskConfig.machineType = "testMachine"

        def task = Mock(TaskRun) {
            name >> "test123"
            script >> "echo Hello World"
            config >> taskConfig
        }
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task])
        

        when: 'jobConfigurationJson is called'
        def value = handlerSpy.jobConfigurationJson()


        then: 'return json with correct config'
        value == '''
        {
            "name": "test123",
            "jobanalyses": [
                {
                    "analysis": {
                        "code": "testCode",
                        "version": "testVersion"
                    },
                    "useRescaleLicense": true,
                    "command": "echo Hello World",
                    "hardware": {
                        "coreType": "testMachine",
                        "coresPerSlot": 123
                    }
                }
            ]
        }
        '''
    }

    def 'should throw an error if improper rescale job json when jobConfigurationJson called'() {
        given: "a RescaleJob"

        // Spy on class
        def taskConfig = new TaskConfig()

        def task = Mock(TaskRun) {
            name >> "test123"
            script >> "echo Hello World"
            config >> taskConfig
        }
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task])
        

        when: 'jobConfigurationJson is called'
        def value = handlerSpy.jobConfigurationJson()


        then: 'return json with correct config'
        thrown(AbortOperationException)
    }

    def 'should return proper json for storage configuration when storageConfigurationJson called'() {
        given: "a RescaleJob"
        def taskConfig = new TaskConfig()
        taskConfig.ext = ["storageId":"test123"]

        def task = Mock(TaskRun) {
            config >> taskConfig
        }
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task])
        

        when: 'storageConfigurationJson is called'
        def value = handlerSpy.storageConfigurationJson()


        then: 'return json with correct config'
        value == '''
        {
            "storageDevice": { "id": "test123" }
        }
        '''
    }

    def 'should throw an error if improper storage configuration json when storageConfigurationJson called'() {
        given: "a RescaleJob"

        // Spy on class
        def taskConfig = new TaskConfig()

        def task = Mock(TaskRun) {
            name >> "test123"
            script >> "echo Hello World"
            config >> taskConfig
        }
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task])
        

        when: 'storageConfigurationJson is called'
        def value = handlerSpy.storageConfigurationJson()


        then: 'return json with correct config'
        thrown(AbortOperationException)

    }
}