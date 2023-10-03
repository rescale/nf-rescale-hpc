
process nastran {
  ext.analysisCode="msc_nastran"
  ext.analysisVersion="2023.2"
  ext.rescaleLicense=true
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