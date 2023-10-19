package nextflow.hello

import java.nio.file.Paths

import nextflow.exception.AbortOperationException

import spock.lang.Specification

class RescaleExecutorTest extends Specification {
    def 'should get the path of output directory' () {
        given: 'a RescaleExecutor'
        def handlerSpy = Spy(RescaleExecutor)

        when: 'a getOutputDir is called'
        def value = handlerSpy.getOutputDir()

        then: 'return a path to subDir'
        value == Paths.get('.')
    }
}