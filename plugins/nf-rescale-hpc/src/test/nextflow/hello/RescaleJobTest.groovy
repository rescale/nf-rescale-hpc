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
        taskConfig.ext = ["analysisCode":"testCode", "analysisVersion":"testVersion", "rescaleLicense":"true"]
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
                    "envVars": {},
                    "command": "cd ~/storage*/projectdata\\necho Hello World",
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

        def task = Mock(TaskRun)
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task]) {
            findStorageId() >> "test123"
        }
        

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
        def task = Mock(TaskRun)
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task]) {
            findStorageId() >> null
        }
        

        when: 'storageConfigurationJson is called'
        def value = handlerSpy.storageConfigurationJson()


        then: 'return json with correct config'
        thrown(AbortOperationException)

    }
    
    def 'should return a structured commandString of a task when commandString is called'() {
        given: "a RescaleJob"

        def task = Mock(TaskRun) {
            script >> """
            export TEST=1
            echo \$TEST 
            """
        }  
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task]) 
        

        when: 'commandString is called'
        def value = handlerSpy.commandString()


        then: 'return string with correct structure'
        value == '"cd ~/storage*/projectdata\\nexport TEST=1\\necho \$TEST"'
    }

    def 'should return a environment variables in json format when envVarsJson is called'() {
        given: "a RescaleJob"

        def task = Mock(TaskRun) {
            getEnvironment() >> ["TEST_ENV": "123", "TEST_ENV_1": "456"]
        }  
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task]) 
        

        when: 'envVarsJson is called'
        def value = handlerSpy.envVarsJson()


        then: 'return json with correct environment'
        value == '{"TEST_ENV":"123","TEST_ENV_1":"456"}'
    }

    def 'should return a empty environment variables in json format if nextflow env is empty'() {
        given: "a RescaleJob"

        def task = Mock(TaskRun)
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task]) 
        

        when: 'envVarsJson is called'
        def value = handlerSpy.envVarsJson()


        then: 'return json with correct environment'
        value == '{}'
    }
}