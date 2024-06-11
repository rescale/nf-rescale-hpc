# Running a job using the `nf-rescale-hpc plugin`

The `nf-rescale-hpc` is used to run jobs in the Rescale platform.  

This can be useful to run Nextflow jobs without setting up a tile for Nextflow,
and for testing changes in the plugin code. 


To run a job using the plugin, we'll first compile the plugin, then we'll set up a job, where we'll
upload the compiled plugin to the platform as an input file along with the config file and the Nextflow file.

## Compiling the plugin
We assumed the project is already set up in our local. Refer to the [project level README](../README.md) if needed. 
```
(jzapien@tunita nf-rescale-hpc)$ alias now="date +'%Y-%m-%d-%H.%M.%S'"
(jzapien@tunita nf-rescale-hpc)$ make buildPlugins # This is the step actually compiling the plugin
(jzapien@tunita nf-rescale-hpc)$ cd build/plugins/
(jzapien@tunita plugins)$ # Assuming the plugin version is 0.4.1
(jzapien@tunita plugins)$ zip -r /tmp/nf-rescale-hpc-0.4.1.zip nf-rescale-hpc-0.4.1/ # Here we create the zip file that will be used as an input file in the job 
```

## Setting up the job in the platform
We need an HPS started. Start one if needed.  
This HPS has to be compatible (although not the same) with the coretype value of `machineType` in `nextflow.config`,
e.g., the HPS is running on an Emerald coretype and `nextflow.config` has `machineType="emerald"`.

We'll create a new job in the platform
 1. Attach the HPS to the job. You can press the _Attach Storage Device Without Adding Files_ button.
 2. Then upload these files
    1. `nf-rescale-hpc-0.4.1.zip`, which was created in the previous step. Unselect the option to automatically decompress this file.
    2. `echo-flow.nf`, is the text file describing our flow, in this example, we use the following:
    ```
    process saySomething {

    output:
        stdout
    
    """
    echo 'PUP-1212 sleeping' > results.txt
    sleep 180
    echo 'PUP-1212 awake' > results.txt
    """
    }

    workflow {
     saySomething()
    }

    ```
    3. `nextflow.config`, is also a text file with the contents below. Make sure the set appropriate values in the `env` section. The value of `RESCALE_CLUSTER_TOKEN` should be a platform API token.
    ```
    plugins {
      id 'nf-rescale-hpc@0.4.1'
    }

    executor {
      name = 'rescale-executor'
    }

    process {
      executor='rescale-executor'
      ext.jobAnalyses=[[
        analysisCode: "user_included",
        analysisVersion: "0",
      ]]
      machineType="emerald"
      cpus=1
      ext.wallTime=1
      ext.billingPriorityValue="INSTANT"
    }

    env {
      RESCALE_PLATFORM_URL = "http://platform-local.rescale.com:8790"
      RESCALE_CLUSTER_TOKEN = "c7b43fee8bde93"
    }
    ```
 3. Select `Bring Your Own Software` - CPU and use the following script:
    ```bash
    # Install Nextflow
    curl -s "https://get.sdkman.io" | bash;
    source ~/.sdkman/bin/sdkman-init.sh
    sdk install java 17.0.6-amzn;
    sdk default java 17.0.6-amzn
    sdk use java 17.0.6-amzn
    wget -qO- https://get.nextflow.io | bash
    chmod +x nextflow
    NF_HOME=$(pwd)

    # Make our plugin available
    mkdir -p ~/.nextflow/plugins
    mv nf-rescale-hpc-0.4.1 ~/.nextflow/plugins/
    unzip nf-rescale-hpc-0.4.1.zip -d ~/.nextflow/plugins/

    # Run our flow
    HPS_DIR=$(find / -maxdepth 1 -name "storage_?" -type d)
    $NF_HOME/nextflow -C nextflow.config run echo-flow.nf -plugins nf-rescale-hpc@0.4.1
    ```
## Launching the job 🚀
Once we have set up the job, we submit it.  

The plugin will use Rescale's API to create the job defined in `echo-flow.nf`, this job should appear shortly in the
platform with the name _saySomething_.
