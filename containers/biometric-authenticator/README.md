# Auth

A example template of a conversational service (chatbot) that requests identity document presentation, and then perform a remote biometric face verification with liveness detection, by comparing the credential embed subject photo with handset user face.

## How it works

- A User scans the QR code of the service. Service will request presentation of an Identity credential.
- Service will receive the credential, extract embed photo
- User will 