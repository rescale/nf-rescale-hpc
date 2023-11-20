
process nastran {
  ext.jobAnalyses=[[
    analysisCode: "msc_nastran",
    analysisVersion: "2023.2", 
    useRescaleLicense: true
    ]]
  machineType "emerald"
  cpus 1

  executor="rescale-executor"

  output:
  stdout

  script:
  """
  sleep 60; echo Second Job!
  """
}

workflow {
   nastran | view
}