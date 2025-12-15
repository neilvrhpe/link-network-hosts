package com.morpheusdata.plugin.util

import com.morpheusdata.core.AbstractTaskService
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.data.DataQuery
import com.morpheusdata.model.ComputeServer
import com.morpheusdata.model.Container
import com.morpheusdata.model.Instance
import com.morpheusdata.model.NetworkPoolIp
import com.morpheusdata.model.Task
import com.morpheusdata.model.TaskResult
import groovy.sql.GroovyRowResult
import groovy.json.JsonOutput
import groovy.sql.Sql
import groovy.util.logging.Slf4j

import java.sql.Connection


@Slf4j
class LinkNetworkHostsTaskService extends AbstractTaskService {

    MorpheusContext context

    LinkNetworkHostsTaskService(MorpheusContext context) {
        this.context = context
    }

    @Override
    MorpheusContext getMorpheus() {
        return context
    }

    @Override
    TaskResult executeLocalTask(Task task, Map opts, Container container, ComputeServer server, Instance instance) {
        log.info("Opts: $opts")

        def poolIdOption = task.taskOptions.find { it.optionType.code == 'poolId' }
        def matchFqdnOption = task.taskOptions.find { it.optionType.code == 'matchFqdn' }
        def replaceExistingOption = task.taskOptions.find { it.optionType.code == 'replaceExisting' }

        String rawPoolId = poolIdOption.value.toString()
        String resolvedPoolId = resolveCustomOptions(rawPoolId, opts.customInputs.customOptions)

        Long poolId = resolvedPoolId.toLong()
        Boolean matchFqdn = matchFqdnOption.value == "on"
        Boolean replaceExisting = replaceExistingOption.value == "on"

        if (!poolId) {
            log.error("Missing 'poolId' input parameter")
            return new TaskResult(success: false, data: null, output : "Missing 'poolId' input parameter")
        }

        return executeTask(poolId, matchFqdn, replaceExisting)
    }


    static String resolveCustomOptions(String input, Map customOptions) {

        // Regex matches:
        // <%= customOptions.key %>
        // <%=customOptions.key%>
        def pattern = ~/\<%=\s*customOptions\.([a-zA-Z0-9_]+)\s*%\>/

        input.replaceAll(pattern) { fullMatch, key ->
            customOptions.containsKey(key) ? customOptions[key].toString() : fullMatch
        }
    }


    @Override
    TaskResult executeServerTask(ComputeServer server, Task task, Map opts) {
        return null
    }

    @Override
    TaskResult executeServerTask(ComputeServer server, Task task) {
        return null
    }

    @Override
    TaskResult executeContainerTask(Container container, Task task, Map opts) {
        return null
    }

    @Override
    TaskResult executeContainerTask(Container container, Task task) {
        return null
    }

    @Override
    TaskResult executeRemoteTask(Task task, Map opts, Container container, ComputeServer server, Instance instance) {
        return null
    }

    @Override
    TaskResult executeRemoteTask(Task task, Container container, ComputeServer server, Instance instance) {
        return null
    }

    /**
     * @param poolId
     * @param replaceExisting
     * @return updated servers
     */
    TaskResult executeTask(Long poolId, Boolean matchFqdn, Boolean replaceExisting) {

        Map<String, Map> poolComputeServers = [:]
        List<NetworkPoolIp> poolRecords = []
        List<Map> returnData = []

        // get the CopmuteServer and NetworkPoolIp lists
        try {
            poolComputeServers = getPoolComputeServers(poolId)
            poolRecords = morpheus.services.network.pool.poolIp.list(new DataQuery().withFilter("networkPool.id", poolId))

            if (!poolComputeServers.size() || !poolRecords.size()) {
                def msg = "Empty match list: ComputeServers ${poolComputeServers.size()}, PoolHosts ${poolRecords.size()}"
                log.warn(msg)
                return new TaskResult(success: true, data: null, output : msg)
            } else {
                log.info("Matching ${poolComputeServers.size()} ComputeServers with ${poolRecords.size()} PoolHosts")
            }
        } catch (Exception e) {
            return new TaskResult(success: false, data: null, output : "Error linking pool hosts with compute servers, $e")
        }

        //Compare and update
        poolRecords.each { NetworkPoolIp poolIp ->
            String poolIpFqdn = poolIp.hostname?.trim()?.toLowerCase()
            if (poolIpFqdn && (replaceExisting || !poolIp.refId)) {
                String poolIpShortHostname = poolIpFqdn.split('\\.')[0]
                def server = poolComputeServers[poolIpShortHostname]

                if (server && (!matchFqdn || server.fqdn == poolIpFqdn)) {
                    updatePoolIpRef(poolIp, server.serverId as Long)
                    returnData << server
                }
            }
        }

        new TaskResult(
                success: true,
                data   : returnData,
                output : "Successfully matched ${returnData.size()} of ${poolRecords.size()} PoolIPs to ComputeServer records: ${JsonOutput.toJson(returnData)}"
        )
    }


    void updatePoolIpRef(NetworkPoolIp poolRecord, Long computeServerId) {
        poolRecord.refType = "ComputeServer"
        poolRecord.refId = computeServerId
        morpheus.services.network.pool.poolIp.save(poolRecord)
    }


    Map<String, Map> getPoolComputeServers(Long networkPoolId) {
        log.info("Search pools with poolId=$networkPoolId")

        Connection dbConnection
        List<GroovyRowResult> rows = []

        String sqlString = """
            SELECT 
                cs.name AS server_name, 
                cs.hostname AS host_name, 
                cs.id AS server_id, 
                csi.name AS interface, 
                np.name AS pool, 
                np.id AS pool_id
            FROM
                compute_server cs INNER JOIN
                compute_server_compute_server_interface cscsi ON cs.id = cscsi.compute_server_interfaces_id INNER JOIN
                compute_server_interface csi ON cscsi.compute_server_interface_id = csi.id INNER JOIN
                network ON csi.network_id = network.id INNER JOIN
                network_pool np ON network.pool_id = np.id 
            WHERE 
                np.id = ?
        """

        try {
            dbConnection = morpheus.async.report.getReadOnlyDatabaseConnection().blockingGet()
            rows = new Sql(dbConnection).rows(sqlString, [networkPoolId])
        } catch (Exception e) {
            log.error("Error querying compute servers for networkPoolId={}", networkPoolId, e)
            throw new RuntimeException("Error querying pool compute servers for poolId=${networkPoolId}", e)
        } finally {
            if (dbConnection) morpheus.async.report.releaseDatabaseConnection(dbConnection)
        }

        Map<String, Map> byHostname = [:]

        rows.each { GroovyRowResult row ->
            String hostKey = row.host_name?.toString()?.trim()?.toLowerCase()?.split('\\.')[0]
            if (hostKey) {
                byHostname.putIfAbsent(hostKey, [
                        serverId  : row.server_id,
                        serverName: row.server_name,
                        hostname  : row.host_name,
                        iface     : row.interface,
                        poolId    : row.pool_id,
                        poolName  : row.pool,
                        fqdn      : row.host_name?.toString()?.trim()?.toLowerCase()
                ])
            }
        }

        if (dbConnection) morpheus.async.report.releaseDatabaseConnection(dbConnection)

        return byHostname
    }

}


