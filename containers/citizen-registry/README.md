# Government Citizen Registry - Verifiable ID Card

## Purpose of the service

A demo module for building DIDcomm conversational services (chatbots) to issue Verifiable ID Cards to citizen by verifying their face against photo stored in a database.

In these demos, and because we are not connected to a true government database of citizens, you use the conversational service to create a fake digital Identity that you protect with your face biometrics. A corresponding Verifiable Credential of your Identity is issued to you.

Then, as soon as you've got you Verifiable Credential, you can present your credential to identify yourself and access passwordless services such as the [biometric-authenticator]() demos.

If you loose you cellphone, then can restore your Identity by simply re-connecting to the Registry service, verifying your face, and recover your Verifiable Credential.

## Try the demo(s)

Several use cases of Citizen Registries have been deployed for your convenience. Just download the Hologram Messenger in the App Store or Google play and scan the QR code of the service you would like to try:

- GaiaID Identity Registry: a government-like registry service. Test URL: [https://gaiaid.io](https://gaiaid.io)
- Colombia Registraduría (spanish chatbot): a demo registry service for the colombian citizen registry. Test URL: [https://colombia.demos.m.2060.io](https://colombia.demos.m.2060.io)
- AvatarID Registry: create your Avatar and protect it with your face biometrics. Test URL: [https://avatar.demos.m.2060.io](https://avatar.demos.m.2060.io)

All these demos are just an instance of the same citizen-registry service with customized settings. You can easily create your own demo for your country by jumping to the [kubernetes howto]() documentation.

## Recover your identity

Lost your device? Flash again the service's QR code and restore your Identity.

## Try Identity Registry

Flash QR with 2060 mobile App.

Demo services are provided with Vision Service module (face capture / recognition).


- [Gaia Registry](https://gaia.demos.m.2060.io/qr)

- [AvatarID](https://avatar.demos.m.2060.io/qr)

You can easily create your own demo by forking one of the demo projects registry-gaia, registry-avatar


## Government Citizen Registry architecture

```plantuml:md-sample-sequence
@startuml
actor Foo123
boundary Foo2
control Foo3
entity Foo4
database Foo5
collections Foo6
Foo1 -> Foo2 : To boundary
Foo1 -> Foo3 : To control
Foo1 -> Foo4 : To entity
Foo1 -> Foo5 : To database
Foo1 -> Foo6 : To collections
@enduml
```

![](./md-sample-sequence.svg)


```plantuml:arch
@startuml

actor "End-user" as Enduser

[2060 Cloud Agent] as CA

rectangle "2060 Mobile App" as App {
  [2060 Mobile Agent] as MA
}


rectangle "Government Citizen Registry" {
    [2060 Service Agent] as VS
    [2060 Data Store] as DS
    [Postgres] as PS
    [Apache Artemis] as AA
    [Government Citizen Registry Backend] as GAIA
    [Vision Services] as VISION
}

MA --> VS
App --> VISION
VISION --> GAIA
VISION --> DS
App <--> Enduser
VS <--> GAIA
VS --> CA
MA <-- CA
GAIA --> PS
GAIA --> AA

@enduml
```
![](./arch.svg)

## How to use API for Vision Service integration

Use the API if you want to integrate your own Vision Service.

### Face Recognition

#### New Identity

- Scan https://registry.dev.gaiaid.io/qr with the 2060 App
- Use the contextual menu and select "create a new identity"
- When bot requests a protection method, choose Face Recognition
- You will receive a URL with a token.

Keep track of this token. You'll need it later.

- Perform capture of user's face and use the datastore API https://d.registry.dev.gaiaid.io/q/swagger-ui/ to save the captured picture(s).
See datastore API' documentation on gitlab: https://gitlab.mobiera.com/2060/2060-data-store/-/tree/dev?ref_type=heads 
Save UUID of created media.

- call /link method of gaia's vision service API https://q.registry.dev.gaiaid.io/q/swagger-ui/ to link the created medias with the corresponding identity by using the token.
- call /success method to finish.
- in case of a problem (cannot capture, slow link,...), call /failure.
- done.

Use FACE for the type field of the API.


#### Verify user


- Scan https://registry.dev.gaiaid.io/qr with the 2060 App
- Use the contextual menu and select "restore an identity"
- When bot requests a protection method, choose Face Recognition
- You will receive a URL with a token.
- call /list method of gaia's vision service API https://q.registry.dev.gaiaid.io/q/swagger-ui/ to get the list of existing pictures of the identity represented by this token.
- perform face recognition.
- if face recognition is successful, call /success service. You can create new media links with /link at any time (for ex for each successful recognition).
- if face recognition does not work, call /failure

Use FACE for the type field of the API.



## Running the application in dev mode

Use the docker-compose example file in /docker to run artemis, postgres and the 2060-service-agent.

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

