include { hifromrescale } from 'plugin/nf-rescale-hpc'

process hello {
  output:
  stdout

  script:
  x = hifromrescale()
  
  """
  echo $x
  """
}

workflow {
   hello | view
}