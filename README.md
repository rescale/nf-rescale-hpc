# Rescale Nextflow Plugin
This project contains Rescale's custom Nextflow executor called `nf-rescale-hpc`. Rescale Executor is used to launch Rescale Jobs using a Nextflow file and supports multiple features of Nextflow.

## How to Launch a Rescale Job
To launch a Rescale Job, the Nextflow file (.nf) should included mandatory configurations:

1. Set the executor to `rescale-executor` either in nextflow.config or inside the process
```
process {
  executor='rescale-executor'
}
```
2. Set RESCALE_PLATFORM_URL and RESCALE_CLUSTER_TOKEN (API Token) in nextflow.config
```
env {
  RESCALE_PLATFORM_URL = "https://platform.rescale.com"
  RESCALE_CLUSTER_TOKEN = "<API KEY>"
}
```

3. Configure the software and hardware to run the process using the directive
```
process nastran {
  ext.analysisCode="msc_nastran"
  ext.analysisVersion="2023.2"
  ext.rescaleLicense=true
  machineType "emerald"
  cpus 1
  
  ...
}
```

### Available Parameters

**ext.analysisCode** (Required): The software code

**ext.analysisVersion** (Required): The software version

**machineType** (Required): The hardware used to run the software

**cpus** (Defaults to 1): The number of cores of the hardware

**ext.rescaleLicense** (Defaults to false): Whether or not to use Rescale License when running a software

4. Run the following command as BYOD in Rescale as a job. Make sure to attach a storage device (HPS) alongside a directory called projectdata
```bash
curl -s "https://get.sdkman.io" | bash
 source ~/.sdkman/bin/sdkman-init.sh
 sdk install java 17.0.6-amzn
 sdk use java 17.0.6-amzn
 git clone https://<personal-access-token>@github.com/rescale/nf-rescale-hpc.git
 cd nf-rescale-hpc
 git clone --depth 1 https://github.com/nextflow-io/nextflow ../nextflow
 echo "includeBuild('../nextflow')" >> settings.gradle
 make compile
 ./gradlew check
 <move-any-files>
 ./launch.sh run <nextflow-file>

```
As stated above replace ```<personal-access-token>``` and ```<nextflow-file>``` 

Make sure to move ```nextflow.config``` and ```nextflow-file``` inside ```nf-rescale-hpc```, and move any input file into ```~/storage*/projectdata``` 

---


The plugin is a rescale version of `nf-hello` used a starting point to create a custom Rescale Executor Plugin. The plugin contains:

- A custom Rescale function called `hifromrescale` that outputs `Hello, Welcome to Rescale`
- A custom trace observer that prints a message when the workflow starts and when the workflow completes
- A custom channel factory called `reverse`
- A custom operator called `goodbye`
- A custom function called `randomString`

## Example Implementation
In `example` directory, there is a simple nextflow script called `hello-rescale.nf`. The following script implements rescale custom function `hifromrescale` to showcase how the plugin can be implemented.

To run the `example` locally with a local Nextflow, configure a local Nextflow build with the following steps:

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
