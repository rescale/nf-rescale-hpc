include { hifromrescale } from 'plugin/nf-rescale-hpc'

process hello {
  ext.analysisCode="user_included"
  ext.analysisVersion="0"
  ext.storageId="bwFSc"
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