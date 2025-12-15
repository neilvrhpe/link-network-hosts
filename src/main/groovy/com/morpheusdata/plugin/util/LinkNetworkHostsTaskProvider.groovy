package com.morpheusdata.plugin.util

import com.morpheusdata.core.AbstractTaskService
import com.morpheusdata.core.ExecutableTaskInterface
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.providers.TaskProvider
import com.morpheusdata.model.ComputeServer
import com.morpheusdata.model.Icon
import com.morpheusdata.model.Instance
import com.morpheusdata.model.OptionType
import com.morpheusdata.model.Task
import com.morpheusdata.model.TaskResult
import com.morpheusdata.model.TaskType
import com.morpheusdata.model.Workload

class LinkNetworkHostsTaskProvider implements TaskProvider {

    MorpheusContext morpheusContext
    Plugin plugin
    AbstractTaskService service

    LinkNetworkHostsTaskProvider(Plugin plugin, MorpheusContext morpheusContext) {
        this.plugin = plugin
        this.morpheusContext = morpheusContext
    }

    @Override
    MorpheusContext getMorpheus() {
        return morpheusContext
    }

    @Override
    Plugin getPlugin() {
        return plugin
    }

    @Override
    ExecutableTaskInterface getService() {
        return new LinkNetworkHostsTaskService(morpheus)
    }

    @Override
    String getCode() {
        return 'linkNetworkHosts'
    }

    @Override
    TaskType.TaskScope getScope() {
        return TaskType.TaskScope.app
    }

    @Override
    String getName() {
        return 'Link Network Hosts'
    }

    @Override
    String getDescription() {
        return 'Link network pool host records to servers present on the network'
    }

    @Override
    Boolean isAllowExecuteLocal() {
        return true
    }

    @Override
    Boolean isAllowExecuteRemote() {
        return false
    }

    @Override
    Boolean isAllowExecuteResource() {
        return false
    }

    @Override
    Boolean isAllowLocalRepo() {
        return true
    }

    @Override
    Boolean isAllowRemoteKeyAuth() {
        return false
    }

    @Override
    Boolean hasResults() {
        return true
    }

    @Override
    List<OptionType> getOptionTypes() {
        List<OptionType> options = []

        options << new OptionType(
                name: 'poolId',
                code: 'poolId',
                fieldName: 'poolId',
                displayOrder: 0,
                fieldLabel: 'Network Pool Id',
                required: true,
                inputType: OptionType.InputType.TEXT,
                defaultValue: "57"
        )

        options << new OptionType(
                code: 'matchFqdn',
                name: 'matchFqdn',
                inputType: OptionType.InputType.CHECKBOX,
                defaultValue: 0,
                fieldName: 'matchFqdn',
                fieldLabel: 'Match Full FQDN',
                fieldContext: 'config',
                displayOrder: 2
        )

        options << new OptionType(
                code: 'replaceExisting',
                name: 'replaceExisting',
                inputType: OptionType.InputType.CHECKBOX,
                defaultValue: 0,
                fieldName: 'replaceExisting',
                fieldLabel: 'Replace Existing Links',
                fieldContext: 'config',
                displayOrder: 3
        )

        return options
    }

    @Override
    Icon getIcon() {
        return new Icon(path:"morpheusGreen.png", darkPath: "morpheusGreen.png")
    }

    @Override
    TaskResult executeLocalTask(Task task, Map opts, Workload workload, ComputeServer server, Instance instance) {
        return null
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
    TaskResult executeContainerTask(Workload workload, Task task, Map opts) {
        return null
    }

    @Override
    TaskResult executeContainerTask(Workload workload, Task task) {
        return null
    }

    @Override
    TaskResult executeRemoteTask(Task task, Map opts, Workload workload, ComputeServer server, Instance instance) {
        return null
    }

    @Override
    TaskResult executeRemoteTask(Task task, Workload workload, ComputeServer server, Instance instance) {
        return null
    }



}
