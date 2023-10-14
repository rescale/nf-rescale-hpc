package nextflow.hello

import java.nio.file.Paths

import nextflow.exception.AbortOperationException

import spock.lang.Specification

class RescaleExecutorTest extends Specification {
    def 'should get the path of sub directory' () {
        given: 'a RescaleExecutor'
        def handlerSpy = Spy(RescaleExecutor)
        handlerSpy.setStorageId('123')

        def baseDir = RescaleExecutor.getDeclaredField('baseDir')
        baseDir.accessible = true
        baseDir.set(handlerSpy, "/test/dir")


        when: 'a getSubDir is called'
        def value = handlerSpy.getSubDir()

        then: 'return a path to subDir'
        value == Paths.get('/test/dir/storage_123/nextflow')
    }

    def 'should get the path of work directory' () {
        given: 'a RescaleExecutor'
        def handlerSpy = Spy(RescaleExecutor) {
            getSubDir() >> Paths.get('/test/subdir')
            ensureDirExists(_) >> null
        }

        def RESCALE_JOB_ID = RescaleExecutor.getDeclaredField('RESCALE_JOB_ID')
        RESCALE_JOB_ID.accessible = true
        RESCALE_JOB_ID.set(handlerSpy, "job123")


        when: 'a getWorkDir is called'
        def value = handlerSpy.getWorkDir()

        then: 'return a path to subDir'
        value == Paths.get('/test/subdir/job123/work')
    }

    def 'should get the path of output directory' () {
        given: 'a RescaleExecutor'
        def handlerSpy = Spy(RescaleExecutor) {
            getSubDir() >> Paths.get('/test/subdir')
            ensureDirExists(_) >> null
        }

        def RESCALE_JOB_ID = RescaleExecutor.getDeclaredField('RESCALE_JOB_ID')
        RESCALE_JOB_ID.accessible = true
        RESCALE_JOB_ID.set(handlerSpy, "job123")


        when: 'a getOutputDir is called'
        def value = handlerSpy.getOutputDir()

        then: 'return a path to subDir'
        value == Paths.get('/test/subdir/job123')
    }
}