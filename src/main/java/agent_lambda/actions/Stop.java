package agent_lambda.actions;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.StopDbInstanceRequest;

import java.util.ArrayList;

@Singleton
public class Stop {
    private static final Logger log = LoggerFactory.getLogger(Stop.class);
    @Inject
    Ec2Client ec2Client;
    @Inject
    RdsClient rdsClient;

    public void stop() {
        var instancesRequest = new ArrayList<String>();
        var dbInstancesRequest = new ArrayList<String>();
        var instanceResp = getDescribeInstances();
        if (instanceResp.hasReservations()) {
            var reservations = instanceResp.reservations();
            reservations.forEach(reservation -> {
                if (reservation.hasInstances()) {
                    var instances = reservation.instances();
                    instances.forEach(instance -> {
                        if(instance.state().nameAsString().equals("running")) {
                            instancesRequest.add(instance.instanceId());
                        }
                    });
                }
            });
        }
        var dbInstanceResp = getDescribeRdsInstances();
        if (dbInstanceResp.hasDbInstances()) {
            dbInstanceResp.dbInstances().forEach(dbInstance -> {
                if(dbInstance.dbInstanceStatus().equals("available")) {
                    dbInstancesRequest.add(dbInstance.dbInstanceIdentifier());
                }
            });
        }
        if (!instancesRequest.isEmpty()) {
            ec2Client.stopInstances(StopInstancesRequest
                .builder()
                .instanceIds(instancesRequest.toArray(new String[]{}))
                .build()
            );
        }

        if (!dbInstancesRequest.isEmpty()) {
            dbInstancesRequest.forEach(instance -> rdsClient.stopDBInstance(
                StopDbInstanceRequest
                    .builder()
                    .dbInstanceIdentifier(instance)
                    .build()
            ));
        }

    }

    private DescribeInstancesResponse getDescribeInstances() {
        var describeInstancesRequest = DescribeInstancesRequest
            .builder()
            .build();

        return ec2Client.describeInstances(describeInstancesRequest);
    }

    private DescribeDbInstancesResponse getDescribeRdsInstances() {
        var describeDbInstancesRequest = DescribeDbInstancesRequest
            .builder()
            .build();

        return rdsClient.describeDBInstances(describeDbInstancesRequest);
    }

}
