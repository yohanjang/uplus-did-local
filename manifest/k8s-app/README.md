# README

## [ 로컬, 개발 환경 ]  ArgoCD 에 각 Application 등록 yaml

- argo-cd

  ```yaml
  apiVersion: argoproj.io/v1alpha1
  kind: Application
  metadata:
    name: argo-cd
  spec:
    destination:
      name: ''
      namespace: k8s-app
      server: 'https://kubernetes.default.svc'
    source:
      path: manifest/k8s-app/argo-cd
      repoURL: 'http://18.220.186.237:9000/lgu-did/infra.git'
      targetRevision: HEAD
      helm:
        valueFiles:
          - values-loc.yaml
    sources: []
    project: default
  ```

<br>

- metric-server

  ```yaml
  apiVersion: argoproj.io/v1alpha1
  kind: Application
  metadata:
    name: metric-server
  spec:
    destination:
      name: ''
      namespace: k8s-app
      server: 'https://kubernetes.default.svc'
    source:
      path: manifest/k8s-app/metric-server
      repoURL: 'http://18.220.186.237:9000/lgu-did/infra.git'
      targetRevision: HEAD
      helm:
        valueFiles:
          - values-loc.yaml
    sources: []
    project: default
  ```

<br>

- rancher-local-path-provisioner

  ```yaml
  apiVersion: argoproj.io/v1alpha1
  kind: Application
  metadata:
    name: rancher-local-path-provisioner
  spec:
    destination:
      name: ''
      namespace: k8s-app
      server: 'https://kubernetes.default.svc'
    source:
      path: manifest/k8s-app/rancher-local-path-provisioner
      repoURL: 'http://18.220.186.237:9000/lgu-did/infra.git'
      targetRevision: HEAD
    sources: []
    project: default
  ```

- bitnami-redis

  - solution-redis

    ```yaml
    apiVersion: argoproj.io/v1alpha1
    kind: Application
    metadata:
      name: solution-redis
    spec:
      destination:
        name: ''
        namespace: dmicma
        server: 'https://kubernetes.default.svc'
      source:
        path: manifest/k8s-app/bitnami-redis
        repoURL: 'http://18.220.186.237:9000/lgu-did/infra.git'
        targetRevision: HEAD
        helm:
          valueFiles:
            - values-solution-dev.yaml
      sources: []
      project: default
    ```

  - service-redis

    ```yaml
    apiVersion: argoproj.io/v1alpha1
    kind: Application
    metadata:
      name: service-redis
    spec:
      destination:
        name: ''
        namespace: dmicma
        server: 'https://kubernetes.default.svc'
      source:
        path: manifest/k8s-app/bitnami-redis
        repoURL: 'http://18.220.186.237:9000/lgu-did/infra.git'
        targetRevision: HEAD
        helm:
          valueFiles:
            - values-service-dev.yaml
      sources: []
      project: default
    ```

- bitnami-postgresql

  - solution-postgresql

    ```yaml
    apiVersion: argoproj.io/v1alpha1
    kind: Application
    metadata:
      name: solution-postgresql
    spec:
      destination:
        name: ''
        namespace: dmicma
        server: 'https://kubernetes.default.svc'
      source:
        path: manifest/k8s-app/bitnami-postgresql
        repoURL: 'http://18.220.186.237:9000/lgu-did/infra.git'
        targetRevision: HEAD
        helm:
          valueFiles:
            - values-solution-dev.yaml
      sources: []
      project: default
    ```

  - service-postgresql

    ```yaml
    apiVersion: argoproj.io/v1alpha1
    kind: Application
    metadata:
      name: service-postgresql
    spec:
      destination:
        name: ''
        namespace: dmicma
        server: 'https://kubernetes.default.svc'
      source:
        path: manifest/k8s-app/bitnami-postgresql
        repoURL: 'http://18.220.186.237:9000/lgu-did/infra.git'
        targetRevision: HEAD
        helm:
          valueFiles:
            - values-service-dev.yaml
      sources: []
      project: default
    ```
