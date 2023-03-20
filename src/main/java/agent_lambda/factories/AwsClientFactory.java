package agent_lambda.factories;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.rds.RdsClient;

@Factory
public class AwsClientFactory {
    ApacheHttpClient.Builder apacheHttpClient = ApacheHttpClient.builder();

    @Singleton
    Ec2Client ec2Client() {
        return Ec2Client.builder()
            .httpClientBuilder(apacheHttpClient)
            .build();
    }

    @Singleton
    RdsClient rdsClient() {
        return RdsClient.builder()
            .httpClientBuilder(apacheHttpClient)
            .build();
    }

    @Singleton
    CloudWatchClient cloudWatchClient() {
        return CloudWatchClient.builder()
            .httpClientBuilder(ApacheHttpClient.builder())
            .build();
    }

}