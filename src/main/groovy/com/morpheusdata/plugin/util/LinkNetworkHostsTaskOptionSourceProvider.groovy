package com.morpheusdata.plugin.util

import com.morpheusdata.core.AbstractOptionSourceProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.data.DataQuery

class LinkNetworkHostsTaskOptionSourceProvider extends AbstractOptionSourceProvider {

    LinkNetworkHostsTaskPlugin plugin
    MorpheusContext morpheusContext

    LinkNetworkHostsTaskOptionSourceProvider(LinkNetworkHostsTaskPlugin plugin, MorpheusContext context) {
        this.plugin = plugin
        this.morpheusContext = context
    }

    @Override
    List<String> getMethodNames() {
        return ["getNetworkPools"]
    }

    def getNetworkPools(args) {
        def options = []
        morpheus.services.network.pool.list(new DataQuery().withFilter("just", "allOfThem")).each {
            options << [name: it.name, value: it.id]
        }
        return options
    }

    @Override
    MorpheusContext getMorpheus() {
        return morpheusContext
    }

    @Override
    String getCode() {
        return 'LinkHosts'
    }

    @Override
    String getName() {
        return "Link Network Pool Hosts"
    }
}
