# Kubernetes Production Readiness Checklist

## About this file

### This file is used to check if the application is ready for production. Do not act upon this file, it is just a checklist. I want you to use this file to check if the application is ready for production. If you find any issues, please log them into:

`/home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/{service_name}/logs.md`

Not: If there is no `{service_name}` directory, create one. Write the issues in the `logs.md` file. 

### If there is a checklist that doesn't make sense in this project for a service, I want you to skip that checklist and move on to the next one. Do not mark it as checked.




## 1. Your Application

### Application behavior

- [ ] Does the application log to `stdout` and `stderr`?
- [ ] Are application logs structured and machine-readable?
- [ ] Are logs collected and stored in a cluster-level logging system?
- [ ] Is configuration separated from the application code?
- [ ] Is non-sensitive configuration stored in `ConfigMap` objects?
- [ ] Are sensitive values stored in `Secret` objects?
- [ ] Are simple scalar configuration values delivered through environment variables?
- [ ] Are structured configuration values delivered through mounted files?
- [ ] Is configuration size kept within Kubernetes object limits?
- [ ] Does the application handle `SIGTERM` correctly?
- [ ] Does the application stop accepting new requests during shutdown?
- [ ] Does the application finish in-flight requests before terminating?
- [ ] Does the application close long-lived and keep-alive connections gracefully?
- [ ] Does the application exit before `terminationGracePeriodSeconds` expires?
- [ ] Does the container entrypoint correctly pass signals to the application?
- [ ] Is the exec form of `CMD` or `ENTRYPOINT` used where possible?
- [ ] Is `preStop` used only for predictable shutdown actions?
- [ ] Does the application expose health signals?
- [ ] Is a readiness endpoint available?
- [ ] Is a liveness endpoint available?
- [ ] Is a startup endpoint available when the application has a long startup time?
- [ ] Do health endpoints return appropriate HTTP status codes?
- [ ] Does the application avoid storing permanent data on the container’s local disk?
- [ ] Are temporary files, caches, and scratch data clearly separated from permanent data?
- [ ] Is permanent data stored in a `PersistentVolume`, database, or external storage system?
- [ ] If the application is stateful, is `StatefulSet` being used where appropriate?
- [ ] Can clients reconnect after a Pod is replaced?
- [ ] Does the application handle long-lived connections correctly?
- [ ] Can clients reconnect safely after disconnections?
- [ ] Can servers drain long-lived connections during shutdown?
- [ ] Are long-lived requests and streams allowed to finish where possible?

### Container image

- [ ] Does the container image contain only the files required at runtime?
- [ ] Are multi-stage Docker builds used?
- [ ] Are build tools excluded from the final runtime image?
- [ ] Are package managers, test files, and unnecessary utilities excluded from production images?
- [ ] Are image tags stable and traceable?
- [ ] Is the `:latest` tag avoided in production?
- [ ] Are production images referenced by a specific version tag or digest?
- [ ] Is the full `sha256` image digest used when a fixed reference is required?

## 2. Your Kubernetes Manifests

### Runtime contract

- [ ] Are readiness probes defined?
- [ ] Are liveness probes defined?
- [ ] Is a startup probe defined when the application requires a long initialization period?
- [ ] Does the readiness probe indicate whether the Pod should receive traffic?
- [ ] Does the liveness probe identify applications that are stuck and need restarting?
- [ ] Are liveness probes configured conservatively?
- [ ] Are `initialDelaySeconds` values appropriate?
- [ ] Is `periodSeconds` configured appropriately?
- [ ] Is `timeoutSeconds` configured appropriately?
- [ ] Is `failureThreshold` configured appropriately?
- [ ] Does every container define CPU requests?
- [ ] Does every container define memory requests?
- [ ] Does every container define a memory limit?
- [ ] Are CPU limits configured according to the workload and cluster design?
- [ ] Are resource requests based on realistic workload measurements?
- [ ] Are resource limits based on realistic workload measurements?
- [ ] Is the resulting Pod QoS class appropriate?
- [ ] Is ephemeral storage usage bounded?
- [ ] Are `ephemeral-storage` requests defined where necessary?
- [ ] Are `ephemeral-storage` limits defined where necessary?
- [ ] Are writable directories mounted through `emptyDir` where appropriate?
- [ ] Is `emptyDir.sizeLimit` defined when the maximum size is known?
- [ ] Can the application run with `readOnlyRootFilesystem: true`?

