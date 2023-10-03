package nextflow.hello

import java.nio.file.Paths
import java.net.URL
import java.net.HttpURLConnection
import groovy.json.JsonSlurper

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.executor.Executor
import nextflow.executor.GridTaskHandler
import nextflow.processor.TaskHandler
import nextflow.processor.TaskMonitor
import nextflow.processor.TaskPollingMonitor
import nextflow.processor.TaskRun
import nextflow.processor.TaskStatus
import nextflow.util.Duration
import nextflow.util.ServiceName
import org.pf4j.ExtensionPoint

@Slf4j
@ServiceName(value='rescale-executor')
@CompileStatic
class RescaleExecutor extends Executor implements ExtensionPoint {

    @Override
    protected void register() {
        super.register()
    }

    @Override
    protected TaskMonitor createTaskMonitor() {
        return TaskPollingMonitor.create(session, name, 1000, Duration.of('10 sec'))
    }

    @Override
    TaskHandler createTaskHandler(TaskRun task) {
        assert task
        assert task.workDir
        log.trace "[Rescale Executor] Launching process > ${task.name} -- work folder: ${task.workDirStr}"
        return new RescaleTaskHandler(task, this)
    }
}

@Slf4j
class NopeTaskHandler extends TaskHandler {

    protected NopeTaskHandler(TaskRun task) {
        super(task)
    }

    @Override
    void submit() {
        log.info ">> launching nope process: ${task}"
        task.workDir = Paths.get('.').complete()
        status = TaskStatus.SUBMITTED
        task.stdout = task.script
        task.exitStatus = 0
    }

    @Override
    boolean checkIfRunning() {
        log.debug "isRunning: $status"
        if( isSubmitted() ) {
            status = TaskStatus.RUNNING
            return true
        }
        return false
    }

    @Override
    boolean checkIfCompleted() {
        log.debug "isTerminated: $status"
        if( isRunning() ) {
            status = TaskStatus.COMPLETED
            return true
        }
        false
    }

    @Override
    void kill() { }

}
