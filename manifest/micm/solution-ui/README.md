# README

## ArgoCD 에 solution-ui 등록 yaml

- 로컬, 개발 환경

  ```yaml
  apiVersion: argoproj.io/v1alpha1
  kind: Application
  metadata:
    name: solution-ui
  spec:
    destination:
      name: ''
      namespace: dmicma
      server: 'https://kubernetes.default.svc'
    source:
      path: manifest/micm/solution-ui/overlays/dev
      repoURL: 'http://18.220.186.237:9000/lgu-did/infra.git'
      targetRevision: HEAD
    sources: []
    project: micm-dev
  ```
