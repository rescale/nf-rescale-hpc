# Rescale Nextflow Plugin
This project contains Rescale's custom Nextflow executor called `nf-rescale-hpc`. Rescale Executor is used to launch Rescale Jobs using a Nextflow file and supports multiple features of Nextflow.

## How to Launch a Rescale Job from a Rescale Platform
To launch a Rescale Job, the Nextflow file (.nf) should included mandatory configurations:

### Pre-requisites
To launch a Rescale Job using Nextflow
1. ```<personal-access-token>``` and ```<API_KEY>``` are required. 

    Personal Access Token can be requested from a Github account and will need the approval of IT. 
    
    The API_KEY can be created from a Rescale Platform

2. `Custom naming policy` is also required to utilize Nextflow caching system `-resume`.

    Request custom naming policy from site/organization admin to set in Django.

3. `HPS` (Storage Device) is required to attach to Nextflow Job/Tile (HPS Specific)


### Step by Step

1. Set the plugin to `nf-rescale-hpc` and executor to `rescale-executor` in `nextflow.config` file

    **NOTE**: Currently you will need to provide the version for the executor (Default 0.4.0). (After official release, version number should reflect the latest release) 
    ```groovy
    plugins {
    id 'nf-rescale-hpc@<version>'
    } 

    process {
    executor='rescale-executor'
    }
    ```
2. Set RESCALE_PLATFORM_URL and RESCALE_CLUSTER_TOKEN (API Token) in `nextflow.config` file

    **NOTE**: Any environmental variable that is required to be shared among jobs need to be provided using env

    **NOTE#2**: User set license must also be provided through env either from `nextflow.config` or through env input [Nextflow Documentation](https://www.nextflow.io/docs/latest/process.html#input-type-env)
    ```groovy 
    env {
    RESCALE_PLATFORM_URL = "https://platform.rescale.com"
    RESCALE_CLUSTER_TOKEN = "<API_KEY>"
    }
    ```

