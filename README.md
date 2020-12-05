# discount-monitoring

## Description

The purpose of this application is the discount monitoring. There is very simply idea:
You give the application link to the item, you want to by, application monitors price changes and when price go up or
down, application will send you notification through Telegram BOT API.

## Building application

You can build application with one simple command:

```
gradle build ktlintFormat
```

## Deployment

We use Docker container for deployment. To build a container, you can execute another one simple command:

```
gradle jibDockerBuild --image=<MY IMAGE>
```
