package org.change.v2.cmd;

import java.lang.annotation.*;
import java.util.HashMap;
import java.util.Map;

public class EQParams {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Parm {
        String value();
    }
    @Parm("--integration-plugin")
    private String integrationPlugin;

    @Parm("--input-port-parm")
    private Map<String, String> inputPortParms = new HashMap<>();

    @Parm("--input-packet-plugin")
    private String inputPacketClass;
    @Parm("--input-packet-parm")
    private Map<String, String> inputPacketParms = new HashMap<>();

    @Parm("--output-port-parm")
    private Map<String, String> outputPortParms = new HashMap<>();

    @Parm("--output-packet-eq-plugin")
    private String packetEqClass;
    @Parm("--output-packet-eq-parm")
    private Map<String, String> packetEqParms = new HashMap<>();

    @Parm("--sieve-plugin")
    private String sieveClass;
    @Parm("--sieve-parm")
    private Map<String, String>  sieveParms = new HashMap<>();

    @Parm("--left-topo-plugin")
    private String leftTopoClass;
    @Parm("--left-topo-parm")
    private Map<String, String> leftTopoParms = new HashMap<>();

    @Parm("--right-topo-plugin")
    private String rightTopoClass;
    @Parm("--right-topo-parm")
    private Map<String, String> rightTopoParms = new HashMap<>();

    @Parm("--output-plugin")
    private String outputClass;
    @Parm("--output-parm")
    private Map<String, String> outputParms = new HashMap<>();

    public String getInputPacketClass() {
        return inputPacketClass;
    }

    public EQParams setInputPacketClass(String inputPacketClass) {
        this.inputPacketClass = inputPacketClass;
        return this;
    }

    public String getPacketEqClass() {
        return packetEqClass;
    }

    public EQParams setPacketEqClass(String packetEqClass) {
        this.packetEqClass = packetEqClass;
        return this;
    }

    public String getSieveClass() {
        return sieveClass;
    }

    public EQParams setSieveClass(String sieveClass) {
        this.sieveClass = sieveClass;
        return this;
    }

    public String getLeftTopoClass() {
        return leftTopoClass;
    }

    public EQParams setLeftTopoClass(String leftTopoClass) {
        this.leftTopoClass = leftTopoClass;
        return this;
    }

    public String getRightTopoClass() {
        return rightTopoClass;
    }

    public EQParams setRightTopoClass(String rightTopoClass) {
        this.rightTopoClass = rightTopoClass;
        return this;
    }

    public String getOutputClass() {
        return outputClass;
    }

    public EQParams setOutputClass(String outputClass) {
        this.outputClass = outputClass;
        return this;
    }

    public Map<String, String> getOutputParms() {
        return outputParms;
    }

    public Map<String, String> getInputPortParms() {
        return inputPortParms;
    }

    public Map<String, String> getInputPacketParms() {
        return inputPacketParms;
    }

    public Map<String, String> getOutputPortParms() {
        return outputPortParms;
    }

    public Map<String, String> getPacketEqParms() {
        return packetEqParms;
    }

    public Map<String, String> getSieveParms() {
        return sieveParms;
    }

    public Map<String, String> getLeftTopoParms() {
        return leftTopoParms;
    }

    public Map<String, String> getRightTopoParms() {
        return rightTopoParms;
    }

    public String getIntegrationPlugin() {
        return integrationPlugin;
    }

    public EQParams setIntegrationPlugin(String integrationPlugin) {
        this.integrationPlugin = integrationPlugin;
        return this;
    }

    @Override
    public String toString() {
        return "EQParams{" +
                " inputPortParms=" + inputPortParms +
                ", inputPacketClass='" + inputPacketClass + '\'' +
                ", inputPacketParms=" + inputPacketParms +
                ", outputPortParms=" + outputPortParms +
                ", packetEqClass='" + packetEqClass + '\'' +
                ", packetEqParms=" + packetEqParms +
                ", sieveClass='" + sieveClass + '\'' +
                ", sieveParms=" + sieveParms +
                ", leftTopoClass='" + leftTopoClass + '\'' +
                ", leftTopoParms=" + leftTopoParms +
                ", rightTopoClass='" + rightTopoClass + '\'' +
                ", rightTopoParms=" + rightTopoParms +
                ", outputClass='" + outputClass + '\'' +
                ", outputParms=" + outputParms +
                '}';
    }
}