3. Configure your nextflow file with the software and hardware to run the process using the directive
    ```groovy
    process <processName> {
    ext.jobAnalyses=[[
        "analysisCode":"msc_nastran"
        "analysisVersion":"2023.2"
        "rescaleLicense":true
    ]]
    machineType "emerald"
    cpus 1
    
    ...
    }
    ```
    **NOTE:** process configuration can be specified in `nextflow.config` using [`withName`](https://www.nextflow.io/docs/latest/config.html#process-selectors) or [`withLabel`](https://www.nextflow.io/docs/latest/config.html#process-selectors) tags.

    ### Available Parameters Supported (Second Pass)
    ---

    The following parameters are currently supported and functional. The parameters supported are required for any Rescale Job, hence it was the first parameters to be supported.

    **ext.jobAnalyses** (Required) (datatype: `List<jobAnalysis>`): The configuration of one or multiple analyses.
    *Must be passed as a list of jobAnalysis*.

    The jobAnalysis (datatype: `Map`) have the following paramaters

    - **analysisCode** (Required)(datatype: `String`): The software code

    - **analysisVersion** (Required)(datatype: `String`): The software version

    - **rescaleLicense** (Defaults to false)(datatype: `bool`): Whether or not to use Rescale License when running a software (Custom License can be passed using Nextflow environmental values)

    - **onDemandLicenseSeller** (Optional)(datatype: `Map`): A dictionary with the schema of `["code":"codeValue", "name":"nameValue"]` where code and name are the license provider’s code and name, respectively.

    - **userDefinedLicenseSettings** (Optional)(datatype: `Map`): User-defined license settings for the analysis is a definition of multiple sets of license feature counts that the user expects the analysis to use for running the job

        ```groovy
        // Example of userDefinedLicense
        process <processName> {
        ext.userDefinedLicenseSettings=[[
            ...
            "userDefinedLicenseSettings": [
                'featureSets':[
                    ['name':'<featureset name>', 'features': [
                        ['name':'<feature name>', 'count':<count value>]
                        ]
                    ]
                ]
            ]
        
        ]]
        ...
        }
        ```

    **machineType** (Required)(datatype: `String`): The hardware used to run the software

    **cpus** (Defaults to 1)(datatype: `int`): The number of cores of the hardware

    **ext.billingPriorityValue** (Optional)(datatype: `String`): Priority for job hardware

    **ext.wallTime** (Optional)(datatype: `int`): The time a Rescale Job will be allowed to run. Options are: 'INSTANT’ for On Demand Priority, 'ON_DEMAND’ for On Demand Economy, 'RESERVED’ for On Demand Reserved.

    **ext.projectId** (Optional)(datatype: `String`)

    **ext.customFields** (Optional)(datatype: `Map`): Custom Field values that should be assigned to jobs.

    ### Future Parameters to be supported (Third Pass)
    ---
    Currently there are no further parameters considered for support

    ### Unsupported and Future-Excluded Parameters
    ---
    The following parameters will be not be supported now or in the future, because the parameters have a Nextflow based solution.

    **preProcessScript**: Pre-processing script for analysis.

    **postProcessScript**: Post-processing script for analysis.

    ---

4. Run the following command as BYOD in Rescale as a job. Make sure to attach a storage device (HPS) alongside a directory called projectdata
    ```bash
    curl -s "https://get.sdkman.io" | bash; 
    source ~/.sdkman/bin/sdkman-init.sh
    sdk install java 17.0.6-amzn;
    sdk default java 17.0.6-amzn
    sdk use java 17.0.6-amzn
    wget -qO- https://get.nextflow.io | bash
    chmod +x nextflow
    nf_home=$(pwd)
    git clone https:/<personal-access-token>@github.com/rescale/nf-rescale-hpc.git
    cd nf-rescale-hpc
    make buildPlugins
    mkdir -p ~/.nextflow/plugins
    cp -r build/plugins/* ~/.nextflow/plugins/
    <move file to where you are running nextflow>
    cd ~/storage*/projectdata # cd into any shared directory the following is an example
    $nf_home/nextflow run <nextflow file>
    ```
    As stated above replace ```<personal-access-token>``` and ```<nextflow-file>``` 

    Make sure to move ```nextflow.config``` and ```nextflow-file``` inside ```nf-rescale-hpc```, and move any input file into ```~/storage*/projectdata```

__4.1 For a step by step tutorial see the [running a Nextlow job on using the nf-rescale-hpc plugin](running-nf-rescale-hpc-jobs.md).

## Potential Non-Error Failures
### 1. Pre-mature termination
#### Condition
A job spawned by Nextflow was terminated before validation is complete, the job will not have update status, hence, Nextflow will be stuck waiting for a completed status or stopping status.
#### Resolution
Stop the master Nextflow job. (Resume parameter will continue from the current point) 
    
### 2. Missing stdout due to job failures
#### Condition
A Missing stdout error from Nextflow config, due to job terminated with a Completed Status that did not execute properly.
#### Resolution
Fix the issue of the terminated job and re-run Nextflow
#### Further Work Required
1. Add the completed status termination in `RescaleTaskHandler.groovy` in `checkIfCompleted` function or
2. Add a status in Rescale Web to signify termination


### 3. Missing stdout due to file location
#### Condition
A Missing stdout error from Nextflow config, due to Nextflow unable to locate .nextflow or work directory. Typically occurs if the directories are situated in an incorrect location or directories have been deleted.
#### Resolution
Verify .nextflow and work directory are created properly at the place of execution

### 4. StorageId Missing
#### Condition
Error thrown due to storageId unable to locate from parsing the `$HOME` directory
#### Resolution
1. Check if storage device is attached 
2. Check if storage device directory is located in the `$HOME` directory
3. Check if storage id search pattern is up-to-practices in `RescaleJob.groovy`

Note: For local execution, manipulate `getStorageId` to output the desired storageId

### 5. General advices for analyzing errors
#### Condition
Error thrown with a minimum explanation
#### Resolution
Check `.nextflow.log` generated from nextflow during execution


## Example Local Implementation
In `example` directory, there is a simple nextflow script called `hello-rescale.nf`. The following script implements rescale custom function `hifromrescale` to showcase how the plugin can be implemented.

To run the `example` locally with a local Nextflow, configure a local Nextflow build with the following steps:

**Note**: Follow [Step by Step](#step-by-step) up to step 4

1. Clone the Nextflow repository in your computer into a sibling directory:
    ```bash
    git clone --depth 1 https://github.com/nextflow-io/nextflow ../nextflow
    ```
  
2. Configure the plugin build to use the local Nextflow code:
    ```bash
    echo "includeBuild('../nextflow')" >> settings.gradle
    ```
  
   (Make sure to not add it more than once!)

3. Compile the plugin alongside the Nextflow code:
    ```bash
    make compile
    ```

4. Check if there is any error by running the unit test
    ```bash
    ./gradlew check
    ```

5. Run Nextflow with the plugin, using `./launch.sh` as a drop-in replacement for the `nextflow` command, and adding the option `-plugins nf-rescale-hpc` to load the plugin:

    **Note**: Change the `getStorageId` inside `RescaleExecutor.groovy` to output the desired storageId of the storage device(HPS) id currently up and running inside Rescale Environment
    ```bash
    ./launch.sh run example/hello-rescale.nf -plugins nf-rescale-hpc
    ```


## Plugin structure
                    
- `settings.gradle`
    
    Gradle project settings. 

- `plugins/nf-rescale-hpc`
    
    The plugin implementation base directory.

- `plugins/nf-rescale-hpc/build.gradle` 
    
    Plugin Gradle build file. Project dependencies should be added here.

- `plugins/nf-rescale-hpc/src/resources/META-INF/MANIFEST.MF` 
    
    Manifest file defining the plugin attributes e.g. name, version, etc. The attribute `Plugin-Class` declares the plugin main class. This class should extend the base class `nextflow.plugin.BasePlugin` e.g. `nextflow.hello.HelloPlugin`.

- `plugins/nf-rescale-hpc/src/resources/META-INF/extensions.idx`
    
    This file declares one or more extension classes provided by the plugin. Each line should contain the fully qualified name of a Java class that implements the `org.pf4j.ExtensionPoint` interface (or a sub-interface).

- `plugins/nf-rescale-hpc/src/main` 

    The plugin implementation sources.

- `plugins/nf-rescale-hpc/src/test` 

    The plugin unit tests. 

## Plugin classes

### Custom Rescale Classes
- `RescaleExecutor`: the custom Rescale Nextflow Executor designed for workflows

- `RescaleTaskHandler`: custom TaskHandler responsible for API calls and status tracking

- `RescaleJob`: custom class responsible for configuring parameters of a Rescale Job

### Existing Classes from nf-hello 
- `HelloConfig`: shows how to handle options from the Nextflow configuration

- `HelloExtension`: shows how to create custom channel factories, operators, and fuctions that can be included into pipeline scripts

- `HelloFactory` and `HelloObserver`: shows how to react to workflow events with custom behavior

- `HelloPlugin`: the plugin entry point

## Unit testing 

To run your unit tests, run the following command in the project root directory (ie. where the file `settings.gradle` is located):

```bash
./gradlew check
```

## Testing and debugging

To build and test the plugin during development, configure a local Nextflow build with the following steps:

1. Clone the Nextflow repository in your computer into a sibling directory:
    ```bash
    git clone --depth 1 https://github.com/nextflow-io/nextflow ../nextflow
    ```
  
2. Configure the plugin build to use the local Nextflow code:
    ```bash
    echo "includeBuild('../nextflow')" >> settings.gradle
    ```
  
   (Make sure to not add it more than once!)

3. Compile the plugin alongside the Nextflow code:
    ```bash
    make compile
    ```

4. Run Nextflow with the plugin, using `./launch.sh` as a drop-in replacement for the `nextflow` command, and adding the option `-plugins nf-rescale-hpc` to load the plugin:
    ```bash
    ./launch.sh run nextflow-io/hello -plugins nf-rescale-hpc
    ```

## Testing without Nextflow build

The plugin can be tested without using a local Nextflow build using the following steps:

1. Build the plugin: `make buildPlugins`
2. Copy `build/plugins/<your-plugin>` to `$HOME/.nextflow/plugins`
3. Create a pipeline that uses your plugin and run it: `nextflow run ./my-pipeline-script.nf`

## Package, upload, and publish

The project should be hosted in a GitHub repository whose name matches the name of the plugin, that is the name of the directory in the `plugins` folder (e.g. `nf-rescale-hpc`).

Follow these steps to package, upload and publish the plugin:

1. Create a file named `gradle.properties` in the project root containing the following attributes (this file should not be committed to Git):

   * `github_organization`: the GitHub organisation where the plugin repository is hosted.
   * `github_username`: The GitHub username granting access to the plugin repository.
   * `github_access_token`: The GitHub access token required to upload and commit changes to the plugin repository.
   * `github_commit_email`: The email address associated with your GitHub account.

2. Use the following command to package and create a release for your plugin on GitHub:
    ```bash
    ./gradlew :plugins:nf-rescale-hpc:upload
    ```

3. Create a pull request against [nextflow-io/plugins](https://github.com/nextflow-io/plugins/blob/main/plugins.json) to make the plugin accessible to Nextflow.
