package nextflow.hello

import java.net.URL
import java.net.HttpURLConnection
import java.nio.file.Paths

import nextflow.processor.TaskRun
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
        def executor = Mock(RescaleExecutor) {
            getOutputDir() >> Paths.get('/test/dir')
        }
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task, executor]) {
            commandString(_) >> '"test command"'
            envVarsJson() >> "{'test':'var'}"
            onDemandLicenseSeller() >> "{code:'onDemand'}"
            hardwareConfig() >> """{"core Type":"testMachine","coresPerSlot":123}"""
        }
        

        when: 'jobConfigurationJson is called'
        def wrapper = Paths.get('/test/file')
        def value = handlerSpy.jobConfigurationJson(wrapper)


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
                    "envVars": {'test':'var'},
                    "command": "test command",
                    "hardware": {"core Type":"testMachine","coresPerSlot":123},
                    "onDemandLicenseSeller": {code:'onDemand'}
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
        def executor = Mock(RescaleExecutor)
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task, executor])
        

        when: 'jobConfigurationJson is called'
        def value = handlerSpy.jobConfigurationJson()


        then: 'return json with correct config'
        thrown(AbortOperationException)
    }

    def 'should return proper json for storage configuration when storageConfigurationJson called'() {
        given: "a RescaleJob"

        def task = Mock(TaskRun)
        def executor = Mock(RescaleExecutor) {
            getStorageId() >> 'storage123'
        }
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task, executor])

        when: 'storageConfigurationJson is called'
        def value = handlerSpy.storageConfigurationJson()


        then: 'return json with correct config'
        value == '''
        {
            "storageDevice": { "id": "storage123" }
        }
        '''
    }
    
    def 'should return a structured commandString of a task when commandString is called'() {
        given: "a RescaleJob"

        def task = Mock(TaskRun)
        def executor = Mock(RescaleExecutor)
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task, executor]) {
            2 * getCustomAbsolutePath(_) >> Paths.get('$HOME/test') >> Paths.get('$HOME/test/file')
        }
        

        when: 'commandString is called'
        def wrapper = Paths.get('/test/file')
        def value = handlerSpy.commandString(wrapper)


        then: 'return string with correct structure'
        value == '"cd $HOME/test\\nchmod +x $HOME/test/file\\n$HOME/test/file"'
    }

    def 'should return a environment variables in json format when envVarsJson is called'() {
        given: "a RescaleJob"

        def task = Mock(TaskRun) {
            getEnvironment() >> ["TEST_ENV": "123", "TEST_ENV_1": "456"]
        }
        def executor = Mock(RescaleExecutor)
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task, executor]) 
        

        when: 'envVarsJson is called'
        def value = handlerSpy.envVarsJson()


        then: 'return json with correct environment'
        value == '{"TEST_ENV":"123","TEST_ENV_1":"456"}'
    }

    def 'should return a empty environment variables in json format if nextflow env is empty'() {
        given: "a RescaleJob"

        def task = Mock(TaskRun)
        def executor = Mock(RescaleExecutor)
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task, executor]) 


        when: 'envVarsJson is called'
        def value = handlerSpy.envVarsJson()


        then: 'return json with correct environment'
        value == '{}'
    }

    def 'should get a custom absolute path path if customAbsolutePath is called' () {
        given: "a RescaleJob"

        def task = Mock(TaskRun)
        def executor = Mock(RescaleExecutor) {
            getBaseDir() >> "/home/dir"
        }
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task, executor])


        when: 'getCustomAbsolutePath is called with a home path'
        def value = handlerSpy.getCustomAbsolutePath(Paths.get('/home/dir/test'))


        then: 'return the custom absolute path'
        value == Paths.get('$HOME/test')
    }

    def 'should get a custom absolute path path if customAbsolutePath is called' () {
        given: "a RescaleJob"

        def task = Mock(TaskRun)
        def executor = Mock(RescaleExecutor) {
            getBaseDir() >> "/home/dir"
        }
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task, executor])


        when: 'getCustomAbsolutePath is called with a no home path'
        def value = handlerSpy.getCustomAbsolutePath(Paths.get('/notHome/dir/test'))


        then: 'return the normal absolute path'
        value == Paths.get('/notHome/dir/test')
    }

    def 'should return a hardware configuraiton json with walltime'() {
        given: "a RescaleJob"

        // Spy on class
        def taskConfig = new TaskConfig()
        taskConfig.ext = []
        taskConfig.cpus = 123
        taskConfig.machineType = "testMachine"

        def task = Mock(TaskRun) {
            config >> taskConfig
        }
        def executor = Mock(RescaleExecutor) {
            getOutputDir() >> Paths.get('/test/dir')
        }
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task, executor])
        

        when: 'hardwareConfig is called without specified walltime'
        def value = handlerSpy.hardwareConfig()


        then: 'return json with correct config'
        value == '''{"coreType":"testMachine","coresPerSlot":123}'''
    }

    def 'should return a hardware configuraiton json with walltime'() {
        given: "a RescaleJob"

        // Spy on class
        def taskConfig = new TaskConfig()
        taskConfig.ext = ["wallTime":5]
        taskConfig.cpus = 123
        taskConfig.machineType = "testMachine"

        def task = Mock(TaskRun) {
            config >> taskConfig
        }
        def executor = Mock(RescaleExecutor) {
            getOutputDir() >> Paths.get('/test/dir')
        }
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task, executor])
        

        when: 'hardwareConfig is called without specified walltime'
        def value = handlerSpy.hardwareConfig()


        then: 'return json with correct config'
        value == '''{"coreType":"testMachine","coresPerSlot":123,"walltime":5}'''
    }

    def 'should throw error if machineType is not specified'() {
        given: "a RescaleJob"

        // Spy on class
        def taskConfig = new TaskConfig()
        taskConfig.cpus = 123

        def task = Mock(TaskRun) {
            config >> taskConfig
        }
        def executor = Mock(RescaleExecutor) {
            getOutputDir() >> Paths.get('/test/dir')
        }
        def handlerSpy = Spy(RescaleJob, constructorArgs: [task, executor])
        

        when: 'hardwareConfig is called'
        def value = handlerSpy.hardwareConfig()


        then: 'return json with correct config'
        thrown(AbortOperationException)
    }
}