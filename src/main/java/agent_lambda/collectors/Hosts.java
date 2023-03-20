package agent_lambda.collectors;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.github.hengyunabc.zabbix.sender.ZabbixSender;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.lang.System.err;

@Singleton
public class Hosts {
    private static final Logger log = LoggerFactory.getLogger(Hosts.class);
    private static final int CONNECT_TIMEOUT = 1000;
    private final Ec2Client ec2Client;

    public Hosts(Ec2Client ec2Client) {
        this.ec2Client = ec2Client;
    }

    public List<HostInfo> getHosts() {
        var hostsInfo = new ArrayList<HostInfo>();
        var server = getDescribeInstances();
        if (server.hasReservations()) {
            server.reservations().forEach(reservation -> {
                if (reservation.hasInstances()) {
                    reservation.instances().forEach(instance -> {
                        if (instance.state().nameAsString().equals("running")) {
                            hostsInfo.add(new HostInfo(instance.publicIpAddress(), instance.instanceId()));
                        }
                    });
                }
            });
        }

        return hostsInfo;
    }

    @Retryable
    public ZabbixSender zabbixSender(JSch jsch, List<Hosts.HostInfo> hostInfos) {
        ZabbixSender zabbixSender = null;
        try {
            for (var host : hostInfos) {
                var session = getSession(jsch, "ubuntu", host.ip);
                var lines = getChannel(session, "sudo zabbix_server -R ha_status | grep active");
                if (!lines.isEmpty()) {
                    if (lines.get(0).equalsIgnoreCase("Runtime commands can be executed only in active mode")) {
                        session.disconnect();
                        continue;
                    }
                    zabbixSender = new ZabbixSender(host.ip, 10051);
                    session.disconnect();
                    break;
                }
            }
        } catch (JSchException e) {
            log.error("Unable to establish the connection", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("An error occurred", e);
            throw e;
        }

        return zabbixSender;
    }

    public List<String> getChannel(Session session, String command) throws JSchException {
        var lines = new LinkedList<String>();
        var channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(err);
        channel.connect(CONNECT_TIMEOUT);

        try {
            var br = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            channel.disconnect();

            return lines;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public Session getSession(JSch jsch, String user, String host) throws JSchException {
        var session = jsch.getSession(user, host, 22);
        var config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(CONNECT_TIMEOUT);

        return session;
    }

    public JSch getjSch() {
        var jsch = new JSch();

        try {
            jsch.addIdentity(this.getClass().getResource("/G2C-Keypair.pem").getPath());
        } catch (JSchException e) {
            var msg = "Error opening the key.";
            log.error(msg);
            throw new RuntimeException(msg);
        }

        return jsch;
    }

    private DescribeInstancesResponse getDescribeInstances() {
        var describeInstancesRequest = DescribeInstancesRequest
            .builder()
            //.filters(Filter.builder().name("tag:Name").values(name).build())
            .build();

        return ec2Client.describeInstances(describeInstancesRequest);
    }

    static class HostInfo {
        String ip;
        String id;
        String free;
        String used;
        String size;
        String cpu;

        public HostInfo(
            String ip,
            String id
        ) {
            this.ip = ip;
            this.id = id;
        }

    }
}
