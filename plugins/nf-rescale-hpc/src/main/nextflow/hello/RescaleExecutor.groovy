package nextflow.hello

import java.nio.file.Paths
import java.nio.file.Path
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
import nextflow.exception.AbortOperationException
import org.pf4j.ExtensionPoint

@Slf4j
@ServiceName(value='rescale-executor')
@CompileStatic
class RescaleExecutor extends Executor implements ExtensionPoint {

    protected String storageId
    protected String RESCALE_JOB_ID
    protected String baseDir = System.getProperty("user.home")
    protected String PROJECT_DATA = "nextflow"

    protected String getStorageId() { return storageId }
    protected String getRESCALE_JOB_ID() { return RESCALE_JOB_ID }

    protected void setStorageId(String storageId) {
        this.storageId = storageId
    }

    @Override
    protected void register() {
        super.register()
        findStorageId()
        getSystemVariable()
    }

    @Override
    protected TaskMonitor createTaskMonitor() {
        return TaskPollingMonitor.create(session, name, 1000, Duration.of('10 sec'))
    }

    private void getSystemVariable() {
        Map<String,String> environment = System.getenv()
        if (!environment.containsKey('RESCALE_JOB_ID')) {
            throw new AbortOperationException("RESCALE_JOB_ID environment variable not found.")
        }

        RESCALE_JOB_ID = environment['RESCALE_JOB_ID']
    }

    void findStorageId() {
        def dir = new File(baseDir)
        def filePattern = ~/storage_.*/

        def storageDir = dir.listFiles().find { File file -> 
            file.isDirectory() && file.name ==~ filePattern
        }

        if (storageDir) {
            String[] parts = storageDir.name.split("_")
            log.info "Directory Found: ${parts}"
            
            setStorageId(parts[1])

        } else {
            throw new AbortOperationException("Can't find storageId")
        }

    }

    protected void ensureDirExists(Path path) {
        def dir = path.toFile()

        if (!dir.exists()) {
            dir.mkdir()
        }
    }

    protected Path getSubDir() {
        def path = Paths.get(baseDir, "storage_$storageId").resolve(PROJECT_DATA)
        ensureDirExists(path)

        return path
    }

    @Override
    Path getWorkDir() {
        def path = getSubDir().resolve(RESCALE_JOB_ID).resolve('work')
        ensureDirExists(path)


        return path
    }

    Path getOutputDir() {
        def path = getSubDir().resolve(RESCALE_JOB_ID)
        ensureDirExists(path)


        return path
    }

    @Override
    TaskHandler createTaskHandler(TaskRun task) {
        assert task
        assert task.workDir
        log.trace "[Rescale Executor] Launching process > ${task.name} -- work folder: ${task.workDirStr}"
        return new RescaleTaskHandler(task, this).initialize()
    }
}