### Rollouts and configuration

- [ ] Are rolling update settings explicitly defined?
- [ ] Is `maxUnavailable` configured?
- [ ] Is `maxSurge` configured?
- [ ] Is `minReadySeconds` configured where appropriate?
- [ ] Is `progressDeadlineSeconds` configured?
- [ ] Is `revisionHistoryLimit` configured?
- [ ] Does the application tolerate old and new Pods running simultaneously?
- [ ] Are database schema changes backward-compatible during rolling updates?
- [ ] Are new APIs compatible with the previous application version?
- [ ] Is `RollingUpdate` appropriate for the workload?
- [ ] Is `Recreate` used when old and new versions must not run simultaneously?
- [ ] Is the availability impact of using `Recreate` understood?
- [ ] Is there a reload strategy for `ConfigMap` updates?
- [ ] Is there a reload strategy for `Secret` updates?
- [ ] Are environment-variable configuration changes followed by a Pod restart or rollout?
- [ ] Are file-based configuration changes handled by the application?
- [ ] Are `ConfigMap` and `Secret` volume updates tested?
- [ ] Are `subPath` mounts avoided when automatic updates are required?
- [ ] Are immutable `ConfigMap` or `Secret` objects used when configuration must not change?
- [ ] Does configuration change trigger a new Deployment revision when necessary?

### Placement and disruption

- [ ] Does the workload run as a non-root user?
- [ ] Is `runAsNonRoot: true` configured?
- [ ] Is `runAsUser` explicitly set to a non-zero UID where appropriate?
- [ ] Is `readOnlyRootFilesystem: true` enabled?
- [ ] Is `allowPrivilegeEscalation: false` enabled?
- [ ] Are unnecessary Linux capabilities dropped?
- [ ] Is `capabilities.drop: ["ALL"]` configured where appropriate?
- [ ] Is a suitable seccomp profile configured?
- [ ] Is `seccompProfile.type: RuntimeDefault` configured?
- [ ] Is a `PodDisruptionBudget` defined?
- [ ] Does the PDB protect against voluntary disruptions?
- [ ] Is the PDB flexible enough to allow node maintenance?
- [ ] Does the PDB avoid setting `minAvailable` equal to the total replica count?
- [ ] Are replicas spread across multiple nodes?
- [ ] Are replicas spread across multiple availability zones where possible?
- [ ] Are `topologySpreadConstraints` configured?
- [ ] Is `topologyKey` present consistently on eligible nodes?
- [ ] Does the topology selector match the Pod template labels?
- [ ] Are `maxSkew` and `whenUnsatisfiable` suitable for the workload?
- [ ] Have topology constraints been tested during node failures?
- [ ] Have topology constraints been tested during scale-out events?
- [ ] Have taints, node affinity, autoscaling, and topology labels been considered?

### Secrets and metadata

