export function formatTaskAppBinding(base) {
    if (!base || base.appType !== 'agent' || !base.appId) {
        return '-';
    } else {
        const agentName = base.appName || base.appId || '-';
        const bundleName = base.appVersionName || base.appVersionId || '-';
        return [agentName, bundleName, base.appAgentAlias || '-'].join(' / ');
    }
}
