# Kubernetes Howto

![2060 logo](https://raw.githubusercontent.com/2060-io/.github/44bf28569fec0251a9367a9f6911adfa18a01a7c/profile/assets/2060_logo.svg)

## Getting Started

In the [citizen-registry demos](../README.md), we provided demos based on the citizen-registry service.
In this document, we will learn how to deploy the same demos services in a Kubernetes cluster.

The demo deployment descriptors are these 2 directories: [registry-gaia](registry-gaia) and [registry-avatar](registry-avatar). Let's the [registry-avatar](registry-avatar).

```
$ docker login -u io2060 -p $DOCKER_HUB_TOKEN
$ docker build -f Dockerfile -t $IMAGE_DH:$IMAGE_TAG .
$ docker push $IMAGE_DH:$IMAGE_TAG

```


```
$ kubectl --kubeconfig=~/.kube/config apply -f /builds/${CI_PROJECT_PATH}/k8s/main/namespace.yml
$ kubectl --kubeconfig=~/.kube//config apply -f /builds/${CI_PROJECT_PATH}/k8s/main/deployment.yml
$ kubectl --kubeconfig=~/.kube//config apply -f /builds/${CI_PROJECT_PATH}/k8s/main/service.yml
$ kubectl --kubeconfig=~/.kube//config apply -f /builds/${CI_PROJECT_PATH}/k8s/main/ingress-ds.yml
$ kubectl --kubeconfig=~/.kube//config apply -f /builds/${CI_PROJECT_PATH}/k8s/main/ingress-public.yml
```