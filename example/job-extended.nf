include { hifromrescale } from 'plugin/nf-rescale-hpc'

process hello {
  ext.jobAnalyses=[[
    analysisCode: "user_included",
    analysisVersion: "0", 
    useRescaleLicense: "true"
    ],
    [
    analysisCode: "ansys_hfss",
    analysisVersion: "2022r2", 
    onDemandLicenseSeller:["code":"rescale-trial", "name":"Rescale Trial"]
    ]]

  machineType="emerald"
  ext.wallTime=4
  ext.projectId="bNewo"

  executor="rescale-executor"

  output:
  stdout

  script:
  x = hifromrescale()
  
  """
  sleep 600; echo $x
  """
}

workflow {
   hello | view
}