process echoEnv {
    ext.analysisCode="user_included"
    ext.analysisVersion="0"
    machineType "emerald"
    cpus 1

    executor="rescale-executor"

    input:
    env TEST_ENV

    output:
    stdout

    script:
    """
    echo \$TEST_ENV
    echo \$TEST_ENV_2
    """

}
workflow {
    Channel.of('123', 'abc') | echoEnv | view
}