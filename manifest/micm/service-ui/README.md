# README

## ArgoCD 에 service-ui 등록 yaml

- 로컬, 개발 환경

  ```yaml
  apiVersion: argoproj.io/v1alpha1
  kind: Application
  metadata:
    name: service-ui
  spec:
    destination:
      name: ''
      namespace: dmicma
      server: 'https://kubernetes.default.svc'
    source:
      path: manifest/micm/service-ui/overlays/dev
      repoURL: 'http://18.220.186.237:9000/lgu-did/infra.git'
      targetRevision: HEAD
    sources: []
    project: micm-dev
  ```
