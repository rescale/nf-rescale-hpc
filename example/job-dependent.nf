#!/usr/bin/env nextflow
nextflow.enable.dsl=2 
process phase1 {
  publishDir "twoprocout", mode: 'copy'
  input: 
    path x
  output:
    path 'phase1out.txt' 
  script:
    """
    sleep 10
    cp ${x} phase1out.txt
    echo "phase 1 done" >> phase1out.txt
    """
}
process phase2 {
    publishDir "twoprocout", mode: 'copy'
    input:
       path 'phase1out.txt'
    output:
       path 'phase2out.txt'
    script:
    """
    wc phase1out.txt > phase2out.txt
    """
}
workflow {
	 Channel.fromPath("input.txt") | phase1 | phase2
}

