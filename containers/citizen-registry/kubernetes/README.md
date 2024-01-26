# Kubernetes Howto

![2060 logo](https://raw.githubusercontent.com/2060-io/.github/44bf28569fec0251a9367a9f6911adfa18a01a7c/profile/assets/2060_logo.svg)

## Getting Started

In the [citizen-registry demos](../README.md), we provided demos based on the citizen-registry service.
In this document, we will learn how to deploy the same demos services in a Kubernetes cluster.

These 2 directories [registry-gaia](registry-gaia) and [registry-avatar](registry-avatar) contains:

- a Dockerfile to build a simple nginx-based image to expose a simple front-end, including the service's icon. See [github workflows](/.github/workflows) for build info.
- a k8s directory with kubernetes deployment files we are using for deploying the demos. To deploy the demos, you need a configured nginx ingress on your cluster. Our demos are deployed in ovh.com, but should work out of the box anywhere.

Just clone one of these directories, customize it and you're done!

## Customize configuration

All files in k8s directory should be customized.

### 2060-service-agent container


## Deploy

```
$ kubectl --kubeconfig=~/.kube/config apply -f k8s/namespace.yml
$ kubectl --kubeconfig=~/.kube/config apply -f k8s/deployment.yml
$ kubectl --kubeconfig=~/.kube/config apply -f k8s/service.yml
$ kubectl --kubeconfig=~/.kube/config apply -f k8s/ingress-ds.yml
$ kubectl --kubeconfig=~/.kube/config apply -f k8s/ingress-public.yml
```