- [ ] Are application secrets mounted as files instead of environment variables where appropriate?
- [ ] Is exposure through process listings, debug output, and crash dumps considered?
- [ ] Are standard `app.kubernetes.io/*` labels applied?
- [ ] Does the workload have an appropriate `app.kubernetes.io/name` label?
- [ ] Does the workload have an appropriate `app.kubernetes.io/instance` label?
- [ ] Does the workload have an appropriate `app.kubernetes.io/version` label?
- [ ] Does the workload have an appropriate `app.kubernetes.io/component` label?
- [ ] Does the workload have an appropriate `app.kubernetes.io/part-of` label?
- [ ] Does the workload have an appropriate `app.kubernetes.io/managed-by` label?
- [ ] Are owner, environment, and cost-center labels included where useful?
- [ ] Are labels applied to top-level resources?
- [ ] Are labels applied to the Pod template?
- [ ] Do manifests use Kubernetes API versions supported by the target cluster?
- [ ] Have deprecated API versions been checked?
- [ ] Have Helm charts been checked for deprecated APIs?
- [ ] Have live Helm releases been checked for deprecated APIs?
- [ ] Have tools such as Pluto, kube-no-trouble, or KubePug been used?
- [ ] Are stored Helm releases migrated before a cluster upgrade when necessary?

## 3. Your Security

### Runtime access controls

- [ ] Are Pod Security Standards enforced at the namespace level?
- [ ] Is the namespace configured with an appropriate Pod Security profile?
- [ ] Is `baseline` used during the initial migration phase where appropriate?
- [ ] Is `restricted` the target profile for production application namespaces?
- [ ] Are `warn`, `audit`, and `enforce` modes configured deliberately?
- [ ] Have warnings and audit findings been resolved before enabling enforcement?
- [ ] Does every workload have a dedicated ServiceAccount?
- [ ] Does each ServiceAccount have only the permissions it needs?
- [ ] Do workloads that do not use the Kubernetes API have no unnecessary RBAC permissions?
- [ ] Is the default ServiceAccount avoided for application workloads?
- [ ] Is automatic ServiceAccount token mounting disabled when unnecessary?
- [ ] Is `automountServiceAccountToken: false` configured where appropriate?
- [ ] Is network access restricted with `NetworkPolicy`?
- [ ] Is expected ingress traffic documented and restricted?
- [ ] Is expected egress traffic documented and restricted?
- [ ] Are default-deny policies used where appropriate?
- [ ] Is DNS traffic explicitly allowed when default-deny policies are used?
- [ ] Does the cluster’s CNI plugin enforce NetworkPolicy?
- [ ] Are NetworkPolicy selectors based on Pods, namespaces, or IP blocks?
- [ ] Are domain-name assumptions avoided in native Kubernetes NetworkPolicies?

### Supply chain and admission control

- [ ] Are all container images scanned before release?
- [ ] Are images scanned regularly after being pushed to the registry?
- [ ] Is there a defined CVE severity threshold for blocking releases?
- [ ] Are lower-severity vulnerabilities tracked through tickets?
- [ ] Is responsibility for resolving vulnerabilities assigned?
- [ ] Are images pulled only from trusted or approved registries?
- [ ] Are private registries or approved image mirrors used?
- [ ] Are registry restrictions enforced through admission policies?
- [ ] Are manifests validated before they reach the cluster?
- [ ] Are required labels and annotations enforced?
- [ ] Are image references restricted to approved registries?
- [ ] Are image digests required instead of mutable tags where appropriate?
- [ ] Are `runAsNonRoot`, `readOnlyRootFilesystem`, and resource requests enforced?
- [ ] Are risky fields such as `hostPath`, `hostNetwork`, and privileged containers restricted?
- [ ] Are `ValidatingAdmissionPolicy` resources used for object-local validation?
- [ ] Is CEL sufficient for the required admission rules?
- [ ] Is Kyverno used when YAML-based policy, mutation, generation, or image verification is needed?
- [ ] Is OPA Gatekeeper used when complex Rego policies or cross-platform policy reuse is needed?
- [ ] Are image signatures verified when supply-chain integrity is required?
- [ ] Are cross-resource or external-data checks handled by an appropriate policy engine?

## 4. Scaling

### Horizontal scaling

- [ ] Is the workload configured to run more than one replica where availability requires it?
- [ ] Is a Horizontal Pod Autoscaler defined where demand varies?
- [ ] Does the HPA use appropriate CPU or memory metrics?
- [ ] Are custom