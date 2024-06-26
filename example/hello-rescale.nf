include { hifromrescale } from 'plugin/nf-rescale-hpc'

process hello {
  ext.jobAnalyses=[[
    analysisCode: "user_included",
    analysisVersion: "0"
    ]]
  machineType "emerald"

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