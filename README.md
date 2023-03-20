# agent_lambda
Lambda agent built as apart of a project workshop for Cloud Treinamentos LTDA.
## Monitoring system by group 2 for Oficina de Projetos from Cloud Treinamentos.

Build:
gradle clean build shadowJar

Upload:
aws s3 cp build/libs/agent_lambda-0.1-all.jar s3://g2c-zabbix-agent/

Go to console and update lambda from s3://g2c-zabbix-agent/agent_lambda-0.1-all.jar