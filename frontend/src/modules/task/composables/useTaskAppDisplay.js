import { reactive } from 'vue';
import { remoteCallApi } from '../../../api/remoteCall';

function bundleKey(agentId, bundleId) {
    return `${agentId}:${bundleId}`;
}

export function useTaskAppDisplay() {
    const agentNames = reactive({});
    const bundleNames = reactive({});
    const detailRequests = new Set();
    const bundleRequests = new Set();
    let agentListRequest;

    function saveVersions(agentId, versions) {
        (versions || [])
            .filter((version) => version?.id)
            .forEach((version) => {
                bundleNames[bundleKey(agentId, version.id)] = version.versionName || version.id;
            });
    }

    function saveAgents(agents) {
        (agents || [])
            .filter((agent) => agent?.id)
            .forEach((agent) => {
                agentNames[agent.id] = agent.agentName || agent.id;
                saveVersions(agent.id, agent.versions);
            });
    }

    async function loadBindings(bindings) {
        agentListRequest ??= remoteCallApi.listAgents();
        const [agentListResult] = await Promise.allSettled([agentListRequest]);
        const agents = agentListResult.status === 'fulfilled' ? agentListResult.value : [];
        saveAgents(agents);

        const agentIds = [...new Set(bindings.map((base) => base.appId))];
        const detailIds = agentIds.filter((agentId) => !agentNames[agentId] && !detailRequests.has(agentId));
        const bundleAgentIds = agentIds.filter((agentId) => {
            const hasUnknownBundle = bindings.some((base) =>
                base.appId === agentId
                && base.appVersionId
                && !bundleNames[bundleKey(agentId, base.appVersionId)]);
            return hasUnknownBundle && !bundleRequests.has(agentId);
        });
        detailIds.forEach((agentId) => detailRequests.add(agentId));
        bundleAgentIds.forEach((agentId) => bundleRequests.add(agentId));

        const [detailResults, bundleResults] = await Promise.all([
            Promise.allSettled(detailIds.map((agentId) => remoteCallApi.getAgentDetail(agentId))),
            Promise.allSettled(bundleAgentIds.map((agentId) => remoteCallApi.listAgentBundles(agentId)))
        ]);
        saveAgents(detailResults.flatMap((result) => result.status === 'fulfilled' ? [result.value] : []));
        bundleResults
            .flatMap((result, index) => result.status === 'fulfilled'
                ? [{ agentId: bundleAgentIds[index], versions: result.value }]
                : [])
            .forEach((item) => saveVersions(item.agentId, item.versions));
    }

    function load(bases) {
        const bindings = (bases || []).filter((base) => base?.appType === 'agent' && base.appId);
        return bindings.length ? loadBindings(bindings) : Promise.resolve();
    }

    function format(base) {
        if (!base || base.appType !== 'agent' || !base.appId) {
            return '-';
        } else {
            const agentName = agentNames[base.appId] || base.appId;
            const bundleName = bundleNames[bundleKey(base.appId, base.appVersionId)] || base.appVersionId || '-';
            return [agentName, bundleName, base.appAgentAlias || '-'].join(' / ');
        }
    }

    return { load, format };
}
